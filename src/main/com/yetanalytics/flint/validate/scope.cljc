(ns com.yetanalytics.flint.validate.scope
  (:require [clojure.zip :as zip]))

(defn- get-kv
  "Given `coll` of `[:keyword value]` pairs, return the pair
   with keyword `k`."
  [coll k]
  (some #(when (-> % first (= k)) %) coll))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Computing variable scopes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti get-scope-vars
  "Return a coll of all variables in the scope of the AST branch."
  (fn [x] (if (and (vector? x) (= 2 (count x)) (keyword? (first x)))
            (first x)
            :default)))

(defmethod get-scope-vars :default [_] nil)

(defmethod get-scope-vars :ax/var [[_ v]] [v])

(defmethod get-scope-vars :expr/as-var [[_ [_expr v]]]
  (get-scope-vars v))

;; SELECT in-scope vars

(defmethod get-scope-vars :select/expr-as-var [[_ [_expr v]]]
  (get-scope-vars v))

;; WHERE in-scope vars

(defmethod get-scope-vars :where [[_ vs]]
  (get-scope-vars vs))

;; Basic Graph Pattern

(defmethod get-scope-vars :triple/vec [[_ spo]]
  (mapcat get-scope-vars spo))

(defmethod get-scope-vars :triple/o [[_ o]]
  (mapcat get-scope-vars o))

(defmethod get-scope-vars :triple/po [[_ po]]
  (reduce-kv (fn [acc p o] (apply concat
                                  acc
                                  (get-scope-vars p)
                                  (map get-scope-vars o)))
             []
             po))

(defmethod get-scope-vars :triple/spo [[_ spo]]
  (reduce-kv (fn [acc s po] (apply concat
                                   acc
                                   (get-scope-vars s)
                                   (map get-scope-vars po)))
             []
             spo))

(defmethod get-scope-vars :triple/nform [[_ nform]]
  (get-scope-vars nform))

;; Path

(defmethod get-scope-vars :triple/path [[_ p]]
  (get-scope-vars p))

(defmethod get-scope-vars :path/branch [[_ [_op [_k paths]]]]
  (mapcat get-scope-vars paths))

(defmethod get-scope-vars :path/terminal [[_ v]]
  (get-scope-vars v))

;; Group

(defmethod get-scope-vars :where/recurse [[_ vs]]
  (get-scope-vars vs))

(defmethod get-scope-vars :select/expr-as-var [[_ ev]]
  (get-scope-vars ev))

(defmethod get-scope-vars :where-sub/select [[_ s]]
  (let [[_ select] (get-kv s :select)
        where      (get-kv s :where)]
    (case (first select)
      :ax/wildcard
      (get-scope-vars where)
      :select/var-or-exprs
      (mapcat get-scope-vars (second select)))))

(defmethod get-scope-vars :where-sub/where [[_ vs]]
  (mapcat get-scope-vars vs))

(defmethod get-scope-vars :where-sub/empty [_] [])

;; WHERE modifiers

(defmethod get-scope-vars :where/union [[_ vs]]
  (mapcat get-scope-vars vs))

(defmethod get-scope-vars :where/optional [[_ vs]]
  (get-scope-vars vs))

(defmethod get-scope-vars :where/graph [[_ [term vs]]]
  (concat (get-scope-vars term) (get-scope-vars vs)))

(defmethod get-scope-vars :where/service [[_ [term vs]]]
  (concat (get-scope-vars term) (get-scope-vars vs)))

(defmethod get-scope-vars :where/service-silent [[_ [term vs]]]
  (concat (get-scope-vars term) (get-scope-vars vs)))

(defmethod get-scope-vars :where/bind [[_ vs]]
  (get-scope-vars vs))

(defmethod get-scope-vars :where/values [[_ [_ values]]]
  (mapcat get-scope-vars (first values)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Validation on AST zipper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- scope-err-map
  [var scope-vars zip-loc k]
  {:variable   var
   :scope-vars scope-vars
   :path       (conj (->> zip-loc zip/path (mapv first)) k)})

(defn- get-bind-var
  "Starting at an `:expr/as-var` node, get the `var` in `expr AS var`."
  [ast-node]
  (-> ast-node ; [:expr/as-var [expr var]]
      second   ; [expr var]
      second   ; [:ax/var ?var]
      second))

(defn- validate-bind
  "Validate `BIND (expr AS var)`"
  [bind loc]
  (let [bind-var   (get-bind-var bind)
        prev-elems (zip/lefts loc)
        scope      (set (mapcat get-scope-vars prev-elems))]
    (when (contains? scope bind-var)
      (scope-err-map bind-var scope loc :where/bind))))

(defn- validate-select
  "Validate `SELECT ... (expr AS var) ..."
  [select-clause loc]
  (let [bind-var (get-bind-var select-clause)
        prev-elems (zip/lefts loc)
        sel-query (->> loc
                       zip/up ; :select/var-or-exprs
                       zip/up ; :select
                       zip/up ; :query/select or :where-sub/select
                       zip/node)
        where (get-kv (second sel-query) :where)
        where-vars (get-scope-vars (second where))
        prev-vars (mapcat get-scope-vars prev-elems)
        scope (set (concat where-vars prev-vars))]
    (when (contains? scope bind-var)
      (scope-err-map bind-var scope loc :select/expr-as-var))))

(defn- validate-node-locs
  [validation-fn node-locs]
  (mapcat (fn [[node locs]]
            (->> locs
                 (map (partial validation-fn node))
                 (filter some?)))
          node-locs))

(defn validate-scoped-vars
  "Given `node-m` a map from nodes to zipper locs, check that each var in
   a `expr AS var` node does not already exist in scope. If invalid, return
   a vector of error maps; otherwise return `nil`."
  [node-m]
  (let [binds          (:where/bind node-m)
        select-clauses (:select/expr-as-var node-m)]
    (some->> (concat (validate-node-locs validate-bind binds)
                     (validate-node-locs validate-select select-clauses))
             not-empty
             vec)))

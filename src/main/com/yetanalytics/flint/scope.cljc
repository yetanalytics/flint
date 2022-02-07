(ns com.yetanalytics.flint.scope
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
;; AST Traversal
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- ast-branch?
  [x]
  (and (vector? x)
       (= 2 (count x))
       (keyword? (first x))
       (not= "ax" (namespace (first x)))))

(defn- ast-zipper
  "Create a zipper out of the AST, where each AST node `[:keyword children]`
   is treated as a zipper branch."
  [ast]
  (zip/zipper ast-branch?
              (fn [[_ children]]
                (if (or (ast-branch? children)
                        (not (coll? children)))
                  [children]
                  children))
              identity
              ast))

(defn- scope-error-map
  [var scope-vars zip-loc k]
  {:variable   var
   :scope-vars scope-vars
   :path       (conj (->> zip-loc
                          zip/path
                          (filter #(-> % first keyword?))
                          (mapv #(-> % first)))
                     k)})

(defn- get-bind-var
  [ast-node]
  ;; [:where/bind [:expr/as-var ...]] OR
  ;; [:select/expr-as-var [:expr/as-var ...]]
  (-> ast-node
      second ; [:expr/as-var [expr var]]
      second ; [expr var]
      second ; [:ax/var ?var]
      second))

(defn validate-scoped-vars
  [ast]
  (loop [loc  (ast-zipper ast)
         errs []]
    (if-not (zip/end? loc)
      (let [ast-node (zip/node loc)]
        (cond
          ;; BIND (expr AS var)
          (and (zip/branch? loc)
               (= :where/bind (first ast-node)))
          (let [bind-var   (get-bind-var ast-node)
                prev-elems (zip/lefts loc)
                scope      (set (mapcat get-scope-vars prev-elems))]
            (if (contains? scope bind-var)
              (recur (zip/next loc)
                     (conj errs (scope-error-map bind-var
                                                 scope
                                                 loc
                                                 (first ast-node))))
              (recur (zip/next loc)
                     errs)))
          ;; SELECT ... (expr AS var) ...
          (and (zip/branch? loc)
               (= :select/expr-as-var (first ast-node)))
          (let [bind-var   (get-bind-var ast-node)
                prev-elems (zip/lefts loc)
                sel-query  (->> loc
                                zip/up ; :select/var-or-exprs
                                zip/up ; :select
                                zip/up ; :query/select or :where-sub/select
                                zip/node)
                where      (get-kv (second sel-query) :where)
                where-vars (get-scope-vars (second where))
                prev-vars  (mapcat get-scope-vars prev-elems)
                scope      (set (concat where-vars prev-vars))]
            (if (contains? scope bind-var)
              (recur (zip/next loc)
                     (conj errs (scope-error-map bind-var
                                                 scope
                                                 loc
                                                 (first ast-node))))
              (recur (zip/next loc)
                     errs)))
          :else
          (recur (zip/next loc) errs)))
      (not-empty errs))))

(ns com.yetanalytics.flint.scope
  (:require [clojure.set :as cset]
            [clojure.zip :as zip]))

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

(defmethod get-scope-vars :expr/expr-as-var [[_ [_expr v]]]
  (get-scope-vars v))

;; SELECT in-scope vars

(defmethod get-scope-vars :select/expr-as-var [[_ [_expr v]]]
  (get-scope-vars v))

;; WHERE in-scope vars

(defmethod get-scope-vars :where [[_ vs]]
  (get-scope-vars vs))

;; Basic Graph Pattern

(defmethod get-scope-vars :triple/vec [[_ [s p o]]]
  [(second s) (second p) (second o)])

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

(defmethod get-scope-vars :path/branch [[_ [_op [_k paths]]]]
  (mapcat get-scope-vars paths))

(defmethod get-scope-vars :path/terminal [[_ v]]
  (get-scope-vars v))

;; Group

(defmethod get-scope-vars :where/recurse [[_ vs]]
  (get-scope-vars vs))

(defmethod get-scope-vars :select/expr-as-var [[_ ev]]
  (get-scope-vars ev))

(defmethod get-scope-vars :sub-where/select [[_ s]]
  (let [select (get-kv s :select)
        where  (get-kv s :where)]
    (case (first select)
      :ax/wildcard
      (get-scope-vars where)
      :select/var-or-exprs
      (mapcat get-scope-vars select))))

(defmethod get-scope-vars :sub-where/where [[_ vs]]
  (mapcat get-scope-vars vs))

(defmethod get-scope-vars :sub-where/empty [_] [])

;; WHERE modifiers

(defmethod get-scope-vars :where/union [[_ vs]]
  (mapcat get-scope-vars vs))

(defmethod get-scope-vars :where/optional [[_ vs]]
  (get-scope-vars vs))

(defmethod get-scope-vars :sub-where/graph [[_ [term vs]]]
  (concat (get-scope-vars term) (get-scope-vars vs)))

(defmethod get-scope-vars :where/service [[_ [term vs]]]
  (concat (get-scope-vars term) (get-scope-vars vs)))

(defmethod get-scope-vars :where/service-silent [[_ [term vs]]]
  (concat (get-scope-vars term) (get-scope-vars vs)))

(defmethod get-scope-vars :where/bind [[_ vs]]
  (get-scope-vars vs))

(defmethod get-scope-vars :values [[_ vmap]]
  (->> vmap first (map get-scope-vars)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; AST Traversal
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- ast-zipper
  "Create a zipper out of the AST, where each AST node `[:keyword children]`
   is treated as a zipper branch."
  [ast]
  (zip/zipper (fn [x]
                (and (vector? x)
                     (= 2 (count x))
                     (keyword? (first x))
                     (not= "ax" (namespace (first x)))))
              (fn [x]
                (if (-> x second first keyword?)
                  [(-> x second)]
                  (-> x second)))
              identity
              ast))

(defn validate-scoped-vars
  [ast]
  (loop [loc  (ast-zipper ast)
         errs []]
    (if-not (zip/end? loc)
      (let [ast-node (zip/node loc)]
        (case (first ast-node)
          ;; BIND (expr AS var)
          :where/bind
          (let [bind-var   (-> ast-node second second second)
                prev-elems (zip/lefts loc)
                scope      (->> prev-elems
                                (map get-scope-vars prev-elems)
                                (apply cset/union))]
            (if (contains? scope bind-var)
              (recur (zip/next loc)
                     (conj errs {:var bind-var}))
              (recur (zip/next loc)
                     errs)))
          ;; SELECT ... (expr AS var) ...
          :select/expr-as-var
          (let [bind-var   (-> ast-node second second second)
                prev-elems (zip/lefts loc)
                sel-query  (->> loc
                                zip/up ; :select/var-or-exprs
                                zip/up ; :select
                                zip/up ; :query/select or :sub-where/select
                                zip/node)
                where      (get-kv sel-query :where)
                where-vars (get-scope-vars (second where))
                prev-vars  (map get-scope-vars prev-elems)
                scope      (apply cset/union (concat where-vars prev-vars))]
            (if (contains? scope bind-var)
              (recur (zip/next loc)
                     (conj errs {:var bind-var}))
              (recur (zip/next loc)
                     errs)))
          ;; else
          (zip/next loc)))
      errs)))

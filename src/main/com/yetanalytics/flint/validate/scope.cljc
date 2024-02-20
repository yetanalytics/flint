(ns com.yetanalytics.flint.validate.scope
  (:require [clojure.zip :as zip]
            [com.yetanalytics.flint.validate.variable :as vv]
            [com.yetanalytics.flint.validate.util     :as vu]
            [com.yetanalytics.flint.util              :as u]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Validation on AST zipper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- in-scope-err-map
  [var scope-vars zip-loc k]
  {:kind       ::var-in-scope
   :variable   var
   :scope-vars scope-vars
   :path       (conj (vu/zip-path zip-loc) k)})

(defn- not-in-scope-err-map
  [vars scope-vars zip-loc k]
  {:kind       ::var-not-in-scope
   :variables  vars
   :scope-vars scope-vars
   :path       (conj (vu/zip-path zip-loc) k)})

(defn- validate-bind
  "Validate `BIND (expr AS var)` in WHERE clauses."
  [[_expr-as-var-k [_ v-kv]] loc]
  (let [[_ bind-var] v-kv
        prev-elems   (-> loc
                         zip/up ; :where/special
                         zip/lefts)
        scope        (set (mapcat vv/get-scope-vars prev-elems))]
    (when (contains? scope bind-var)
      (in-scope-err-map bind-var scope loc :where/bind))))

(defn- validate-select
  "Validate `SELECT ... (expr AS var) ..."
  [[_expr-as-var-k [expr v-kv]] loc]
  (let [[_ bind-var] v-kv
        expr-vars    (vv/get-expr-vars expr)
        prev-elems   (zip/lefts loc)
        sel-query    (->> loc
                          zip/up ; :select/var-or-exprs
                          zip/up ; :select
                          zip/up ; :query/select or :where-sub/select
                          zip/node)
        where        (-> sel-query second (u/get-kv-pair :where))
        ?group-by    (-> sel-query second (u/get-kv-pair :group-by))
        where-vars   (-> where second vv/get-scope-vars)
        group-vars   (some-> ?group-by vv/group-by-projected-vars)
        prev-vars    (mapcat vv/get-scope-vars prev-elems)
        scope        (set (concat where-vars group-vars prev-vars))]
    (if-some [bad-expr-vars (not-empty (filter #(not (scope %)) expr-vars))]
      (not-in-scope-err-map bad-expr-vars scope loc :select/expr-as-var)
      (when (contains? scope bind-var)
        (in-scope-err-map bind-var scope loc :select/expr-as-var)))))

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

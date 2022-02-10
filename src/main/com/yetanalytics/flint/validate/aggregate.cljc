(ns com.yetanalytics.flint.validate.aggregate
  (:require [com.yetanalytics.flint.spec.expr :as es]))

;; In a query level which uses aggregates, only expressions consisting of
;; aggregates and constants may be projected, with one exception.
;; When GROUP BY is given with one or more simple expressions consisting of
;; just a variable, those variables may be projected from the level.

;; GOOD:
;;   SELECT (2 AS ?two) WHER { ?x ?y ?z }
;;   SELECT (SUM(?x) AS ?sum) WHERE { ?x ?y ?z }
;;   SELECT ?x WHERE { ?x ?y ?z } GROUP BY ?x

;; BAD:
;;   SELECT (AVG(?x)) AS ?avg) ((?x + ?y) AS ?sum) WHERE { ?x ?y ?z }
;;   SELECT ?y ?z WHERE { ?x ?y ?z } GROUP BY ?x

(defmulti group-by-projected-vars (fn [[k _]] k))

(defmethod group-by-projected-vars :group-by [[_ group-by-coll]]
  (->> group-by-coll
       (map group-by-projected-vars)
       (filter some?)))

(defmethod group-by-projected-vars :mod/expr [_] nil)

(defmethod group-by-projected-vars :ax/var [[_ v]] v)

(defmethod group-by-projected-vars :mod/expr-as-var [[_ expr-as-var]]
  (-> expr-as-var second second second))

(defmulti invalid-agg-expr-vars (fn [_ [k _]] k))

(defmethod invalid-agg-expr-vars :default [_] [])

(defmethod invalid-agg-expr-vars :expr/branch [valid-vars [_ [[_ op] [_ args]]]]
  (if (or (es/aggregate-ops op)
          (not (symbol? op)))
    []
    (mapcat (partial invalid-agg-expr-vars valid-vars) args)))

(defmethod invalid-agg-expr-vars :expr/terminal [valid-vars [_ x]]
  (invalid-agg-expr-vars valid-vars x))

(defmethod invalid-agg-expr-vars :ax/var [valid-vars [_ v]]
  (if-not (valid-vars v) [v] []))

(defn- validate-agg-select-clause
  [group-by-vars sel-clause]
  (let [err-or-valid
        (reduce (fn [valid-vars [k x]]
                  (case k
                    :ax/var
                    (if (valid-vars x)
                      valid-vars
                      (reduced {:errors [x]}))
                    :select/expr-as-var
                    (let [[_expr-as-var-kw [expr [_v-kw v]]] x]
                      (if-some [bad-evars (not-empty (invalid-agg-expr-vars valid-vars expr))]
                        (reduced {:errors bad-evars})
                        (conj valid-vars v)))))
                group-by-vars
                sel-clause)]
    (if-some [bad-vars (:errors err-or-valid)]
      bad-vars
      nil)))

(defn validate-agg-select
  [select loc]
  (let [select-cls  (some #(when (= :select (first %)) (second %)) (second select))
        ?group-by   (some #(when (= :group-by (first %)) (second %)) (second select))
        group-by-vs (if ?group-by
                      (->> ?group-by
                           (map group-by-projected-vars)
                           (filter some?)
                           set)
                      #{})]
    (if (= :ax/wildcard (first select-cls))
      {:kind ::wildcard-group-by
       :loc  loc}
      (when-some [bad-vars (validate-agg-select-clause group-by-vs (second select-cls))]
        {:kind      ::invalid-aggregate-var
         :variables bad-vars
         :loc       loc}))))

(defn validate-agg-selects
  [node-m]
  (->> (:agg/select node-m)
       (map (fn [[sel loc]] (validate-agg-select sel loc)))
       (filter some?)))

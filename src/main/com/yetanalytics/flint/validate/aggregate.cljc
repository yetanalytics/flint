(ns com.yetanalytics.flint.validate.aggregate
  (:require [com.yetanalytics.flint.spec.expr :as es]
            [com.yetanalytics.flint.util :as u]))

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

;; GROUP BY projection vars

(defmulti group-by-projected-vars (fn [[k _]] k))

(defmethod group-by-projected-vars :group-by [[_ group-by-coll]]
  (->> group-by-coll
       (map group-by-projected-vars)
       (filter some?)))

(defmethod group-by-projected-vars :mod/expr [_] nil)

(defmethod group-by-projected-vars :ax/var [[_ v]] v)

(defmethod group-by-projected-vars :mod/expr-as-var [[_ expr-as-var]]
  (-> expr-as-var second second second))

;; SELECT exprs

(defmulti invalid-agg-expr-vars (fn [_ [k _]] k))

(defmethod invalid-agg-expr-vars :default [_] [])

(defmethod invalid-agg-expr-vars :expr/branch [valid-vars [_ [op-kv args-kv]]]
  (let [[_ op] op-kv
        [_ args] args-kv]
    (if (or (es/aggregate-ops op)
            (not (symbol? op)))
      []
      (mapcat (partial invalid-agg-expr-vars valid-vars) args))))

(defmethod invalid-agg-expr-vars :expr/terminal [valid-vars [_ x]]
  (invalid-agg-expr-vars valid-vars x))

(defmethod invalid-agg-expr-vars :ax/var [valid-vars [_ v]]
  (if-not (valid-vars v) [v] []))

;; Validation

(defn- validate-agg-select-clause
  "Return a coll of invalid vars in a SELECT clause with aggregates, or
   `nil` if valid."
  [group-by-vars sel-clause]
  (let [[_ bad-vars]
        (reduce (fn [[valid-vars bad-vars] [k x]]
                  (case k
                    :ax/var
                    (if-not (valid-vars x)
                      [valid-vars (conj bad-vars x)]
                      [valid-vars bad-vars])
                    :select/expr-as-var
                    (let [[_ [expr v-kv]] x
                          [_ v]           v-kv]
                      (if-some [bad-expr-vars
                                (->> expr
                                     (invalid-agg-expr-vars valid-vars)
                                     not-empty)]
                        [valid-vars (concat bad-vars bad-expr-vars)]
                        ;; Somehow already-projected vars are now valid,
                        ;; at least according to Apache Jena's query parser
                        [(conj valid-vars v) bad-vars]))))
                [group-by-vars []]
                sel-clause)]
    (not-empty bad-vars)))

(defn- validate-agg-select
  [[[_select-k select] loc]]
  (let [[_ select-cls] (u/get-kv-pair select :select)
        [_ ?group-by]  (u/get-kv-pair select :group-by)
        group-by-vs    (if ?group-by
                         (->> ?group-by
                              (map group-by-projected-vars)
                              (filter some?)
                              set)
                         #{})
        [sel-k sel-v]  select-cls]
    (if (= :ax/wildcard sel-k)
      {:kind ::wildcard-group-by
       :loc  loc}
      (when-some [bad-vars (validate-agg-select-clause group-by-vs sel-v)]
        {:kind      ::invalid-aggregate-var
         :variables bad-vars
         :loc       loc}))))

(defn validate-agg-selects
  [node-m]
  (->> (:agg/select node-m)
       (map validate-agg-select)
       (filter some?)))

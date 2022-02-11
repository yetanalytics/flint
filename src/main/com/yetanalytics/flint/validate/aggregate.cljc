(ns com.yetanalytics.flint.validate.aggregate
  (:require [clojure.zip :as zip]
            [com.yetanalytics.flint.validate.variable :as vv]
            [com.yetanalytics.flint.util              :as u]))

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
                                     (vv/invalid-agg-expr-vars valid-vars)
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
                              (map vv/group-by-projected-vars)
                              (filter some?)
                              set)
                         #{})
        [sel-k sel-v]  select-cls]
    (if (= :ax/wildcard sel-k)
      {:kind ::wildcard-group-by
       :path (->> loc zip/path (mapv first))}
      (when-some [bad-vars (validate-agg-select-clause group-by-vs sel-v)]
        {:kind      ::invalid-aggregate-var
         :variables bad-vars
         :path      (->> loc zip/path (mapv first))}))))

(defn validate-agg-selects
  "Validate, given `node-m` that contains a map from `SELECT` AST nodes to
   their zipper locs, that any SELECT that includes aggregates are valid
   according to the SPARQL spec. Returns `nil` if valid, a coll of error
   maps otherwise."
  [node-m]
  (->> (:agg/select node-m)
       (map validate-agg-select)
       (filter some?)
       not-empty))

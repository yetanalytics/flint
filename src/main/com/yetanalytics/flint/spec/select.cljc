(ns com.yetanalytics.flint.spec.select
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]
            [com.yetanalytics.flint.spec.expr  :as es]))

(defn- no-duplicate-vars?
  [var-or-exprs]
  (boolean (reduce (fn [seen [k x]]
                     (case k
                       :ax/var
                       (if (contains? seen x)
                         (reduced false)
                         (conj seen x))
                       :select/expr-as-var
                       (let [v (-> x second second second)]
                         (if (contains? seen v)
                           (reduced false)
                           (conj seen v)))))
                   #{}
                   var-or-exprs)))

(def select-spec
  (s/or :select/var-or-exprs
        (s/and (s/* (s/alt :ax/var ax/variable?
                           :select/expr-as-var ::es/agg-expr-as-var))
               no-duplicate-vars?)
        :ax/wildcard
        ax/wildcard?))

(s/def ::select select-spec)
(s/def ::select-distinct select-spec)
(s/def ::select-reduced select-spec)

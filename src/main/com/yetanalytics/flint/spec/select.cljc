(ns com.yetanalytics.flint.spec.select
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.axiom.protocol :as p]
            [com.yetanalytics.flint.spec.axiom     :as ax]
            [com.yetanalytics.flint.spec.expr      :as es]))

(defn- no-duplicate-vars?
  [var-or-exprs]
  (boolean (reduce (fn [seen [k x]]
                     (case k
                       :ax/var
                       (let [vname (p/variable-name x)]
                         (if (contains? seen vname)
                           (reduced false)
                           (conj seen vname)))
                       :select/expr-as-var
                       (let [v     (-> x second second second)
                             vname (p/variable-name v)]
                         (if (contains? seen vname)
                           (reduced false)
                           (conj seen vname)))))
                   #{}
                   var-or-exprs)))

(def select-spec
  (s/or :select/var-or-exprs
        (s/and (s/* (s/alt :ax/var ax/variable-spec
                           :select/expr-as-var ::es/agg-expr-as-var))
               no-duplicate-vars?)
        :ax/wildcard ax/wildcard-spec))

(s/def ::select select-spec)
(s/def ::select-distinct select-spec)
(s/def ::select-reduced select-spec)

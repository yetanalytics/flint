(ns com.yetanalytics.flint.spec.where
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom    :as ax]
            [com.yetanalytics.flint.spec.expr     :as es]
            [com.yetanalytics.flint.spec.modifier :as ms]
            [com.yetanalytics.flint.spec.select   :as ss]
            [com.yetanalytics.flint.spec.triple   :as ts]
            [com.yetanalytics.flint.spec.values   :as vs])
  #?(:clj (:require
           [com.yetanalytics.flint.spec :refer [sparql-keys]])
     :cljs (:require-macros
            [com.yetanalytics.flint.spec :refer [sparql-keys]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sub-SELECT query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def key-order-map
  {:select          2
   :select-distinct 2
   :select-reduced  2
   :where           5
   :group-by        6
   :order-by        7
   :having          8
   :limit           9
   :offset          10
   :values          11})

(defn- key-comp
  [k1 k2]
  (let [n1 (get key-order-map k1 100)
        n2 (get key-order-map k2 100)]
    (- n1 n2)))

(s/def ::select
  (sparql-keys :req-un [(or ::ss/select
                            ::ss/select-distinct
                            ::ss/select-reduced)
                        ::where]
               :opt-un [::vs/values
                        ;; s/merge does not result in correct conformation
                        ::ms/group-by
                        ::ms/order-by
                        ::ms/having
                        ::ms/limit
                        ::ms/offset]
               :key-comp-fn key-comp))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WHERE clause
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::where
  (s/or :where-sub/select
        ::select
        :where-sub/where
        (s/coll-of (s/or
                    :triple/vec     ts/triple-vec-spec
                    :triple/nform   ts/normal-form-spec
                    :where/recurse  (s/& (s/cat :k #{:where}
                                                :v ::where)
                                         (s/conformer #(:v %)))
                    :where/union    (s/& (s/cat :k #{:union}
                                                :v (s/+ ::where))
                                         (s/conformer #(:v %)))
                    :where/optional (s/& (s/cat :k #{:optional}
                                                :v ::where)
                                         (s/conformer #(:v %)))
                    :where/minus    (s/& (s/cat :k #{:minus}
                                                :v ::where)
                                         (s/conformer #(:v %)))
                    :where/graph    (s/& (s/cat :k #{:graph}
                                                :v1 ax/var-or-iri-spec
                                                :v2 ::where)
                                         (s/conformer
                                          (fn [x] [(:v1 x) (:v2 x)])))
                    :where/service  (s/& (s/cat :k #{:service}
                                                :v1 ax/var-or-iri-spec
                                                :v2 ::where)
                                         (s/conformer
                                          (fn [x] [(:v1 x) (:v2 x)])))
                    :where/service-silent (s/& (s/cat :k #{:service-silent}
                                                      :v1 ax/var-or-iri-spec
                                                      :v2 ::where)
                                               (s/conformer
                                                (fn [x] [(:v1 x) (:v2 x)])))
                    :where/filter   (s/& (s/cat :k #{:filter}
                                                :v ::es/expr)
                                         (s/conformer #(:v %)))
                    :where/bind     (s/& (s/cat :k #{:bind}
                                                :v ::es/expr-as-var)
                                         (s/conformer #(:v %)))
                    :where/values   (s/& (s/cat :k #{:values}
                                                :v ::vs/values)
                                         (s/conformer #(:v %))))
                   :min-count 1
                   :kind vector?)
        :where-sub/empty
        (s/and vector? empty?)))

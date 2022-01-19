(ns syrup.sparql.spec.where
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]
            [syrup.sparql.spec.expr :as ex]
            [syrup.sparql.spec.triple :as triple]
            [syrup.sparql.spec.modifier :as ms]
            [syrup.sparql.spec.select :as ss]
            [syrup.sparql.spec.value :as vs]))

(s/def ::select
  (s/keys :req-un [(or ::ss/select ::ss/select-distinct ::ss/select-reduced)
                   ::where]
          :opt-un [::vs/values
                   ;; s/merge does not result in correct conformation
                   ::ms/group-by
                   ::ms/order-by
                   ::ms/having
                   ::ms/limit
                   ::ms/offset]))

(s/def ::where
  (s/or :sub-select ::select
        :sub-where (s/coll-of
                    (s/or
                     :recurse  ::where
                     :tvec     triple/triple-vec-spec
                     :nform    triple/normal-form-spec
                     :union    (s/& (s/cat :k #{:union}
                                           :v (s/+ ::where))
                                    (s/conformer #(:v %)))
                     :optional (s/& (s/cat :k #{:optional}
                                           :v ::where)
                                    (s/conformer #(:v %)))
                     :minus    (s/& (s/cat :k #{:minus}
                                           :v ::where)
                                    (s/conformer #(:v %)))
                     :graph    (s/& (s/cat :k #{:graph}
                                           :v1 ax/var-or-iri-spec
                                           :v2 ::where)
                                    (s/conformer (fn [x] [(:v1 x) (:v2 x)])))
                     :service  (s/& (s/cat :k #{:service}
                                           :v1 ax/var-or-iri-spec
                                           :v2 ::where)
                                    (s/conformer (fn [x] [(:v1 x) (:v2 x)])))
                     :service-silent (s/& (s/cat :k #{:service-silent}
                                                 :v1 ax/var-or-iri-spec
                                                 :v2 ::where)
                                          (s/conformer (fn [x] [(:v1 x) (:v2 x)])))
                     :filter   (s/& (s/cat :k #{:filter}
                                           :v ::ex/expr)
                                    (s/conformer #(:v %)))
                     :bind     (s/& (s/cat :k #{:bind}
                                           :v ::ex/expr-as-var)
                                    (s/conformer #(:v %)))
                     :values   (s/& (s/cat :k #{:values}
                                           :v ::vs/values)
                                    (s/conformer #(:v %))))
                    :min-count 1
                    :kind vector?)
        :sub-empty (s/and vector? empty?)))

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
  (s/or :select ::select
        :where (s/coll-of
                (s/or
                 :recurse  ::where
                 :tvec     triple/triple-vec-spec
                 :nform    triple/normal-form-spec
                 :union    (s/cat :k #{:union}
                                  :v (s/+ ::where))
                 :optional (s/cat :k #{:optional}
                                  :v ::where)
                 :minus    (s/cat :k #{:minus}
                                  :v ::where)
                 :graph    (s/cat :k #{:graph}
                                  :v1 ax/var-or-iri-spec
                                  :v2 ::where)
                 :service  (s/cat :k #{:service :service-silent}
                                  :v1 ax/var-or-iri-spec
                                  :v2 ::where)
                 :filter   (s/cat :k #{:filter}
                                  :v ::ex/expr)
                 :bind     (s/cat :k #{:bind}
                                  :v ::ex/expr-as-var)
                 :values   (s/cat :k #{:values}
                                  :v ::vs/values)))))

(comment
  (s/conform ::select
             '{:select [?s] :where [[?s ?p ?o]]})
  (s/conform ::where
             '[[:union [[?s ?p ?o]]]]))


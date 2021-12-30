(ns syrup.sparql.spec.where
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.expr :as ex]
            [syrup.sparql.spec.triple :as triple]
            [syrup.sparql.spec.value :as vs]))

;; Forward declare where spec
(declare where-spec)

(s/def ::union
  (s/coll-of where-spec :min-count 1))

(s/def ::optional where-spec)

(s/def ::minus where-spec)

(s/def ::graph (s/map-of ::var-or-iri where-spec))

(s/def ::silent? boolean?)
(s/def ::service (s/merge (s/map-of ::var-or-iri where-spec)
                          (s/keys :opt-un [::silent?])))

(s/def ::filter ex/expr-spec)
(s/def ::filter-exists where-spec)
(s/def ::filter-not-exists where-spec)

(s/def ::bind ex/expr-as-var-spec)

(s/def ::values vs/values-clause-spec)

(def where-nonselect-spec
  (s/coll-of (s/or :triples  triple/triples-spec
                   :union    (s/keys :req-un [::union])
                   :optional (s/keys :req-un [::optional])
                   :minus    (s/keys :req-un [::minus])
                   :graph    (s/keys :req-un [::graph])
                   :service  (s/keys :req-un [::service])
                   :filter   (s/keys :req-un [(or ::filter
                                                  ::filter-exists
                                                  ::filter-not-exists)])
                   :bind     (s/keys :req-un [::bind])
                   :values   (s/keys :req-un [::values]))))

;; TODO: SolutionModifier
;; TODO: where clause
(def where-select-spec
  (s/keys :req-un [(or ::select ::select-distinct ::select-where)
                   ::values]))

(def where-spec
  (s/or :sub-select where-select-spec
        :graph-pattern where-nonselect-spec))

(s/def ::where where-spec)

(comment
  (s/conform (s/keys :req-un [::union])
             {:union [[{:?s2 {:?p2 #{:?o2}}}]
                      [{:?s3 {:?p3 #{:?o3}}}]]})
  
  (s/conform ::where
             [{:?s {:?p #{:?o}}}
              {:union [[{:?s2 {:?p2 #{:?o2}}}
                        {:?s3 {:?p3 #{:?o3}}}]]}]))

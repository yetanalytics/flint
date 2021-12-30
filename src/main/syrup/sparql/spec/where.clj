(ns syrup.sparql.spec.where
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.expr :as ex]
            [syrup.sparql.spec.triple :as triple]))

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

(s/def ::values
  (s/or :old
        (s/and (s/map-of (s/coll-of ::var)
                         (s/coll-of (s/coll-of any?)))
               (fn [[vars values]]
                 (let [nv (count vars)]
                   (every? #(= nv (count %)) values))))
        :new
        (s/and (s/map-of ::var (s/coll-of any?))
               (fn [m]
                 (let [values (vals m)
                       nv     (count (first values))]
                   (every? #(= nv (count %)) values))))))

(def where-spec
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

(s/def ::where where-spec)

(comment
  (s/conform (s/keys :req-un [::union])
             {:union [[{:?s2 {:?p2 #{:?o2}}}]
                      [{:?s3 {:?p3 #{:?o3}}}]]})
  
  (s/conform ::where
             [{:?s {:?p #{:?o}}}
              {:union [[{:?s2 {:?p2 #{:?o2}}}
                        {:?s3 {:?p3 #{:?o3}}}]]}]))

(ns syrup.sparql.spec.prologue
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]))

(s/def ::bases
  (s/and (s/coll-of ax/iri?)
         (s/conformer (partial map (fn [b] [:base b])))))

(s/def ::prefixes
  (s/and (s/map-of keyword? ax/iri?)
         (s/conformer (partial map (fn [[pre iri]] [:prefix [pre iri]])))))

(def prologue-spec
  (s/keys :opt-un [::bases ::prefixes]))

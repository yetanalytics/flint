(ns syrup.sparql.spec.prologue
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]))

(s/def ::bases
  (s/coll-of ax/iri?))

(s/def ::prefixes
  (s/map-of keyword? ax/iri?))

(def prologue-spec
  (s/keys :opt-un [::bases ::prefixes]))

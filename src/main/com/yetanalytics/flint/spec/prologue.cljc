(ns com.yetanalytics.flint.spec.prologue
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]
            [com.yetanalytics.flint.axiom.impl.validation :refer []]))

;; single `s/or` used to conform

(s/def ::base
  (s/or :ax/iri ax/iri-spec))

(s/def ::prefixes
  (s/and (s/map-of ax/prefix-spec ax/iri-spec)
         (s/conformer
          (partial map
                   (fn [[pre iri]]
                     [:prologue/prefix [[:ax/prefix pre]
                                        [:ax/iri iri]]])))))

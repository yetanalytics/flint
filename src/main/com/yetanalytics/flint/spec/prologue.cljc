(ns com.yetanalytics.flint.spec.prologue
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]))

(defn- prefix-keyword?
  [k]
  (and (keyword? k)
       (nil? (namespace k))
       (or (re-matches ax/prefix-iri-ns-regex (name k))
           (= :$ k))))

;; single `s/or` used to conform

(s/def ::base
  (s/or :ax/iri ax/iri?))

(s/def ::prefixes
  (s/and (s/map-of prefix-keyword? ax/iri?)
         (s/conformer (partial map
                               (fn [[pre iri]]
                                 [:prologue/prefix [pre [:ax/iri iri]]])))))

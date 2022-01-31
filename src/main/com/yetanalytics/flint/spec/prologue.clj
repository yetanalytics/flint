(ns com.yetanalytics.flint.spec.prologue
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]))

(defn- prefix-keyword?
  [k]
  (and (keyword? k)
       (nil? (namespace k))
       (or (re-matches ax/prefix-iri-ns-regex (name k))
           (= :$ k))))

(s/def ::bases
  (s/and (s/coll-of ax/iri?)
         (s/conformer (partial map
                               (fn [b] [:base [:iri b]])))))

(s/def ::prefixes
  (s/and (s/map-of prefix-keyword? ax/iri?)
         (s/conformer (partial map
                               (fn [[pre iri]] [:prefix [pre [:iri iri]]])))))

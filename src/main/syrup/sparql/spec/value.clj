(ns syrup.sparql.spec.value
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]))

;; TODO: Add conformer
(def values-clause-spec
  (s/or :sparql
        (s/and (s/map-of (s/coll-of ax/variable?)
                         (s/coll-of (s/coll-of any?)))
               (fn [m]
                 (let [vars   (first (keys m))
                       values (first (vals m))
                       nv     (count vars)]
                   (every? #(= nv (count %)) values))))
        :clojure
        (s/and (s/map-of ax/variable? (s/coll-of any?))
               (fn [m]
                 (let [values (vals m)
                       nv     (count (first values))]
                   (every? #(= nv (count %)) values))))))

(s/def ::values values-clause-spec)

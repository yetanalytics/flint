(ns syrup.sparql.spec.value
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]))

(defn- matching-val-lens*
  [m]
  (let [vars   (first (keys m))
        values (first (vals m))
        nv     (count vars)]
    (every? #(= nv (count %)) values)))

(defn- matching-val-lens
  [m]
  (let [values (vals m)
        nv     (count (first values))]
    (every? #(= nv (count %)) values)))

(def values-clause-spec
  (s/and
   (s/or :sparql
         (s/and (s/map-of (s/and coll? (partial every? ax/variable?))
                          (s/and coll? (partial every? coll?)))
                matching-val-lens*)
         :clojure
         (s/and (s/map-of ax/variable? coll?)
                matching-val-lens
                (s/conformer
                 (fn clojure->sparql [m]
                   (let [mfn    (fn [& ks] (vec ks))
                         vars   (->> m keys vec)
                         values (->> m vals (apply map mfn) vec)]
                     {vars values})))))
   (s/conformer second)))

(s/def ::values (s/or :values-map values-clause-spec))

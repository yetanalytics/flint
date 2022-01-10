(ns syrup.sparql.spec.value
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]))

(def values-clause-spec
  (s/and
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
                    (every? #(= nv (count %)) values)))
                (s/conformer
                 (fn [m]
                   (let [mfn    (fn [& ks] (vec ks))
                         vars   (->> m keys vec)
                         values (->> m vals (apply map mfn) vec)]
                     {vars values})))))
   (s/conformer second)))

(s/def ::values values-clause-spec)

(comment
  (s/conform ::values '{[?foo ?bar] [[1 :a] [2 :b] [3 :c]]})

  (s/conform ::values '{?foo [1 2 3]
                        ?bar [:a :b :c]})
  
  (vals '{?foo [1 2 3]
          ?bar [:a :b :c]})
  (map (fn [& ks] ks) [1 2 3] [:a :b :c]))

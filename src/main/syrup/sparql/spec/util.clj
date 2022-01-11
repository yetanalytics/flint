(ns syrup.sparql.spec.util
  (:require [clojure.spec.alpha :as s]))

(defn- unk [k]
  (-> k name keyword))

(defmacro keys-cat
  [ks]
  `(s/cat ~@(mapcat (fn [k] [::s/k #{(unk k)} ::s/v (s/form k)]) ks)))

(comment
  (s/def ::foo int?)
  (require '[clojure.spec.gen.alpha :as sgen])
  (mapcat (fn [k] [k (s/form k)]) [::foo])
  (macroexpand '(keys-cat [::foo]))
  
  (s/gen (s/keys :req-un [::foo]))
  (sgen/generate (s/gen (keys-cat [:foo 2])))
  (s/conform (keys-cat [::foo]) [:foo 2])
  (s/explain-data (keys-cat [::foo ::foo]) [:foo 2 :foo false]))

(def datomic-spec
  (s/and (s/+ (s/cat ::s/k keyword?
                     ::s/v (s/* (comp not keyword?))))
         (s/conformer
          (fn [coll]
            (reduce
             (fn [acc {k ::s/k v ::s/v}]
               (assoc acc
                      k
                      (if (#{:prefixes :from :limit :offset} k)
                        (first v)
                        v)))
             {}
             coll)))))

(ns syrup.sparql.spec.util
  (:require [clojure.spec.alpha :as s]))

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

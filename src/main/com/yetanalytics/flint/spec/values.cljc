(ns com.yetanalytics.flint.spec.values
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]))

(def value-spec
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/literal    ax/literal-spec
        :values/undef  nil?))

(defn- matching-val-lengths*
  [m]
  (let [vars   (first (keys m))
        values (first (vals m))
        nv     (count vars)]
    (every? #(= nv (count %)) values)))

(defn- matching-val-lengths
  [m]
  (let [values (vals m)
        nv     (count (first values))]
    (every? #(= nv (count %)) values)))

(defn- clojure->sparql
  [m]
  (let [mfn    (fn [& ks] (vec ks))
        vars   (->> m keys vec)
        values (->> m vals (apply map mfn) vec)]
    {vars values}))

;; For some reason `s/map-of` doesn't conform the keys properly
;; so we have to do it manually.
(defn- conform-vars
  [m]
  (let [k (first (keys m))
        v (first (vals m))]
    [(mapv (fn [vr] [:ax/var vr]) k) v]))

(def values-clause-spec
  (s/and
   (s/or :values/sparql-format
         (s/and (s/map-of any? any? :min-count 1 :max-count 1)
                (s/map-of (s/coll-of ax/variable-spec)
                          (s/coll-of (s/coll-of value-spec)))
                matching-val-lengths*
                (s/conformer conform-vars))
         :values/clojure-format
         (s/and (s/map-of any? any? :min-count 1)
                (s/map-of ax/variable-spec (s/coll-of value-spec))
                matching-val-lengths
                (s/conformer clojure->sparql)
                (s/conformer conform-vars)))
   (s/conformer second)))

;; single-branch `s/or` is used to conform values
(s/def ::values (s/or :values/map values-clause-spec))

(ns syrup.sparql.spec.axiom
  (:require [clojure.spec.alpha :as s]))

;; Use a regex defined by the SPARQL grammar instead of copying from
;; xapi-schema or other non-SPARQL library.
;; TODO: Test that SPARQL IRIs and xapi-schema IRIs are compatible
(def iri-regex
  #"<([^<>\"{}|\^\\`\s])*>")

(defn iri? [s]
  (boolean (and (string? s) (re-matches iri-regex s))))

(defn prefix-iri? [s]
  (boolean (and (keyword? s) (not (#{:a :*} s)))))

(defn variable? [x]
  (boolean (and (symbol? x)
                (->> x name (re-matches #"\?.*")))))

(defn bnode? [x]
  (boolean (and (symbol? x)
                (->> x name (re-matches #"_.*")))))

(defn wildcard? [x]
  (boolean (#{'* :*} x)))

(defn rdf-type? [x]
  (boolean (#{'a :a} x)))

;; TODO: Incomplete specs

(def iri-spec
  (s/or :iri iri?
        :prefix-iri prefix-iri?))

(def iri-pred-spec
  (s/or :iri iri?
        :rdf-type rdf-type?))

(def var-or-iri-spec
  (s/or :var variable?
        :iri iri?
        :prefix-iri prefix-iri?))

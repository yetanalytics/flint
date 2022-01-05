(ns syrup.sparql.spec.axiom
  (:require [clojure.spec.alpha :as s]))

;; Use a regex defined by the SPARQL grammar instead of copying from
;; xapi-schema or other non-SPARQL library.
;; TODO: Test that SPARQL IRIs and xapi-schema IRIs are compatible
(def iri-regex
  #"([^<>\"{}|\^\\`\s])*")

(defn iri? [s]
  (boolean (or (and (string? s) (re-matches iri-regex s))
               (and (keyword? s) (not (#{:a :*} s))))))

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

(def iri-pred-spec
  (s/or :iri iri?
        :rdf-type rdf-type?))

(def var-or-iri-spec
  (s/or :var variable?
        :iri iri?))

(def var-or-iri-subj-spec
  (s/or :var variable?
        :iri iri?
        :bnode bnode?))

(def var-or-iri-pred-spec
  (s/or :var variable?
        :iri iri?
        :rdf-type rdf-type?))

(def var-or-term-spec
  (s/or :var variable?
        :iri iri?
        :bnode bnode?
        :nil nil?
        :string-literal string?
        :numeric-literal number?
        :boolean-literal boolean?
        :date-time-literal inst?))

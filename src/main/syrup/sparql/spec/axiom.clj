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

(defn valid-string?
  "Is `x` a string and does not contains unescaped `\"`, `\\`, `\\n`, nor
   `\\r`? (This filtering is to avoid SPARQL injection attacks.)"
  [x]
  (boolean (and (string? x)
                (re-matches #"([^\"\r\n\\]|(?:\\(?:\n|\r|\"|\\)))*" x))))

(defn lang-map?
  "Is `x` a singleton map between a language tag and valid string?"
  [x]
  (s/valid? (s/map-of keyword? valid-string? :min-count 1 :max-count 1) x))

;; Composite specs

(def iri-spec
  (s/or :iri iri?
        :prefix-iri prefix-iri?))

(def var-or-iri-spec
  (s/or :var variable?
        :iri iri?
        :prefix-iri prefix-iri?))

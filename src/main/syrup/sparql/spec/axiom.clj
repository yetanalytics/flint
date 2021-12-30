(ns main.syrup.sparql.spec.axiom
  (:require [clojure.spec.alpha :as s]))

;; Use a regex defined by the SPARQL grammar instead of copying from
;; xapi-schema or other non-SPARQL library.
;; TODO: Test that SPARQL IRIs and xapi-schema IRIs are compatible
(def iri-regex
  #"([^<>\"{}|\^\\`\s])*")

(defn iri? [s]
  (boolean (or (re-matches iri-regex s)
               (keyword? s))))

(defn variable? [x]
  (boolean (and (instance? clojure.lang.Named x)
                (->> x name (re-matches #"\?.*")))))

(defn wildcard? [x]
  (boolean (#{'* :*} x)))

(defn rdf-type? [x]
  (boolean (#{'a :a} x)))

;; TODO: Incomplete specs

(def var-or-iri-spec
  (s/or :var variable?
        :iri iri?))

(def var-or-term-spec
  (s/or :var variable?
        :nil nil?
        :string-literal string?
        :numeric-literal number?
        :boolean-literal boolean?
        :date-time-literal inst?))

(ns com.yetanalytics.flint.spec.axiom
  (:require [clojure.spec.alpha :as s]))

;; Use regexes defined by the SPARQL grammar instead of copying from
;; xapi-schema or other non-SPARQL library.

(def iri-regex
  #"<([^<>{}\"\\|\^`\s])*>")

(def prefix-iri-ns-regex
  #"[A-Za-z_]([\w\-\.]*[\w\-])?")

(def prefix-iri-name-regex
  #"[\w\-]+")

(def variable-regex
  #"\?\w+")

(def bnode-regex
  #"_(\w([\w\.]*\w)?)?")

;; Note in the second part that we need to match the letters `n`, `r`, etc.,
;; not the newline char `\n` nor the return char `\r`.
(def valid-str-regex
  #"([^\"\r\n\\]|(?:\\(?:t|b|n|r|f|\\|\"|')))*")

(defn iri?
  "Is `x` a wrapped (i.e. starts with `<` and ends with `>`) IRI?
   Note that `x` can be an otherwise non-IRI (e.g. `<foo>`)."
  [x]
  (boolean (and (string? x)
                (re-matches iri-regex x))))

(defn prefix-iri?
  "Is `x` a potentially namespaced keyword?"
  [x]
  (boolean (and (keyword? x)
                (not (#{:a :*} x))
                (or (->> x namespace nil?)
                    (->> x namespace (re-matches prefix-iri-ns-regex)))
                (->> x name (re-matches prefix-iri-name-regex)))))

(defn variable?
  "Is `x` a symbol that starts with `?`?"
  [x]
  (boolean (and (symbol? x)
                (->> x name (re-matches variable-regex)))))

(defn bnode?
  "Is `x` a symbol that starts with `_` and has zero or more trailing chars?"
  [x]
  (boolean (and (symbol? x)
                (->> x name (re-matches bnode-regex)))))

(defn wildcard?
  "Is `x` a symbol or keyword that is `*`?"
  [x]
  (boolean (#{'* :*} x)))

(defn rdf-type?
  "Is `x` a symbol or keyword that is `a`?"
  [x]
  (boolean (#{'a :a} x)))

(defn valid-string?
  "Is `x` a string and does not contains unescaped `\"`, `\\`, `\\n`, nor
   `\\r`? (This filtering is to avoid SPARQL injection attacks.)"
  [x]
  (boolean (and (string? x)
                (re-matches valid-str-regex x))))

(defn lang-map?
  "Is `x` a singleton map between a language tag and valid string?"
  [x]
  (s/valid? (s/map-of keyword? valid-string? :min-count 1 :max-count 1) x))

;; Composite specs

(def iri-spec
  (s/or :ax/iri iri?
        :ax/prefix-iri prefix-iri?))

(def var-or-iri-spec
  (s/or :ax/var variable?
        :ax/iri iri?
        :ax/prefix-iri prefix-iri?))

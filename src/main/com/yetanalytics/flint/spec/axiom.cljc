(ns com.yetanalytics.flint.spec.axiom
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.axiom.protocol :as p]
            [com.yetanalytics.flint.axiom.impl]))

;; Axiom specs

(def iri-spec
  p/-valid-iri?)

(def prefix-spec
  p/-valid-prefix?)

(def prefix-iri-spec
  p/-valid-prefix-iri?)

(def variable-spec
  p/-valid-variable?)

(def bnode-spec
  p/-valid-bnode?)

(def wildcard-spec
  p/-valid-wildcard?)

(def rdf-type-spec
  p/-valid-rdf-type?)

(def literal-spec
  p/-valid-literal?)

;; Composite specs

(def iri-or-prefixed-spec
  "Spec for both prefixed and full IRIs."
  (s/or :ax/iri iri-spec
        :ax/prefix-iri prefix-iri-spec))

(def iri-or-var-spec
  "Spec for both prefixed IRIs, full IRIs, and variables."
  (s/or :ax/var variable-spec
        :ax/iri iri-spec
        :ax/prefix-iri prefix-iri-spec))

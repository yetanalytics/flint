(ns com.yetanalytics.flint.spec.axiom
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.axiom.protocol :as p]
            [com.yetanalytics.flint.axiom.impl]))

;; Axiom specs

(def iri-spec
  (s/and #(satisfies? p/IRI %)
         p/-valid-iri?))

(def prefix-spec
  (s/and #(satisfies? p/Prefix %)
         p/-valid-prefix?))

(def prefix-iri-spec
  (s/and #(satisfies? p/PrefixedIRI %)
         p/-valid-prefix-iri?))

(def variable-spec
  (s/and #(satisfies? p/Variable %)
         p/-valid-variable?))

(def bnode-spec
  (s/and #(satisfies? p/BlankNode %)
         p/-valid-bnode?))

(def wildcard-spec
  (s/and #(satisfies? p/Wildcard %)
         p/-valid-wildcard?))

(def rdf-type-spec
  (s/and #(satisfies? p/RDFType %)
         p/-valid-rdf-type?))

(def literal-spec
  (s/and #(satisfies? p/Literal %)
         p/-valid-literal?))

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

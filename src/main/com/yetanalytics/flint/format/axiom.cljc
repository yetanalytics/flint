(ns com.yetanalytics.flint.format.axiom
  (:require [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.axiom.protocol :as p]
            [com.yetanalytics.flint.axiom.impl]))

(defmethod f/format-ast-node :ax/iri [_ [_ iri]]
  (p/-format-iri iri))

(defmethod f/format-ast-node :ax/prefix [_ [_ prefix]]
  (p/-format-prefix prefix))

(defmethod f/format-ast-node :ax/prefix-iri [_ [_ prefix-iri]]
  (p/-format-prefix-iri prefix-iri))

(defmethod f/format-ast-node :ax/var [_ [_ variable]]
  (p/-format-variable variable))

(defmethod f/format-ast-node :ax/bnode [_ [_ bnode]]
  (p/-format-bnode bnode))

(defmethod f/format-ast-node :ax/wildcard [_ [_ wildcard]]
  (p/-format-wildcard wildcard))

(defmethod f/format-ast-node :ax/rdf-type [_ [_ rdf-type]]
  (p/-format-rdf-type rdf-type))

(defmethod f/format-ast-node :ax/literal [opts [_ literal]]
  (p/-format-literal literal opts))

;; Technically literals in the SPARQL spec but they behave differently in Flint
(defmethod f/format-ast-node :ax/numeric [_ [_ num]]
  (str num))

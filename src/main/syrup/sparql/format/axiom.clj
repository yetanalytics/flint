(ns syrup.sparql.format.axiom
  (:require [syrup.sparql.format :as f]))

(defmethod f/format-ast :iri [[_ iri]]
  iri)

(defmethod f/format-ast :prefix-iri [[_ prefix-iri]]
  (str (namespace prefix-iri) ":" (name prefix-iri)))

(defmethod f/format-ast :var [[_ variable]]
  (name variable))

(defmethod f/format-ast :bnode [[_ bnode]]
  (if-some [?suffix (second (re-matches #"_(.+)" (name bnode)))]
    (str "_:" ?suffix)
    "[]"))

(defmethod f/format-ast :wildcard [[_ _]]
  "*")

(defmethod f/format-ast :rdf-type [[_ _]]
  "a")

(defmethod f/format-ast :str-lit [[_ str-value]]
  (str "'" str-value "'"))

(defmethod f/format-ast :num-lit [[_ num-value]]
  (str num-value))

(defmethod f/format-ast :bool-lit [[_ bool-value]]
  (str bool-value))

(defmethod f/format-ast :dt-lit [[_ dt-value]]
  (str (.toInstant dt-value) "^^<http://www.w3.org/2001/XMLSchema#dateTime>"))

(defmethod f/format-ast :lmap-lit [[_ value]]
  (let [[ltag lval] (first value)]
    (str lval "@" (name ltag))))

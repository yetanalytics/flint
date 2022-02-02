(ns com.yetanalytics.flint.format.axiom
  (:require [com.yetanalytics.flint.format :as f]))

(defmethod f/format-ast-node :iri [_ [_ iri]]
  iri)

(defmethod f/format-ast-node :prefix-iri [_ [_ prefix-iri]]
  (str (namespace prefix-iri) ":" (name prefix-iri)))

(defmethod f/format-ast-node :var [_ [_ variable]]
  (name variable))

(defmethod f/format-ast-node :bnode [_ [_ bnode]]
  (if-some [?suffix (second (re-matches #"_(.+)" (name bnode)))]
    (str "_:" ?suffix)
    "[]"))

(defmethod f/format-ast-node :wildcard [_ [_ _]]
  "*")

(defmethod f/format-ast-node :rdf-type [_ [_ _]]
  "a")

(defmethod f/format-ast-node :str-lit [_ [_ str-value]]
  (str "\"" str-value "\""))

(defmethod f/format-ast-node :lmap-lit [_ [_ lang-map]]
  (let [ltag (-> lang-map keys first)
        lval (-> lang-map vals first)]
    (str "\"" lval "\"@" (name ltag))))

(defmethod f/format-ast-node :num-lit [_ [_ num-value]]
  (str num-value))

(defmethod f/format-ast-node :bool-lit [_ [_ bool-value]]
  (str bool-value))

(defmethod f/format-ast-node :dt-lit [{:keys [xsd-prefix]} [_ dt-value]]
  (let [xsd-suffix (if xsd-prefix
                     (str xsd-prefix ":dateTime")
                     "<http://www.w3.org/2001/XMLSchema#dateTime>")]
    (str "\"" (.toInstant dt-value) "\"^^" xsd-suffix)))

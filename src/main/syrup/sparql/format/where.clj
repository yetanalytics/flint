(ns syrup.sparql.format.where
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]
            [syrup.sparql.format.expr]
            [syrup.sparql.format.triple]
            [syrup.sparql.format.modifier]
            [syrup.sparql.format.select]
            [syrup.sparql.format.values]))

(defn format-select-query
  [select-query]
  (cstr/join "\n" select-query))

(defmethod f/format-ast :where-sub/select [[_ sub-select]]
  (str "{\n" (f/indent-str (format-select-query sub-select)) "\n}"))

(defmethod f/format-ast :where-sub/where [[_ sub-where]]
  (str "{\n" (f/indent-str (cstr/join "\n" sub-where)) "\n}"))

(defmethod f/format-ast :where-sub/empty [_]
  "{}")

(defmethod f/format-ast :where/recurse [[_ pattern]]
  pattern)

(defmethod f/format-ast :where/union [[_ patterns]]
  (cstr/join "\nUNION\n" patterns))

(defmethod f/format-ast :where/optional [[_ pattern]]
  (str "OPTIONAL " pattern))

(defmethod f/format-ast :where/minus [[_ pattern]]
  (str "MINUS " pattern))

(defmethod f/format-ast :where/graph [[_ [iri pattern]]]
  (str "GRAPH " iri " " pattern))

(defmethod f/format-ast :where/service [[_ [iri pattern]]]
  (str "SERVICE " iri " " pattern))

(defmethod f/format-ast :where/service-silent [[_ [iri pattern]]]
  (str "SERVICE SILENT " iri " " pattern))

(defmethod f/format-ast :where/filter [[_ expr]]
  (str "FILTER " expr))

(defmethod f/format-ast :where/bind [[_ expr-as-var]]
  (str "BIND (" expr-as-var ")"))

(defmethod f/format-ast :where/values [[_ values]]
  (str "VALUES " values))

(defmethod f/format-ast :where [[_ where]]
  (str "WHERE " where))

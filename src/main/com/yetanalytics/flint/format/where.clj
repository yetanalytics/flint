(ns com.yetanalytics.flint.format.where
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]
            [com.yetanalytics.flint.format.expr]
            [com.yetanalytics.flint.format.modifier]
            [com.yetanalytics.flint.format.select]
            [com.yetanalytics.flint.format.triple]
            [com.yetanalytics.flint.format.values]))

(defn format-select-query
  [select-query]
  (cstr/join "\n" select-query))

(defmethod f/format-ast :where-sub/select [_ [_ sub-select]]
  (str "{\n" (f/indent-str (format-select-query sub-select)) "\n}"))

(defmethod f/format-ast :where-sub/where [_ [_ sub-where]]
  (str "{\n" (f/indent-str (cstr/join "\n" sub-where)) "\n}"))

(defmethod f/format-ast :where-sub/empty [_ _]
  "{}")

(defmethod f/format-ast :where/recurse [_ [_ pattern]]
  pattern)

(defmethod f/format-ast :where/union [_ [_ patterns]]
  (cstr/join "\nUNION\n" patterns))

(defmethod f/format-ast :where/optional [_ [_ pattern]]
  (str "OPTIONAL " pattern))

(defmethod f/format-ast :where/minus [_ [_ pattern]]
  (str "MINUS " pattern))

(defmethod f/format-ast :where/graph [_ [_ [iri pattern]]]
  (str "GRAPH " iri " " pattern))

(defmethod f/format-ast :where/service [_ [_ [iri pattern]]]
  (str "SERVICE " iri " " pattern))

(defmethod f/format-ast :where/service-silent [_ [_ [iri pattern]]]
  (str "SERVICE SILENT " iri " " pattern))

(defmethod f/format-ast :where/filter [_ [_ expr]]
  (str "FILTER " expr))

(defmethod f/format-ast :where/bind [_ [_ expr-as-var]]
  (str "BIND (" expr-as-var ")"))

(defmethod f/format-ast :where/values [_ [_ values]]
  (str "VALUES " values))

(defmethod f/format-ast :where [_ [_ where]]
  (str "WHERE " where))

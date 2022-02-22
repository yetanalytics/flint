(ns com.yetanalytics.flint.format.where
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]
            [com.yetanalytics.flint.format.expr]
            [com.yetanalytics.flint.format.modifier]
            [com.yetanalytics.flint.format.select]
            [com.yetanalytics.flint.format.triple]
            [com.yetanalytics.flint.format.values]))

(defmethod f/format-ast-node :where-sub/select [{:keys [pretty?]} [_ sub-select]]
  (-> sub-select
      (f/join-clauses pretty?)
      (f/wrap-in-braces pretty?)))

(defmethod f/format-ast-node :where-sub/where [{:keys [pretty?]} [_ sub-where]]
  (-> sub-where
      (f/join-clauses pretty?)
      (f/wrap-in-braces pretty?)))

(defmethod f/format-ast-node :where-sub/empty [_ _]
  "{}")

(defmethod f/format-ast-node :where/recurse [_ [_ pattern]]
  pattern)

(defmethod f/format-ast-node :where/union [{:keys [pretty?]} [_ patterns]]
  (if pretty?
    (cstr/join "\nUNION\n" patterns)
    (cstr/join " UNION " patterns)))

(defmethod f/format-ast-node :where/optional [_ [_ pattern]]
  (str "OPTIONAL " pattern))

(defmethod f/format-ast-node :where/minus [_ [_ pattern]]
  (str "MINUS " pattern))

(defmethod f/format-ast-node :where/graph [_ [_ [iri pattern]]]
  (str "GRAPH " iri " " pattern))

(defmethod f/format-ast-node :where/service [_ [_ [iri pattern]]]
  (str "SERVICE " iri " " pattern))

(defmethod f/format-ast-node :where/service-silent [_ [_ [iri pattern]]]
  (str "SERVICE SILENT " iri " " pattern))

(defmethod f/format-ast-node :where/filter [_ [_ expr]]
  (if (f/bracketted-or-fn-expr-str? expr)
    (str "FILTER " expr)
    (str "FILTER (" expr ")")))

(defmethod f/format-ast-node :where/bind [_ [_ expr-as-var]]
  (str "BIND (" expr-as-var ")"))

(defmethod f/format-ast-node :where/values [_ [_ values]]
  (str "VALUES " values))

(defmethod f/format-ast-node :where [_ [_ where]]
  (str "WHERE " where))

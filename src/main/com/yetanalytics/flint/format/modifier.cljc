(ns com.yetanalytics.flint.format.modifier
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]))

(defmethod f/format-ast-node :mod/op [_ [_ op]]
  (name op))

(defmethod f/format-ast-node :mod/asc-desc [_ [_ [op sub-expr]]]
  (let [op-name (cstr/upper-case op)]
    (str op-name "(" sub-expr ")")))

(defmethod f/format-ast-node :mod/expr [_ [_ expr]]
  expr)

(defmethod f/format-ast-node :mod/expr-as-var [_ [_ expr-as-var]]
  (str "(" expr-as-var ")"))

(defmethod f/format-ast-node :group-by [_ [_ value]]
  (str "GROUP BY " (cstr/join " " value)))

(defmethod f/format-ast-node :order-by [_ [_ value]]
  (str "ORDER BY " (cstr/join " " value)))

(defmethod f/format-ast-node :having [_ [_ value]]
  (str "HAVING " (cstr/join " " value)))

(defmethod f/format-ast-node :limit [_ [_ value]]
  (str "LIMIT " value))

(defmethod f/format-ast-node :offset [_ [_ value]]
  (str "OFFSET " value))

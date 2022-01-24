(ns syrup.sparql.format.modifier
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]))

(defmethod f/format-ast :mod/op [[_ op]]
  (name op))

(defmethod f/format-ast :mod/asc-desc [[_ [op sub-expr]]]
  (let [op-name (cstr/upper-case op)]
    (str op-name "(" sub-expr ")")))

(defmethod f/format-ast :mod/expr [[_ expr]]
  expr)

(defmethod f/format-ast :mod/expr-as-var [[_ expr-as-var]]
  (str "(" expr-as-var ")"))

(defmethod f/format-ast :group-by [[_ value]]
  (str "GROUP BY " (cstr/join " " value)))

(defmethod f/format-ast :order-by [[_ value]]
  (str "ORDER BY " (cstr/join " " value)))

(defmethod f/format-ast :having [[_ value]]
  (str "HAVING " (cstr/join " " value)))

(defmethod f/format-ast :limit [[_ value]]
  (str "LIMIT " value))

(defmethod f/format-ast :offset [[_ value]]
  (str "OFFSET " value))

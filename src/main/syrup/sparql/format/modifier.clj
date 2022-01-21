(ns syrup.sparql.format.modifier
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]))

(defmethod f/format-ast :asc-desc [[_ {:keys [op sub-expr]}]]
  (if op ; has ASC/DESC
    (let [op-name (cstr/upper-case (name op))]
      (str op-name "(" sub-expr ")"))
    sub-expr))

(defmethod f/format-ast :expr [[_ expr]]
  (str "(" expr ")"))

(defmethod f/format-ast :expr-var [[_ expr-as-var]]
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

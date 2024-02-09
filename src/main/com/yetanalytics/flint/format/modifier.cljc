(ns com.yetanalytics.flint.format.modifier
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]))

(defmethod f/format-ast-node :mod/op [_ [_ op]]
  (str op))

(defmethod f/format-ast-node :mod/asc-desc [_ [_ [op sub-expr]]]
  (let [op-name (cstr/upper-case op)]
    (str op-name sub-expr)))

(defmethod f/format-ast-node :mod/asc-desc-expr [_ [_ expr]]
  (if (f/bracketted-expr-str? expr)
    expr
    (str "(" expr ")")))

(defmethod f/format-ast-node :mod/group-expr [_ [_ expr]]
  expr)

(defmethod f/format-ast-node :mod/order-expr [_ [_ expr]]
  (if (f/bracketted-or-fn-expr-str? expr)
    expr
    (str "(" expr ")")))

(defmethod f/format-ast-node :mod/expr-as-var [_ [_ expr-as-var]]
  (str "(" expr-as-var ")"))

(defmethod f/format-ast-node :group-by [_ [_ group-bys]]
  (str "GROUP BY " (cstr/join " " group-bys)))

(defmethod f/format-ast-node :order-by [_ [_ order-bys]]
  (str "ORDER BY " (cstr/join " " order-bys)))

(defmethod f/format-ast-node :having [_ [_ exprs]]
  (->> exprs
       (map (fn [e] (if (f/bracketted-or-fn-expr-str? e) e (str "(" e ")"))))
       (cstr/join " ")
       (str "HAVING ")))

(defmethod f/format-ast-node :limit [_ [_ limit-val]]
  (str "LIMIT " limit-val))

(defmethod f/format-ast-node :offset [_ [_ offset-val]]
  (str "OFFSET " offset-val))

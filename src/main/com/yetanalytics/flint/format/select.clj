(ns com.yetanalytics.flint.format.select
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]
            [com.yetanalytics.flint.format.expr]))

(defmethod f/format-ast :select/var-or-exprs [[_ value]]
  (cstr/join " " value))

(defmethod f/format-ast :select/expr-as-var [[_ expr-as-var]]
  (str "(" expr-as-var ")"))

(defmethod f/format-ast :select [[_ select]]
  (str "SELECT " select))

(defmethod f/format-ast :select-distinct [[_ select-distinct]]
  (str "SELECT DISTINCT " select-distinct))

(defmethod f/format-ast :select-reduced [[_ select-reduced]]
  (str "SELECT REDUCED " select-reduced))

(ns syrup.sparql.format.select
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]
            [syrup.sparql.format.expr]))

(defmethod f/format-ast :select/var-or-exprs [[_ value]]
  (cstr/join " " value))

(defmethod f/format-ast :select [[_ select]]
  (str "SELECT " select))

(defmethod f/format-ast :select-distinct [[_ select-distinct]]
  (str "SELECT DISTINCT " select-distinct))

(defmethod f/format-ast :select-reduced [[_ select-reduced]]
  (str "SELECT REDUCED " select-reduced))

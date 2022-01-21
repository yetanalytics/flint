(ns syrup.sparql.format.prologue
  (:require [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]))

(defmethod f/format-ast :base [[_ value]]
  (str "BASE " value))

(defmethod f/format-ast :prefix [[_ [prefix iri]]]
  (let [prefix-name (if (= :$ prefix) "" (name prefix))]
    (str "PREFIX " prefix-name ": " iri)))

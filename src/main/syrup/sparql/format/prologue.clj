(ns syrup.sparql.format.prologue
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]))

(defmethod f/format-ast :base [[_ value]]
  (str "BASE " value))

(defmethod f/format-ast :bases [[_ bases]]
  (cstr/join "\n" bases))

(defmethod f/format-ast :prefix [[_ [prefix iri]]]
  (let [prefix-name (if (= :$ prefix) "" (name prefix))]
    (str "PREFIX " prefix-name ": " iri)))

(defmethod f/format-ast :prefixes [[_ prefixes]]
  (cstr/join "\n" prefixes))

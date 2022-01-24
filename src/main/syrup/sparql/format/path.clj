(ns syrup.sparql.format.path
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]))

(defmethod f/format-ast :path/op [[_ op]] (keyword op))

(defmethod f/format-ast :path/args [[_ args]] args)

(defmethod f/format-ast :path/branch [[_ [op args]]]
  (case op
    :alt (str "(" (cstr/join " | " args) ")")
    :cat (str "(" (cstr/join " / " args) ")")
    :inv (str "^" (first args))
    :?   (str (first args) "?")
    :*   (str (first args) "*")
    :+   (str (first args) "+")
    :not (str "!" (cstr/join " | " args))))

(defmethod f/format-ast :path/terminal [[_ value]]
  value)

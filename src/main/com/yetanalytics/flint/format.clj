(ns com.yetanalytics.flint.format
  (:require [clojure.string :as cstr]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ast-node?
  "Is the node an AST node (created via `s/conform`)?"
  [x]
  (and (vector? x)
       (= 2 (count x))
       (keyword? (first x))))

(defn dispatch-ast-node
  "Dispatch on the AST node of the form `[keyword value]`."
  [ast-node]
  (if (ast-node? ast-node)
    (first ast-node)
    :default))

(defn indent-str
  "Add 4 spaces after each line break (including at the beginning)."
  [s]
  (str "    " (cstr/replace s #"\n" "\n    ")))

(defn wrap-in-braces
  "Wrap the `clause` string in curly braces. If `pretty?` is true,
   also add line breaks and indent `clause`."
  [clause pretty?]
  (if pretty?
    (str "{\n" (indent-str clause) "\n}")
    (str "{ " clause " }")))

(defn join-clauses
  "Join the `clauses` coll. If `pretty?` is true, separate by line
   breaks; otherwise separate by space."
  [clauses pretty?]
  (if pretty?
    (cstr/join "\n" clauses)
    (cstr/join " " clauses )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Multimethods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti format-ast
  "Convert the AST node into a string."
  (fn [_ x] (dispatch-ast-node x)))

(defmethod format-ast :default [_ ast-node] ast-node)

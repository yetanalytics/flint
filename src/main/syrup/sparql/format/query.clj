(ns syrup.sparql.format.query
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]
            [syrup.sparql.format.expr]
            [syrup.sparql.format.prologue]
            [syrup.sparql.format.triple]
            [syrup.sparql.format.modifier]
            [syrup.sparql.format.select]
            [syrup.sparql.format.where]))

(defmethod f/format-ast :construct [[_ construct]]
  (if (not-empty construct)
    (str "CONSTRUCT " construct)
    "CONSTRUCT"))

(defmethod f/format-ast :describe [[_ describe]]
  (str "DESCRIBE " (cstr/join " " describe)))

(defmethod f/format-ast :ask [_]
  "ASK")

(defmethod f/format-ast :where [[_ where]]
  (str "WHERE " where))

(defmethod f/format-ast :from [[_ iri]]
  (str "FROM " iri))

(defmethod f/format-ast :from-named [[_ iri-coll]]
  (->> iri-coll
       (map (fn [iri] (str "FROM NAMED " iri)))
       (cstr/join "\n")))

(defn format-query [query]
  (cstr/join "\n" query))

(defmethod f/format-ast :select-query [[_ select-query]]
  (format-query select-query))

(defmethod f/format-ast :construct-query [[_ construct-query]]
  (format-query construct-query))

(defmethod f/format-ast :describe-query [[_ describe-query]]
  (format-query describe-query))

(defmethod f/format-ast :ask-query [[_ ask-query]]
  (format-query ask-query))

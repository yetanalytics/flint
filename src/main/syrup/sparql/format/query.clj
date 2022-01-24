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

(defn format-select-query [select-query]
  (cstr/join "\n" select-query))

(defmethod f/format-ast :select-query [[_ select-query]]
  (format-select-query select-query))

(defn format-construct-query [construct-query]
  (cstr/join "\n" construct-query)
  #_(let [pro (subvec construct-query 0 2)
        rst (subvec construct-query 2)]
    (if (and (= "CONSTRUCT" (first rst))
             (re-matches #"WHERE.*" (second rst)))
      (let [rst* (into [(str (first rst) (second rst))]
                       (subvec rst 2))]
        (cstr/join "\n" (concat pro rst*)))
      (cstr/join "\n" construct-query))))

(defmethod f/format-ast :construct-query [[_ construct-query]]
  (format-construct-query construct-query))

(defn format-describe-query [describe-query]
  (cstr/join "\n" describe-query))

(defmethod f/format-ast :describe-query [[_ describe-query]]
  (format-describe-query describe-query))

(defn format-ask-query [ask-query]
  (cstr/join "\n" ask-query)
  #_(let [pro (subvec ask-query 0 2)
        rst (subvec ask-query 2)]
    (if (re-matches #"WHERE.*" (second rst))
      (let [rst* (into [(str (first rst) (second rst))])]
        (cstr/join "\n" (concat pro rst*)))
      (cstr/join "\n" ask-query))))

(defmethod f/format-ast :ask-query [[_ ask-query]]
  (format-ask-query ask-query))

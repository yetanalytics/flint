(ns syrup.sparql.format.where
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]
            [syrup.sparql.format.expr]
            [syrup.sparql.format.triple]
            [syrup.sparql.format.modifier]
            [syrup.sparql.format.values]))

(defn format-select-query
  [{:keys [select select-distinct select-reduced where]}]
  (let [select-clause (or select
                          select-distinct
                          select-reduced)
        select-clause-str (cond-> "SELECT "
                            select-distinct (str " DISTINCT")
                            select-reduced (str " REDUCED")
                            true (str select-clause))]
    (->> [select-clause-str
          where
          #_(modifier-str select-query-m)]
         (filter some?)
         (cstr/join "\n"))))

(defmethod f/format-ast :where-sub/select [[_ sub-select]]
  (str "{\n" (f/indent-str (format-select-query sub-select)) "\n}"))

(defmethod f/format-ast :where-sub/where [[_ sub-where]]
  (str "{\n" (f/indent-str (cstr/join "\n" sub-where)) "\n}"))

(defmethod f/format-ast :where-sub/empty [_]
  "{}")

(defmethod f/format-ast :where/recurse [[_ pattern]]
  pattern)

(defmethod f/format-ast :where/union [[_ patterns]]
  (cstr/join " UNION " patterns))

(defmethod f/format-ast :where/optional [[_ pattern]]
  (str "OPTIONAL " pattern))

(defmethod f/format-ast :where/minus [[_ pattern]]
  (str "MINUS " pattern))

(defmethod f/format-ast :where/graph [[_ [iri pattern]]]
  (str "GRAPH " iri " " pattern))

(defmethod f/format-ast :where/service [[_ [iri pattern]]]
  (str "SERVICE " iri " " pattern))

(defmethod f/format-ast :where/service-silent [[_ [iri pattern]]]
  (str "SERVICE SILENT " iri " " pattern))

(defmethod f/format-ast :where/filter [[_ expr]]
  (str "FILTER " expr)
  #_(if (re-matches #"[\w\:]+\(.*\)" expr)
    (str "FILTER " expr)
    (str "FILTER (" expr ")")))

(defmethod f/format-ast :where/bind [[_ expr-as-var]]
  (str "BIND (" expr-as-var ")"))

(defmethod f/format-ast :where/values [[_ values]]
  (str "VALUES " values))

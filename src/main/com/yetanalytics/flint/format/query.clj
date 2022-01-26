(ns com.yetanalytics.flint.format.query
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]
            [com.yetanalytics.flint.format.prologue]
            [com.yetanalytics.flint.format.triple]
            [com.yetanalytics.flint.format.modifier]
            [com.yetanalytics.flint.format.select]
            [com.yetanalytics.flint.format.where]
            [com.yetanalytics.flint.format.values]))

(defmethod f/format-ast :construct [[_ construct]]
  (if (not-empty construct)
    (str "CONSTRUCT {\n" (f/indent-str (cstr/join "\n" construct)) "\n}")
    "CONSTRUCT"))

(defmethod f/format-ast :describe/vars-or-iris [[_ var-or-iris]]
  (cstr/join " " var-or-iris))

(defmethod f/format-ast :describe [[_ describe]]
  (str "DESCRIBE " describe))

(defmethod f/format-ast :ask [_]
  "ASK")

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

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

(defmethod f/format-ast :construct [{:keys [pretty?]} [_ construct]]
  (if (not-empty construct)
    (if pretty?
      (str "CONSTRUCT {\n" (f/indent-str (cstr/join "\n" construct)) "\n}")
      (str "CONSTRUCT { " (cstr/join " " construct) " }"))
    "CONSTRUCT"))

(defmethod f/format-ast :describe/vars-or-iris [_ [_ var-or-iris]]
  (cstr/join " " var-or-iris))

(defmethod f/format-ast :describe [_ [_ describe]]
  (str "DESCRIBE " describe))

(defmethod f/format-ast :ask [_ _]
  "ASK")

(defmethod f/format-ast :from [_ [_ iri]]
  (str "FROM " iri))

(defmethod f/format-ast :from-named [{:keys [pretty?]} [_ iri-coll]]
  (let [join-sep (if pretty? "\n" " ")]
    (->> iri-coll
         (map (fn [iri] (str "FROM NAMED " iri)))
         (cstr/join join-sep))))

(defn format-query [query pretty?]
  (if pretty?
    (cstr/join "\n" query)
    (cstr/join " " query)))

(defmethod f/format-ast :select-query [{:keys [pretty?]} [_ select-query]]
  (format-query select-query pretty?))

(defmethod f/format-ast :construct-query [{:keys [pretty?]} [_ construct-query]]
  (format-query construct-query pretty?))

(defmethod f/format-ast :describe-query [{:keys [pretty?]} [_ describe-query]]
  (format-query describe-query pretty?))

(defmethod f/format-ast :ask-query [{:keys [pretty?]} [_ ask-query]]
  (format-query ask-query pretty?))

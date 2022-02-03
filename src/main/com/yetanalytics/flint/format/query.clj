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

(defmethod f/format-ast-node :construct [{:keys [pretty?]} [_ construct]]
  (if (not-empty construct)
    (str "CONSTRUCT " (-> construct
                          (f/join-clauses pretty?)
                          (f/wrap-in-braces pretty?)))
    "CONSTRUCT"))

(defmethod f/format-ast-node :describe/vars-or-iris [_ [_ var-or-iris]]
  (cstr/join " " var-or-iris))

(defmethod f/format-ast-node :describe [_ [_ describe]]
  (str "DESCRIBE " describe))

(defmethod f/format-ast-node :ask [_ _]
  "ASK")

(defmethod f/format-ast-node :from [_ [_ iri]]
  (str "FROM " iri))

(defmethod f/format-ast-node :from-named [{:keys [pretty?]} [_ iri-coll]]
  (-> (map (fn [iri] (str "FROM NAMED " iri)) iri-coll)
      (f/join-clauses pretty?)))

(defmethod f/format-ast-node :select-query [{:keys [pretty?]} [_ select-query]]
  (f/join-clauses select-query pretty?))

(defmethod f/format-ast-node :construct-query [{:keys [pretty?]} [_ construct-query]]
  (f/join-clauses construct-query pretty?))

(defmethod f/format-ast-node :describe-query [{:keys [pretty?]} [_ describe-query]]
  (f/join-clauses describe-query pretty?))

(defmethod f/format-ast-node :ask-query [{:keys [pretty?]} [_ ask-query]]
  (f/join-clauses ask-query pretty?))

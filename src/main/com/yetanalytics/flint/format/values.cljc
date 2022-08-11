(ns com.yetanalytics.flint.format.values
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]))

(defmulti format-values-clause
  (fn [vars _ _] (if (= 1 (count vars)) :values/single :values/default)))

(defmethod format-values-clause :values/single
  [var values pretty?]
  (let [kstr  (first var)
        vstrs (map first values)
        vstr  (-> vstrs
                  (f/join-clauses pretty?)
                  (f/wrap-in-braces pretty?))]
    (str kstr " " vstr)))

(defmethod format-values-clause :values/default
  [vars values pretty?]
  (let [kstr  (str "(" (cstr/join " " vars) ")")
        vstrs (map #(str "(" (cstr/join " " %) ")") values)
        vstr  (-> vstrs
                 (f/join-clauses pretty?)
                 (f/wrap-in-braces pretty?))]
    (str kstr " " vstr)))

(defmethod f/format-ast-node :values/undef [_ _]
  "UNDEF")

(defmethod f/format-ast-node :values/map [{:keys [pretty?]} [_ [vars values]]]
  (format-values-clause vars values pretty?))

(defmethod f/format-ast-node :values [_ [_ values-map]]
  (str "VALUES " values-map))

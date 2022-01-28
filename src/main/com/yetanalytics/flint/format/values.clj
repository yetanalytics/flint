(ns com.yetanalytics.flint.format.values
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]))

(defmulti format-values-clause
  (fn [vars _] (if (= 1 (count vars)) :values/single :values/default)))

(defmethod format-values-clause :values/single
  [var values]
  (let [kstr  (first var)
        vstrs (map (fn [v-tuple] (str "    " (first v-tuple))) values)
        vstr  (str "{\n" (cstr/join "\n" vstrs) "\n}")]
    (str kstr " " vstr)))

(defmethod format-values-clause :values/default
  [vars values]
  (let [kstr  (str "(" (cstr/join " " vars) ")")
        vstrs (map (fn [v-tuple] (str "    (" (cstr/join " " v-tuple) ")"))
                   values)
        vstr  (str "{\n" (cstr/join "\n" vstrs) "\n}")]
    (str kstr " " vstr)))

(defmethod f/format-ast :values/undef [_ _]
  "UNDEF")

(defmethod f/format-ast :values/map [_ [_ [vars values]]]
  (format-values-clause vars values))

(defmethod f/format-ast :values [_ [_ values-map]]
  (str "VALUES " values-map))

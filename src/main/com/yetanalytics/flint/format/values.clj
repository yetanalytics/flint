(ns com.yetanalytics.flint.format.values
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]))

(defmulti format-values-clause
  (fn [vars _ _] (if (= 1 (count vars)) :values/single :values/default)))

(defmethod format-values-clause :values/single
  [var values pretty?]
  (let [kstr (first var)]
    (if pretty?
      (let [vstrs (map (fn [v-tuple] (str "    " (first v-tuple))) values)
            vstr  (str "{\n" (cstr/join "\n" vstrs) "\n}")]
        (str kstr " " vstr))
      (let [vstrs (map (fn [v-tuple] (first v-tuple)) values)
            vstr  (str "{ " (cstr/join " " vstrs) " }")]
        (str kstr " " vstr)))))

(defmethod format-values-clause :values/default
  [vars values pretty?]
  (let [kstr (str "(" (cstr/join " " vars) ")")]
    (if pretty?
      (let [vstrs (map (fn [v-tuple] (str "    (" (cstr/join " " v-tuple) ")"))
                       values)
            vstr  (str "{\n" (cstr/join "\n" vstrs) "\n}")]
        (str kstr " " vstr))
      (let [vstrs (map (fn [v-tuple] (str "(" (cstr/join " " v-tuple) ")"))
                       values)
            vstr  (str "{ " (cstr/join " " vstrs) " }")]
        (str kstr " " vstr)))))

(defmethod f/format-ast :values/undef [_ _]
  "UNDEF")

(defmethod f/format-ast :values/map [{:keys [pretty?]} [_ [vars values]]]
  (format-values-clause vars values pretty?))

(defmethod f/format-ast :values [_ [_ values-map]]
  (str "VALUES " values-map))

(ns syrup.sparql.format.values
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]))

(defmulti format-values-clause*
  (fn [vars _] (if (= 1 (count vars)) :single :default)))

(defmethod format-values-clause* :single
  [var values]
  (let [kstr  (first var)
        vstrs (map (fn [v-tuple] (str "  " (first v-tuple))) values)
        vstr  (str "{\n" (cstr/join "\n" vstrs) "\n}")]
    (str kstr " " vstr)))

(defmethod format-values-clause* :default
  [vars values]
  (let [kstr  (str "(" (cstr/join " " vars) ")")
        vstrs (map (fn [v-tuple] (str "  (" (cstr/join " " v-tuple) ")"))
                   values)
        vstr  (str "{\n" (cstr/join "\n" vstrs) "\n}")]
    (str kstr " " vstr)))

(defn format-values-clause
  [values-clause-m]
  (let [ks (first (keys values-clause-m))
        vs (first (vals values-clause-m))]
    (format-values-clause* ks vs)))

(defmethod f/format-ast :undef [_]
  "UNDEF")

(defmethod f/format-ast :values-map [[_ values]]
  (format-values-clause values))

(comment
  (println
   (format-values-clause
    {["?foo"] [["1"] ["2"] ["3"]]}))
  (println
   (format-values-clause
    {["?foo" "?bar"] [["1" "a"] ["2" "b"] ["3" "c"]]})))

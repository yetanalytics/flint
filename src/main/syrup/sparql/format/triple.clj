(ns syrup.sparql.format.triple
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]
            [syrup.sparql.format.path]))

(defmethod f/format-ast :triple/path [[_ path]]
  path)

(defmethod f/format-ast :tvec [[_ [s p o]]]
  (str s " " p " " o " ."))

(defmethod f/format-ast :nform [[_ nform]]
  nform)

(defmethod f/format-ast :spo [[_ spo]]
  (str (->> spo
            (map (fn [[s po]]
                   (let [indent (->> (repeat (inc (count s)) " ")
                                     (cstr/join "")
                                     (str "\n"))]
                     (str s " " (cstr/replace po #"\n" indent)))))
            (cstr/join " .\n"))
       " ."))

(defmethod f/format-ast :po [[_ po]]
  (->> po
       (map (fn [[p o]] (str p " " o)))
       (cstr/join " ;\n")))

(defmethod f/format-ast :o [[_ o]]
  (->> o (cstr/join " , ")))

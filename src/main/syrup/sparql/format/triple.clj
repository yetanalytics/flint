(ns syrup.sparql.format.triple
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]
            [syrup.sparql.format.path]))

(defmethod f/format-ast :tvec [[_ [s p o]]]
  (str s " " p " " o " ."))

(defmethod f/format-ast :nform [[_ nform]]
  nform)

(defn- break-s-po?
  [s po]
  (let [s-len  (count s)
        po-len (count po)]
    (< 80 (+ s-len po-len 2))))

(defmethod f/format-ast :spo [[_ spo]]
  (->> spo
       (map (fn [[s po]]
              (if (break-s-po? s po) (str s "\n    " po) (str s " " po))))
       (cstr/join " .\n")))

(defmethod f/format-ast :po [[_ po]]
  (->> po
       (map (fn [[p o]] (str p " " o)))
       (cstr/join "; ")))

(defmethod f/format-ast :o [[_ o]]
  (->> o (cstr/join ", ")))

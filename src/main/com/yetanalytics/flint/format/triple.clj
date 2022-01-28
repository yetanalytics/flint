(ns com.yetanalytics.flint.format.triple
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]
            [com.yetanalytics.flint.format.path]))

(defmethod f/format-ast :triple/path [_ [_ path]]
  path)

(defmethod f/format-ast :triple/vec [_ [_ [s p o]]]
  (str s " " p " " o " ."))

(defmethod f/format-ast :triple/nform [_ [_ nform]]
  nform)

(defmethod f/format-ast :spo [_ [_ spo]]
  (str (->> spo
            (map (fn [[s po]]
                   (let [indent (->> (repeat (inc (count s)) " ")
                                     (cstr/join "")
                                     (str "\n"))]
                     (str s " " (cstr/replace po #"\n" indent)))))
            (cstr/join " .\n"))
       " ."))

(defmethod f/format-ast :po [_ [_ po]]
  (->> po
       (map (fn [[p o]] (str p " " o)))
       (cstr/join " ;\n")))

(defmethod f/format-ast :o [_ [_ o]]
  (->> o (cstr/join " , ")))

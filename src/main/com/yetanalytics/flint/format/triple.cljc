(ns com.yetanalytics.flint.format.triple
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]
            [com.yetanalytics.flint.format.path]))

(defmethod f/format-ast-node :triple/path [_ [_ path]]
  path)

(defmethod f/format-ast-node :triple/vec [_ [_ [s p o]]]
  (str s " " p " " o " ."))

(defmethod f/format-ast-node :triple/nform [_ [_ nform]]
  nform)

(defmethod f/format-ast-node :triple/spo [{:keys [pretty?]} [_ spo]]
  (if pretty?
    (str (->> spo
              (map (fn [[s po]]
                     (let [indent (->> (repeat (inc (count s)) " ")
                                       (cstr/join "")
                                       (str "\n"))]
                       (str s " " (cstr/replace po #"\n" indent)))))
              (cstr/join " .\n"))
         " .")
    (str (->> spo
              (map (fn [[s po]] (str s " " po)))
              (cstr/join " . "))
         " .")))

(defmethod f/format-ast-node :triple/po [{:keys [pretty?]} [_ po]]
  (let [join-sep (if pretty? " ;\n" " ; ")]
    (->> po
         (map (fn [[p o]] (str p " " o)))
         (cstr/join join-sep))))

(defmethod f/format-ast-node :triple/o [_ [_ o]]
  (->> o (cstr/join " , ")))

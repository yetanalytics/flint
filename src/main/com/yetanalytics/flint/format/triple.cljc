(ns com.yetanalytics.flint.format.triple
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]
            [com.yetanalytics.flint.format.path]))

(defmethod f/format-ast-node :triple/list [_ [_ list]]
  (str "( " (cstr/join " " list) " )"))

(defmethod f/format-ast-node :triple/path [_ [_ path]]
  path)

(defmethod f/format-ast-node :triple/vec [_ [_ [s p o]]]
  (str s " " p " " o " ."))

(defmethod f/format-ast-node :triple/vec-no-po [_ [_ [s]]]
  (str s " ."))

(defn- format-spo-map [spo pretty?]
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

(defmethod f/format-ast-node :triple.nform/spo [{:keys [pretty?]} [_ spo]]
  (format-spo-map spo pretty?))

(defmethod f/format-ast-node :triple.nform/s [{:keys [pretty?]} [_ spo]]
  (format-spo-map spo pretty?))

(defmethod f/format-ast-node :triple.nform/po [{:keys [pretty?]} [_ po]]
  (let [join-sep (if pretty? " ;\n" " ; ")]
    (->> po
         (map (fn [[p o]] (str p " " o)))
         (cstr/join join-sep))))

(defmethod f/format-ast-node :triple.nform/o [_ [_ o]]
  (->> o (cstr/join " , ")))

(defn format-quads [quads pretty?]
  (-> quads
      (f/join-clauses pretty?)
      (f/wrap-in-braces pretty?)))

(defmethod f/format-ast-node :triple/quads
  [_ [_ [var-or-iri triples-str]]]
  (str "GRAPH " var-or-iri " " triples-str))

(defmethod f/format-ast-node :triple/quad-triples
  [{:keys [pretty?]} [_ triples]]
  (format-quads triples pretty?))

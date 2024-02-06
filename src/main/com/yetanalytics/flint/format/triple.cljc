(ns com.yetanalytics.flint.format.triple
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]
            [com.yetanalytics.flint.format.path]))

(defmethod f/format-ast-node :triple/path [_ [_ path]]
  path)

(defmethod f/format-ast-node :triple/list [_ [_ list]]
  (str "( " (cstr/join " " list) " )"))

(defmethod f/format-ast-node :triple/bnodes [{:keys [pretty?]} [_ po-pairs]]
  (if (empty? po-pairs)
    "[]" ; Treat as a scalar blank node
    (let [join-sep (if pretty? " ;\n  " " ; ")
          po-strs  (mapv (fn [[p-str o-str]] (str p-str " " o-str)) po-pairs)]
      (str "[ " (cstr/join join-sep po-strs) " ]"))))

(defmethod f/format-ast-node :triple.vec/spo [_ [_ [s-str p-str o-str]]]
  (str s-str " " p-str " " o-str " ."))

(defmethod f/format-ast-node :triple.vec/s [_ [_ [s-str]]]
  (str s-str " ."))

(defmethod f/format-ast-node :triple.nform/spo [{:keys [pretty?]} [_ spo-pairs]]
  (if pretty?
    (str (->> spo-pairs
              (map (fn [[s-str po-str]]
                     (let [indent (->> (repeat (inc (count s-str)) " ")
                                       (cstr/join "")
                                       (str "\n"))]
                       (str s-str " " (cstr/replace po-str #"\n" indent)))))
              (cstr/join " .\n"))
         " .")
    (str (->> spo-pairs
              (map (fn [[s-str po-str]] (str s-str " " po-str)))
              (cstr/join " . "))
         " .")))

(defmethod f/format-ast-node :triple.nform/s [{:keys [pretty?]} [_ s-pairs]]
  (let [join-sep (if pretty? " .\n" " . ")]
    (str (->> s-pairs (map (fn [[s-str _]] s-str)) (cstr/join join-sep))
         " .")))

(defmethod f/format-ast-node :triple.nform/po [{:keys [pretty?]} [_ po-strs]]
  (let [join-sep (if pretty? " ;\n" " ; ")]
    (->> po-strs
         (map (fn [[p o]] (str p " " o)))
         (cstr/join join-sep))))

(defmethod f/format-ast-node :triple.nform/o [_ [_ o-strs]]
  (->> o-strs (cstr/join " , ")))

(defn format-quads [quads pretty?]
  (-> quads
      (f/join-clauses pretty?)
      (f/wrap-in-braces pretty?)))

(defmethod f/format-ast-node :triple.quad/gspo
  [_ [_ [graph-str spo-str]]]
  (str "GRAPH " graph-str " " spo-str))

(defmethod f/format-ast-node :triple.quad/spo
  [{:keys [pretty?]} [_ spo-strs]]
  (format-quads spo-strs pretty?))

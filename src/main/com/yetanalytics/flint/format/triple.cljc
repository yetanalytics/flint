(ns com.yetanalytics.flint.format.triple
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]
            [com.yetanalytics.flint.format.path]))

(defmethod f/format-ast-node :triple/path [_ [_ path]]
  path)

(defmethod f/format-ast-node :triple/list [_ [_ list]]
  (if (empty? list)
    "()" ; Special case for empty lists
    (str "( " (cstr/join " " list) " )")))

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

(defn- format-spo-pretty [s-str po-str]
  (let [indent (->> (repeat (inc (count s-str)) " ")
                    (cstr/join "")
                    (str "\n"))]
    (str s-str " " (cstr/replace po-str #"\n" indent))))

(defn- format-spo [s-str po-str]
  (str s-str " " po-str))

(defmethod f/format-ast-node :triple.nform/spo [{:keys [pretty?]} [_ spo-pairs]]
  (let [format-spo (if pretty? format-spo-pretty format-spo)
        join-sep   (if pretty? " .\n" " . ")]
    (str (->> spo-pairs
              (map (fn [[s-str po-str]]
                     (if (empty? po-str) s-str (format-spo s-str po-str))))
              (cstr/join join-sep))
         " .")))

(defmethod f/format-ast-node :triple.nform/po [{:keys [pretty?]} [_ po-strs]]
  (let [join-sep (if pretty? " ;\n" " ; ")]
    (->> po-strs
         (map (fn [[p o]] (str p " " o)))
         (cstr/join join-sep))))

(defmethod f/format-ast-node :triple.nform/po-empty [_ _]
  "")

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

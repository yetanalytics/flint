(ns com.yetanalytics.flint.format.prologue
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]))

(defmethod f/format-ast :base [_ [_ value]]
  (str "BASE " value))

(defmethod f/format-ast :bases [_ [_ bases]]
  (cstr/join "\n" bases))

(defmethod f/format-ast :prefix [_ [_ [prefix iri]]]
  (let [prefix-name (if (= :$ prefix) "" (name prefix))]
    (str "PREFIX " prefix-name ": " iri)))

(defn- align-prefixes
  [prefixes]
  (let [pre-groups  (map (fn [pre] (re-matches #"(PREFIX (.*)\:) \<.*\>" pre))
                         prefixes)
        pre-lens    (map (fn [grps] (count (get grps 2)))
                         pre-groups)
        longest-len (apply max pre-lens)
        paddings    (map (fn [len] (cstr/join "" (repeat (- longest-len len) " ")))
                         pre-lens)
        pre-strs    (map second pre-groups)]
    (map (fn [pre pre-str pad]
           (cstr/replace-first pre #"PREFIX[^\<\>]*\:" (str pre-str pad)))
         prefixes
         pre-strs
         paddings)))

(defmethod f/format-ast :prefixes [_ [_ prefixes]]
  (cstr/join "\n" (align-prefixes prefixes)))

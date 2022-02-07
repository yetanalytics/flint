(ns com.yetanalytics.flint.bnode
  (:require [clojure.set :as cset]
            [clojure.zip :as zip]))

(defn- ast-branch?
  [x]
  (and (vector? x)
       (= 2 (count x))
       (keyword? (first x))
       (not= "ax" (namespace (first x)))))

(defn- ast-zipper
  "Create a zipper out of the AST, where each AST node `[:keyword children]`
   is treated as a zipper branch."
  [ast]
  (zip/zipper ast-branch?
              (fn [[_ children]]
                (if (or (ast-branch? children)
                        (not (coll? children)))
                  [children]
                  children))
              identity
              ast))

(defn- get-bgp-bnodes
  [ast]
  (loop [loc         (ast-zipper ast)
         bgp-bnodes  []
         curr-bnodes #{}]
    (if-not (zip/end? loc)
      (let [ast-node (zip/node loc)]
        (cond
          ;; We have left the previous Basic Graph Pattern
          ;; Recall that a BGP consists of triples + FILTERs
          (and (zip/branch? loc)
               (#{:where/recurse :where/union :where/optional
                  :where/minus :where/graph :where/service
                  :where/service-silent :where/bind :where/values}
                (first ast-node)))
          (recur (zip/next loc)
                 (conj bgp-bnodes curr-bnodes)
                 #{})
          ;; We encountered a new blank node
          (and (vector? ast-node)
               (= :ax/bnode (first ast-node))
               (not= '_ (second ast-node)))
          (recur (zip/next loc)
                 bgp-bnodes
                 (conj curr-bnodes (second ast-node)))
          :else
          (recur (zip/next loc)
                 bgp-bnodes
                 curr-bnodes)))
      (conj bgp-bnodes curr-bnodes))))

(defn validate-bnodes
  ([ast]
   (validate-bnodes #{} ast))
  ([bnodes ast]
   (let [bgp-bnodes (get-bgp-bnodes ast)
         bnode-set  (apply cset/union bgp-bnodes)
         bnode-int  (cset/intersection bnodes bnode-set)]
     (if (not-empty bnode-int)
       ;; Used in a previous Update
       (->> bnode-int
            (map (fn [bnode] {:bnode bnode})))
       ;; Used in multiple Basic Graph Patterns
       (let [bgp-count-m (reduce (fn [m bgp]
                                   (reduce (fn [m bnode]
                                             (if (contains? m bnode)
                                               (update m bnode inc)
                                               (assoc m bnode 1)))
                                           m
                                           bgp))
                                 {}
                                 bgp-bnodes)]
         (->> bgp-count-m
              (filterv (fn [[_ n]] (< 1 n)))
              (map (fn [[bnode n]] {:bnode     bnode
                                    :bgp-count n}))))))))

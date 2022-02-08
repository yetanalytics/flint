(ns com.yetanalytics.flint.bnode
  (:require [clojure.set :as cset]
            [clojure.zip :as zip]))

(defn- ast-branch?
  [x]
  (and (vector? x)
       (= 2 (count x))
       (keyword? (first x))
       (not= "ax" (namespace (first x)))))

(defn- ast-children
  [[k children]]
  (cond
    (#{:triple/spo :triple/po} k)
    (apply concat children)
    (or (ast-branch? children)
        (not (coll? children)))
    [children]
    :else
    children))

(defn- ast-zipper
  "Create a zipper out of the AST, where each AST node `[:keyword children]`
   is treated as a zipper branch."
  [ast]
  (zip/zipper ast-branch?
              ast-children
              identity
              ast))

(def bgp-dividers
  #{:where/recurse :where/union :where/optional
    :where/minus :where/graph :where/service
    :where/service-silent :where/bind :where/values})

(defn get-bgps
  "Return a sequence of the BGPs of the AST."
  [ast]
  (loop [loc (ast-zipper ast)
         bgps []]
    (if-not (zip/end? loc)
      (let [ast-node (zip/node loc)]
        (if (and (zip/branch? loc)
                 (#{:where-sub/where} (first ast-node)))
          (let [new-bgps
                (->> loc
                     zip/children
                     (reduce (fn [acc child]
                               (if (and (vector? child)
                                        (bgp-dividers (first child)))
                                 ;; New BGP
                                 (conj acc [])
                                 ;; Add to previous BGP
                                 (conj (pop acc) (conj (peek acc) child))))
                             [[]])
                     (filter not-empty))]
            (recur (zip/next loc) (concat bgps new-bgps)))
          (recur (zip/next loc) bgps)))
      bgps)))

(defn- get-bnodes
  [bgp]
  (set (mapcat (fn [[kw t]]
                 (cond
                   (= :triple/vec kw)
                   (->> t
                        (filter #(and (= :ax/bnode (first %))
                                      (not= '_ (second %))))
                        (map second))
                   (= :triple/nform kw)
                   (loop [loc (ast-zipper t)
                          bnodes []]
                     (if-not (zip/end? loc)
                       (let [ast-node (zip/node loc)]
                         (if (and (vector? ast-node)
                                  (= :ax/bnode (first ast-node))
                                  (not= '_ (second ast-node)))
                           (recur (zip/next loc) (conj bnodes (second ast-node)))
                           (recur (zip/next loc) bnodes)))
                       bnodes))
                   :else
                   nil))
               bgp)))

(defn validate-bnodes
  ([ast]
   (validate-bnodes #{} ast))
  ([bnodes ast]
   (let [bgps        (get-bgps ast)
         bgp-bnodes  (map get-bnodes bgps)
         bnode-union (apply cset/union bgp-bnodes)]
     (if-some [reused-bnodes (-> (cset/intersection bnodes bnode-union)
                                 not-empty)]
       {:kind   ::update-request-bnode-error
        :bnodes (cset/union bnodes bnode-union)
        :errors (map (fn [bnode] {:bnode bnode}) reused-bnodes)}
       (let [bgp-count-m
             (reduce (fn [m bgp]
                       (reduce (fn [m bnode]
                                 (if (contains? m bnode)
                                   (update m bnode inc)
                                   (assoc m bnode 1)))
                               m
                               bgp))
                     {}
                     bgp-bnodes)]
         {:kind   ::bgp-bnode-error
          :bnodes (cset/union bnodes bnode-union)
          :errors (->> bgp-count-m
                       (filterv (fn [[_ n]] (< 1 n)))
                       (map (fn [[bnode n]] {:bnode     bnode
                                             :bgp-count n})))})))))

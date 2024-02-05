(ns com.yetanalytics.flint.validate.bnode
  (:require [clojure.set :as cset]
            [clojure.zip :as zip]))

(defn- get-parent-loc
  "Given a bnode's loc, return its parent (either a `:triple/vec`
   or `:triple/nform` node)."
  [loc]
  (let [penultimate (-> loc zip/path last first)]
    (case penultimate
      :triple/vec
      (-> loc    ; [:ax/bnode ...]
          zip/up ; [:triple/vec ...]
          )
      :triple/list
      (-> loc    ; [:ax/bnode ...]
          zip/up ; [:triple/list ...]
          zip/up ; [:triple/o ...]
          zip/up ; [:triple/po ...]
          zip/up ; [:triple/spo ...]
          zip/up ; [:triple/nform ...]
          )
      :triple/object
      (-> loc    ; [:ax/bnode ...]
          zip/up ; [:triple/object ...]
          zip/up ; [:triple/o ...]
          zip/up ; [:triple/po ...]
          zip/up ; [:triple/spo ...]
          zip/up ; [:triple/nform ...]
          )
      :triple/spo
      (-> loc    ; [:ax/bnode ...]
          zip/up ; [:triple/spo ...]
          zip/up ; [:triple/nform ...]
          )
      :triple/spo-list
      (-> loc    ; [:ax/bnode ...]
          zip/up ; [:triple/spo-list ...]
          zip/up ; [:triple/nform ...]
          )
      )))

(defn- bgp-divider?
  [ast-node]
  (and (-> ast-node (get-in [0]) (= :where/special))
       (-> ast-node (get-in [1 0]) (not= :where/filter))))

(defn- get-bgp-index
  "The BGP path is the regular zip loc path appended with an index that
   corresponds to that of the BGP in the WHERE vector. For example:
   
     [:triple/vec ...]       => 0
     [:triple/nform ...]     => 0
     [:where/special
      [:where/filter ...]]   => 0 ; FILTERs don't divide BGPs
     [:where/special
      [:where/optional ...]] => X ; BGP divider
     [:triple/nform ...]     => 1
   
   Note that this only works with locs that are immediate children
   of `:where-sub/where` nodes.
   "
  [loc]
  (let [lefts (zip/lefts loc)]
    (count (filter bgp-divider? lefts))))

(defn- get-where-index
  [pnode nnode]
  (let [indexed (map-indexed (fn [i x] [x i]) (second pnode))]
    (some (fn [[x i]] (when (= x nnode) i)) indexed)))

(defn- annotated-path
  "Create a coll of AST keywords where all `:where-sub/where`s are
   followed by indices, either the index in the WHERE vector or the
   index of the BGP (for the very last one)."
  [loc]
  (let [parent-loc (get-parent-loc loc)]
    (loop [zip-path (zip/path parent-loc)
           res-path []]
      (let [?pnode (first zip-path)
            ?nnode (second zip-path)]
        (cond
          (and ?pnode
               ?nnode
               (= :where-sub/where (first ?pnode)))
          (recur (rest zip-path)
                 (conj res-path (first ?pnode) (get-where-index ?pnode ?nnode)))
          (and ?pnode
               (not ?nnode)
               (= :where-sub/where (first ?pnode)))
          (recur (rest zip-path)
                 (conj res-path (first ?pnode) (get-bgp-index parent-loc)))
          ?pnode
          (recur (rest zip-path)
                 (conj res-path (first ?pnode)))
          :else
          res-path)))))

(defn- invalid-bnode?
  "Is the blank node that is associated with `bgp-loc-m` invalid? It is
   if the map has more than one entry, indicating that the blank node is
   located across multiple BGPs."
  [bgp-loc-m]
  (< 1 (count bgp-loc-m)))

(defn- bnode-err-map
  [bnode loc]
  {:bnode bnode
   :path  (annotated-path loc)})

(defn- bnode-locs->err-map
  [bnode-locs]
  (mapcat (fn [[bnode locs]] (map (partial bnode-err-map bnode) locs))
          bnode-locs))

(defn validate-bnodes
  "Given the map `node-m` between nodes and zipper locs, validate that
   all bnodes satisfy the following conditions:
   
   - They cannot be duplicated in different Basic Graph Patterns (BGPs).
   - They cannot be duplicated across different Updates in a request.
  
   Returns a pair between the union of `prev-bnodes` and the bnodes in
   `node-m`, and a nilable error map."
  ([node-m]
   (validate-bnodes #{} node-m))
  ([prev-bnodes node-m]
   (let [bnode-bgp-m (-> (:ax/bnode node-m) (dissoc '_))
         new-bnodes  (set (keys bnode-bgp-m))
         bnode-union (cset/union prev-bnodes new-bnodes)]
     (if-some [bad-bnode-locs (->> bnode-bgp-m
                                   (keep (fn [[bnode bgp-loc-m]]
                                           (when (contains? prev-bnodes bnode)
                                             [bnode (apply concat (vals bgp-loc-m))])))
                                   not-empty)]
       [bnode-union
        {:kind        ::dupe-bnodes-update
         :errors      (bnode-locs->err-map bad-bnode-locs)
         :prev-bnodes prev-bnodes}]
       (if-some [bad-bnode-locs (->> bnode-bgp-m
                                     (keep (fn [[bnode bgp-loc-m]]
                                             (when (invalid-bnode? bgp-loc-m)
                                               [bnode (apply concat (vals bgp-loc-m))])))
                                     not-empty)]
         [bnode-union
          {:kind   ::dupe-bnodes-bgp
           :errors (bnode-locs->err-map bad-bnode-locs)}]
         [bnode-union nil])))))

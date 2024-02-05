(ns com.yetanalytics.flint.validate.bnode
  (:require [clojure.set :as cset]
            [com.yetanalytics.flint.validate.util :as vu]))

(defn- invalid-bnode?
  "Is the blank node that is associated with `bgp-loc-m` invalid? It is
   if the map has more than one entry, indicating that the blank node is
   located across multiple BGPs."
  [bgp-loc-m]
  (< 1 (count bgp-loc-m)))

(defn- bnode-err-map
  [bnode loc]
  {:bnode bnode
   :path  (vu/zip-path loc)})

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

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

(defn- valid-bnode-locs?
  "Given `locs`, return `false` if `bnode` is duplicated across multiple
   BGPs, `true` otherwise."
  [[bnode locs]]
  (if (<= (count locs) 1)
    true ; Can't have dupe bnodes if there's only one instance :p
    (let [loc-paths   (map (fn [loc] (mapv first (zip/path loc))) locs)
          [wh non-wh] (split-with #(some #{:where-sub/where} %) loc-paths)
          ?wheres     (not-empty wh)
          ?non-wheres (not-empty non-wh)]
      (cond
        ;; Blank nodes only exist in a non-WHERE clause (e.g. CONSTRUCT,
        ;; INSERT DATA, or INSERT). Since only one such clause may exist
        ;; in a Query or Update, and since each counts as a single BGP,
        ;; we are done.
        (and (not ?wheres)
             ?non-wheres)
        true
        ;; Blank nodes exist in both a WHERE and non-WHERE clause. Since
        ;; those automatically count as two different BGPs, we are done.
        (and ?wheres
             ?non-wheres)
        false
        ;; Blank nodes only exist in WHERE clauses. They may all be in one
        ;; or more BGP, so we need to investigate further.
        (and ?wheres
             (not ?non-wheres))
        (let [bgp-paths (map annotated-path locs)]
          (apply = bgp-paths))
        :else
        (throw (ex-info "Blank nodes located in invalid locations!"
                        {:kind     ::invalid-bnode-loc
                         :bnode    bnode
                         :zip-locs locs}))))))

(defn- bnode-err-map
  [bnode loc]
  {:bnode bnode
   ;; Rather wasteful to call `annotated-path` twice, but this only
   ;; occurs during exn throwing so performance isn't a priority. 
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
   (let [bnode-locs  (->> (:ax/bnode node-m)
                          (filter (fn [[bnode _]] (not= '_ bnode))))
         new-bnodes  (set (keys bnode-locs))
         bnode-union (cset/union prev-bnodes new-bnodes)]
     (if-some [bad-bnode-locs (->> bnode-locs
                                   (filter (comp prev-bnodes first))
                                   not-empty)]
       [bnode-union
        {:kind        ::dupe-bnodes-update
         :errors      (bnode-locs->err-map bad-bnode-locs)
         :prev-bnodes prev-bnodes}]
       (if-some [bad-bnode-locs (->> bnode-locs
                                     (filter (comp not valid-bnode-locs?))
                                     not-empty)]
         [bnode-union
          {:kind   ::dupe-bnodes-bgp
           :errors (bnode-locs->err-map bad-bnode-locs)}]
         [bnode-union nil])))))

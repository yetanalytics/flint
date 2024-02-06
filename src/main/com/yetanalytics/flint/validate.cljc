(ns com.yetanalytics.flint.validate
  (:require [clojure.zip :as zip]
            [com.yetanalytics.flint.spec.expr :as es]
            [com.yetanalytics.flint.util :as u]))

(defn- bgp-divider?
  "Is `ast-node` a divider between BGPs? BGPs are divided by `:where/special`
   clauses, with the exception of filters (unless they themselves have a
   subquery):
   
     [:where/triple
      [:triple/vec ...]]     => 0
     [:where/triple
      [:triple.nform/spo ...]]   => 0
     [:where/special
      [:where/filter ...]]   => 0 ; FILTERs don't divide BGPs
     [:where/special
      [:where/optional ...]] => X ; BGP divider
     [:where/triple
      [:triple.nform/spo ...]]   => 1
   "
  [loc]
  (let [?left-node (some-> loc zip/left zip/node)
        ?curr-node (some-> loc zip/node)]
    (or
     ;; BGPs are separated by special clauses, with the exception of FILTER...
     (and (some-> ?left-node (get-in [0]) (= :where/special))
          (some-> ?left-node (get-in [1 0]) (not= :where/filter)))
     ;; BGPs are also separated via nesting (which also separates BGPs at the
     ;; top level)
     (some-> ?curr-node (get-in [0]) (= :where-sub/where))
     ;; ...including the case of FILTER (NOT) EXISTS, which has its own BGP
     ;; as a subquery (c.f. MINUS)
     (and (some-> ?curr-node (get-in [0]) (= :expr/branch))
          (or (some-> ?curr-node (get-in [1 0]) (= [:expr/op 'exists]))
              (some-> ?curr-node (get-in [1 0]) (= [:expr/op 'not-exists])))))))

(def axiom-keys
  #{:ax/iri :ax/prefix-iri :ax/var :ax/bnode :ax/wildcard :ax/rdf-type
    :ax/str-lit :ax/lmap-list :ax/num-lit :ax/bool-lit :ax/dt-lit
    ;; Not actually axioms but still counted as AST terminals
    :distinct? :separator})

(defn- ast-branch?
  [x]
  (when-some [k (u/get-keyword x)]
    (not (axiom-keys k))))

(defn- ast-children
  [[k children]]
  (cond
    (#{:triple.nform/spo :triple.nform/po} k)
    (apply concat children)
    (or (and (vector? children)
             (keyword? (first children)))
        (not (coll? children)))
    [children]
    :else
    children))

(defn ast-zipper
  "Create a zipper out of the AST, where each AST node `[:keyword children]`
   is treated as a zipper branch."
  [ast]
  (zip/zipper ast-branch?
              ast-children
              identity
              ast))

(def node-keys
  #{;; foo:bar
    :ax/prefix-iri
    ;; _:b0
    :ax/bnode
    ;; BIND (expr AS var)
    :where/bind
    ;; SELECT ... (expr AS var) ...
    :select/expr-as-var})

(defn- get-select-clause-loc
  "Return the loc at a SELECT, SELECT DISTINCT, or SELECT REDUCED clause."
  [loc]
  (loop [loc loc]
    (when-not (nil? loc) ; Reached the top w/o finding a SELECT
      (let [ast-node (zip/node loc)
            k        (u/get-keyword ast-node)]
        (cond
          (#{:select :select-distinct :select-reduced} k)
          loc
          ;; These are the only other nodes where aggregate functions may
          ;; be contained in. Since that means it's not in a SELECT clause,
          ;; we can short circuit.
          (#{:order-by :having} k)
          nil
          :else
          (recur (zip/up loc)))))))

(defn collect-nodes
  [ast]
  (loop [loc     (ast-zipper ast)
         node-m  {}
         bgp-idx 0]
    (if-not (zip/end? loc)
      (let [ast-node (zip/node loc)
            bgp-idx* (cond-> bgp-idx (bgp-divider? loc) inc)]
        (if-some [k (u/get-keyword ast-node)]
          (cond
            ;; Prefixes, BIND (expr AS var), and SELECT ... (expr AS var) ...
            (#{:ax/prefix-iri :where/bind :select/expr-as-var} k)
            (let [v (second ast-node)]
              (recur (zip/next loc)
                     (update-in node-m [k v] conj loc)
                     bgp-idx*))
            ;; Blank nodes
            (#{:ax/bnode} k)
            (let [bnode     (second ast-node)
                  top-level (-> loc zip/path second first)
                  bgp-path  (cond-> [top-level]
                              (= :where top-level)
                              (conj bgp-idx*))]
              (recur (zip/next loc)
                     (update-in node-m [k bnode bgp-path] conj loc)
                     bgp-idx*))
            ;; SELECT with GROUP BY
            (#{:group-by} k)
            (let [select-loc (zip/up loc)
                  select     (zip/node select-loc)]
              (recur (zip/next loc)
                     (update node-m :agg/select assoc select select-loc)
                     bgp-idx*))
            ;; SELECT with an aggregate expression
            (#{:expr/branch} k)
            (let [op (-> ast-node ; [:expr/branch ...]
                         second   ; [[:expr/op ...] [:expr/args ...]]
                         first    ; [:expr/op ...]
                         second)]
              (if-some [select-cls-loc (and (or (not (symbol? op))
                                                (es/aggregate-ops op))
                                            (get-select-clause-loc loc))]
                (let [select-loc (zip/up select-cls-loc)
                      select     (zip/node select-loc) 
                      node-m*    (update node-m
                                         :agg/select
                                         assoc
                                         select
                                         select-loc)]
                  (recur (zip/next loc) node-m* bgp-idx*))
                (recur (zip/next loc) node-m bgp-idx*)))
            :else
            (recur (zip/next loc) node-m bgp-idx*))
          (recur (zip/next loc) node-m bgp-idx*)))
      node-m)))

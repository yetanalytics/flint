(ns com.yetanalytics.flint.validate
  (:require [clojure.zip :as zip]
            [com.yetanalytics.flint.spec.expr :as es]))

(def axiom-keys
  #{:ax/iri :ax/prefix-iri :ax/var :ax/bnode :ax/wildcard :ax/rdf-type :ax/nil
    :ax/str-lit :ax/lmap-list :ax/num-lit :ax/bool-lit :ax/dt-lit
    ;; Not actually axioms but still counted as AST terminals
    :distinct? :separator})

(defn- get-keyword
  [x]
  (when (vector? x)
    (let [fst (first x)]
      (when (keyword? fst)
        fst))))

(defn- ast-branch?
  [x]
  (when-some [k (get-keyword x)]
    (not (axiom-keys k))))

(defn- ast-children
  [[k children]]
  (cond
    (#{:triple/spo :triple/po} k)
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

(defn- get-agg-select-loc
  [loc]
  (loop [loc loc]
    (when-not (nil? loc) ; Reached the top w/o finding a SELECT
      (let [ast-node (zip/node loc)]
        (if (#{:query/select :where-sub/select} (get-keyword ast-node))
          loc
          (recur (zip/up loc)))))))

(defn collect-nodes
  [ast]
  (loop [loc    (ast-zipper ast)
         node-m {}]
    (if-not (zip/end? loc)
      (let [ast-node (zip/node loc)]
        (if-some [k (get-keyword ast-node)]
          (cond
            ;; Prefixes, blank nodes, and BIND clauses
            (node-keys k)
            (recur (zip/next loc)
                   (update-in node-m [k (second ast-node)] conj loc))
            ;; SELECT with GROUP BY
            (#{:group-by} k)
            (let [select-loc (zip/up loc)
                  select     (zip/node select-loc)]
              (recur (zip/next loc)
                     (update node-m :agg/select assoc select select-loc)))
            ;; SELECT with an aggregate expression
            (#{:expr/branch} k)
            (let [op (-> ast-node ; [:expr/branch ...]
                         second   ; [[:expr/op ...] [:expr/args ...]]
                         first    ; [:expr/op ...]
                         second)]
              (if (or (not (symbol? op))
                      (es/aggregate-ops op))
                (let [node-m* (if-some [select-loc (get-agg-select-loc loc)]
                                (update node-m
                                        :agg/select
                                        assoc
                                        (zip/node select-loc)
                                        select-loc)
                                node-m)]
                  (recur (zip/next loc) node-m*))
                (recur (zip/next loc) node-m)))
            :else
            (recur (zip/next loc) node-m))
          (recur (zip/next loc) node-m)))
      node-m)))

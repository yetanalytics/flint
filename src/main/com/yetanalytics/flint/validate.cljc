(ns com.yetanalytics.flint.validate
  (:require [clojure.zip :as zip]
            [com.yetanalytics.flint.spec.expr :as es]
            [com.yetanalytics.flint.util :as u]))

(def axiom-keys
  #{:ax/iri :ax/prefix-iri :ax/var :ax/bnode :ax/wildcard :ax/rdf-type :ax/nil
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
  (loop [loc    (ast-zipper ast)
         node-m {}]
    (if-not (zip/end? loc)
      (let [ast-node (zip/node loc)]
        (if-some [k (u/get-keyword ast-node)]
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
                  (recur (zip/next loc) node-m*))
                (recur (zip/next loc) node-m)))
            :else
            (recur (zip/next loc) node-m))
          (recur (zip/next loc) node-m)))
      node-m)))

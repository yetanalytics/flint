(ns com.yetanalytics.flint.validate
  (:require [clojure.zip :as zip]))

(def axiom-keys
  #{:ax/iri :ax/prefix-iri :ax/var :ax/bnode :ax/wildcard :ax/rdf-type :ax/nil
    :ax/str-lit :ax/lmap-list :ax/num-lit :ax/bool-lit :ax/dt-lit})

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

(defn collect-nodes
  [ast]
  (loop [loc    (ast-zipper ast)
         node-m {}]
    (if-not (zip/end? loc)
      (let [ast-node (zip/node loc)]
        (if-some [k (get-keyword ast-node)]
          (if (node-keys k)
            (recur (zip/next loc)
                   (update-in node-m [k (second ast-node)] conj loc))
            (recur (zip/next loc) node-m))
          (recur (zip/next loc) node-m)))
      node-m)))

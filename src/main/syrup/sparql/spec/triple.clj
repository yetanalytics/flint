(ns syrup.sparql.spec.triple
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as w]
            [clojure.string :as cstr]
            [syrup.sparql.spec.axiom :as ax]
            [syrup.sparql.spec.path :as path]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;; Defining nopath specs ;;;;;

(def ^:private nopath-spec-syms
  #{`pred-spec
    `pred-objs-spec
    `normal-form-spec
    `triple-vec-spec
    `triples-spec})

(defn- form->nopath-sym
  [sym]
  (if (and (symbol? sym) (nopath-spec-syms sym))
    (let [sym-ns (namespace sym)
          sym-name (name sym)]
      (symbol sym-ns
              (cstr/replace sym-name #"-spec" "-nopath-spec")))
    sym))

(defn- form->nopath-spec-form
  [form]
  (w/postwalk form->nopath-sym form))

(defn- spec->nopath-spec
  [spec]
  (-> spec s/form form->nopath-spec-form eval))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subj/Pred/Obj Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def subj-spec
  (s/or :var ax/variable?
        :iri ax/iri?
        :prefix-iri ax/prefix-iri?
        :bnode ax/bnode?))

(def obj-spec
  (s/or :var ax/variable?
        :iri ax/iri?
        :prefix-iri ax/prefix-iri?
        :bnode ax/bnode?
        :nil nil?
        :str-lit string?
        :num-lit number?
        :bool-lit boolean?
        :dt-lit inst?))

(def pred-spec
  (s/or :var ax/variable?
        :iri ax/iri?
        :prefix-iri ax/prefix-iri?
        :rdf-type ax/rdf-type?
        :path ::path/path))

(def pred-nopath-spec
  (s/or :var ax/variable?
        :iri ax/iri?
        :prefix-iri ax/prefix-iri?
        :rdf-type ax/rdf-type?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Combo Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def obj-set-spec
  (s/or :o (s/coll-of obj-spec
                      :min-count 1
                      :kind set?
                      :into [])))

(def pred-objs-spec
  (s/or :po (s/map-of pred-spec obj-set-spec
                      :min-count 1
                      :into [])))

(def pred-objs-nopath-spec
  (spec->nopath-spec pred-objs-spec))

(def normal-form-spec
  (s/or :spo (s/map-of subj-spec pred-objs-spec
                       :conform-keys true
                       :into [])))

(def normal-form-nopath-spec
  (spec->nopath-spec normal-form-spec))

(def triple-vec-spec
  (s/tuple subj-spec pred-spec obj-spec))

(def triple-vec-nopath-spec
  (spec->nopath-spec triple-vec-spec))

;; NOTE: Subjects can be non-IRIs in SPARQL, but not in RDF
;; NOTE: RDF collections not supported (yet?)

(comment
  ;; Hack to shut up `unused-public-var` warnings
  pred-objs-nopath-spec)

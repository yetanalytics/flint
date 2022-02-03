(ns com.yetanalytics.flint.spec.triple
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as w]
            [clojure.string :as cstr]
            [com.yetanalytics.flint.spec.axiom :as ax]
            [com.yetanalytics.flint.spec.path  :as ps]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;; Defining nopath specs ;;;;;

(defn- sym->new-sym
  [old-sym-re new-sym sym]
  (if (symbol? sym)
    (let [sym-ns (namespace sym)
          sym-name (name sym)]
      (symbol sym-ns
              (cstr/replace sym-name old-sym-re new-sym)))
    sym))

(defn- form->nopath-spec-form
  [form]
  (w/postwalk (partial sym->new-sym #"-spec" "-nopath-spec") form))

(defn- spec->nopath-spec
  [spec]
  (-> spec s/form form->nopath-spec-form eval))

(defn- form->novar-spec-form
  [form]
  (w/postwalk (partial sym->new-sym #"-spec" "-novar-spec") form))

(defn- spec->novar-spec
  [spec]
  (-> spec s/form form->novar-spec-form eval))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subj/Pred/Obj Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def subj-spec
  (s/or :ax/var        ax/variable?
        :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/bnode      ax/bnode?))

(def pred-spec
  (s/or :ax/var        ax/variable?
        :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/rdf-type   ax/rdf-type?
        :triple/path   ::ps/path))

(def obj-spec
  (s/or :ax/var        ax/variable?
        :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/bnode      ax/bnode?
        :ax/nil        nil?
        :ax/str-lit    ax/valid-string?
        :ax/lmap-lit   ax/lang-map?
        :ax/num-lit    number?
        :ax/bool-lit   boolean?
        :ax/dt-lit     inst?))

;; No property paths

(def subj-nopath-spec subj-spec)

(def pred-nopath-spec
  (s/or :ax/var        ax/variable?
        :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/rdf-type   ax/rdf-type?))

(def obj-nopath-spec obj-spec)

;; No variables (or bnodes or property paths)

(def subj-novar-spec
  (s/or :ax/iri ax/iri?
        :ax/prefix-iri ax/prefix-iri?))

(def pred-novar-spec
  (s/or :ax/iri ax/iri?
        :ax/prefix-iri ax/prefix-iri?))

(def obj-novar-spec
  (s/or :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/nil        nil?
        :ax/str-lit    ax/valid-string?
        :ax/lmap-lit   ax/lang-map?
        :ax/num-lit    number?
        :ax/bool-lit   boolean?
        :ax/dt-lit     inst?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Combo Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; NOTE: Subjects can be non-IRIs in SPARQL, but not in RDF
;; NOTE: RDF collections not supported (yet?)

;; single-branch `s/or`s are used to conform values

(def obj-set-spec
  (s/or :triple/o (s/coll-of obj-spec
                             :min-count 1
                             :kind set?
                             :into [])))

(def obj-set-nopath-spec
  obj-set-spec)

(def obj-set-novar-spec
  (spec->novar-spec obj-set-spec))

(def pred-objs-spec
  (s/or :triple/po (s/map-of pred-spec obj-set-spec
                             :min-count 1
                             :into [])))

(def pred-objs-nopath-spec
  (spec->nopath-spec pred-objs-spec))

(def pred-objs-novar-spec
  (spec->novar-spec pred-objs-spec))

(def normal-form-spec
  (s/or :triple/spo (s/map-of subj-spec pred-objs-spec
                              :conform-keys true
                              :into [])))

(def normal-form-nopath-spec
  (spec->nopath-spec normal-form-spec))

(def normal-form-novar-spec
  (spec->novar-spec normal-form-spec))

(def triple-vec-spec
  (s/tuple subj-spec pred-spec obj-spec))

(def triple-vec-nopath-spec
  (spec->nopath-spec triple-vec-spec))

(def triple-vec-novar-spec
  (spec->novar-spec triple-vec-spec))

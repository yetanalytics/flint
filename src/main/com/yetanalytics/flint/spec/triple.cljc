(ns com.yetanalytics.flint.spec.triple
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]
            [com.yetanalytics.flint.spec.path  :as ps])
  #?(:cljs (:require-macros [com.yetanalytics.flint.spec.triple
                             :refer [make-obj-spec
                                     make-pred-objs-spec
                                     make-nform-spec]])))

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

(def pred-nopath-spec
  (s/or :ax/var        ax/variable?
        :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/rdf-type   ax/rdf-type?))

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

;; Object

#?(:clj
   (defmacro make-obj-spec
     [obj-spec]
     `(s/or :triple/o (s/coll-of ~obj-spec
                                 :min-count 1
                                 :kind set?
                                 :into []))))

(def obj-set-spec
  (make-obj-spec obj-spec))

(def obj-set-novar-spec
  (make-obj-spec obj-novar-spec))

;; Predicate Object

#?(:clj
   (defmacro make-pred-objs-spec
     [pred-spec objs-spec]
     `(s/or :triple/po (s/map-of ~pred-spec ~objs-spec
                                 :min-count 1
                                 :into []))))

(def pred-objs-spec
  (make-pred-objs-spec pred-spec obj-set-spec))

(def pred-objs-nopath-spec
  (make-pred-objs-spec pred-nopath-spec obj-set-spec))

(def pred-objs-novar-spec
  (make-pred-objs-spec pred-novar-spec obj-set-novar-spec))

;; Subject Predicate Object

#?(:clj
   (defmacro make-nform-spec
     [subj-spec pred-objs-spec]
     `(s/or :triple/spo (s/map-of ~subj-spec ~pred-objs-spec
                                  :conform-keys true
                                  :into []))))

(def normal-form-spec
  (make-nform-spec subj-spec pred-objs-spec))

(def normal-form-nopath-spec
  (make-nform-spec subj-spec pred-objs-nopath-spec))

(def normal-form-novar-spec
  (make-nform-spec subj-novar-spec pred-objs-novar-spec))

;; Triple Vectors

(def triple-vec-spec
  (s/tuple subj-spec pred-spec obj-spec))

(def triple-vec-nopath-spec
  (s/tuple subj-spec pred-nopath-spec obj-spec))

(def triple-vec-novar-spec
  (s/tuple subj-novar-spec pred-novar-spec obj-novar-spec))

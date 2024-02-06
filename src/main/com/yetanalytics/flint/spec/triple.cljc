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

;; Just paths banned in
;; - CONSTRUCT

;; Variables (and paths) banned in
;; - DELETE DATA
;; - INSERT DATA

;; Blank nodes (and paths) banned in
;; - DELETE WHERE
;; - DELETE DATA
;; - DELETE

;; Subjects

(def subj-spec
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec))

(def subj-novar-spec
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec))

(def subj-noblank-spec
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec))

(def subj-novar-noblank-spec
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec))

;; Predicates

(def pred-spec
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/rdf-type   ax/rdf-type-spec
        :triple/path   ::ps/path))

(def pred-nopath-spec
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/rdf-type   ax/rdf-type-spec))

(def pred-novar-spec
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/rdf-type   ax/rdf-type-spec))

;; Objects

(def obj-spec
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :ax/literal    ax/literal-spec))

(def obj-novar-spec
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :ax/literal    ax/literal-spec))

(def obj-noblank-spec
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/literal    ax/literal-spec))

(def obj-novar-noblank-spec
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/literal    ax/literal-spec))

;; Lists

;; List entries have the same spec as objects + `:triple/list`

;; Since lists are constructed out of blank nodes, we do not allow list
;; syntactic sugar (i.e. `:triple/list`) where blank nodes are banned.

(declare list-spec)
(declare list-novar-spec)

(def list-entry-spec
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :ax/literal    ax/literal-spec
        :triple/list   list-spec))

(def list-entry-novar-spec
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :ax/literal    ax/literal-spec
        :triple/list   list-novar-spec))

(def list-spec
  (s/coll-of list-entry-spec :kind list?))

(def list-novar-spec
  (s/coll-of list-entry-novar-spec :kind list?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Combo Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; NOTE: Subjects can be non-IRIs in SPARQL, but not in RDF
;; single-branch `s/or`s are used to conform values

;; Object

#?(:clj
   (defmacro make-obj-spec
     ([obj-spec]
      `(s/or :triple/o
             (s/coll-of (s/or :triple/object ~obj-spec)
                        :min-count 1
                        :kind set?
                        :into [])))
     ([obj-spec obj-list-spec]
      `(s/or :triple/o
             (s/coll-of (s/or :triple/object ~obj-spec
                              :triple/list ~obj-list-spec)
                        :min-count 1
                        :kind set?
                        :into [])))))

(def obj-set-spec
  (make-obj-spec obj-spec list-spec))

(def obj-set-novar-spec
  (make-obj-spec obj-novar-spec list-novar-spec))

(def obj-set-noblank-spec
  (make-obj-spec obj-noblank-spec))

(def obj-set-novar-noblank-spec
  (make-obj-spec obj-novar-noblank-spec))

;; Predicate Object

#?(:clj
   (defmacro make-pred-objs-spec
     [pred-spec objs-spec]
     `(s/or :triple/po (s/map-of ~pred-spec ~objs-spec
                                 :into []))))

(def pred-objs-spec
  (make-pred-objs-spec pred-spec obj-set-spec))

(def pred-objs-nopath-spec
  (make-pred-objs-spec pred-nopath-spec obj-set-spec))

(def pred-objs-novar-spec
  (make-pred-objs-spec pred-novar-spec obj-set-novar-spec))

(def pred-objs-noblank-spec
  (make-pred-objs-spec pred-nopath-spec obj-set-noblank-spec))

(def pred-objs-novar-noblank-spec
  (make-pred-objs-spec pred-novar-spec obj-set-novar-noblank-spec))

;; Subject Predicate Object

#?(:clj
   (defmacro make-nform-spec [subj-spec pred-objs-spec]
     `(s/or :triple/spo
            (s/map-of ~subj-spec (s/and ~pred-objs-spec not-empty)
                      :conform-keys true :into []))))

(def normal-form-spec
  (make-nform-spec subj-spec pred-objs-spec))

(def normal-form-nopath-spec
  (make-nform-spec subj-spec pred-objs-nopath-spec))

(def normal-form-novar-spec
  (make-nform-spec subj-novar-spec pred-objs-novar-spec))

(def normal-form-noblank-spec
  (make-nform-spec subj-noblank-spec pred-objs-noblank-spec))

(def normal-form-novar-noblank-spec
  (make-nform-spec subj-novar-noblank-spec pred-objs-novar-noblank-spec))

;; Subject Predicate Object (List)

#?(:clj
   (defmacro make-nform-list-spec
     [subj-list-spec pred-objs-spec]
     `(s/or :triple/spo-list
            (s/map-of ~subj-list-spec ~pred-objs-spec
                      :conform-keys true :into []))))

(def normal-form-list-spec
  (make-nform-list-spec list-spec pred-objs-spec))

(def normal-form-list-nopath-spec
  (make-nform-list-spec list-spec pred-objs-nopath-spec))

(def normal-form-list-novar-spec
  (make-nform-list-spec list-spec pred-objs-novar-spec))

;; Triple Vectors

(def triple-vec-spec
  (s/tuple subj-spec pred-spec obj-spec))

(def triple-vec-nopath-spec
  (s/tuple subj-spec pred-nopath-spec obj-spec))

(def triple-vec-novar-spec
  (s/tuple subj-novar-spec pred-novar-spec obj-novar-spec))

(def triple-vec-noblank-spec
  (s/tuple subj-noblank-spec pred-nopath-spec obj-noblank-spec))

(def triple-vec-novar-noblank-spec
  (s/tuple subj-novar-noblank-spec pred-novar-spec obj-novar-noblank-spec))

;; Triples

(def triple-spec
  (s/or :triple/vec        triple-vec-spec
        :triple/nform      normal-form-spec
        :triple/nform-list normal-form-list-spec))

(def triple-nopath-spec
  (s/or :triple/vec        triple-vec-nopath-spec
        :triple/nform      normal-form-nopath-spec
        :triple/nform-list normal-form-list-nopath-spec))

(def triple-novar-spec
  (s/or :triple/vec        triple-vec-novar-spec
        :triple/nform      normal-form-novar-spec
        :triple/nform-list normal-form-list-nopath-spec))

(def triple-noblank-spec
  (s/or :triple/vec   triple-vec-noblank-spec
        :triple/nform normal-form-noblank-spec))

(def triple-novar-noblank-spec
  (s/or :triple/vec   triple-vec-novar-noblank-spec
        :triple/nform normal-form-novar-noblank-spec))

;; Collection of Triples

(def triple-coll-spec
  (s/coll-of triple-spec :kind vector?))

(def triple-coll-nopath-spec
  (s/coll-of triple-nopath-spec :kind vector?))

(def triple-coll-novar-spec
  (s/coll-of triple-novar-spec :kind vector?))

(def triple-coll-noblank-spec
  (s/coll-of triple-noblank-spec :kind vector?))

(def triple-coll-novar-noblank-spec
  (s/coll-of triple-novar-noblank-spec :kind vector?))

;; Quads (for UPDATE)

(def quad-nopath-spec
  (s/and (s/tuple #{:graph}
                  ax/iri-or-var-spec
                  (s/or :triple/quad-triples triple-coll-nopath-spec))
         (s/conformer (fn [[_ iri triples]] [iri triples]))))

(def quad-novar-spec
  (s/and (s/tuple #{:graph}
                  ax/iri-or-var-spec
                  (s/or :triple/quad-triples triple-coll-novar-spec))
         (s/conformer (fn [[_ iri triples]] [iri triples]))))

(def quad-noblank-spec
  (s/and (s/tuple #{:graph}
                  ax/iri-or-var-spec
                  (s/or :triple/quad-triples triple-coll-noblank-spec))
         (s/conformer (fn [[_ iri triples]] [iri triples]))))

(def quad-novar-noblank-spec
  (s/and (s/tuple #{:graph}
                  ax/iri-or-var-spec
                  (s/or :triple/quad-triples triple-coll-novar-noblank-spec))
         (s/conformer (fn [[_ iri triples]] [iri triples]))))

;; Collection of Quads (for UPDATE)

(def quad-coll-nopath-spec
  (s/coll-of (s/or :triple/vec        triple-vec-nopath-spec
                   :triple/nform      normal-form-nopath-spec
                   :triple/nform-list normal-form-list-nopath-spec
                   :triple/quads      quad-nopath-spec)
             :kind vector?))

(def quad-coll-novar-spec
  (s/coll-of (s/or :triple/vec        triple-vec-novar-spec
                   :triple/nform      normal-form-novar-spec
                   :triple/nform-list normal-form-list-novar-spec
                   :triple/quads      quad-novar-spec)
             :kind vector?))

(def quad-coll-noblank-spec
  (s/coll-of (s/or :triple/vec   triple-vec-noblank-spec
                   :triple/nform normal-form-noblank-spec
                   :triple/quads quad-noblank-spec)
             :kind vector?))

(def quad-coll-novar-noblank-spec
  (s/coll-of (s/or :triple/vec   triple-vec-novar-noblank-spec
                   :triple/nform normal-form-novar-noblank-spec
                   :triple/quads quad-novar-noblank-spec)
             :kind vector?))

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

(s/def ::subject
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :triple/list   ::list
        :triple/bnodes ::bnodes))

(s/def ::subject-coll
  (s/or :triple/list   ::list
        :triple/bnodes ::bnodes))

(s/def ::subject-nopath
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :triple/list   ::list-nopath
        :triple/bnodes ::bnodes-nopath))

(s/def ::subject-coll-nopath
  (s/or :triple/list   ::list-nopath
        :triple/bnodes ::bnodes-nopath))

(s/def ::subject-novar
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :triple/list   ::list-novar
        :triple/bnodes ::bnodes-novar))

(s/def ::subject-coll-novar
  (s/or :triple/list   ::list-novar
        :triple/bnodes ::bnodes-novar))

(s/def ::subject-noblank
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec))

(s/def ::subject-novar-noblank
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec))

;; Predicates

(s/def ::predicate
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/rdf-type   ax/rdf-type-spec
        :triple/path   ::ps/path))

(s/def ::predicate-nopath
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/rdf-type   ax/rdf-type-spec))

(s/def ::predicate-novar
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/rdf-type   ax/rdf-type-spec))

(s/def ::predicate-noblank
  ::predicate-nopath)

(s/def ::predicate-novar-noblank
  ::predicate-novar)

;; Objects (includes Lists)

(s/def ::object
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :ax/literal    ax/literal-spec
        :triple/list   ::list
        :triple/bnodes ::bnodes))

(s/def ::object-nopath
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :ax/literal    ax/literal-spec
        :triple/list   ::list-nopath
        :triple/bnodes ::bnodes-nopath))

(s/def ::object-novar
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :ax/literal    ax/literal-spec
        :triple/list   ::list-novar
        :triple/bnodes ::bnodes-novar))

(s/def ::object-noblank
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/literal    ax/literal-spec))

(s/def ::object-novar-noblank
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/literal    ax/literal-spec))

;; Lists

;; Since lists are constructed out of blank nodes, we do not allow list
;; syntactic sugar (i.e. `:triple/list`) where blank nodes are banned.

(s/def ::list
  (s/coll-of ::object :kind list? :into []))

(s/def ::list-nopath
  (s/coll-of ::object-nopath :kind list? :into []))

(s/def ::list-novar
  (s/coll-of ::object-novar :kind list? :into []))

;; Blank Node Vectors

;; For obvious reasons, we don't allow bnode vectors to exist where blank
;; nodes are banned.

(defn- conform-pred-obj-pairs [po-pairs]
  (mapv (fn [{:keys [pred obj]}] [pred obj]) po-pairs))

(s/def ::bnodes
  (s/and vector?
         (s/* (s/cat :pred ::predicate :obj ::object))
         (s/conformer conform-pred-obj-pairs)))

(s/def ::bnodes-nopath
  (s/and vector?
         (s/* (s/cat :pred ::predicate-nopath :obj ::object))
         (s/conformer conform-pred-obj-pairs)))

(s/def ::bnodes-novar
  (s/and vector?
         (s/* (s/cat :pred ::predicate-novar :obj ::object-novar))
         (s/conformer conform-pred-obj-pairs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Combo Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; NOTE: Subjects can be non-IRIs in SPARQL, but not in RDF
;; single-branch `s/or`s are used to conform values

;; Object

#?(:clj
   (defmacro make-obj-spec
     ([obj-spec]
      `(s/coll-of ~obj-spec :min-count 1 :kind set? :into []))))

(def obj-set-spec
  (make-obj-spec ::object))

(def obj-set-nopath-spec
  (make-obj-spec ::object-nopath))

(def obj-set-novar-spec
  (make-obj-spec ::object-novar))

(def obj-set-noblank-spec
  (make-obj-spec ::object-noblank))

(def obj-set-novar-noblank-spec
  (make-obj-spec ::object-novar-noblank))

;; Predicate Object

#?(:clj
   (defmacro make-pred-objs-spec [pred-spec objs-spec]
     `(s/map-of ~pred-spec (s/or :triple.nform/o ~objs-spec)
                :min-count 1 :conform-keys true :into [])))

(def pred-objs-spec
  (make-pred-objs-spec ::predicate obj-set-spec))

(def pred-objs-nopath-spec
  (make-pred-objs-spec ::predicate-nopath obj-set-nopath-spec))

(def pred-objs-novar-spec
  (make-pred-objs-spec ::predicate-novar obj-set-novar-spec))

(def pred-objs-noblank-spec
  (make-pred-objs-spec ::predicate-noblank obj-set-noblank-spec))

(def pred-objs-novar-noblank-spec
  (make-pred-objs-spec ::predicate-novar-noblank obj-set-novar-noblank-spec))

;; Subject Predicate Object

(def empty-map-spec
  (s/map-of any? any? :count 0 :conform-keys true :into []))

;; cljs-specific ignore is needed since this fn is called in a macro.
#_{:clj-kondo/ignore #?(:clj [] :cljs [:unused-private-var])}
(defn- valid-conformed-spo? [spo-pairs]
  (every?
   (fn [[s po]]
     (or (#{:triple/list :triple/bnodes} (first s))
         (#{:triple.nform/po} (first po))))
   spo-pairs))

#?(:clj
   (defmacro make-nform-spec [subj-spec pred-objs-spec]
     `(s/and (s/map-of ~subj-spec
                       (s/or :triple.nform/po ~pred-objs-spec
                             :triple.nform/po-empty empty-map-spec)
                       :conform-keys true :into [])
             valid-conformed-spo?)))

(def normal-form-spec
  (make-nform-spec ::subject pred-objs-spec))

(def normal-form-nopath-spec
  (make-nform-spec ::subject-nopath pred-objs-nopath-spec))

(def normal-form-novar-spec
  (make-nform-spec ::subject-novar pred-objs-novar-spec))

(def normal-form-noblank-spec
  (make-nform-spec ::subject-noblank pred-objs-noblank-spec))

(def normal-form-novar-noblank-spec
  (make-nform-spec ::subject-novar-noblank pred-objs-novar-noblank-spec))

;; Triple Vectors

(def triple-vec-spec
  (s/tuple ::subject ::predicate ::object))

(def triple-vec-nopath-spec
  (s/tuple ::subject-nopath ::predicate-nopath ::object-nopath))

(def triple-vec-novar-spec
  (s/tuple ::subject-novar ::predicate-novar ::object-novar))

(def triple-vec-noblank-spec
  (s/tuple ::subject-noblank ::predicate-noblank ::object-noblank))

(def triple-vec-novar-noblank-spec
  (s/tuple ::subject-novar-noblank ::predicate-novar-noblank ::object-novar-noblank))

;; Triple Vectors (Coll, no predicates + objects)

(def triple-vec-no-po-spec
  (s/tuple ::subject-coll))

(def triple-vec-no-po-nopath-spec
  (s/tuple ::subject-coll-nopath))

(def triple-vec-no-po-novar-spec
  (s/tuple ::subject-coll-novar))

;; Triples

(def triple-spec
  (s/or :triple.vec/spo   triple-vec-spec
        :triple.vec/s     triple-vec-no-po-spec
        :triple.nform/spo normal-form-spec))

(def triple-nopath-spec
  (s/or :triple.vec/spo   triple-vec-nopath-spec
        :triple.vec/s     triple-vec-no-po-nopath-spec
        :triple.nform/spo normal-form-nopath-spec))

(def triple-novar-spec
  (s/or :triple.vec/spo   triple-vec-novar-spec
        :triple.vec/s     triple-vec-no-po-novar-spec
        :triple.nform/spo normal-form-novar-spec))

(def triple-noblank-spec
  (s/or :triple.vec/spo   triple-vec-noblank-spec
        :triple.nform/spo normal-form-noblank-spec))

(def triple-novar-noblank-spec
  (s/or :triple.vec/spo   triple-vec-novar-noblank-spec
        :triple.nform/spo normal-form-novar-noblank-spec))

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
                  (s/or :triple.quad/spo triple-coll-nopath-spec))
         (s/conformer (fn [[_ iri triples]] [iri triples]))))

(def quad-novar-spec
  (s/and (s/tuple #{:graph}
                  ax/iri-or-var-spec
                  (s/or :triple.quad/spo triple-coll-novar-spec))
         (s/conformer (fn [[_ iri triples]] [iri triples]))))

(def quad-noblank-spec
  (s/and (s/tuple #{:graph}
                  ax/iri-or-var-spec
                  (s/or :triple.quad/spo triple-coll-noblank-spec))
         (s/conformer (fn [[_ iri triples]] [iri triples]))))

(def quad-novar-noblank-spec
  (s/and (s/tuple #{:graph}
                  ax/iri-or-var-spec
                  (s/or :triple.quad/spo triple-coll-novar-noblank-spec))
         (s/conformer (fn [[_ iri triples]] [iri triples]))))

;; Collection of Quads (for UPDATE)

(def quad-coll-nopath-spec
  (s/coll-of (s/or :triple.vec/spo   triple-vec-nopath-spec
                   :triple.vec/s     triple-vec-no-po-nopath-spec
                   :triple.nform/spo normal-form-nopath-spec
                   :triple.quad/gspo quad-nopath-spec)
             :kind vector?))

(def quad-coll-novar-spec
  (s/coll-of (s/or :triple.vec/spo   triple-vec-novar-spec
                   :triple.vec/s     triple-vec-no-po-novar-spec
                   :triple.nform/spo normal-form-novar-spec
                   :triple.quad/gspo quad-novar-spec)
             :kind vector?))

(def quad-coll-noblank-spec
  (s/coll-of (s/or :triple.vec/spo   triple-vec-noblank-spec
                   :triple.nform/spo normal-form-noblank-spec
                   :triple.quad/gspo quad-noblank-spec)
             :kind vector?))

(def quad-coll-novar-noblank-spec
  (s/coll-of (s/or :triple.vec/spo   triple-vec-novar-noblank-spec
                   :triple.nform/spo normal-form-novar-noblank-spec
                   :triple.quad/gspo quad-novar-noblank-spec)
             :kind vector?))

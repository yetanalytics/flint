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

;; Blank Node Vectors

(declare pred-spec)
(declare pred-nopath-spec)
(declare pred-novar-spec)

(declare obj-spec)
(declare obj-novar-spec)

(defn- conform-pred-obj-pairs [po-pairs]
  (mapv (fn [{:keys [pred obj]}] [pred obj]) po-pairs))

(def bnode-vec-spec
  (s/and vector?
         (s/* (s/cat :pred pred-spec :obj obj-spec))
         (s/conformer conform-pred-obj-pairs)))

(def bnode-vec-nopath-spec
  (s/and vector?
         (s/* (s/cat :pred pred-nopath-spec :obj obj-spec))
         (s/conformer conform-pred-obj-pairs)))

(def bnode-vec-novar-spec
  (s/and vector?
         (s/* (s/cat :pred pred-novar-spec :obj obj-novar-spec))
         (s/conformer conform-pred-obj-pairs)))

;; Subjects

(def subj-spec
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :triple/list   list-spec
        :triple/bnodes bnode-vec-spec))

(def subj-coll-spec
  (s/or :triple/list   list-spec
        :triple/bnodes bnode-vec-spec))

(def subj-nopath-spec
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :triple/list   list-spec
        :triple/bnodes bnode-vec-nopath-spec))

(def subj-coll-nopath-spec
  (s/or :triple/list   list-spec
        :triple/bnodes bnode-vec-nopath-spec))

(def subj-novar-spec
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :triple/list   list-spec
        :triple/bnodes bnode-vec-novar-spec))

(def subj-coll-novar-spec
  (s/or :triple/list   list-novar-spec
        :triple/bnodes bnode-vec-novar-spec))

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

;; Objects (includes Lists)

(def obj-spec
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :ax/literal    ax/literal-spec
        :triple/list   list-spec
        :triple/bnodes bnode-vec-spec))

(def obj-nopath-spec
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :ax/literal    ax/literal-spec
        :triple/list   list-spec
        :triple/bnodes bnode-vec-nopath-spec))

(def obj-novar-spec
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/bnode      ax/bnode-spec
        :ax/literal    ax/literal-spec
        :triple/list   list-novar-spec
        :triple/bnodes bnode-vec-novar-spec))

(def obj-noblank-spec
  (s/or :ax/var        ax/variable-spec
        :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/literal    ax/literal-spec))

(def obj-novar-noblank-spec
  (s/or :ax/iri        ax/iri-spec
        :ax/prefix-iri ax/prefix-iri-spec
        :ax/literal    ax/literal-spec))

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
  (make-obj-spec obj-spec))

(def obj-set-nopath-spec
  (make-obj-spec obj-nopath-spec))

(def obj-set-novar-spec
  (make-obj-spec obj-novar-spec))

(def obj-set-noblank-spec
  (make-obj-spec obj-noblank-spec))

(def obj-set-novar-noblank-spec
  (make-obj-spec obj-novar-noblank-spec))

;; Predicate Object

#?(:clj
   (defmacro make-pred-objs-spec
     [pred-spec objs-spec]
     `(s/map-of ~pred-spec (s/or :triple.nform/o ~objs-spec)
                :into []
                :min-count 1)))

(def pred-objs-spec
  (make-pred-objs-spec pred-spec obj-set-spec))

(def pred-objs-nopath-spec
  (make-pred-objs-spec pred-nopath-spec obj-set-nopath-spec))

(def pred-objs-novar-spec
  (make-pred-objs-spec pred-novar-spec obj-set-novar-spec))

(def pred-objs-noblank-spec
  (make-pred-objs-spec pred-nopath-spec obj-set-noblank-spec))

(def pred-objs-novar-noblank-spec
  (make-pred-objs-spec pred-novar-spec obj-set-novar-noblank-spec))

;; Subject Predicate Object

#?(:clj
   (defmacro make-nform-spec [subj-spec pred-objs-spec]
     `(s/map-of ~subj-spec (s/or :triple.nform/po ~pred-objs-spec)
                :conform-keys true :into [])))

(def normal-form-spec
  (make-nform-spec subj-spec pred-objs-spec))

(def normal-form-nopath-spec
  (make-nform-spec subj-nopath-spec pred-objs-nopath-spec))

(def normal-form-novar-spec
  (make-nform-spec subj-novar-spec pred-objs-novar-spec))

(def normal-form-noblank-spec
  (make-nform-spec subj-noblank-spec pred-objs-noblank-spec))

(def normal-form-novar-noblank-spec
  (make-nform-spec subj-novar-noblank-spec pred-objs-novar-noblank-spec))

;; Subject Predicate Object (List)

(def empty-map-spec
  (s/map-of any? any? :max-count 0 :conform-keys true :into []))

#?(:clj
   (defmacro make-nform-no-po-spec [subj-list-spec]
     `(s/map-of ~subj-list-spec empty-map-spec
                :conform-keys true :into [])))

(def normal-form-no-po-spec
  (make-nform-no-po-spec subj-coll-spec))

(def normal-form-no-po-nopath-spec
  (make-nform-no-po-spec subj-coll-nopath-spec))

(def normal-form-no-po-novar-spec
  (make-nform-no-po-spec subj-coll-novar-spec))

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

;; Triple Vectors (List, no predicates + objects)

(def triple-vec-no-po-spec
  (s/tuple subj-coll-spec))

(def triple-vec-no-po-nopath-spec
  (s/tuple subj-coll-spec))

(def triple-vec-no-po-novar-spec
  (s/tuple subj-coll-novar-spec))

;; Triples

(def triple-spec
  (s/or :triple.vec/spo   triple-vec-spec
        :triple.vec/s     triple-vec-no-po-spec
        :triple.nform/spo normal-form-spec
        :triple.nform/s   normal-form-no-po-spec))

(def triple-nopath-spec
  (s/or :triple.vec/spo   triple-vec-nopath-spec
        :triple.vec/s     triple-vec-no-po-nopath-spec
        :triple.nform/spo normal-form-nopath-spec
        :triple.nform/s   normal-form-no-po-nopath-spec))

(def triple-novar-spec
  (s/or :triple.vec/spo   triple-vec-novar-spec
        :triple.vec/s     triple-vec-no-po-novar-spec
        :triple.nform/spo normal-form-novar-spec
        :triple.nform/s   normal-form-no-po-nopath-spec))

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
                   :triple.nform/s   normal-form-no-po-nopath-spec
                   :triple.quad/gspo quad-nopath-spec)
             :kind vector?))

(def quad-coll-novar-spec
  (s/coll-of (s/or :triple.vec/spo   triple-vec-novar-spec
                   :triple.vec/s     triple-vec-no-po-novar-spec
                   :triple.nform/spo normal-form-novar-spec
                   :triple.nform/s   normal-form-no-po-novar-spec
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

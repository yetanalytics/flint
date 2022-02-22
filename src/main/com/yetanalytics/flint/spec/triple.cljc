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

;; Blank nodes (and paths) banned in
;; - DELETE WHERE
;; - DELETE DATA
;; - DELETE

;; Variables (and paths) banned in
;; - DELETE DATA
;; - INSERT DATA

;; Subjects

(def subj-spec
  (s/or :ax/var        ax/variable?
        :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/bnode      ax/bnode?))

(def subj-novar-spec
  (s/or :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/bnode      ax/bnode?))

(def subj-noblank-spec
  (s/or :ax/var        ax/variable?
        :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?))

(def subj-novar-noblank-spec
  ax/iri-spec)

;; Predicates

(def pred-spec
  (s/or :ax/var        ax/variable?
        :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/rdf-type   ax/rdf-type?
        :triple/path   ::ps/path))

(def pred-nopath-spec
  (s/or :ax/var        ax/variable?
        :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/rdf-type   ax/rdf-type?))

(def pred-novar-spec
  (s/or :ax/iri ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/rdf-type ax/rdf-type?))

;; Objects

(def obj-spec
  (s/or :ax/var        ax/variable?
        :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/bnode      ax/bnode?
        :ax/str-lit    ax/valid-string?
        :ax/lmap-lit   ax/lang-map?
        :ax/num-lit    number?
        :ax/bool-lit   boolean?
        :ax/dt-lit     inst?))

(def obj-novar-spec
  (s/or :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/bnode      ax/bnode?
        :ax/str-lit    ax/valid-string?
        :ax/lmap-lit   ax/lang-map?
        :ax/num-lit    number?
        :ax/bool-lit   boolean?
        :ax/dt-lit     inst?))

(def obj-noblank-spec
  (s/or :ax/var        ax/variable?
        :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
        :ax/str-lit    ax/valid-string?
        :ax/lmap-lit   ax/lang-map?
        :ax/num-lit    number?
        :ax/bool-lit   boolean?
        :ax/dt-lit     inst?))

(def obj-novar-noblank-spec
  (s/or :ax/iri        ax/iri?
        :ax/prefix-iri ax/prefix-iri?
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

(def obj-set-noblank-spec
  (make-obj-spec obj-noblank-spec))

(def obj-set-novar-noblank-spec
  (make-obj-spec obj-novar-noblank-spec))

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

(def pred-objs-noblank-spec
  (make-pred-objs-spec pred-nopath-spec obj-set-noblank-spec))

(def pred-objs-novar-noblank-spec
  (make-pred-objs-spec pred-novar-spec obj-set-novar-noblank-spec))

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

(def normal-form-noblank-spec
  (make-nform-spec subj-noblank-spec pred-objs-noblank-spec))

(def normal-form-novar-noblank-spec
  (make-nform-spec subj-novar-noblank-spec pred-objs-novar-noblank-spec))

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

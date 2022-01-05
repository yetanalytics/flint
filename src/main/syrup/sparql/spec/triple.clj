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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def subj-spec
  (s/or :subject ax/var-or-iri-subj-spec))

(def obj-spec
  (s/or :object ax/var-or-term-spec))

(def pred-spec
  (s/or :predicate ax/var-or-iri-pred-spec
        :pred-path path/path-spec))

(def pred-nopath-spec
  (s/or :predicate ax/var-or-iri-pred-spec))

(def obj-set-spec
  (s/or :o-set (s/coll-of obj-spec
                          :min-count 1
                          :kind set?
                          :into [])))

(def ^:private pred-objs-spec-form
  `(s/or :po-map (s/map-of pred-spec obj-set-spec
                           :min-count 1
                           :into [])))

(def pred-objs-spec
  (eval pred-objs-spec-form))
(def pred-objs-nopath-spec
  (eval (form->nopath-spec-form pred-objs-spec-form)))

(def ^:private normal-form-spec-form
  `(s/or :spo-map (s/map-of subj-spec pred-objs-spec
                            :conform-keys true
                            :into [])))

(def normal-form-spec
  (eval normal-form-spec-form))
(def normal-form-nopath-spec
  (eval (form->nopath-spec-form normal-form-spec)))

;; TODO: Optimize
(def ^:private triple-vec-spec-form
  `(s/tuple (s/nonconforming subj-spec)
            (s/nonconforming pred-spec)
            (s/nonconforming obj-spec)))

(def triple-vec-spec
  (eval triple-vec-spec-form))
(def triple-vec-nopath-spec
  (eval (form->nopath-spec-form triple-vec-spec-form)))

(def ^:private triples-spec-form
  `(s/and (s/coll-of (s/or :vector triple-vec-spec
                           :normal-form normal-form-spec))
          ;; Remove s/or tag
          (s/conformer second)))

(def triples-spec
  (eval triples-spec-form))
(def triples-nopath-spec
  (eval (form->nopath-spec-form triples-spec-form)))

(def quads-spec
  (s/and (s/coll-of
          (s/or :vector triple-vec-nopath-spec
                :normal-form normal-form-nopath-spec
                :quad (s/and vector?
                             (s/cat ::s/k #{:graph}
                                    ::s/v (s/cat :iri ax/var-or-iri-spec
                                                 :pat triples-nopath-spec)))))
          ;; Remove s/or tag
         (s/conformer second)))

;; NOTE: Subjects can be non-IRIs in SPARQL, but not in RDF
;; NOTE: RDF collections not supported (yet?)

(comment
  (s/conform subj-spec '?subj)

  (s/conform pred-spec '?pred)
  (s/conform pred-nopath-spec '?pred)

  (s/describe pred-spec)
  (s/describe pred-nopath-spec)

  (s/conform pred-spec '(alt "foo" "bar"))
  (s/explain pred-nopath-spec '(alt "foo" "bar"))
  
  (s/conform pred-objs-spec
             {'?p1 #{'?oa '?ob}
              '?p2 #{'?oa '?ob}})
  
  (s/conform pred-objs-nopath-spec
             {'?p1 #{'?oa '?ob}
              '?p2 #{'?oa '?ob}})
  (=
   (s/conform triples-spec
              {'?subj {'?pred #{'?obj}}})
   (s/conform triples-spec
              ['?subj '?pred '?obj]))
  
  (=
   (s/conform triples-spec
              [{'?subj {'?p1 #{'?oa '?ob}
                        '?p2 #{'?oa '?ob}}}])
   (s/conform triples-nopath-spec
              [{'?subj {'?p1 #{'?oa '?ob}
                        '?p2 #{'?oa '?ob}}}]))
  )

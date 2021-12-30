(ns syrup.sparql.spec.triple
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]))

(def subj-spec
  (s/or :subject ax/var-or-iri-spec))

(def obj-spec
  (s/or :object ax/var-or-term-spec))

(def pred-spec
  (s/or :predicate ax/var-or-iri-pred-spec
        :pred-path coll?))

(def obj-set-spec
  (s/or :o-set (s/coll-of obj-spec
                          :min-count 1
                          :kind set?
                          :into [])))

(def pred-objs-spec
  (s/or :po-map (s/map-of pred-spec obj-set-spec
                          :min-count 1
                          :into [])))

(def normal-form-spec
  (s/or :spo-map (s/map-of subj-spec pred-objs-spec
                           :conform-keys true
                           :into [])))

;; NOTE: Subjects can be non-IRIs in SPARQL, but not in RDF
;; NOTE: RDF collections not supported (yet?)

(comment
  (s/conform subj-spec '?subj)
  (s/conform normal-form-spec
             {'?subj {'?pred #{'?obj}}}))

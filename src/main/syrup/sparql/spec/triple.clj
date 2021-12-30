(ns syrup.sparql.spec.triple
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]))

(defn- conj-set
  [s x]
  (if s (conj s x) #{x}))

(defn- triples->nform
  [triple-vecs]
  (reduce (fn [acc [s p o]]
            (if-not (and p o)
              (assoc acc s nil)
              (update-in acc [s p] conj-set o)))
          {}
          triple-vecs))

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

;; TODO: Optimize
(def triples-vec-spec
  (s/and (s/coll-of (s/tuple (s/nonconforming subj-spec)
                             (s/nonconforming pred-spec)
                             (s/nonconforming obj-spec))
                    :min-count 1
                    :kind vector?)
         (s/conformer triples->nform)
         (s/conformer normal-form-spec)))

(def triples-spec
  (s/and (s/or :sugared triples-vec-spec
               :normal-form normal-form-spec)
         ;; Remove s/or tag
         (s/conformer second)))

;; NOTE: Subjects can be non-IRIs in SPARQL, but not in RDF
;; NOTE: RDF collections not supported (yet?)

(comment
  (s/conform subj-spec '?subj)
  (=
   (s/conform triples-spec
              {'?subj {'?p1 #{'?oa '?ob}
                       '?p2 #{'?oa '?ob}}})
   (s/conform triples-spec
              [['?subj '?p1 '?oa]
               ['?subj '?p1 '?ob]
               ['?subj '?p2 '?oa]
               ['?subj '?p2 '?ob]])))

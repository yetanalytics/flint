(ns syrup.sparql.spec.query
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom    :as ax]
            [syrup.sparql.spec.expr     :as ex]
            [syrup.sparql.spec.prologue :as pro]
            [syrup.sparql.spec.triple   :as triple]
            [syrup.sparql.spec.where    :as ws]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dataset Clause specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::named ax/iri?)

(s/def ::from
  (s/or :default-graph ax/iri?
        :named-graph (s/keys :req-un [::named])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Solution Modifier specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::group-by
  (s/coll-of (s/or :builtin ex/expr-spec ; TODO
                   :custom ex/expr-spec ; TODO
                   :expr ex/expr-as-var-spec
                   :var ax/variable?)
             :min-count 1))

(s/def ::order-by
  (s/coll-of (s/cat :op #{'asc 'desc}
                    :expr ex/expr-spec)
             :min-count 1))

(s/def ::having
  (s/coll-of ex/expr-spec
             :min-count 1))

(s/def ::limit int?)
(s/def ::offset int?)

(def solution-modifier-spec
  (s/keys :opt-un [::group-by
                   ::order-by
                   ::having
                   ::limit
                   ::offset]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def select-spec
  (s/or :vars    (s/coll-of ax/variable? :min-count 1)
        :expr     ex/expr-as-var-spec
        :wildcard ax/wildcard?))

(s/def ::select select-spec)
(s/def ::select-distinct select-spec)
(s/def ::select-reduced select-spec)

(def select-query-spec
  (s/merge
   (s/keys :req-un [(or ::select ::select-distinct ::select-where)
                    ::ws/where]
           :opt-un [::from])
   solution-modifier-spec
   pro/prologue-spec))

(s/def ::construct triple/triples-nopath-spec)

(def construct-query-spec
  (s/merge
   (s/keys :req-un [::construct
                    ::ws/where]
           :opt-un [::from])
   solution-modifier-spec
   pro/prologue-spec))

(s/def ::describe
  (s/or :vars-or-iris (s/coll-of ax/var-or-iri-spec)
        :wildcard ax/wildcard?))

(def describe-query-spec
  (s/merge
   (s/keys :req-un [::describe
                    ::ws/where]
           :opt-un [::from])
   solution-modifier-spec
   pro/prologue-spec))

(def ask-query-spec
  (s/merge
   (s/keys :req-un [::ws/where]
           :opt-un [::from])
   solution-modifier-spec
   pro/prologue-spec))

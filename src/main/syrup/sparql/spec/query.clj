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

(s/def ::from ax/iri?)
(s/def ::from-named ax/iri?)

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

(s/cat :prologue (s/* (s/alt)))

;; (defmacro datomic-query-spec-form
;;   [qname qform]
;;   `(s/cat :bases    (s/? (s/cat ::s/k #{:bases}
;;                                 ::s/v ::pro/bases))
;;           :prefixes (s/? (s/cat ::s/k #{:prefixes}
;;                                 ::s/v ::pro/prefixes))
;;           ~qname    ~qform
;;           :from     (s/? (s/cat ::s/k #{:from :from-named}
;;                                 ::s/v ax/iri?))
;;           :where    (s/cat ::s/k #{:where}
;;                            ::s/v (s/* (s/alt :triple   triple/triple-vec-spec
;;                                              :nform    triple/normal-form-spec
;;                                              :union    ws/union-spec
;;                                              :optional ws/optional-spec
;;                                              :minus    ws/minus-spec
;;                                              :graph    ws/graph-spec
;;                                              :service  ws/service-spec
;;                                              :filter   ws/filter-spec
;;                                              :bind     ws/bind-spec
;;                                              :values   ws/values-spec)))
;;           :group-by (s/? (s/cat ::s/k #{:group-by}
;;                                 ::s/v ::group-by))
;;           :having   (s/? (s/cat ::s/k #{:having}
;;                                 ::s/v ::having))
;;           :order-by (s/? (s/cat ::s/k #{:order-by}
;;                                 ::s/v ::order-by))
;;           :limit    (s/? (s/cat ::s/k #{:limit}
;;                                 ::s/v ::limit))
;;           :offset   (s/? (s/cat ::s/k #{:offset}
;;                                 ::s/v ::offset))))

(def select-query-spec
  (s/merge
   pro/prologue-spec
   (s/keys* :req-un [(or ::select ::select-distinct ::select-where)
                     ::ws/where]
            :opt-un [::from ::from-named])
   solution-modifier-spec))

(s/def ::construct triple/triples-nopath-spec)

(def construct-query-spec
  (s/merge
   (s/keys* :req-un [::construct
                     ::ws/where]
            :opt-un [::from ::from-named])
   solution-modifier-spec
   pro/prologue-spec))

(s/def ::describe
  (s/or :vars-or-iris (s/coll-of ax/var-or-iri-spec)
        :wildcard ax/wildcard?))

(def describe-query-spec
  (s/merge
   (s/keys* :req-un [::describe
                     ::ws/where]
            :opt-un [::from ::from-named])
   solution-modifier-spec
   pro/prologue-spec))

(def ask-query-spec
  (s/merge
   (s/keys* :req-un [::ws/where]
            :opt-un [::from ::from-named])
   solution-modifier-spec
   pro/prologue-spec))

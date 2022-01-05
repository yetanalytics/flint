(ns syrup.sparql.spec.query
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom    :as ax]
            [syrup.sparql.spec.expr     :as ex]
            [syrup.sparql.spec.prologue :as pro]
            [syrup.sparql.spec.triple   :as triple]
            [syrup.sparql.spec.modifier :as mod]
            [syrup.sparql.spec.select   :as ss]
            [syrup.sparql.spec.where    :as ws]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dataset Clause specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::from ax/iri?)
(s/def ::from-named (s/coll-of ax/iri? :min-count 1))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def select-spec
  (s/or :var-or-exprs (s/* (s/alt :var ax/variable?
                                  :expr ex/expr-as-var-spec))
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
   (s/keys :req-un [(or ::ss/select ::ss/select-distinct ::ss/select-reduced)
                    ::ws/where]
           :opt-un [::from ::from-named])
   pro/prologue-spec
   mod/solution-modifier-spec))

(s/def ::construct triple/triples-nopath-spec)
(s/def ::construct-where triple/triples-nopath-spec)

(def construct-query-spec
  (s/or :construct
        (s/merge
         (s/keys :req-un [::construct ::ws/where]
                 :opt-un [::from ::from-named])
         pro/prologue-spec
         mod/solution-modifier-spec)
        :construct-where
        (s/merge
         (s/keys :req-un [::construct-where]
                 :opt-un [::from ::from-named])
         pro/prologue-spec
         mod/solution-modifier-spec)))

(s/def ::describe
  (s/or :vars-or-iris (s/coll-of ax/var-or-iri-spec)
        :wildcard ax/wildcard?))

(def describe-query-spec
  (s/merge
   (s/keys :req-un [::describe]
           :opt-un [::from ::from-named ::ws/where])
   pro/prologue-spec
   mod/solution-modifier-spec))

(s/def ::ask ws/where-spec)

(def ask-query-spec
  (s/merge
   (s/keys :req-un [::ask]
           :opt-un [::from ::from-named])
   pro/prologue-spec
   mod/solution-modifier-spec))

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

(s/def ::from ax/iri-spec)
(s/def ::from-named (s/coll-of ax/iri-spec :min-count 1))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def select-spec
  (s/or :vars-or-exprs (s/coll-of (s/or :var ax/variable?
                                        :expr ::ex/expr-as-var))
        :wildcard ax/wildcard?))

(s/def ::select select-spec)
(s/def ::select-distinct select-spec)
(s/def ::select-reduced select-spec)

(def select-query-spec
  (s/merge
   (s/keys :req-un [(or ::ss/select ::ss/select-distinct ::ss/select-reduced)
                    ::ws/where]
           :opt-un [::from ::from-named])
   pro/prologue-spec
   mod/solution-modifier-spec))

(def triples-spec
  (s/coll-of (s/or :tvec triple/triple-vec-nopath-spec
                   :nform triple/normal-form-nopath-spec)
             :min-count 1))

(s/def ::construct triples-spec)
(s/def ::construct-where triples-spec)

(def construct-query-spec
  (s/merge
   (s/keys :req-un [(or (and ::construct ::ws/where) ::construct-where)]
           :opt-un [::from ::from-named])
   pro/prologue-spec
   mod/solution-modifier-spec))

(s/def ::describe
  (s/or :vars-or-iris (s/coll-of ax/var-or-iri-spec)
        :wildcard ax/wildcard?))

(def describe-query-spec
  (s/merge
   (s/keys :req-un [::describe]
           :opt-un [::from ::from-named ::ws/where])
   pro/prologue-spec
   mod/solution-modifier-spec))

(s/def ::ask ::ws/where)

(def ask-query-spec
  (s/merge
   (s/keys :req-un [::ask]
           :opt-un [::from ::from-named])
   pro/prologue-spec
   mod/solution-modifier-spec))

(def query-spec
  (s/or :select-query    select-query-spec
        :construct-query construct-query-spec
        :describe-query  describe-query-spec
        :ask-query       ask-query-spec))

(ns syrup.sparql.spec.modifier
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]
            [syrup.sparql.spec.expr  :as ex]))

(s/def ::group-by
  (s/coll-of (s/or :builtin ex/expr-spec ; TODO
                   :custom ex/expr-spec ; TODO
                   :expr ex/expr-as-var-spec
                   :var ax/variable?)
             :min-count 1))

(s/def ::order-by
  (s/coll-of (s/or :asc-desc (s/cat :op #{'asc 'desc}
                                    :expr ex/expr-spec)
                   :expr ex/expr-spec
                   :var  ax/variable?)
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
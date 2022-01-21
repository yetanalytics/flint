(ns syrup.sparql.spec.modifier
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]
            [syrup.sparql.spec.expr  :as ex]))

(s/def ::group-by
  (s/coll-of (s/and (s/or :expr ::ex/expr
                          :var ax/variable?
                          :expr-var ::ex/expr-as-var)
                    #_(s/conformer (fn [[kw x]]
                                   (if (#{:var} kw) [kw x] x))))
             :min-count 1))

(s/def ::order-by
  (s/coll-of (s/or :asc-desc (s/cat :op #{'asc 'desc}
                                    :sub-expr ::ex/expr)
                   :var  ax/variable?
                   :expr ::ex/expr)
             :min-count 1))

(s/def ::having
  (s/coll-of ::ex/expr
             :min-count 1))

(s/def ::limit (s/or :num-lit int?))
(s/def ::offset (s/or :num-lit int?))

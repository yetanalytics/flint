(ns syrup.sparql.spec.modifier
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]
            [syrup.sparql.spec.expr  :as ex]))

(s/def ::group-by
  (s/coll-of (s/or :mod/expr ::ex/expr
                   :var ax/variable?
                   :mod/expr-as-var ::ex/expr-as-var)
             :min-count 1))

(s/def ::order-by
  (s/coll-of (s/or :mod/asc-desc (s/& (s/cat :mod/op #{'asc 'desc}
                                             :mod/expr ::ex/expr)
                                      (s/conformer #(into [] %)))
                   :var  ax/variable?
                   :mod/expr ::ex/expr)
             :min-count 1))

(s/def ::having
  (s/coll-of ::ex/expr
             :min-count 1))

(s/def ::limit (s/or :num-lit int?))
(s/def ::offset (s/or :num-lit int?))

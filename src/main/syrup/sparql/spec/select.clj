(ns syrup.sparql.spec.select
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]
            [syrup.sparql.spec.expr :as ex]))

(def select-spec
  (s/or :select/var-or-exprs (s/* (s/alt :var ax/variable?
                                         :expr ::ex/expr-as-var))
        :wildcard ax/wildcard?))

(s/def ::select select-spec)
(s/def ::select-distinct select-spec)
(s/def ::select-reduced select-spec)

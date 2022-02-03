(ns com.yetanalytics.flint.spec.select
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]
            [com.yetanalytics.flint.spec.expr  :as es]))

(def select-spec
  (s/or :select/var-or-exprs
        (s/* (s/alt :ax/var ax/variable?
                    :select/expr-as-var ::es/expr-as-var))
        :ax/wildcard
        ax/wildcard?))

(s/def ::select select-spec)
(s/def ::select-distinct select-spec)
(s/def ::select-reduced select-spec)

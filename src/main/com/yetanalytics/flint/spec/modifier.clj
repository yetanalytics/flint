(ns com.yetanalytics.flint.spec.modifier
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]
            [com.yetanalytics.flint.spec.expr  :as es]))

(s/def ::group-by
  (s/coll-of (s/or :mod/expr ::es/expr
                   :var ax/variable?
                   :mod/expr-as-var ::es/expr-as-var)
             :min-count 1
             :kind vector?))

(s/def ::order-by
  (s/coll-of (s/or :mod/asc-desc (s/& (s/cat :mod/op #{'asc 'desc}
                                             :mod/expr ::es/expr)
                                      (s/conformer #(into [] %)))
                   :var  ax/variable?
                   :mod/expr ::es/expr)
             :min-count 1
             :kind vector?))

(s/def ::having
  (s/coll-of ::es/expr
             :min-count 1
             :kind vector?))

;; single-branch `s/or`s are used to conform values

(s/def ::limit (s/or :num-lit int?))

(s/def ::offset (s/or :num-lit int?))

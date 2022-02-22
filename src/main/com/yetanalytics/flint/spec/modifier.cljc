(ns com.yetanalytics.flint.spec.modifier
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]
            [com.yetanalytics.flint.spec.expr  :as es]))

;; TODO: Need to restrict `mod/group-expr` to function calls.
;; However, this may count as a breaking change.

(s/def ::group-by
  (s/coll-of (s/or :ax/var          ax/variable?
                   :mod/group-expr  ::es/expr
                   :mod/expr-as-var ::es/expr-as-var)
             :min-count 1
             :kind vector?))

(s/def ::order-by
  (s/coll-of (s/or :ax/var         ax/variable?
                   :mod/asc-desc   (s/& (s/cat :mod/op #{'asc 'desc}
                                               :mod/asc-desc-expr ::es/agg-expr)
                                        (s/conformer #(into [] %)))
                   :mod/order-expr ::es/agg-expr)
             :min-count 1
             :kind vector?))

(s/def ::having
  (s/coll-of ::es/agg-expr
             :min-count 1
             :kind vector?))

;; single-branch `s/or`s are used to conform values

(s/def ::limit (s/or :ax/num-lit nat-int?))

(s/def ::offset (s/or :ax/num-lit int?))

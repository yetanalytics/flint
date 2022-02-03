(ns com.yetanalytics.flint.spec.expr
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def expr-terminal-spec
  (s/and
   (comp not list?)
   (s/or :ax/var      ax/variable?
         :ax/num-lit  number?
         :ax/bool-lit boolean?
         :ax/str-lit  ax/valid-string?
         :ax/lmap-lit ax/lang-map?
         :ax/dt-lit   inst?)))

(def expr-branch-spec
  (s/and
   (s/coll-of any? :kind list? :min-count 1)
   (s/or :expr/nilary      (s/cat :expr/op #{'rand 'now 'uuid 'struuid})
         :expr/nil-unary   (s/cat :expr/op #{'bnode}
                                    :expr/arg-1 (s/? ::expr))
         :expr/unary       (s/cat :expr/op #{'not
                                             'str 'strlen 'ucase 'lcase
                                             'lang 'datatype 'blank? 'literal? 'numeric?
                                             'iri 'uri 'iri? 'uri? 'encode-for-uri
                                             'abs 'ceil 'floor 'round
                                             'year 'month 'day
                                             'hours 'minutes 'seconds
                                             'timezone 'tz
                                             'md5 'sha1 'sha256 'sha384 'sha512
                                             'sum 'sum-distinct
                                             'min 'min-distinct
                                             'max 'max-distinct
                                             'avg 'avg-distinct
                                             'sample 'sample-distinct
                                             'count 'count-distinct}
                                  :expr/arg-1 ::expr)
         :expr/unary-wild  (s/cat :expr/op #{'count 'count-distinct}
                                  :expr/arg-1 (s/or :expr/terminal
                                                    (s/or :ax/wildcard ax/wildcard?)))
         :expr/unary-var   (s/cat :expr/op #{'bound}
                                  :expr/arg-1 (s/or :expr/terminal
                                                    (s/or :ax/var ax/variable?)))
         :expr/unary-where (s/cat :expr/op #{'exists 'not-exists}
                             ;; Avoid mutually recursive `:require`
                                  :expr/arg-1 :com.yetanalytics.flint.spec.where/where)
         :expr/binary       (s/cat :expr/op #{'lang-matches 'contains 'strlang 'strdt
                                              'strstarts 'strends 'strbefore 'strafter
                                              'sameterm
                                              '= 'not= '< '> '<= '>=}
                                   :expr/arg-1 ::expr
                                   :expr/arg-2 ::expr)
         :expr/binary-plus  (s/cat :expr/op #{'regex 'substr
                                              'and 'or
                                              '+ '- '* '/
                                              'in 'not-in}
                                   :expr/arg-1 ::expr
                                   :expr/vargs (s/+ ::expr))
         :expr/binary-kwarg (s/cat :expr/op #{'group-concat 'group-concat-distinct}
                                   :expr/arg-1 ::expr
                                   :expr/arg-2
                                   (s/? (s/& (s/cat :expr/k #{:separator}
                                                    :expr/v string?)
                                             (s/conformer
                                              (fn [x]
                                                [:expr/terminal
                                                 [:expr/kwarg (into [] x)]])))))
         :expr/ternary       (s/cat :expr/op #{'if}
                                    :expr/arg-1 ::expr
                                    :expr/arg-2 ::expr
                                    :expr/arg-3 ::expr)
         :expr/ternary-plus  (s/cat :expr/op #{'substr}
                                    :expr/arg-1 ::expr
                                    :expr/arg-2 ::expr
                                    :expr/vargs (s/+ ::expr))
         :expr/varardic     (s/cat :expr/op #{'concat 'coalesce}
                                   :expr/vargs (s/* ::expr))
         :expr/custom      (s/cat :expr/op ax/iri-spec
                                  :expr/vargs (s/* ::expr)))
   (s/conformer second)
   (s/conformer (fn [{op    :expr/op
                      arg-1 :expr/arg-1
                      arg-2 :expr/arg-2
                      arg-3 :expr/arg-3
                      vargs :expr/vargs}]
                  [[:expr/op op]
                   [:expr/args (cond-> []
                                 arg-1 (conj arg-1)
                                 arg-2 (conj arg-2)
                                 arg-3 (conj arg-3)
                                 vargs (concat vargs))]]))))

(s/def ::expr
  (s/or :expr/terminal expr-terminal-spec
        :expr/branch expr-branch-spec))

(s/def ::expr-as-var
  (s/or :expr/as-var (s/and vector?
                            (s/tuple ::expr (s/or :ax/var ax/variable?)))))

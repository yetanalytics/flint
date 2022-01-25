(ns syrup.sparql.spec.expr
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def expr-terminal-spec
  (s/and
   (comp not list?)
   (s/or :var      ax/variable?
         :dt-lit   inst?
         :num-lit  number?
         :bool-lit boolean?
         :str-lit  string?
         :lmap-lit ax/lang-map?)))

(def expr-branch-spec
  (s/and
   (s/coll-of any? :kind list? :min-count 1)
   (s/or :0-ary       (s/cat :op #{'rand 'now 'uuid 'struuid})
         :0-1-ary     (s/cat :op #{'bnode}
                             :arg-1 (s/? ::expr))
         :1-ary       (s/cat :op #{'not
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
                             :arg-1 ::expr)
         :1-wild-ary  (s/cat :op #{'count 'count-distinct}
                             :arg-1 (s/or :expr/terminal
                                          (s/or :wildcard ax/wildcard?)))
         :1-var-ary   (s/cat :op #{'bound}
                             :arg-1 (s/or :expr/terminal
                                          (s/or :var ax/variable?)))
         :1-where-ary (s/cat :op #{'exists 'not-exists}
                             ;; Avoid mutually recursive `:require`
                             :arg-1 :syrup.sparql.spec.where/where)
         :2-ary       (s/cat :op #{'lang-matches 'contains 'strlang 'strdt
                                   'strstarts 'strends 'strbefore 'strafter
                                   'sameterm
                                   '= 'not= '< '> '<= '>= 'in 'not-in}
                             :arg-1 ::expr
                             :arg-2 ::expr)
         :2-plus-ary  (s/cat :op #{'regex 'substr
                                   'and 'or
                                   '+ '- '* '/}
                             :arg-1 ::expr
                             :vargs (s/+ ::expr))
         :2-kwarg-ary (s/cat :op #{'group-concat 'group-concat-distinct}
                             :arg-1 ::expr
                             :arg-2 (s/? (s/& (s/cat :expr/k #{:separator}
                                                     :expr/v string?)
                                              (s/conformer
                                               (fn [x]
                                                 [:expr/terminal
                                                  [:expr/kwarg (into [] x)]])))))
         :3-ary       (s/cat :op #{'if}
                             :arg-1 ::expr
                             :arg-2 ::expr
                             :arg-3 ::expr)
         :3-plus-ary  (s/cat :op #{'substr}
                             :arg-1 ::expr
                             :arg-2 ::expr
                             :vargs (s/+ ::expr))
         :var-ary     (s/cat :op #{'concat 'coalesce}
                             :vargs (s/* ::expr))
         :custom      (s/cat :op ax/iri-spec
                             :vargs (s/* ::expr)))
   (s/conformer second)
   (s/conformer (fn [{:keys [op arg-1 arg-2 arg-3 vargs]}]
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
                            (s/tuple ::expr (s/or :var ax/variable?)))))

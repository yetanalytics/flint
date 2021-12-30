(ns syrup.sparql.spec.expr
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Forward declare ::where spec
(s/def ::where any?)

(def terminal-expr?
  (some-fn number? boolean? string? ax/variable?))

(def expr-spec
  (s/and
   list?
   (s/or :terminal    terminal-expr?
         :0-ary       (s/cat :op #{'rand 'now 'uuid 'struuid})
         :0-1-ary     (s/cat :op #{'bnode}
                             :arg-1 (s/? expr-spec))
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
                                   'sample 'sample-distinct}
                             :arg-1 expr-spec)
         :1-wild-ary  (s/cat :op #{'count 'count-distinct}
                             :arg-1 (s/or :expr expr-spec
                                          :wildcard ax/wildcard?))
         :1-var-ary   (s/cat :op #{'bound}
                             :arg-1 ax/variable?)
         :1-coll-ary  (s/cat :op #{'concat 'coalesce}
                             :arg-1 (s/coll-of expr-spec))
         :1-where-ary (s/cat :op #{'exists 'not-exists}
                             :arg-1 ::where)
         :2-ary       (s/cat :op #{'lang-matches 'contains 'strlang 'strdt
                                   'strstarts 'strends 'strbefore 'strafter
                                   'sameterm}
                             :arg-1 expr-spec
                             :arg-2 expr-spec)
         :2-plus-ary  (s/cat :op #{'regex 'substr}
                             :arg-1 expr-spec
                             :vargs (s/+ expr-spec))
         :2-kwarg-ary (s/cat :op #{'group-concat 'group-concat-distinct}
                             :arg-1 expr-spec
                             :arg-2 (s/? (s/cat :kword :separator
                                                :kwarg string?)))
         :3-ary       (s/cat :op #{'if}
                             :arg-1 expr-spec
                             :arg-2 expr-spec
                             :arg-3 expr-spec)
         :3-plus-ary  (s/cat :op #{'substr}
                             :arg-1 expr-spec
                             :arg-2 expr-spec
                             :vargs (s/+ expr-spec))
         :coll        (s/cat :op #{'concat 'coalesce}
                             :arg-1 (s/coll-of expr-spec))
         :var-ary     (s/cat :op #{'and 'or
                                   '= 'not= '< '> '<= '>= 'in 'not-in
                                   '+ '- '* '/
                                   ax/iri?}
                             :vargs (s/* expr-spec)))))

(comment
  (s/conform expr-spec '(+ 2 2))
  (s/conform expr-spec '(avg (+ 2 2))))

(def expr-as-var-spec
  (s/and vector?
         (s/cat :expr expr-spec
                :as #{:as}
                :var ax/variable?)
         (s/conformer #(dissoc % :as))))

(comment
  (s/conform expr-as-var-spec ['(+ 2 2) :as :?foo]))

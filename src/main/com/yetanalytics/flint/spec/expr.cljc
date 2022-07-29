(ns com.yetanalytics.flint.spec.expr
  (:require [clojure.spec.alpha :as s]
            [clojure.set :as cset]
            [com.yetanalytics.flint.spec.axiom :as ax])
  #?(:cljs (:require-macros
            [com.yetanalytics.flint.spec.expr :refer [keyword-args]])))

;; Terminals

(def expr-terminal-spec
  (s/and
   (comp not list?)
   (s/or :ax/iri        ax/iri?
         :ax/prefix-iri ax/prefix-iri?
         :ax/var        ax/variable?
         :ax/num-lit    number?
         :ax/bool-lit   boolean?
         :ax/str-lit    ax/valid-string?
         :ax/lmap-lit   ax/lang-map?
         :ax/dt-lit     inst?)))

(def var-terminal-spec
  (s/or :expr/terminal
        (s/or :ax/var ax/variable?)))

(def wildcard-terminal-spec
  (s/or :expr/terminal
        (s/or :ax/wildcard ax/wildcard?)))

;; Keyword arguments

(s/def ::distinct? boolean?)
(s/def ::separator ax/valid-string?)

(defn- kvs->map [kvs]
  (reduce (fn [m {k :expr/k v :expr/v}] (assoc m k v))
          {}
          kvs))

(defmacro keyword-args
  [& kspecs]
  `(s/& (s/* (s/cat :expr/k keyword? :expr/v any?))
        (s/conformer kvs->map)
        (s/keys :opt-un ~kspecs)
        #(every? ~(->> kspecs (map (comp keyword name)) set) (keys %))
        (s/conformer #(into [] %))))

;; Op symbols

(def nilary-ops
  #{'rand 'now 'uuid 'struuid 'bnode})

(def unary-ops
  #{'bnode
    'not
    'str 'strlen 'ucase 'lcase
    'lang 'datatype 'blank? 'literal? 'numeric?
    'iri 'uri 'iri? 'uri? 'encode-for-uri
    'abs 'ceil 'floor 'round
    'year 'month 'day
    'hours 'minutes 'seconds
    'timezone 'tz
    'md5 'sha1 'sha256 'sha384 'sha512})

(def unary-agg-ops
  #{'sum 'min 'max 'avg 'sample 'count})

(def unary-agg-wild-ops
  #{'count})

(def unary-agg-sep-ops
  #{'group-concat})

(def aggregate-ops
  (cset/union unary-agg-ops unary-agg-wild-ops unary-agg-sep-ops))

(def unary-var-ops
  #{'bound})

(def unary-where-ops
  #{'exists 'not-exists})

(def binary-ops
  #{'lang-matches 'contains 'strlang 'strdt
    'strstarts 'strends 'strbefore 'strafter
    'sameterm
    'regex 'substr
    '= 'not= '< '> '<= '>=})

(def binary-plus-ops
  #{'and 'or
    '+ '- '* '/
    'in 'not-in})

(def ternary-ops
  #{'if
    'regex 'substr 'replace})

(def four-ary-ops
  #{'replace})

(def varardic-ops
  #{'concat 'coalesce})

;; Branches

;; BEGIN NEW ;;;;;

(def nilary-spec
  (s/cat :expr/op symbol?))

(def unary-spec
  (s/cat :expr/op symbol?
         :expr/arg-1 ::expr))

(def unary-var-spec
  (s/cat :expr/op symbol?
         :expr/arg-1 var-terminal-spec))

(def unary-where-spec
  (s/cat :expr/op unary-where-ops
         ;; Fully qualify ns to avoid mutually recursive require
         :expr/arg-1 :com.yetanalytics.flint.spec.where/where))

(def binary-spec
  (s/cat :expr/op symbol?
         :expr/arg-1 ::expr
         :expr/arg-2 ::expr))

(def binary-plus-spec
  (s/cat :expr/op symbol?
         :expr/arg-1 ::expr
         :expr/vargs (s/+ ::expr)))

(def ternary-spec
  (s/cat :expr/op symbol?
         :expr/arg-1 ::expr
         :expr/arg-2 ::expr
         :expr/arg-3 ::expr))

(def four-ary-spec
  (s/cat :expr/op symbol?
         :expr/arg-1 ::expr
         :expr/arg-2 ::expr
         :expr/arg-3 ::expr
         :expr/arg-4 ::expr))

(def varardic-spec
  (s/cat :expr/op symbol?
         :expr/vargs (s/* ::expr)))

(def custom-fn-spec
  (s/cat :expr/op ax/iri-spec
         :expr/vargs (s/* ::expr)))

(defmulti expr-spec
  (fn [e]
    (let [op (first e)]
      (cond
        (symbol? op) op
        (s/valid? ax/iri-spec op) :custom))))

(defmethod expr-spec 'rand [_] nilary-spec)
(defmethod expr-spec 'now [_] nilary-spec)
(defmethod expr-spec 'uuid [_] nilary-spec)
(defmethod expr-spec 'struuid [_] nilary-spec)

(defmethod expr-spec 'bnode [_] (s/and (s/or :nilary nilary-spec
                                             :unary unary-spec)
                                       (s/conformer second)))

(defmethod expr-spec 'not [_] unary-spec)

(defmethod expr-spec 'str [_] unary-spec)
(defmethod expr-spec 'strlen [_] unary-spec)
(defmethod expr-spec 'ucase [_] unary-spec)
(defmethod expr-spec 'lcase [_] unary-spec)
(defmethod expr-spec 'lang [_] unary-spec)
(defmethod expr-spec 'datatype [_] unary-spec)

(defmethod expr-spec 'blank? [_] unary-spec)
(defmethod expr-spec 'literal? [_] unary-spec)
(defmethod expr-spec 'numeric? [_] unary-spec)
(defmethod expr-spec 'iri? [_] unary-spec)
(defmethod expr-spec 'uri? [_] unary-spec)

(defmethod expr-spec 'iri [_] unary-spec)
(defmethod expr-spec 'uri [_] unary-spec)
(defmethod expr-spec 'encode-for-uri [_] unary-spec)

(defmethod expr-spec 'abs [_] unary-spec)
(defmethod expr-spec 'ceil [_] unary-spec)
(defmethod expr-spec 'floor [_] unary-spec)
(defmethod expr-spec 'round [_] unary-spec)

(defmethod expr-spec 'year [_] unary-spec)
(defmethod expr-spec 'month [_] unary-spec)
(defmethod expr-spec 'day [_] unary-spec)
(defmethod expr-spec 'hours [_] unary-spec)
(defmethod expr-spec 'minutes [_] unary-spec)
(defmethod expr-spec 'seconds [_] unary-spec)
(defmethod expr-spec 'timezone [_] unary-spec)
(defmethod expr-spec 'tz [_] unary-spec)

(defmethod expr-spec 'md5 [_] unary-spec)
(defmethod expr-spec 'sha1 [_] unary-spec)
(defmethod expr-spec 'sha256 [_] unary-spec)
(defmethod expr-spec 'sha384 [_] unary-spec)
(defmethod expr-spec 'sha512 [_] unary-spec)

(defmethod expr-spec 'bound [_] unary-var-spec)

(defmethod expr-spec 'exists [_] unary-where-spec)
(defmethod expr-spec 'not-exists [_] unary-where-spec)

(defmethod expr-spec 'lang-matches [_] binary-spec)
(defmethod expr-spec 'contains [_] binary-spec)
(defmethod expr-spec 'strlang [_] binary-spec)
(defmethod expr-spec 'strdt [_] binary-spec)
(defmethod expr-spec 'strstarts [_] binary-spec)
(defmethod expr-spec 'strends [_] binary-spec)
(defmethod expr-spec 'strbefore [_] binary-spec)
(defmethod expr-spec 'strafter [_] binary-spec)
(defmethod expr-spec 'sameterm [_] binary-spec)

(defmethod expr-spec '= [_] binary-spec)
(defmethod expr-spec 'not= [_] binary-spec)
(defmethod expr-spec '< [_] binary-spec)
(defmethod expr-spec '> [_] binary-spec)
(defmethod expr-spec '<= [_] binary-spec)
(defmethod expr-spec '>= [_] binary-spec)

(defmethod expr-spec 'and [_] binary-plus-spec)
(defmethod expr-spec 'or [_] binary-plus-spec)
(defmethod expr-spec 'in [_] binary-plus-spec)
(defmethod expr-spec 'not-in [_] binary-plus-spec)
(defmethod expr-spec '+ [_] binary-plus-spec)
(defmethod expr-spec '- [_] binary-plus-spec)
(defmethod expr-spec '* [_] binary-plus-spec)
(defmethod expr-spec '/ [_] binary-plus-spec)

(defmethod expr-spec 'if [_] ternary-spec)

(defmethod expr-spec 'regex [_] (s/and (s/or :binary binary-spec
                                             :ternary ternary-spec)
                                       (s/conformer second)))

(defmethod expr-spec 'substr [_] (s/and (s/or :binary binary-spec
                                              :ternary ternary-spec)
                                        (s/conformer second)))

(defmethod expr-spec 'replace [_] (s/and (s/or :ternary ternary-spec
                                               :four-ary four-ary-spec)
                                         (s/conformer second)))

(defmethod expr-spec 'concat [_] varardic-spec)
(defmethod expr-spec 'coalesce [_] varardic-spec)

(defmethod expr-spec :custom [_] custom-fn-spec)

;; We're not gentesting so retag is irrelevant
(s/multi-spec expr-spec identity)

;; END NEW ;;;;;;;

(defn- conform-expr
  [{op     :expr/op
    arg-1  :expr/arg-1
    arg-2  :expr/arg-2
    arg-3  :expr/arg-3
    arg-4  :expr/arg-4
    vargs  :expr/vargs
    kwargs :expr/kwargs}]
  (cond-> [[:expr/op op]
           [:expr/args (cond-> []
                         arg-1 (conj arg-1)
                         arg-2 (conj arg-2)
                         arg-3 (conj arg-3)
                         arg-4 (conj arg-4)
                         vargs (concat vargs)
                         true vec)]]
    kwargs (conj [:expr/kwargs kwargs])))

(def agg-expr-branch-spec
  (s/and
   list?
   (s/or
    :expr/nilary        (s/cat :expr/op nilary-ops)
    :expr/unary         (s/cat :expr/op unary-ops
                               :expr/arg-1 ::agg-expr)
    :expr/unary-agg     (s/cat :expr/op unary-agg-ops
                               :expr/arg-1 ::agg-expr
                               :expr/kwargs (keyword-args ::distinct?))
    :expr/unary-wild    (s/cat :expr/op unary-agg-wild-ops
                               :expr/arg-1 wildcard-terminal-spec
                               :expr/kwargs (keyword-args ::distinct?))
    :expr/unary-agg-sep (s/cat :expr/op unary-agg-sep-ops
                               :expr/arg-1 ::agg-expr
                               :expr/kwargs (keyword-args ::distinct?
                                                          ::separator))
    :expr/unary-var     (s/cat :expr/op unary-var-ops
                               :expr/arg-1 var-terminal-spec)
    :expr/unary-where   (s/cat :expr/op unary-where-ops
                               ;; Avoid mutually recursive `:require`
                               :expr/arg-1 :com.yetanalytics.flint.spec.where/where)
    :expr/binary        (s/cat :expr/op binary-ops
                               :expr/arg-1 ::agg-expr
                               :expr/arg-2 ::agg-expr)
    :expr/binary-plus   (s/cat :expr/op binary-plus-ops
                               :expr/arg-1 ::agg-expr
                               :expr/vargs (s/+ ::agg-expr))
    :expr/ternary       (s/cat :expr/op ternary-ops
                               :expr/arg-1 ::agg-expr
                               :expr/arg-2 ::agg-expr
                               :expr/arg-3 ::agg-expr)
    :expr/four-ary      (s/cat :expr/op four-ary-ops
                               :expr/arg-1 ::agg-expr
                               :expr/arg-2 ::agg-expr
                               :expr/arg-3 ::agg-expr
                               :expr/arg-4 ::agg-expr)
    :expr/varardic      (s/cat :expr/op varardic-ops
                               :expr/vargs (s/* ::agg-expr))
    ;; Only custom functions that are aggregates use the DISTINCT keyword
    :expr/custom        (s/cat :expr/op ax/iri-spec
                               :expr/vargs (s/* ::agg-expr)
                               :expr/kwargs (keyword-args ::distinct?)))
   (s/conformer second)
   (s/conformer conform-expr)))

(def expr-branch-spec
  (s/and
   list?
   (s/or
    :expr/nilary        (s/cat :expr/op nilary-ops)
    :expr/unary         (s/cat :expr/op unary-ops
                               :expr/arg-1 ::expr)
    :expr/unary-var     (s/cat :expr/op unary-var-ops
                               :expr/arg-1 var-terminal-spec)
    :expr/unary-where   (s/cat :expr/op unary-where-ops
                               ;; Avoid mutually recursive `:require`
                               :expr/arg-1 :com.yetanalytics.flint.spec.where/where)
    :expr/binary        (s/cat :expr/op binary-ops
                               :expr/arg-1 ::expr
                               :expr/arg-2 ::expr)
    :expr/binary-plus   (s/cat :expr/op binary-plus-ops
                               :expr/arg-1 ::expr
                               :expr/vargs (s/+ ::expr))
    :expr/ternary       (s/cat :expr/op ternary-ops
                               :expr/arg-1 ::expr
                               :expr/arg-2 ::expr
                               :expr/arg-3 ::expr)
    :expr/four-ary      (s/cat :expr/op four-ary-ops
                               :expr/arg-1 ::expr
                               :expr/arg-2 ::expr
                               :expr/arg-3 ::expr
                               :expr/arg-4 ::expr)
    :expr/varardic      (s/cat :expr/op varardic-ops
                               :expr/vargs (s/* ::expr))
    :expr/custom        (s/cat :expr/op ax/iri-spec
                               :expr/vargs (s/* ::expr)))
   (s/conformer second)
   (s/conformer conform-expr)))

;; Expr specs

(s/def ::expr
  (s/or :expr/terminal expr-terminal-spec
        :expr/branch expr-branch-spec))

(s/def ::agg-expr
  (s/or :expr/terminal expr-terminal-spec
        :expr/branch agg-expr-branch-spec))

;; Expr AS var specs

(s/def ::expr-as-var
  (s/or :expr/as-var (s/tuple ::expr (s/or :ax/var ax/variable?))))

(s/def ::agg-expr-as-var
  (s/or :expr/as-var (s/tuple ::agg-expr (s/or :ax/var ax/variable?))))

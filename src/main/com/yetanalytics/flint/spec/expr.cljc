(ns com.yetanalytics.flint.spec.expr
  (:require [clojure.spec.alpha :as s]
            [clojure.set :as cset]
            [com.yetanalytics.flint.spec.axiom :as ax])
  #?(:cljs (:require-macros
            [com.yetanalytics.flint.spec.expr :refer [defexprspecs
                                                      keyword-args]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Terminals
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def expr-terminal-spec
  (s/and
   (comp not list?)
   (s/or :ax/iri        ax/iri-spec
         :ax/prefix-iri ax/prefix-iri-spec
         :ax/var        ax/variable-spec
         :ax/literal    ax/literal-spec)))

(def var-terminal-spec
  (s/or :expr/terminal (s/or :ax/var ax/variable-spec)))

(def wildcard-terminal-spec
  (s/or :expr/terminal (s/or :ax/wildcard ax/wildcard-spec)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keyword Argument Specs and Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::distinct? boolean?)

;; This will conform to `[:separator string-literal]`, which will be formatted
;; as a plain string literal (i.e. does not get passed to `p/-format-literal`).
(s/def ::separator (s/and string? ax/literal-spec))

(defn- kvs->map [kvs]
  (reduce (fn [m {k :expr/k v :expr/v}] (assoc m k v))
          {}
          kvs))

(defmacro keyword-args
  "Given `kspecs` (e.g. `(keyword-args ::distinct? ::selector)`), return
   a regex spec that can be used to spec a sequence of `:keyword value`
   pairs (e.g. `(:distinct? true :selector \";\")`)."
  [& kspecs]
  `(s/& (s/* (s/cat :expr/k keyword? :expr/v any?))
        (s/conformer kvs->map)
        (s/keys :opt-un ~kspecs)
        #(every? ~(->> kspecs (map (comp keyword name)) set) (keys %))
        (s/conformer #(into [] %))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Operation Symbols
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def nilary-ops
  #{'rand 'now 'uuid 'struuid})

(def nilary-or-unary-ops
  #{'bnode})

(def unary-ops
  #{'not
    'str 'strlen 'ucase 'lcase
    'lang 'datatype 'blank? 'literal? 'numeric?
    'iri 'uri 'iri? 'uri? 'encode-for-uri
    'abs 'ceil 'floor 'round
    'year 'month 'day
    'hours 'minutes 'seconds
    'timezone 'tz
    'md5 'sha1 'sha256 'sha384 'sha512})

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

(def binary-or-ternary-ops
  #{'regex 'substr})

(def ternary-ops
  #{'if})

(def ternary-or-fourary-ops
  #{'replace})

(def varardic-ops
  #{'concat 'coalesce})

;; Aggregates

(def aggregate-expr-ops
  #{'sum 'min 'max 'avg 'sample})

(def aggregate-expr-or-wild-ops
  #{'count})

(def aggregate-expr-with-sep-ops
  #{'group-concat})

(def aggregate-ops
  "A set of all operations used in aggregate expressions."
  (cset/union aggregate-expr-ops
              aggregate-expr-or-wild-ops
              aggregate-expr-with-sep-ops))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Branch Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Built-in Expressions

(def nilary-spec
  (s/cat :expr/op nilary-ops))

(defn- nilary-or-unary-spec* [expr-spec]
  (s/cat :expr/op nilary-or-unary-ops
         :expr/arg-1 (s/? expr-spec)))

(def nilary-or-unary-spec (nilary-or-unary-spec* ::expr))
(def nilary-or-unary-agg-spec (nilary-or-unary-spec* ::agg-expr))

(defn- unary-spec* [expr-spec]
  (s/cat :expr/op unary-ops
         :expr/arg-1 expr-spec))

(def unary-spec (unary-spec* ::expr))
(def unary-agg-spec (unary-spec* ::agg-expr))

(def unary-var-spec
  (s/cat :expr/op unary-var-ops
         :expr/arg-1 var-terminal-spec))

(def unary-where-spec
  (s/cat :expr/op unary-where-ops
         ;; Fully qualify ns to avoid mutually recursive require
         :expr/arg-1 :com.yetanalytics.flint.spec.where/where))

(defn- binary-spec* [expr-spec]
  (s/cat :expr/op binary-ops
         :expr/arg-1 expr-spec
         :expr/arg-2 expr-spec))

(def binary-spec (binary-spec* ::expr))
(def binary-agg-spec (binary-spec* ::agg-expr))

(defn- binary-plus-spec* [expr-spec]
  (s/cat :expr/op binary-plus-ops
         :expr/arg-1 expr-spec
         :expr/vargs (s/+ expr-spec)))

(def binary-plus-spec (binary-plus-spec* ::expr))
(def binary-plus-agg-spec (binary-plus-spec* ::agg-expr))

(defn- binary-or-ternary-spec* [expr-spec]
  (s/cat :expr/op binary-or-ternary-ops
         :expr/arg-1 expr-spec
         :expr/arg-2 expr-spec
         :expr/arg-3 (s/? expr-spec)))

(def binary-or-ternary-spec (binary-or-ternary-spec* ::expr))
(def binary-or-ternary-agg-spec (binary-or-ternary-spec* ::agg-expr))

(defn- ternary-spec* [expr-spec]
  (s/cat :expr/op ternary-ops
         :expr/arg-1 expr-spec
         :expr/arg-2 expr-spec
         :expr/arg-3 expr-spec))

(def ternary-spec (ternary-spec* ::expr))
(def ternary-agg-spec (ternary-spec* ::agg-expr))

(defn- ternary-or-fourary-spec* [expr-spec]
  (s/cat :expr/op ternary-or-fourary-ops
         :expr/arg-1 expr-spec
         :expr/arg-2 expr-spec
         :expr/arg-3 expr-spec
         :expr/arg-4 (s/? expr-spec)))

(def ternary-or-fourary-spec (ternary-or-fourary-spec* ::expr))
(def ternary-or-fourary-agg-spec (ternary-or-fourary-spec* ::agg-expr))

(defn- varardic-spec* [expr-spec]
  (s/cat :expr/op varardic-ops
         :expr/vargs (s/* expr-spec)))

(def varardic-spec (varardic-spec* ::expr))
(def varardic-agg-spec (varardic-spec* ::agg-expr))

;; Aggregate Expressions (as opposed to regular exprs with aggregates)

;; NOTE: Aggregates inside other aggregates are not allowed in Apache Jena,
;; but are technically allowed by the grammar, so it may be allowed by other
;; implementations. After all, these two are equivalent:
;;
;; SELECT (MAX(AVG(?x)) AS ?maxAvg)                => Banned by Jena
;; SELECT (AVG(?x) AS ?avg) (MAX(?avg) AS ?maxAvg) => Allowed by Jena

(def aggregate-spec
  (s/cat :expr/op aggregate-ops
         :expr/arg-1 ::agg-expr
         :expr/kwargs (keyword-args ::distinct?)))

(def aggregate-wildcard-spec
  (s/cat :expr/op aggregate-expr-or-wild-ops
         :expr/arg-1 (s/& (s/alt :expr ::agg-expr
                                 :wildcard wildcard-terminal-spec)
                          (s/conformer second))
         :expr/kwargs (keyword-args ::distinct?)))

(def aggregate-separator-spec
  (s/cat :expr/op aggregate-expr-with-sep-ops
         :expr/arg-1 ::agg-expr
         :expr/kwargs (keyword-args ::distinct?
                                    ::separator)))

;; Custom Expressions

(def custom-fn-spec ; non-aggregates
  (s/cat :expr/op ax/iri-or-prefixed-spec
         :expr/vargs (s/* ::expr)))

(def aggregate-custom-fn-spec
  (s/cat :expr/op ax/iri-or-prefixed-spec
         :expr/vargs (s/* ::agg-expr)
         :expr/kwargs (keyword-args ::distinct?)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mutli-specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro defexprspecs
  "Macro to repeatedly define `(defmethod mm-name 'sym [_] expr-spec)`."
  [mm-name syms expr-spec]
  `(do ~@(map (fn [sym] `(defmethod ~mm-name (quote ~sym) [~'_] ~expr-spec))
              (eval syms))))

(defn- expr-spec-dispatch
  [expr-list]
  (let [op (first expr-list)]
    (cond
      (symbol? op) op
      (s/valid? ax/iri-or-prefixed-spec op) :custom)))

;; No Aggregate Expressions
(defmulti expr-spec-mm
  "Given a Flint expr list, returns a spec determined by its first arg."
  expr-spec-dispatch)

(defexprspecs expr-spec-mm nilary-ops nilary-spec)
(defexprspecs expr-spec-mm nilary-or-unary-ops nilary-or-unary-spec)
(defexprspecs expr-spec-mm unary-ops unary-spec)
(defexprspecs expr-spec-mm unary-var-ops unary-var-spec)
(defexprspecs expr-spec-mm unary-where-ops unary-where-spec)
(defexprspecs expr-spec-mm binary-ops binary-spec)
(defexprspecs expr-spec-mm binary-plus-ops binary-plus-spec)
(defexprspecs expr-spec-mm binary-or-ternary-ops binary-or-ternary-spec)
(defexprspecs expr-spec-mm ternary-ops ternary-spec)
(defexprspecs expr-spec-mm ternary-or-fourary-ops ternary-or-fourary-spec)
(defexprspecs expr-spec-mm varardic-ops varardic-spec)

(defmethod expr-spec-mm :custom [_] custom-fn-spec)

;; We're not gentesting so retag is irrelevant
(def expr-multi-spec
  (s/multi-spec expr-spec-mm first))

;; Aggregate Expressions

(defmulti agg-expr-spec-mm
  "Like `expr-spec-mm`, but covers also covers aggregate expressions."
  expr-spec-dispatch)

(defexprspecs agg-expr-spec-mm nilary-ops nilary-spec)
(defexprspecs agg-expr-spec-mm nilary-or-unary-ops nilary-or-unary-agg-spec)
(defexprspecs agg-expr-spec-mm unary-ops unary-agg-spec)
(defexprspecs agg-expr-spec-mm unary-var-ops unary-var-spec)
(defexprspecs agg-expr-spec-mm unary-where-ops unary-where-spec)
(defexprspecs agg-expr-spec-mm binary-ops binary-agg-spec)
(defexprspecs agg-expr-spec-mm binary-plus-ops binary-plus-agg-spec)
(defexprspecs agg-expr-spec-mm binary-or-ternary-ops binary-or-ternary-agg-spec)
(defexprspecs agg-expr-spec-mm ternary-ops ternary-agg-spec)
(defexprspecs agg-expr-spec-mm ternary-or-fourary-ops ternary-or-fourary-agg-spec)
(defexprspecs agg-expr-spec-mm varardic-ops varardic-agg-spec)

(defexprspecs agg-expr-spec-mm aggregate-ops aggregate-spec)
(defexprspecs agg-expr-spec-mm aggregate-expr-or-wild-ops aggregate-wildcard-spec)
(defexprspecs agg-expr-spec-mm aggregate-expr-with-sep-ops aggregate-separator-spec)

(defmethod agg-expr-spec-mm :custom [_] aggregate-custom-fn-spec)

(def agg-expr-multi-spec
  (s/multi-spec agg-expr-spec-mm first))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Expression Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- conform-expr
  "Conforms the map arg into a
   `[[:expr/op op] [:expr/args args] [:expr/kwargs kwargs]]` vector."
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

(def expr-branch-spec
  (s/and list?
         expr-multi-spec
         (s/conformer conform-expr)))

(def agg-expr-branch-spec
  (s/and list?
         agg-expr-multi-spec
         (s/conformer conform-expr)))

(s/def ::expr
  (s/or :expr/terminal expr-terminal-spec
        :expr/branch expr-branch-spec))

(s/def ::agg-expr
  (s/or :expr/terminal expr-terminal-spec
        :expr/branch agg-expr-branch-spec))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Expression AS Variable Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::expr-as-var
  (s/or :expr/as-var (s/tuple ::expr (s/or :ax/var ax/variable-spec))))

(s/def ::agg-expr-as-var
  (s/or :expr/as-var (s/tuple ::agg-expr (s/or :ax/var ax/variable-spec))))

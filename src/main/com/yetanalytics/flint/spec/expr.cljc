(ns com.yetanalytics.flint.spec.expr
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax])
  #?(:cljs (:require-macros
            [com.yetanalytics.flint.spec.expr :refer [kwarg-spec]])))

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

(def var-terminal-spec
  (s/or :expr/terminal
        (s/or :ax/var ax/variable?)))

(def wildcard-terminal-spec
  (s/or :expr/terminal
        (s/or :ax/wildcard ax/wildcard?)))

(s/def ::distinct? boolean?)
(s/def ::separator ax/valid-string?)

;; (defn- conform-kwargs [kvs]
;;   (->> kvs
;;        (into (sorted-map))
;;        (reduce-kv (fn [acc k v]
;;                     (conj acc [:expr/terminal [:expr/kwarg [k v]]]))
;;                   [])))

(defn- kvs->map [kvs]
  (reduce (fn [m {k :expr/k v :expr/v}] (assoc m k v))
          {}
          kvs))

(defmacro kwarg-spec
  [& kspecs]
  `(s/& (s/* (s/cat :expr/k keyword? :expr/v any?))
        (s/conformer kvs->map)
        (s/keys :opt-un ~kspecs)
        (s/conformer #(into [] %))))

;; (defn- conform-kwarg
;;   [kwarg-m]
;;   [:expr/terminal [:expr/kwarg (into [] kwarg-m)]])

;; (defn- kwargs->map
;;   [kwarg-ms]
;;   (reduce (fn [m {k :expr/k v :expr/v}] (assoc m k v))
;;           {}
;;           kwarg-ms))

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

(def agg-expr-branch-spec
  (s/and
   list?
   (s/or
    :expr/nilary        (s/cat :expr/op nilary-ops)
    :expr/unary         (s/cat :expr/op unary-ops
                               :expr/arg-1 ::agg-expr)
    :expr/unary-agg     (s/cat :expr/op unary-agg-ops
                               :expr/arg-1 ::agg-expr
                               :expr/kwargs (kwarg-spec ::distinct?))
    :expr/unary-wild    (s/cat :expr/op unary-agg-wild-ops
                               :expr/arg-1 wildcard-terminal-spec
                               :expr/kwargs (kwarg-spec ::distinct?))
    :expr/unary-agg-sep (s/cat :expr/op unary-agg-sep-ops
                               :expr/arg-1 ::agg-expr
                               :expr/kwargs (kwarg-spec ::distinct? ::separator))
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
                               :expr/kwargs (kwarg-spec ::distinct?)))
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

(s/def ::expr
  (s/or :expr/terminal expr-terminal-spec
        :expr/branch expr-branch-spec))

(s/def ::agg-expr
  (s/or :expr/terminal expr-terminal-spec
        :expr/branch agg-expr-branch-spec))

(s/def ::expr-as-var
  (s/or :expr/as-var (s/tuple ::expr (s/or :ax/var ax/variable?))))

(s/def ::agg-expr-as-var
  (s/or :expr/as-var (s/tuple ::agg-expr (s/or :ax/var ax/variable?))))

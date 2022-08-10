(ns com.yetanalytics.flint.axiom.impl.validation
  #?(:cljs (:require [goog.string :refer [format]]
                     [goog.string.format])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Ranges
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; 

(defn- char->int
  [c]
  #?(:clj (int c) :cljs (.charCodeAt c)))

(def qmark-range [(char->int \?)])
(def uscore-range [(char->int \_)])
(def bslash-range [(char->int \\)])
(def percent-range [(char->int \%)])

(def unicode-range-no-digits
  [[(char->int \A) (char->int \Z)]
   [(char->int \a) (char->int \z)]
   [0x00C0 0x00D6] ; * Latin letters with diacritics + spacing modifiers
   [0x00D8 0x00F6] ;   Excludes multiplication + division signs
   [0x0370 0x037D] ; * Various alphabets (e.g. Greek, Cyrillic, Arabic)
   [0x037F 0x1FFF] ;   Excludes Greek question mark
   [0x200C 0x200D] ; * Zero-width Joiner + Non-joiner
   [0x2070 0x218F] ; * Super/sub-scripts + other punctuation
   [0x2C00 0x2FEF] ; * More alphabets + Punctuation + Hanzi radicals
   [0x3001 0xD7FF] ; * CJK punctuation + hanzi + hangeul
   [0xF900 0xFDCF] ; * More CJK characters + Hebrew + Arabic
   [0xFDF0 0xFFFD]])

;; The Unicode code points \u10000-\uEFFFF are permitted in the SPARQL spec,
;; but are unsupported here since Java only supports chars up to \uFFFF.
;; In addition, the spec prohibits surrogate pair chars (\uD800-\uF8FF) so
;; only Basic Multilingual Plane chars are allowed.
;; This is okay since it's doubtful people will be using emojis and other
;; supplemental plane chars in Flint.

(def unicode-range-start
  (into [[(char->int \0) (char->int \9)] (char->int \_)]
        unicode-range-no-digits))

;; \u00B7        = middle dot ·
;; \u0300-\u036F = combining characters
;; \u203F-\u2040 = ties
(def unicode-range
  (into [0x00B7 [0x0300 0x036F] [0x203F 0x2040]]
        unicode-range-start))

(def var-start-range
  unicode-range-start)

(def var-body-range
  unicode-range)

(def bnode-start-range
  unicode-range-start)

(def bnode-body-range
  (into [(char->int \-) (char->int \.)] unicode-range))

(def bnode-end-range
  (into [(char->int \-)] unicode-range))

(def prefix-ns-start-range
  unicode-range-no-digits)

(def prefix-ns-body-range
  (into [(char->int \-) (char->int \.)] unicode-range))

(def prefix-ns-end-range
  (into [(char->int \-)] unicode-range))

(def prefix-name-start-range
  (into [(char->int \:)] unicode-range-start))

(def prefix-name-body-range
  (into [(char->int \:) (char->int \-) (char->int \.)] unicode-range))

(def prefix-name-end-range
  (into [(char->int \:) (char->int \-)] unicode-range))

(def iri-start-range
  [(char->int \<)])

(def iri-banned-range
  (conj (mapv char->int [\< \> \" \{ \} \| \^ \` \\])
        [0x0000 0x0020]))

(def iri-end-range
  [(char->int \>)])

;; 0x0022, 0x005C, 0x000A, 0x000D, respectively
(def literal-banned-range
  (mapv char->int [\" \return \newline \\]))

;; Note in the second part that we need to match the letters `n`, `r`, etc.,
;; not the newline char `\n` nor the return char `\r`.
(def literal-escape-range
  (mapv char->int [\t \b \n \r \f \\ \" \']))

(def hex-range
  [[(char->int \0) (char->int \9)]
   [(char->int \A) (char->int \F)]
   [(char->int \a) (char->int \f)]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Regexes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; 

#?(:cljs
   (defn- hex-str [n]
     (.toUpperCase (.toString ^number n 16))))

(defn- ascii-point->str
  [n]
  (cond
    ;; Control characters + space
    (<= n 0x000F)
    #?(:clj (format "\\u000%x" n)
       :cljs (format "\\u000%s" (hex-str n)))
    (or (<= 0x0010 n 0x0020)
        (= 0x00FF n))
    #?(:clj (format "\\u00%x" n)
       :cljs (format "\\u00%s" (hex-str n)))
    ;; Chars to be escaped in regex
    (#{\- \" \\ \^ \[ \] \?} (char n))
    (str "\\" (char n))
    ;; Regular chars
    :else
    (str (char n))))

(defn- code-point->str
  [n]
  #?(:clj
     (cond
       (<= n 0x007F)
       (ascii-point->str n)
       (<= n 0x00FF)
       (format "\\u00%x" n)
       (<= n 0x0FFF)
       (format "\\u0%x" n)
       :else
       (format "\\u%x" n))
     :cljs
     (cond
       (<= n 0x007F)
       (ascii-point->str n)
       (<= n 0x00FF)
       (format "\\u00%s" (hex-str n))
       (<= n 0x0FFF)
       (format "\\u0%s" (hex-str n))
       :else
       (format "\\u%s" (hex-str n)))))

(defn- ranges->regex-charset
  ([ranges]
   (ranges->regex-charset ranges false))
  ([ranges banned?]
   (let [charset
         (reduce (fn [s r]
                   (if (vector? r)
                     (let [[start end] r
                           start-str (code-point->str start)
                           end-str   (code-point->str end)]
                       (str s start-str "-" end-str))
                     (let [range-str (code-point->str r)]
                       (str s range-str))))
                 ""
                 ranges)]
     (if banned?
       (str "[^" charset "]")
       (str "[" charset "]")))))

(def iri-body-regex
  (let [iri-banned (ranges->regex-charset iri-banned-range true)]
    (re-pattern (format "%s*" iri-banned))))

(def iri-regex
  (let [iri-banned (ranges->regex-charset iri-banned-range true)]
    (re-pattern (format "<%s*>" iri-banned))))

(def var-regex
  (let [var-start (ranges->regex-charset var-start-range)
        var-body  (ranges->regex-charset var-body-range)]
    (re-pattern (format "\\?%s%s*"
                        var-start
                        var-body))))

(def bnode-regex
  (let [bnode-start (ranges->regex-charset bnode-start-range)
        bnode-body  (ranges->regex-charset bnode-body-range)
        bnode-end   (ranges->regex-charset bnode-end-range)]
    (re-pattern (format "\\_(?:%s(?:%s*%s)?)?"
                        bnode-start
                        bnode-body
                        bnode-end))))

(def prefix-ns-regex
  (let [prefix-ns-start (ranges->regex-charset prefix-ns-start-range)
        prefix-ns-body  (ranges->regex-charset prefix-ns-body-range)
        prefix-ns-end   (ranges->regex-charset prefix-ns-end-range)]
    (re-pattern (format "%s(?:%s*%s)?"
                        prefix-ns-start
                        prefix-ns-body
                        prefix-ns-end))))

(def prefix-name-regex
  (let [percent-encode*   (ranges->regex-charset hex-range)
        percent-encode    (str "(?:%" percent-encode* percent-encode* ")")
        prefix-name-start (ranges->regex-charset prefix-name-start-range)
        prefix-name-body  (ranges->regex-charset prefix-name-body-range)
        prefix-name-end   (ranges->regex-charset prefix-name-end-range)]
    (re-pattern (format "(?:%s|%s)(?:(?:%s|%s)*(?:%s|%s))?"
                        prefix-name-start
                        percent-encode
                        prefix-name-body
                        percent-encode
                        prefix-name-end
                        percent-encode))))

(def literal-regex
  (let [literal-banned (ranges->regex-charset literal-banned-range true)
        literal-escape (ranges->regex-charset literal-escape-range false)]
    (re-pattern (format "(?:%s|(?:\\\\%s))*"
                        literal-banned
                        literal-escape))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clojure-specific validation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; 

#?(:clj
   (defmacro unicode-bitset
     "Instantiate a `java.util.BitSet` instance and add set all bits
      at or (inclusive) between the ranges in `code-point-ranges`."
     [code-point-ranges]
     (let [cp-ranges# (if (symbol? code-point-ranges)
                        @(ns-resolve *ns* code-point-ranges)
                        code-point-ranges)]
       `(doto (java.util.BitSet. 0xFFFF)
          ~@(map (fn [r#]
                   (if (vector? r#)
                     `(.set ~(first r#) ~(inc (second r#)))
                     `(.set ~r#)))
                 cp-ranges#)))))

#?(:clj (def ^java.util.BitSet qmark-bitset
          (unicode-bitset qmark-range)))
#?(:clj (def ^java.util.BitSet uscore-bitset
          (unicode-bitset uscore-range)))
#?(:clj (def ^java.util.BitSet bslash-bitset
          (unicode-bitset bslash-range)))
#?(:clj (def ^java.util.BitSet percent-bitset
          (unicode-bitset percent-range)))

#?(:clj (def ^java.util.BitSet iri-start-bitset
          (unicode-bitset iri-start-range)))
#?(:clj (def ^java.util.BitSet iri-banned-bitset
          (unicode-bitset iri-banned-range)))
#?(:clj (def ^java.util.BitSet iri-end-bitset
          (unicode-bitset iri-end-range)))

#?(:clj (def ^java.util.BitSet var-start-bitset
          (unicode-bitset var-start-range)))
#?(:clj (def ^java.util.BitSet var-body-bitset
          (unicode-bitset var-body-range)))

#?(:clj (def ^java.util.BitSet bnode-start-bitset
          (unicode-bitset bnode-start-range)))
#?(:clj (def ^java.util.BitSet bnode-body-bitset
          (unicode-bitset bnode-body-range)))
#?(:clj (def ^java.util.BitSet bnode-end-bitset
          (unicode-bitset bnode-end-range)))

#?(:clj (def ^java.util.BitSet prefix-ns-start-bitset
          (unicode-bitset prefix-ns-start-range)))
#?(:clj (def ^java.util.BitSet prefix-ns-body-bitset
          (unicode-bitset prefix-ns-body-range)))
#?(:clj (def ^java.util.BitSet prefix-ns-end-bitset
          (unicode-bitset prefix-ns-end-range)))

#?(:clj (def ^java.util.BitSet prefix-name-start-bitset
          (unicode-bitset prefix-name-start-range)))
#?(:clj (def ^java.util.BitSet prefix-name-body-bitset
          (unicode-bitset prefix-name-body-range)))
#?(:clj (def ^java.util.BitSet prefix-name-end-bitset
          (unicode-bitset prefix-name-end-range)))

;; Exclude backslash since we don't want to return false upon encountering
;; the start of an escape sequence.
#?(:clj (def literal-banned-range*
          (filterv #(not= % (char->int \\)) literal-banned-range)))
#?(:clj (def ^java.util.BitSet string-banned-bitset
          (unicode-bitset literal-banned-range*)))
#?(:clj (def ^java.util.BitSet string-escape-bitset
          (unicode-bitset literal-escape-range)))

#?(:clj (def ^java.util.BitSet hex-bitset
          (unicode-bitset hex-range)))

#?(:clj
   (defmacro in-bitset?
     "Inlined check on whether the code point at `idx` in `s` can be found
      in `bitset`."
     [bitset s idx]
     `(.get ^java.util.BitSet ~bitset (.codePointAt ^String ~s ~idx))))

#?(:clj
   (defmacro recur-if
     "Perform a recursion only when `pred` evaluates to `true`; otherwise,
      return `false` and do not recur."
     [pred & exprs]
     `(if ~pred
        (recur ~@exprs)
        false)))

#?(:clj
   #_{:clj-kondo/ignore [:loop-without-recur]}
   (defn- valid-iri-body-str?*
     [^String ibs]
     (let [ccnt (count ibs)]
       (loop [idx 0]
         (cond
           (>= idx ccnt)
           true
           :else
           (recur-if (not (in-bitset? iri-banned-bitset ibs idx))
                     (inc idx)))))))

#?(:clj
   #_{:clj-kondo/ignore [:loop-without-recur]}
   (defn- valid-iri-str?*
     [^String is]
     (let [ccnt (count is)
           lidx (dec ccnt)]
       (loop [idx 0]
         (cond
           (>= idx ccnt)
           (<= 2 ccnt) ; Need to have opening and closing brackets
           (= idx 0)
           (recur-if (in-bitset? iri-start-bitset is idx)
                     (inc idx))
           (= idx lidx)
           (recur-if (in-bitset? iri-end-bitset is idx)
                     (inc idx))
           :else
           (recur-if (not (in-bitset? iri-banned-bitset is idx))
                     (inc idx)))))))

#?(:clj
   #_{:clj-kondo/ignore [:loop-without-recur]}
   (defn- valid-var-str?*
     [^String vs]
     (let [ccnt (count vs)]
       (loop [idx 0]
         (cond
           (>= idx ccnt)
           (<= 2 ccnt) ; Need to have initial qmark + at least one start char
           (= idx 0)
           (recur-if (in-bitset? qmark-bitset vs idx)
                     (inc idx))
           (= idx 1)
           (recur-if (in-bitset? var-start-bitset vs idx)
                     (inc idx))
           :else
           (recur-if (in-bitset? var-body-bitset vs idx)
                     (inc idx)))))))

#?(:clj
   #_{:clj-kondo/ignore [:loop-without-recur]}
   (defn- valid-bnode-str?*
     [^String bns]
     (let [ccnt (count bns)
           lidx (dec ccnt)]
       (loop [idx 0]
         (cond
           (>= idx ccnt)
           (<= 1 ccnt) ; Need to have at least the initial underscore
           (= idx 0)
           (recur-if (in-bitset? uscore-bitset bns idx)
                     (inc idx))
           (= idx 1)
           (recur-if (in-bitset? bnode-start-bitset bns idx)
                     (inc idx))
           (= idx lidx)
           (recur-if (in-bitset? bnode-end-bitset bns idx)
                     (inc idx))
           :else
           (recur-if (in-bitset? bnode-body-bitset bns idx)
                     (inc idx)))))))

#?(:clj
   #_{:clj-kondo/ignore [:loop-without-recur]}
   (defn- valid-prefix-ns-str?*
     [^String ns-str]
     (let [ccnt (count ns-str)
           lidx (dec ccnt)]
       (loop [idx 0]
         (cond
           (>= idx ccnt)
           (<= 1 ccnt) ; Need to have at least one char
           (= idx 0)
           (recur-if (in-bitset? prefix-ns-start-bitset ns-str idx)
                     (inc idx))
           (= idx lidx)
           (recur-if (in-bitset? prefix-ns-end-bitset ns-str idx)
                     (inc idx))
           :else
           (recur-if (in-bitset? prefix-ns-body-bitset ns-str idx)
                     (inc idx)))))))

#?(:clj
   #_{:clj-kondo/ignore [:loop-without-recur]}
   (defn- valid-prefix-name-str?*
     [^String name-str]
     (let [ccnt (count name-str)
           lidx (dec ccnt)]
       (loop [idx  0
              pesc 0]
         (cond
           ;; End of string
           (>= idx ccnt)
           (and (<= 1 ccnt)   ; Need to have at least one char
                (zero? pesc)) ; Cannot terminate in the middle of hex seq
           ;; Percent encoding
           (not= 0 pesc)
           (recur-if (in-bitset? hex-bitset name-str idx)
                     (inc idx)
                     (dec pesc))
           (in-bitset? percent-bitset name-str idx)
           (recur-if true
                     (inc idx)
                     2)
           ;; Not percent encoding
           (= idx 0)
           (recur-if (in-bitset? prefix-name-start-bitset name-str idx)
                     (inc idx)
                     0)
           (= idx lidx)
           (recur-if (in-bitset? prefix-name-end-bitset name-str idx)
                     (inc idx)
                     0)
           :else
           (recur-if (in-bitset? prefix-name-body-bitset name-str idx)
                     (inc idx)
                     0))))))

#?(:clj
   #_{:clj-kondo/ignore [:loop-without-recur]}
   (defn- valid-literal-str?*
     [^String s]
     (let [ccnt (count s)]
       (loop [idx  0
              esc? false]
         (cond
           (>= idx ccnt)
           (not esc?) ; Can't terminate in the middle of an escape sequence
           esc?
           (recur-if (in-bitset? string-escape-bitset s idx)
                     (inc idx)
                     false)
           :else
           (recur-if (not (in-bitset? string-banned-bitset s idx))
                     (inc idx)
                     (in-bitset? bslash-bitset s idx)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Validation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; IRIs and RDF terms

(defn- valid-iri-body-str?
  [iri-body-str]
  #?(:clj (valid-iri-body-str?* iri-body-str)
     :cljs (boolean (re-matches iri-body-regex iri-body-str))))

(defn- valid-iri-str?
  [iri-str]
  #?(:clj (valid-iri-str?* iri-str)
     :cljs (boolean (re-matches iri-regex iri-str))))

(defn- valid-prefix-ns-str?
  [prefix-ns-str]
  #?(:clj (valid-prefix-ns-str?* prefix-ns-str)
     :cljs (boolean (re-matches prefix-ns-regex prefix-ns-str))))

(defn- valid-prefix-name-str?
  [prefix-name-str]
  #?(:clj (valid-prefix-name-str?* prefix-name-str)
     :cljs (boolean (re-matches prefix-name-regex prefix-name-str))))

(defn valid-iri-string?*
  "Is `x` an IRI string? Note that `x` can be an otherwise non-IRI
   (e.g. `<foo>`)."
  [s]
  (valid-iri-body-str? s))

(defn valid-iri-string?
  "Is `x` a wrapped (i.e. starts with `<` and ends with `>`) IRI?
   Note that `x` can be an otherwise non-IRI (e.g. `<foo>`)."
  [s]
  (valid-iri-str? s))

(defn valid-prefix-keyword?
  "Is `k` a valid SPARQL prefix keyword?"
  [k]
  (boolean (and (nil? (namespace k))
                (or (valid-prefix-ns-str? (name k))
                    (= :$ k)))))

(defn valid-prefix-iri-keyword?
  "Is `k` a potentially namespaced keyword?"
  [k]
  (let [kns   (namespace k)
        kname (name k)]
    (boolean (and (not (#{:a :*} k))
                  (or (nil? kns) (valid-prefix-ns-str? kns))
                  (valid-prefix-name-str? kname)))))

(defn- valid-var-str?
  [var-str]
  #?(:clj (valid-var-str?* var-str)
     :cljs (boolean (re-matches var-regex var-str))))

(defn- valid-bnode-str?
  [bnode-str]
  #?(:clj (valid-bnode-str?* bnode-str)
     :cljs (boolean (re-matches bnode-regex bnode-str))))

(defn valid-var-symbol?
  "Is `var-sym` a symbol that starts with `?`?"
  [var-sym]
  (valid-var-str? (name var-sym)))

(defn valid-bnode-symbol?
  "Is `bnode-sym` a symbol that starts with `_` and has zero or more
   trailing chars?"
  [bnode-sym]
  (valid-bnode-str? (name bnode-sym)))

;; Literals

(defn- valid-literal-str?
  [str-lit]
  #?(:clj (valid-literal-str?* str-lit)
     :cljs (boolean (re-matches literal-regex str-lit))))

(defn valid-string-literal?
  "Is `str-lit` a string and does not contains unescaped `\"`, `\\`, `\\n`,
   nor `\\r`? (This filtering is to avoid SPARQL injection attacks.)"
  [str-lit]
  (valid-literal-str? str-lit))

(defn valid-lang-map-literal?
  "Is `lang-map` a singleton map between a language tag and valid string?"
  [lang-map]
  (boolean (and (->> lang-map count (= 1))
                (->> lang-map keys first keyword?)
                (->> lang-map vals first (re-matches literal-regex)))))

(comment
  (require '[criterium.core :as crit])

  ;; ASCII bench
  (crit/quick-bench
   (re-matches #"\?\w+" (name '?supercalifragilisticexpialidocious)))

  (crit/quick-bench
   (re-matches var-regex (name '?supercalifragilisticexpialidocious)))

  (crit/bench
   (re-matches var-regex (name '?x)))

  (crit/quick-bench
   (valid-var-symbol? '?supercalifragilisticexpialidocious))

  (crit/bench
   (valid-var-symbol? '?x))

  ;; Non-ASCII bench
  (crit/quick-bench
   (re-matches var-regex (name '?我们去西安吃面)))

  (crit/quick-bench
   (valid-var-symbol? '?我们今天吃西安炒面))

  ;; String literal bench
  (crit/quick-bench
   (re-matches literal-regex
               "\\\"supercalifragilisticexpialidocious\\\""))

  (crit/quick-bench
   (valid-literal-str? "\\\"supercalifragilisticexpialidocious\\\""))

  ;; Prefix name bench
  (crit/bench
   (re-matches prefix-name-regex (name :foo%80bar)))

  (crit/bench
   (valid-prefix-keyword? :foo%80bar)))

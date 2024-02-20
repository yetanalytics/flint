(ns com.yetanalytics.flint.axiom.impl
  {:clj-kondo/config
   '{:lint-as {com.yetanalytics.flint.axiom.impl/extend-protocol-default
               clojure.core/extend-protocol}}}
  (:require [com.yetanalytics.flint.axiom.protocol        :as p]
            [com.yetanalytics.flint.axiom.impl.format     :as fmt-impl]
            [com.yetanalytics.flint.axiom.impl.validation :as val-impl]
            #?(:clj [clojure.string :as cstr]))
  #?(:cljs (:require-macros [com.yetanalytics.flint.axiom.impl
                             :refer [extend-protocol-default]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Macros and Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:clj
   (defn- extend-protocol-default-err-msg
     [method protocol]
     ;; Base this off the IllegalArgumentException error message when you call
     ;; a protocol on a non-implementing instance.
     (format "Call of method: %s on default implementation of protocol %s is not permitted"
             method
             protocol)))

#?(:clj
   (defmacro extend-protocol-default
     "Perform `extend-protocol` on `protocol` and `types`, expanding it on each
      type as so:
   
     (extend-protocol protocol
       type
       (validation-f args false)
       (other-fn args (throw ex-info err-msg {}))
       ...)"
     [protocol types validation-fsig & fsigs]
     `(extend-protocol ~protocol
        ~@(mapcat
           (fn [type]
             (concat
              `(~type
                ~(concat validation-fsig '(false)))
              (map
               (fn [fsig#]
                 (let [fname# (first fsig#)
                       fargs# (rest fsig#)
                       ermsg# (extend-protocol-default-err-msg
                               fname#
                               protocol)]
                   (if (= 1 (count fargs#))
                     ;; Single arity
                     `(~fname# ~(first fargs#) (throw (ex-info ~ermsg# {})))
                     ;; Multiple arity
                     `(~fname# ~@(map (fn [farg#]
                                        `(~farg# (throw (ex-info ~ermsg# {}))))
                                      fargs#)))))
               fsigs)))
           (if (coll? types) types [types]))))

   ;; Forward declaration manages to shut up clj-kondo's undeclared symbol
   ;; errors in cljs mode
   :cljs
   (declare extend-protocol-default))

#?(:clj
   (defmacro extend-xsd-literal
     "Macro that expands into a basic `extend-type` over the Literal protocol.
      If `force-iri?` is true, then the resulting literal always has the XSD
      datatype IRI appended; `strval-fn` overrides the default call to `str`.
      Without any kwargs, `(extend-xsd-literal t iri-suffix)` expands into:
      
      ```
      (extend-type t p/Literal
        (-valid-literal?
          [_] true)
        (-format-literal
          ([n] (p/-format-literal n {}))
          ([n opts] (fmt-impl/format-literal n opts)))
        (-format-literal-strval
          [n] (str n))
        (-format-literal-lang-tag
          [n] nil)
        (-format-literal-url
          ([n] (p/-format-literal-url n {}))
          ([n opts] (fmt-impl/format-xsd-iri iri-suffix opts))))
      ```"
     [t iri-suffix & {:keys [force-iri?
                             strval-fn]
                      :or {strval-fn str}}]
     `(extend-type ~t
        p/Literal
        (~'-valid-literal? [~'_] true)
        (~'-format-literal
          ([~'n] (p/-format-literal ~'n {}))
          ([~'n ~'opts]
           ~(if force-iri?
              `(fmt-impl/format-literal ~'n (assoc ~'opts :force-iri? true))
              `(fmt-impl/format-literal ~'n ~'opts))))
        (~'-format-literal-strval [~'n]
          (~strval-fn ~'n))
        (~'-format-literal-lang-tag [~'_] nil)
        (~'-format-literal-url
          ([~'n] (p/-format-literal-url ~'n {}))
          ([~'_ ~'opts] (fmt-impl/format-xsd-iri ~iri-suffix ~'opts))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; IRIs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-protocol p/IRI
  #?(:clj String :cljs string)
  (-valid-iri? [s] (val-impl/valid-iri-string? s))
  (-unwrap-iri [s] (fmt-impl/unwrap-iri-string s))
  (-format-iri [s] s)

  ;; Don't extend java.net.URL due to the fact that its .equals method
  ;; performs HTTP resolution.
  #?(:clj java.net.URI
     :cljs js/URL)
  (-valid-iri? [uri] (val-impl/valid-iri-string?* (.toString uri)))
  (-format-iri [uri] (str "<" uri ">"))
  (-unwrap-iri [uri] (.toString uri)))

(extend-protocol p/Prefix
  #?(:clj clojure.lang.Keyword :cljs Keyword)
  (-valid-prefix? [k] (val-impl/valid-prefix-keyword? k))
  (-format-prefix [k] (fmt-impl/format-prefix-keyword k)))

(extend-protocol p/PrefixedIRI
  #?(:clj clojure.lang.Keyword :cljs Keyword)
  (-valid-prefix-iri? [k] (val-impl/valid-prefix-iri-keyword? k))
  (-format-prefix-iri [k] (fmt-impl/format-prefix-iri-keyword k)))

;; IRI defaults

(extend-protocol-default p/IRI
                         #?(:clj [Object nil] :cljs default)
                         (-valid-iri? [_])
                         (-format-iri [_])
                         (-unwrap-iri [_]))

(extend-protocol-default p/Prefix
                         #?(:clj [Object nil] :cljs default)
                         (-valid-prefix? [_])
                         (-format-prefix [_]))

(extend-protocol-default p/PrefixedIRI
                         #?(:clj [Object nil] :cljs default)
                         (-valid-prefix-iri? [_])
                         (-format-prefix-iri [_]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Variables, Blank Nodes, and other symbols
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-protocol p/Variable
  #?(:clj clojure.lang.Symbol :cljs Symbol)
  (-valid-variable? [v] (val-impl/valid-var-symbol? v))
  (-format-variable [v] (fmt-impl/format-var-symbol v)))

(extend-protocol p/BlankNode
  #?(:clj clojure.lang.Symbol :cljs Symbol)
  (-valid-bnode? [b] (val-impl/valid-bnode-symbol? b))
  (-format-bnode [b] (fmt-impl/format-bnode-symbol b)))

(extend-protocol p/Wildcard
  #?(:clj clojure.lang.Keyword :cljs Keyword)
  (-valid-wildcard? [k] (= :* k))
  (-format-wildcard [k] (name k))

  #?(:clj clojure.lang.Symbol :cljs Symbol)
  (-valid-wildcard? [sym] (= '* sym))
  (-format-wildcard [sym] (str sym)))

(extend-protocol p/RDFType
  #?(:clj clojure.lang.Keyword :cljs Keyword)
  (-valid-rdf-type? [k] (= :a k))
  (-format-rdf-type [k] (name k))

  #?(:clj clojure.lang.Symbol :cljs Symbol)
  (-valid-rdf-type? [sym] (= 'a sym))
  (-format-rdf-type [sym] (str sym)))

;; Defaults

(extend-protocol-default p/Variable
                         #?(:clj [Object nil] :cljs default)
                         (-valid-variable? [_])
                         (-format-variable [_]))

(extend-protocol-default p/BlankNode
                         #?(:clj [Object nil] :cljs default)
                         (-valid-bnode? [_])
                         (-format-bnode [_]))

(extend-protocol-default p/Wildcard
                         #?(:clj [Object nil] :cljs default)
                         (-valid-wildcard? [_])
                         (-format-wildcard [_]))

(extend-protocol-default p/RDFType
                         #?(:clj [Object nil] :cljs default)
                         (-valid-rdf-type? [_])
                         (-format-rdf-type [_]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Literals
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Basic Literals (strings, lang maps, booleans)

(extend-protocol p/Literal
  #?(:clj String :cljs string)
  (-valid-literal?
    [s]
    (val-impl/valid-string-literal? s))
  (-format-literal
    ([s] (str "\"" s "\"")) ; Special treatment of plain string literals
    ([s {:keys [force-iri?] :as opts}]
     (if force-iri?
       (fmt-impl/format-literal s opts)
       (p/-format-literal s))))
  (-format-literal-strval [s] s)
  (-format-literal-lang-tag [_] nil)
  (-format-literal-url
    ([s] (p/-format-literal-url s {}))
    ([_ opts] (fmt-impl/format-xsd-iri "string" opts)))

  #?(:clj clojure.lang.IPersistentMap
     :cljs PersistentArrayMap)
  (-valid-literal? [m]
    (val-impl/valid-lang-map-literal? m))
  (-format-literal
    ([m] (fmt-impl/format-lang-map-literal m))
    ([m _] (fmt-impl/format-lang-map-literal m)))
  (-format-literal-strval [m]
    (fmt-impl/format-lang-map-val m))
  (-format-literal-lang-tag [m]
    (fmt-impl/format-lang-map-tag m))
  (-format-literal-url ; Always unused during formatting
    ([n] (p/-format-literal-url n {}))
    ([_ opts] (fmt-impl/format-rdf-iri "langString" opts)))

  #?(:clj Boolean :cljs boolean)
  (-valid-literal? [_] true)
  (-format-literal
    ([n] (p/-format-literal-strval n))
    ([n opts] (fmt-impl/format-literal n opts)))
  (-format-literal-strval [n] (.toString n))
  (-format-literal-lang-tag [_] nil)
  (-format-literal-url
    ([n] (p/-format-literal-url n {}))
    ([_ opts] (fmt-impl/format-xsd-iri "boolean" opts))))

;; Numeric Literals

;; See: "XSD data types" in
;; https://jena.apache.org/documentation/notes/typed-literals.html

;; Note: Clojure decimal default is Double, integer default is Long
#?(:clj
   (do (extend-xsd-literal Float "float")
       (extend-xsd-literal Double "double")
       (extend-xsd-literal Integer "int")
       (extend-xsd-literal Long "long")
       (extend-xsd-literal Short "short")
       (extend-xsd-literal Byte "byte")
       (extend-xsd-literal java.math.BigDecimal "decimal")
       (extend-xsd-literal java.math.BigInteger "integer")
       (extend-xsd-literal clojure.lang.BigInt "integer")))

#?(:cljs
   (extend-protocol p/Literal
     number
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal n {}))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n]
        (p/-format-literal-url n {}))
       ([n opts]
        (if (js/Number.isInteger n)
          (fmt-impl/format-xsd-iri "integer" opts)
          (fmt-impl/format-xsd-iri "double" opts))))))

;; DateTime Literals

;; DateTime in Clojure covers all `inst?` values, i.e. java.time.Instant and
;; java.util.Date. The latter is included because that is the default class
;; `#inst` literals are evaluated to, even though it is deprecated for most
;; purposes. In addition, DateTime covers additional java.time.Temporal
;; classes, e.g. if a user wants to use ZonedDateTime to convey timezone info.

;; The java.util.Date class has the java.sql.Timestamp, Date, and Time
;; subclasses. The latter two have separate implementations since they throw
;; exceptions if `.toInstant` is directly called on them.

#?(:clj
   (defn- local-ts->local-str
     [^java.time.LocalDateTime local-ts]
     (.format java.time.format.DateTimeFormatter/ISO_LOCAL_DATE_TIME
              local-ts)))

#?(:clj
   (defn- zoned-ts->offset-str
     [^java.time.ZonedDateTime zoned-ts]
     (.format java.time.format.DateTimeFormatter/ISO_OFFSET_DATE_TIME
              (.toOffsetDateTime zoned-ts))))

#?(:clj
   (defn- offset-ts->offset-str
     [^java.time.OffsetDateTime offset-ts]
     (.format java.time.format.DateTimeFormatter/ISO_OFFSET_DATE_TIME
              offset-ts)))

#?(:clj
   (defn- local-time->local-str
     [^java.time.LocalTime local-time]
     (.format java.time.format.DateTimeFormatter/ISO_LOCAL_TIME
              local-time)))

#?(:clj
   (defn- offset-time->offset-str
     [^java.time.OffsetTime offset-time]
     (.format java.time.format.DateTimeFormatter/ISO_OFFSET_TIME
              offset-time)))

#?(:clj
   (defn- local-date->local-str
     [^java.time.LocalDate local-date]
     (.format java.time.format.DateTimeFormatter/ISO_LOCAL_DATE
              local-date)))

#?(:clj
   (defn- date->inst-str
     [^java.util.Date date-ts]
     (.toString (.toInstant date-ts))))

#?(:clj
   (defn- sql-date->inst-str
     [^java.sql.Date sql-date-ts]
     (let [millis  (.getTime sql-date-ts)
           instant (java.time.Instant/ofEpochMilli millis)]
       (-> (.toString ^java.time.Instant instant)
           (cstr/split #"T" 2)
           first))))

#?(:clj
   (defn- sql-time->inst-str
     [^java.sql.Time sql-time-ts]
     (let [millis  (.getTime sql-time-ts)
           instant (java.time.Instant/ofEpochMilli millis)]
       (-> (.toString ^java.time.Instant instant)
           (cstr/split #"T" 2)
           second))))

#?(:clj
   (do
     ;; java.time.Temporal classes
     (extend-xsd-literal java.time.Instant "dateTime"
                         :force-iri? true)
     (extend-xsd-literal java.time.ZonedDateTime "dateTime"
                         :strval-fn zoned-ts->offset-str
                         :force-iri? true)
     (extend-xsd-literal java.time.OffsetDateTime "dateTime"
                         :strval-fn offset-ts->offset-str
                         :force-iri? true)
     (extend-xsd-literal java.time.OffsetTime "time"
                         :strval-fn offset-time->offset-str
                         :force-iri? true)
     (extend-xsd-literal java.time.LocalDateTime "dateTime"
                         :strval-fn local-ts->local-str
                         :force-iri? true)
     (extend-xsd-literal java.time.LocalDate "date"
                         :strval-fn local-date->local-str
                         :force-iri? true)
     (extend-xsd-literal java.time.LocalTime "time"
                         :strval-fn local-time->local-str
                         :force-iri? true)
     ;; java.util.Date classes
     (extend-xsd-literal java.util.Date "dateTime"
                         :strval-fn date->inst-str
                         :force-iri? true)
     (extend-xsd-literal java.sql.Date "date"
                         :strval-fn sql-date->inst-str
                         :force-iri? true)
     (extend-xsd-literal java.sql.Time "time"
                         :strval-fn sql-time->inst-str
                         :force-iri? true)))

#?(:cljs
   (extend-protocol p/Literal
     js/Date
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal n {}))
       ([n opts] (fmt-impl/format-literal n (assoc opts :force-iri? true))))
     (-format-literal-strval [n] (.toISOString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "dateTime" opts)))))

;; Default Literal Implementation

(extend-protocol-default p/Literal
                         #?(:clj [Object nil] :cljs default)
                         (-valid-literal? [_])
                         (-format-literal [_] [_ _])
                         (-format-literal-strval [_])
                         (-format-literal-lang-tag [_])
                         (-format-literal-url [_] [_ _]))

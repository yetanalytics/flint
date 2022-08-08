(ns com.yetanalytics.flint.axiom.impl
  {:clj-kondo/config
   '{:lint-as {com.yetanalytics.flint.axiom.impl/extend-protocol-default
               clojure.core/extend-protocol}}}
  (:require [com.yetanalytics.flint.axiom.protocol        :as p]
            [com.yetanalytics.flint.axiom.impl.format     :as fmt-impl]
            [com.yetanalytics.flint.axiom.impl.validation :as val-impl])
  #?(:cljs (:require-macros [com.yetanalytics.flint.axiom.impl
                             :refer [extend-protocol-default]])))

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
  (-format-wildcard [sym] (name sym)))

(extend-protocol p/RDFType
  #?(:clj clojure.lang.Keyword :cljs Keyword)
  (-valid-rdf-type? [k] (= :a k))
  (-format-rdf-type [k] (name k))

  #?(:clj clojure.lang.Symbol :cljs Symbol)
  (-valid-rdf-type? [sym] (= 'a sym))
  (-format-rdf-type [sym] (name sym)))

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
;; Basic Literals (strings, lang maps, booleans)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Numeric Literals
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:clj
   ;; See: "XSD data types" in
   ;; https://jena.apache.org/documentation/notes/typed-literals.html
   (extend-protocol p/Literal
     ;; Decimal types
     Float
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal n {}))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "float" opts)))

     Double ; Clojure decimal default type
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal-strval n))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "double" opts)))

     java.math.BigDecimal
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal-strval n))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "decimal" opts)))

     ;; Integral types
     Integer
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal-strval n))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "int" opts)))

     Long ; Clojure integer default type
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal-strval n))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "long" opts)))

     Short
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal-strval n))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "short" opts)))

     Byte
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal-strval n))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "byte" opts)))

     java.math.BigInteger
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal-strval n))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "integer" opts)))

     clojure.lang.BigInt
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal-strval n))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "integer" opts))))

   :cljs
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DateTime Literals
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; DateTime in Clojure covers all `inst?` values, i.e. java.time.Instant and
;; java.util.Date. The latter is included because that is the default class
;; `#inst` literals are evaluated to, even though it is deprecated for most
;; purposes.

;; The java.util.Date class has the java.sql.Timestamp, Date, and Time
;; subclasses. The latter two have separate implementations since they throw
;; exceptions if `.toInstant` is directly called on them.

;; Despite their names these java.sql objects can hold both date and time
;; info, being wrappers for java.util.Date, hence the use of xsd:dateTime.

#?(:clj
   (defn- date->inst
     [^java.util.Date date-ts]
     (.toInstant date-ts)))

#?(:clj
   (defn- sql-date->inst
     [^java.sql.Date sql-date-ts]
     (.toInstant (java.sql.Timestamp. (.getTime sql-date-ts)))))

#?(:clj
   (defn- sql-time->inst
     [^java.sql.Time sql-time-ts]
     (.toInstant (java.sql.Timestamp. (.getTime sql-time-ts)))))

#?(:clj
   (extend-protocol p/Literal
     java.time.Instant
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal n {}))
       ([n opts] (fmt-impl/format-literal n (assoc opts :force-iri? true))))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "dateTime" opts)))

     java.util.Date
     (-valid-literal? [_] true)
     (-format-literal
       ([date-ts]
        (p/-format-literal (date->inst date-ts)))
       ([date-ts opts]
        (p/-format-literal (date->inst date-ts) opts)))
     (-format-literal-strval [date-ts]
       (p/-format-literal-strval (date->inst date-ts)))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "dateTime" opts)))

     java.sql.Date
     (-valid-literal? [_] true)
     (-format-literal
       ([sql-date-ts]
        (p/-format-literal (sql-date->inst sql-date-ts)))
       ([sql-date-ts opts]
        (p/-format-literal (sql-date->inst sql-date-ts) opts)))
     (-format-literal-strval [sql-date-ts]
       (p/-format-literal-strval (sql-date->inst sql-date-ts)))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "dateTime" opts)))

     java.sql.Time
     (-valid-literal? [_] true)
     (-format-literal
       ([sql-time-ts]
        (p/-format-literal (sql-time->inst sql-time-ts)))
       ([sql-time-ts opts]
        (p/-format-literal (sql-time->inst sql-time-ts) opts)))
     (-format-literal-strval [sql-time-ts]
       (p/-format-literal-strval (sql-time->inst sql-time-ts)))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "dateTime" opts))))

   :cljs
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Default Literals Implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-protocol-default p/Literal
                         #?(:clj [Object nil] :cljs default)
                         (-valid-literal? [_])
                         (-format-literal [_] [_ _])
                         (-format-literal-strval [_])
                         (-format-literal-lang-tag [_])
                         (-format-literal-url [_] [_ _]))

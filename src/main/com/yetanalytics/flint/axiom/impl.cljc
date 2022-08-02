(ns com.yetanalytics.flint.axiom.impl
  (:require [com.yetanalytics.flint.axiom.protocol        :as p]
            [com.yetanalytics.flint.axiom.impl.format     :as fmt-impl]
            [com.yetanalytics.flint.axiom.impl.validation :as val-impl]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; IRIs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-protocol p/IRI
  #?(:clj java.lang.String
     :cljs string)
  (-valid-iri? [s] (val-impl/valid-iri-string? s))
  (-unwrap-iri [s] (fmt-impl/unwrap-iri-string s))
  (-format-iri [s] s))

#?(:clj
   (extend-protocol p/IRI
     java.net.URI
     (-valid-iri? [uri] (val-impl/valid-iri-string?* (.toString uri)))
     (-format-iri [uri] (str "<" uri ">"))
     (-unwrap-iri [uri] (.toString uri))

     java.net.URL
     (-valid-iri? [url] (val-impl/valid-iri-string?* (.toString url)))
     (-format-iri [url] (str "<" url ">"))
     (-unwrap-iri [uri] (.toString uri)))
   
   :cljs
   (extend-protocol p/IRI
     js/URL
     (-valid-iri? [s] (val-impl/valid-iri-string?* (.toString s)))
     (-format-iri [s] (str "<" s ">"))
     (-unwrap-iri [uri] (.toString uri))))

(extend-protocol p/Prefix
  #?(:clj clojure.lang.Keyword :cljs Keyword)
  (-valid-prefix? [k] (val-impl/valid-prefix-keyword? k))
  (-format-prefix [k] (fmt-impl/format-prefix-keyword k)))

(extend-protocol p/PrefixedIRI
  #?(:clj clojure.lang.Keyword :cljs Keyword)
  (-valid-prefix-iri? [k] (val-impl/valid-prefix-iri-keyword? k))
  (-format-prefix-iri [k] (fmt-impl/format-prefix-iri-keyword k)))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Basic Literals (strings, lang maps, booleans)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-protocol p/Literal
  #?(:clj java.lang.String
     :cljs string)
  (-valid-literal?
    [s]
    (val-impl/valid-string-literal? s))
  (-format-literal
    ([s] (str "\"" s "\"")) ; Special treatment of plain string literals
    ([s {:keys [append-iri?] :as opts}]
     (if append-iri?
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

  #?(:clj java.lang.Boolean
     :cljs boolean)
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
   (extend-protocol p/Literal
     ;; See: "XSD data types" in
     ;; https://jena.apache.org/documentation/notes/typed-literals.html
     java.lang.Float
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal n {}))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "float" opts)))

     java.lang.Double
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal-strval n))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "double" opts)))

     java.lang.Integer
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal-strval n))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "integer" opts)))

     java.lang.Long
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal-strval n))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "long" opts)))

     java.lang.Short
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal-strval n))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "short" opts)))

     java.lang.Byte
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

     java.math.BigDecimal
     (-valid-literal? [_] true)
     (-format-literal
       ([n] (p/-format-literal-strval n))
       ([n opts] (fmt-impl/format-literal n opts)))
     (-format-literal-strval [n] (.toString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "decimal" opts))))

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
       ([n opts] (fmt-impl/format-literal n (assoc opts :append-iri? true))))
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

     ;; Despite their names these java.sql objects can hold both date and time
     ;; info, being wrappers for java.util.Date, hence the use of xsd:dateTime.
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
       ([n opts] (fmt-impl/format-literal n (assoc opts :append-iri? true))))
     (-format-literal-strval [n] (.toISOString n))
     (-format-literal-lang-tag [_] nil)
     (-format-literal-url
       ([n] (p/-format-literal-url n {}))
       ([_ opts] (fmt-impl/format-xsd-iri "dateTime" opts)))))

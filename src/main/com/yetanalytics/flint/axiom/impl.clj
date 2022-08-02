(ns com.yetanalytics.flint.axiom.impl
  (:require [com.yetanalytics.flint.axiom.protocol        :as p]
            [com.yetanalytics.flint.axiom.impl.format     :as fmt-impl]
            [com.yetanalytics.flint.axiom.impl.validation :as val-impl]))

(extend-protocol p/IRI
  java.lang.String
  (-valid-iri? [s] (val-impl/valid-iri-string? s))
  (-format-iri [s] s)

  java.net.URI
  (-valid-iri? [uri] (val-impl/valid-iri-string?* (.toString uri)))
  (-format-iri [uri] (str "<" uri ">"))

  java.net.URL
  (-valid-iri? [url] (val-impl/valid-iri-string?* (.toString url)))
  (-format-iri [url] (str "<" url ">")))

(extend-protocol p/PrefixedIRI
  clojure.lang.Keyword
  (-valid-prefix-iri? [k] (val-impl/valid-prefix-iri-keyword? k))
  (-format-prefix-iri [k] (fmt-impl/format-prefix-iri-keyword k)))

(extend-protocol p/Variable
  clojure.lang.Symbol
  (-valid-variable? [v] (val-impl/valid-var-symbol? v))
  (-format-variable [v] (fmt-impl/format-var-symbol v)))

(extend-protocol p/BlankNode
  clojure.lang.Symbol
  (-valid-bnode? [b] (val-impl/valid-bnode-symbol? b))
  (-format-bnode [b] (fmt-impl/format-bnode-symbol b)))

;; Date helpers

(defn- date->inst
  [^java.util.Date date-ts]
  (.toInstant date-ts))

(defn- sql-date->inst
  [^java.sql.Date sql-date-ts]
  (.toInstant (java.sql.Timestamp. (.getTime sql-date-ts))))

(defn- sql-time->inst
  [^java.sql.Time sql-time-ts]
  (.toInstant (java.sql.Timestamp. (.getTime sql-time-ts))))

(extend-protocol p/Literal
  ;; String literals
  java.lang.String
  (-valid-literal?
    [s]
    (val-impl/valid-string-literal? s))
  (-format-literal
    ([s] (str "\"" s "\"")) ; Special treatment of plain string iterals
    ([s opts] (fmt-impl/format-literal s opts)))
  (-format-literal-strval [s] s)
  (-format-literal-lang-tag [_] nil)
  (-format-literal-url
    ([s] (p/-format-literal-url s {}))
    ([_ opts] (fmt-impl/format-xsd-iri "string" opts)))

  ;; Language Map literals
  clojure.lang.IPersistentMap
  (-valid-literal? [m]
    (val-impl/valid-lang-map-literal? m))
  (-format-literal
    ([m] (p/-format-literal m {}))
    ([m _] (fmt-impl/format-lang-map-literal m)))
  (-format-literal-strval [m]
    (fmt-impl/format-lang-map-val m))
  (-format-literal-lang-tag [m]
    (fmt-impl/format-lang-map-tag m))
  (-format-literal-url ; Always unused during formatting
    ([n] (p/-format-literal-url n {}))
    ([_ opts] (fmt-impl/format-rdf-iri "langString" opts)))

  ;; Numeric literals
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
    ([_ opts] (fmt-impl/format-xsd-iri "decimal" opts)))

  ;; Boolean literals
  java.lang.Boolean
  (-valid-literal? [_] true)
  (-format-literal
    ([n] (p/-format-literal-strval n))
    ([n opts] (fmt-impl/format-literal n opts)))
  (-format-literal-strval [n] (.toString n))
  (-format-literal-lang-tag [_] nil)
  (-format-literal-url
    ([n] (p/-format-literal-url n {}))
    ([_ opts] (fmt-impl/format-xsd-iri "boolean" opts)))

  ;; DateTime literals
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

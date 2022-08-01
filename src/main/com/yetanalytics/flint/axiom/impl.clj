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

(extend-protocol p/Literal
  ;; String literals
  java.lang.String
  (-valid-literal?
    [s]
    (val-impl/valid-string-literal? s))
  (-format-literal
    ([s] (fmt-impl/format-string-literal s))
    ([s _] (fmt-impl/format-string-literal s)))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] nil)

  ;; Language Map literals
  clojure.lang.IPersistentMap
  (-valid-literal?
    [m]
    (val-impl/valid-lang-map-literal? m))
  (-format-literal
    ([m] (fmt-impl/format-lang-map-literal m))
    ([m _] (fmt-impl/format-lang-map-literal m)))
  (-literal-lang-tag
    [m]
   (fmt-impl/format-lang-map-tag m))
  (-literal-url
   [_]
   nil)

  ;; Numeric literals
  java.lang.Float
  (-valid-literal? [_] true)
  (-format-literal ([n] (.toString n)) ([n _] (.toString n)))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#float")

  java.lang.Double
  (-valid-literal? [_] true)
  (-format-literal ([n] (.toString n)) ([n _] (.toString n)))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#double")

  java.lang.Integer
  (-valid-literal? [_] true)
  (-format-literal ([n] (.toString n)) ([n _] (.toString n)))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#integer")

  java.lang.Long
  (-valid-literal? [_] true)
  (-format-literal ([n] (.toString n)) ([n _] (.toString n)))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#long")

  java.lang.Short
  (-valid-literal? [_] true)
  (-format-literal ([n] (.toString n)) ([n _] (.toString n)))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#short")

  java.lang.Byte
  (-valid-literal? [_] true)
  (-format-literal ([n] (.toString n)) ([n _] (.toString n)))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#byte")

  java.math.BigInteger
  (-valid-literal? [_] true)
  (-format-literal ([n] (.toString n)) ([n _] (.toString n)))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#integer")

  java.math.BigDecimal
  (-valid-literal? [_] true)
  (-format-literal ([n] (.toString n)) ([n _] (.toString n)))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#double")

  ;; Boolean literals
  java.lang.Boolean
  (-valid-literal? [_] true)
  (-format-literal ([b] (.toString b)) ([b _] (.toString b)))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#boolean")

  ;; DateTime literals
  java.time.Instant
  (-valid-literal? [_] true)
  (-format-literal
   ([inst-ts]
    (fmt-impl/format-xsd-typed-literal inst-ts "dateTime"))
   ([inst-ts prefixes]
    (fmt-impl/format-xsd-typed-literal inst-ts "dateTime" prefixes)))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#dateTime")

  java.util.Date
  (-valid-literal? [_] true)
  (-format-literal
    ([date-ts]
     (p/-format-literal (.toInstant date-ts)))
    ([date-ts prefixes]
     (p/-format-literal (.toInstant date-ts) prefixes)))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#dateTime")

  ;; Despite their names these java.sql objects can hold both date and time
  ;; info, being wrappers for java.util.Date, hence the use of xsd:dateTime.
  java.sql.Date
  (-valid-literal? [_] true)
  (-format-literal
    ([date-ts]
     (p/-format-literal date-ts {}))
    ([date-ts prefixes]
     (let [ts (.toInstant (java.sql.Timestamp. (.getTime date-ts)))]
       (p/-format-literal ts prefixes))))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#dateTime")

  java.sql.Time
  (-valid-literal? [_] true)
  (-format-literal
    ([date-ts]
     (p/-format-literal date-ts {}))
    ([date-ts prefixes]
     (let [ts (.toInstant (java.sql.Timestamp. (.getTime date-ts)))]
       (p/-format-literal ts prefixes))))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#dateTime"))

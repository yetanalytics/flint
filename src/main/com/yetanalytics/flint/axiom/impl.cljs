(ns com.yetanalytics.flint.axiom.impl
  (:require [com.yetanalytics.flint.axiom.protocol        :as p]
            [com.yetanalytics.flint.axiom.impl.format     :as fmt-impl]
            [com.yetanalytics.flint.axiom.impl.validation :as val-impl]))

(extend-protocol p/IRI
  string
  (-valid-iri? [s] (val-impl/valid-iri-string? s))
  (-format-iri [s] s)

  js/URL
  (-valid-iri? [s] (val-impl/valid-iri-string?* (.toString s)))
  (-format-iri [s] (str "<" s ">")))

(extend-protocol p/PrefixedIRI
  Keyword
  (-valid-prefix-iri? [k] (val-impl/valid-prefix-iri-keyword? k))
  (-format-prefix-iri [k] (fmt-impl/format-prefix-iri-keyword k)))

(extend-protocol p/Variable
  Symbol
  (-valid-variable? [v] (val-impl/valid-var-symbol? v))
  (-format-variable [v] (fmt-impl/format-var-symbol v)))

(extend-protocol p/BlankNode
  Symbol
  (-valid-bnode? [b] (val-impl/valid-bnode-symbol? b))
  (-format-bnode [b] (fmt-impl/format-bnode-symbol b)))

(extend-protocol p/Literal
  ;; String literals
  string
  (-format-literal
    ([s] (fmt-impl/format-string-literal s))
    ([s _] (fmt-impl/format-string-literal s)))
  (-valid-literal? [s] (val-impl/valid-string-literal? s))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] nil)

  ;; Lang map literals
  PersistentArrayMap
  (-format-literal
    ([m] (fmt-impl/format-lang-map-literal m))
    ([m _] (fmt-impl/format-lang-map-literal m)))
  (-valid-literal? [m] (val-impl/valid-lang-map-literal? m))
  (-literal-lang-tag [m] (fmt-impl/format-lang-map-tag m))
  (-literal-url [_] nil)

  ;; Numeric literals
  number
  (-valid-literal? [_] true)
  (-format-literal ([n] (.toString n)) ([n _] (.toString n)))
  (-literal-lang-tag [_] nil)
  (-literal-url [n]
    (if (js/Number.isInteger n)
      "http://www.w3.org/2001/XMLSchema#integer"
      "http://www.w3.org/2001/XMLSchema#double"))

  ;; Boolean literals
  boolean
  (-valid-literal? [_] true)
  (-format-literal ([b] (.toString b)) ([b _] (.toString b)))
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#boolean")

  ;; DateTime literals
  js/Date
  (-format-literal
    ([date-ts]
     (fmt-impl/format-xsd-typed-literal (.toISOString date-ts)
                                        "dateTime"))
    ([date-ts prefixes]
     (fmt-impl/format-xsd-typed-literal (.toISOString date-ts)
                                        "dateTime"
                                        prefixes)))
  (-valid-literal? [_] true)
  (-literal-lang-tag [_] nil)
  (-literal-url [_] "http://www.w3.org/2001/XMLSchema#dateTime"))

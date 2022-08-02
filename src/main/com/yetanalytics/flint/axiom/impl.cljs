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
  (-valid-literal? [s] (val-impl/valid-string-literal? s))
  (-format-literal
    ([s] (str "\"" s "\"")) ; Special treatment of plain string literals
    ([s opts] (fmt-impl/format-literal s opts)))
  (-format-literal-strval [s] s)
  (-format-literal-lang-tag [_] nil)
  (-format-literal-url
    ([s] (p/-format-literal-url s {}))
    ([_ opts] (fmt-impl/format-xsd-iri "string" opts)))

  ;; Lang map literals
  PersistentArrayMap
  (-valid-literal? [m] (val-impl/valid-lang-map-literal? m))
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
  
  ;; Numeric literals
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
       (fmt-impl/format-xsd-iri "double" opts))))

  ;; Boolean literals
  boolean
  (-valid-literal? [_] true)
  (-format-literal
    ([n] (p/-format-literal n {}))
    ([n opts] (fmt-impl/format-literal n opts)))
  (-format-literal-strval [n] (.toString n))
  (-format-literal-lang-tag [_] nil)
  (-format-literal-url
    ([n] (p/-format-literal-url n {}))
    ([_ opts] (fmt-impl/format-xsd-iri "boolean" opts)))

  ;; DateTime literals
  js/Date
  (-valid-literal? [_] true)
  (-format-literal
    ([n] (p/-format-literal n {}))
    ([n opts] (fmt-impl/format-literal n (assoc opts :append-iri? true))))
  (-format-literal-strval [n] (.toISOString n))
  (-format-literal-lang-tag [_] nil)
  (-format-literal-url
    ([n] (p/-format-literal-url n {}))
    ([_ opts] (fmt-impl/format-xsd-iri "dateTime" opts))))

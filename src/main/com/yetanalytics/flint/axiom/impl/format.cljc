(ns com.yetanalytics.flint.axiom.impl.format
  (:require [com.yetanalytics.flint.axiom.iri      :as iri]
            [com.yetanalytics.flint.axiom.protocol :as p]
            #?@(:cljs
                [[goog.string :refer [format]]
                 [goog.string.format]])))

;; IRIs and RDF terms

(defn format-prefix-iri-keyword [k]
  (let [kns   (namespace k)
        kname (name k)]
    (str kns ":" kname)))

(defn format-var-symbol [v-sym]
  (name v-sym))

(defn format-bnode-symbol [b-sym]
  (if-some [suffix (second (re-matches #"_(.+)" (name b-sym)))]
    (format "_:%s" suffix)
    "[]"))

;; Lang Map Literals

(defn format-lang-map-tag [lang-map]
  (-> lang-map keys first name))

(defn format-lang-map-val [lang-map]
  (-> lang-map vals first))

(defn format-lang-map-literal [lang-map]
  (let [ltag (format-lang-map-tag lang-map)
        lval (format-lang-map-val lang-map)]
    (format "\"%s\"@%s" lval ltag)))

;; Common format functions

(defn format-xsd-iri
  "Create an XSD datatype IRI of the form `(str xsd-prefix xsd-suffix)`,
   where `xsd-suffix` should be a string like `\"boolean\"` or `\"dateTime\"`.
   If `iri-prefix-m` is provided, it will use the prefix associated with
   the XSD IRI prefix."
  [xsd-suffix {:keys [iri-prefix-m]}]
  (if-some [xsd-prefix (get iri-prefix-m iri/xsd-iri-prefix)]
    (format "%s:%s" (name xsd-prefix) xsd-suffix)
    (format "<%s%s>" iri/xsd-iri-prefix xsd-suffix)))

(defn format-rdf-iri
  "Similar to `format-xsd-iri`, but for the RDF datatype IRI."
  [rdf-suffix {:keys [iri-prefix-m]}]
  (if-some [rdf-prefix (get iri-prefix-m iri/rdf-iri-prefix)]
    (format "%s:%s" (name rdf-prefix) rdf-suffix)
    (format "<%s%s>" iri/rdf-iri-prefix rdf-suffix)))

(defn format-literal
  "Create a literal of the form `\"strval^^iri\"`. If `append-url?` is `true`
   then the datatype IRI will be appended, and `iri-prefix-m` will map any
   IRI string prefix to a keyword prefix. `literal` should extend
   `p/Literal` and as such implement `p/-format-literal-strval` and
   `p/-format-literal-url`."
  [literal {:keys [append-iri? _iri-prefix-m] :as opts}]
  (let [strval (p/-format-literal-strval literal)]
    (if append-iri?
      (format "\"%s\"^^%s"
              strval
              (p/-format-literal-url literal opts))
      strval)))

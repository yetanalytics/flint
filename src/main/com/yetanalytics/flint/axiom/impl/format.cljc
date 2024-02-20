(ns com.yetanalytics.flint.axiom.impl.format
  (:require [com.yetanalytics.flint.axiom.iri      :as iri]
            [com.yetanalytics.flint.axiom.protocol :as p]))

;; IRIs, Vars, and Blank Nodes

(defn unwrap-iri-string
  "Given a string `s` of the form `<iri>`, return `iri`."
  [^String s]
  (.substring s 1 (dec (count s))))

(defn format-prefix-keyword
  "Return the string of the keyword `k`, or the empty string if `k` is `:$`."
  [k]
  (if (= :$ k) "" (name k)))

(defn format-prefix-iri-keyword
  "Given a potentially-qualified keyword `k`, return a prefixed IRI in the
   form `prefix:name`."
  [k]
  (let [kns   (namespace k)
        kname (name k)]
    (str kns ":" kname)))

(defn format-var-symbol
  "Return the var `v-sym` as a string of the form `?var`."
  [v-sym]
  (str v-sym))

(defn format-bnode-symbol
  "Return the bnode `b-sym` as a string of the form `_:bnode`. Returns
   `[]` if `b-sym` is a single underscore."
  [b-sym]
  (if (not= '_ b-sym)
    (let [^String s  (str b-sym)
          sub-string (.substring s 1 (count s))]
      (str "_:" sub-string))
    "[]"))

;; Lang Map Literals

(defn format-lang-map-tag
  "Return the lang tag string from `lang-map`."
  [lang-map]
  (-> lang-map keys first name))

(defn format-lang-map-val
  "Return the string literal value from `lang-map`."
  [lang-map]
  (-> lang-map vals first))

(defn format-lang-map-literal
  "Format `lang-map` into a string of the form `\"value\"@lang-tag`."
  [lang-map]
  (let [ltag (format-lang-map-tag lang-map)
        lval (format-lang-map-val lang-map)]
    (str "\"" lval "\"@" ltag)))

;; Common format functions

(defn format-xsd-iri
  "Create an XSD datatype IRI of the form `(str xsd-prefix xsd-suffix)`,
   where `xsd-suffix` should be a string like `\"boolean\"` or `\"dateTime\"`.
   If `iri-prefix-m` is provided, it will use the prefix associated with
   the XSD IRI prefix."
  [xsd-suffix {:keys [iri-prefix-m]}]
  (if-some [xsd-prefix (get iri-prefix-m iri/xsd-iri-prefix)]
    (str (name xsd-prefix) ":" xsd-suffix)
    (str "<" iri/xsd-iri-prefix xsd-suffix ">")))

(defn format-rdf-iri
  "Similar to `format-xsd-iri`, but for the RDF datatype IRI."
  [rdf-suffix {:keys [iri-prefix-m]}]
  (if-some [rdf-prefix (get iri-prefix-m iri/rdf-iri-prefix)]
    (str (name rdf-prefix) ":" rdf-suffix)
    (str "<" iri/rdf-iri-prefix rdf-suffix ">")))

;; This could cause fowrard declaration problems, but in practice the impls
;; should have already been defined when `format-literal` is called.
(defn format-literal
  "Create a literal of the form `\"strval^^iri\"`. If `force-iri?` is `true`
   then the datatype IRI will be appended, and `iri-prefix-m` will map any
   IRI string prefix to a keyword prefix. `literal` should extend
   `p/Literal` and as such implement `p/-format-literal-strval` and
   `p/-format-literal-url`."
  [literal {:keys [force-iri? _iri-prefix-m] :as opts}]
  (let [strval (p/-format-literal-strval literal)]
    (if force-iri?
      (str "\"" strval "\"^^" (p/-format-literal-url literal opts)) 
      strval)))

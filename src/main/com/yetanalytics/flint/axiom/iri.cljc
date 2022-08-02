(ns com.yetanalytics.flint.axiom.iri)

(def xsd-iri-prefix
  "The XSD IRI prefix string."
  "http://www.w3.org/2001/XMLSchema#")

(def xsd-iri-prefix-formatted
  "The XSD IRI prefix string, post-Flint/SPARQL formatting."
  "<http://www.w3.org/2001/XMLSchema#>")

(defn- xsd-iri-prefix-str
  [prefix]
  (or (some-> prefix name (str ":"))
      xsd-iri-prefix))

(defn xsd-iri
  "Return an XSD datatype IRI, which will be prefixed by `xsd-prefix`,
   or the default full `xsd-iri-prefix` if `nil`."
  ([xsd-suffix]
   (xsd-iri nil xsd-suffix))
  ([xsd-prefix xsd-suffix]
   (str (xsd-iri-prefix-str xsd-prefix) xsd-suffix)))

(def rdf-iri-prefix
  "http://www.w3.org/1999/02/22-rdf-syntax-ns#")

(def rdf-iri-prefix-formatted
  "<http://www.w3.org/1999/02/22-rdf-syntax-ns>")

(defn- rdf-iri-prefix-str
  [prefix]
  (or (some-> prefix name (str ":"))
      rdf-iri-prefix))

(defn rdf-iri
  ([rdf-suffix]
   (rdf-iri nil rdf-suffix))
  ([rdf-prefix rdf-suffix]
   (str (rdf-iri-prefix-str rdf-prefix) rdf-suffix)))

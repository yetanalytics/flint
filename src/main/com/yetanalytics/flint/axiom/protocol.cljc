(ns com.yetanalytics.flint.axiom.protocol)

;; We want to keep IRI and PrefixedIRI as seprate protocols since we want
;; them to be distinguished with different tags in the AST, for the sake
;; of prefix validation.

(defprotocol IRI
  "A SPARQL full IRI (e.g. `<http://foo.org>`)."
  (-valid-iri? [this]
    "Return `true` if `this` is a valid full IRI of its type.")
  (-format-iri [this]
    "Convert the full IRI `this` into its string representation."))

(defprotocol PrefixedIRI
  "A SPARQL prefixed IRI (e.g. `foo:bar`)."
  (-valid-prefix-iri? [this]
    "Return `true` if `this` is a valid prefixed IRI of its type.")
  (-format-prefix-iri [this]
    "Convert the prefixed IRI `this` into its string representation."))

(defprotocol Variable
  "A SPARQL variable (e.g. `?var`)."
  (-valid-variable? [this]
    "Return `true` if `this` is a valid variable of its type.")
  (-format-variable [this]
    "Convert the variable `this` into its string representation."))

(defprotocol BlankNode
  "A SPARQL blank node (e.g. `_:b0`)."
  (-valid-bnode? [this]
    "Return `true` if `this` is a valid blank node of its type.")
  (-format-bnode [this]
    "Convert the blank node `this` into its string representation."))

(defprotocol Literal
  "A SPARQL literal (e.g. `\"foo\"`, `\"bar\"@en`, `2`, and `true`)."
  (-valid-literal? [this]
    "Return `true` if `this` is a valid literal of its type.")
  (-format-literal [this] [this opts]
    "Convert the literal `this` into its string representation.
     The `opts` arg map is implementation-specific, but two common args
     are `:append-iri?` to force the appending of the datatype IRI suffix
     and `:iri-prefix-m` to map full datatype IRI strings to prefixes.")
  (-format-literal-strval [this]
    "Return the string value reprentation of `this` literal.")
  (-format-literal-lang-tag [this]
    "Return the language tag string associated with `this` literal.
     Returns `nil` if the literal does not have any language tags
     or is a typed literal.")
  (-format-literal-url [this] [this opts]
    "Return the RDF datatype URL associated with `this` literal.
     The `opts` arg map is implementation-specific, but a common arg is
     `:iri-prefix-m` to map full datatype IRI strings to prefixes."))

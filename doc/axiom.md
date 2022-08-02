# RDF Terms

Reference: [4.1 RDF Term Syntax](https://www.w3.org/TR/sparql11-query/#syntaxTerms)

This section discusses IRIs, variables, blank nodes, and literals in Flint and SPARQL. Many of the conventions here, including for prefixed IRIs, variables, and blank nodes, were borrowed from the [Datomic query and update grammar](https://docs.datomic.com/on-prem/query/query.html).

**NOTE:** For simplicity, many terms in Flint only allow a subset of the characters that the SPARQL spec allows. For example, the latter often accepts Unicode characters, while Flint is ASCII-only outside of IRIs or string literals.

## IRIs

Internationalized Resource Identifiers (IRIs) and their subset Universal Resource Identifiers (URIs) are represented in two ways: as full IRIs or as prefixed IRIs.

### Full IRIs

Examples: `<http://absolute-iri-example.com/>`, `<relative-iri>`, `(java.net.URL. <http://foo.org>)`

Full IRIs in Flint are written as one of the following:
- Strings of the form `<my-iri-string>`. The string inside the angle bracket pair can include any characters **except** for whitespace, `^`, `<`, `>`, `"`, `\`, `|`, or `` ` ``. Translating to SPARQL does not affect full IRIs.
- Instances of either `java.net.URL`, `java.net.URI` (in Clojure), or `js/URL` (in ClojureScript). The inner string must follow the above restrictions.

**NOTE:** This can mean that any string can become a IRI in Flint, though in practice they should conform to the [specification for IRIs](https://www.google.com/search?q=iri+spec&oq=IRI+spec&aqs=chrome.0.69i59j0i512j0i22i30l5.2040j0j7&sourceid=chrome&ie=UTF-8) after expansion.

### Prefixed IRIs

Examples: `:my-prefix/foo`, `:bar`

Prefixed IRIs in Flint are written as keywords of the form `:prefix/name`, where the prefix is optional. When translating to SPARQL, prefixed IRIs are transformed into the form `prefix:name`.

Prefixed IRIs accept word characters and hyphens, with the exception of the first prefix char (which does not allow digits); periods can also be included in the middle of the prefix.

**NOTE:** Prefixed IRIs must have the prefix be an entry in the `:prefixes` map; otherwise, validation will fail (unless `:validate?` is set to `false`).

### `a`

Examples: `a` and `:a`.

There is a third IRI representation allowed in Flint for predicates: the keyword `:a`/symbol `a`. This is provided as syntactic sugar to compactly represent the IRI:
```sparql
<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>
```
Translating to SPARQL does not affect `:a`/`a` other than stringifying it.

Therefore, in the the following query:

```clojure
{:prefixes {:rdf "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"}
 :ask      []
 :where    [[?x "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" ?y]
            [?x :rdf/type ?y]
            [?x :a ?y]
            [?x a ?y]]}
```
the `:where` triples are all equivalent.

## Variables

Examples: `?var`

Variables are written as symbols prefixed with a question mark `?`. The characters after the question mark can be any word character. Translating to SPARQL does not change the variable other than stringifying it.

## Blank Nodes

Examples: `_`, `_b0`

Blank nodes are written as symbols prefixed with an underscore `_`. When translating a blank node to SPARQL, a colon is added after the underscore, e.g. `_:b0`. The exception is `_`, which is rewritten as `[]` instead.

The characters after the underscore can be written as any word character; periods are also allowed in the middle.

**NOTE:** Blank nodes have certain restrictions: they cannot be used in any delete-related clauses, nor can the same blank node be repeated across different [basic graph patterns](where.md) or SPARQL updates.

## Wildcard

Examples: `*` and `:*`

The wildcard is used in certain query clauses and expressions in order to "return everything". It can be written as either a symbol or keyword.

## Literals

Flint supports the following literals by default: simple strings, language-tagged strings, numbers, booleans, and dateTime timestamps.

During SPARQL translation, an IRI denoting the datatype is added as a suffix
if `:force-iris?` is `true` or if it is not a primitive type (e.g. timestamps). For example, the following is a stringified `dateTime` timestamp with an appended datatype IRI:
```sparql
"2022-01-01T10:10:10Z"^^<http://www.w3.org/2001/XMLSchema#dateTime>
```
If one includes an entry for the XMLSchema IRI prefix in their prefixes map, they can shorten the resulting string considerably; for example, if that IRI prefix is associated with `:xsd`, then the string becomes:
```sparql
"2022-01-01T10:10:10Z"^^xsd:dateTime
```

### Numbers

Examples: `0`, `-2`, `3.14`

Numbers cover both integers and doubles, which are represented as integer and double literals in SPARQL, respectively. Neither are transformed during SPARQL translation beyond stringification. Note that the datatype IRI will vary depending on whether it is a double or integer, as well the underlying representation (e.g. Clojure integers will have the datatype IRI `xsd:long` as they are Java long values by default).

### Booleans

Examples: `true` and `false`

Booleans are not transformed during SPARQL translation beyond stringification.

### Strings

Examples: `"Hello World!"`, `"你好世界"`, `"cat: \\\"meow\\\""`, `"foo\\nbar"`

String literals can contain any characters **except** unescaped line breaks, carriage returns, backslashes, or double quotes; this is in order to prevent SPARQL injection attacks. (Therefore strings like `"cat: \"meow\"` and `"foo\nbar"` are not allowed.) Strings are not transformed during SPARQL translation.

### Language Maps

Examples: `{:en "Hello World!"}`, `{:zh "你好世界"}`

Strings with language tags are represented by a map between **one** language tag keyword and the string literal (which follows the same restrictions as simple strings). Note that this is the only literal that cannot have a datatype IRI appended, even if `:force-iris?` is `true`.

### Timestamps

Examples: `#inst "2022-01-01T10:10:10Z"`

Timestamps are any values for which `inst?` is `true`, i.e. an instance of one of the following:
- `java.time.Instant` (Clojure)
- `java.util.Date` (Clojure)
- `java.sql.Date` (Clojure)
- `java.sql.Time` (Clojure)
- `js/Date` (ClojureScript)

As mentioned above, timestamps will be stringified (in the Clojure case, they will be stringified as `java.time.Instant` instances), and will have the `xsd:dateTime` IRI appended regardless of the value of `:force-iris?`.

### Custom Literals

A user can implement a custom literal by extending `com.yetanalytics.flint.axiom.protocol/Literal` and defining the following:
- `-valid-literal?` to validate the value.
- `-format-literal` to format the entire string.
- `-format-literal-strval`, `-format-literal-lang-tag`, and `-format-literal-url` to format individual aspects of the literal value.

Note that `-format-literal` and `-format-literal-url` also accept an `opts` map for optional arguments that affect formatting. Currently implement literals accept the args `:force-iri?` (to force-append datatype IRIs) and `:iri-prefix-m` (a map from base IRI strings to prefixes to shorten datatype IRIs).

Here is an example of an implementation of a `Rational` literal (inspired by an example given by the [Apache Jena documentation](https://jena.apache.org/documentation/notes/typed-literals.html)):

```clojure
(require '[com.yetanalytics.flint.axiom.protocol :as p])

(defrecord Rational [numerator denominator]
  p/Literal
  (p/-valid-literal? [_rational]
    (and (int? numerator)
         (int? denominator)
         (not (zero? denominator))))
  (p/-format-literal [rational]
    (p/-format-literal rational {}))
  (p/-format-literal [rational opts]
    (str "\"" (p/-format-literal-strval rational)
         "\"^^" (p/-format-literal-url rational opts)))
  (p/-format-literal-strval [_rational]
    (str numerator "/" denominator))
  (p/-format-literal-lang-tag [_rational]
    nil)
  (p/-format-literal-url [rational]
    (p/-format-literal-url rational {}))
  (p/-format-literal-url [_rational {:keys [iri-prefix-m]}]
    (if-some [prefix (get iri-prefix-m "http://foo.org/literals#")]
      (str (name prefix) ":rational")
      "<http://foo.org/literals#rational>")))
```

which can then be used in a SPARQL query as so:
```clojure
(def rational-value (map->Rational {:numerator 5 :denominator 6}))

{:prefixes {:foo "<http://foo.org/literals#>"}
 :select   ['?x]
 :where    [['?x '?y rational-value]]}}
```
and thus becomes:
```sparql
PREFIX foo: http://foo.org/literals#
SELECT ?x
WHERE {
    ?x ?y \"5/6\"^^foo:rational .
}
```

**NOTE:** A user can also extend other protocols in the namespace to create custom implementations of IRIs and other RDF Term values.

# RDF Terms

This section discusses IRIs, variables, blank nodes, and literals in Flint and SPARQL. Many of the conventions here, including for prefixed IRIs, variables, and blank nodes, were borrowed from the [Datalog grammar](https://docs.datomic.com/on-prem/query/query.html).

**NOTE:** For simplicity, many terms in Flint only allow a subset of the characters that the SPARQL spec allows. For example, the latter often accepts Unicode characters, while Flint is ASCII-only outside of IRIs or string literals.

## IRIs

Internationalized Resource Identifiers (IRIs) and their subset Universal Resource Identifiers (URIs) are represented in two ways: as full IRIs or as prefixed IRIs.

### Full IRIs

Full IRIs in Flint are written as strings of the form `<my-iri-string>`. The string inside the angle bracket pair can include any characters **except** for `^`, `<`, `>`, `"`, `\`, `|`, `` ` ``, or whitespace. Translating to SPARQL does not change full IRIs.

**NOTE:** This can mean that any string can become a IRI in Flint, though in practice they should conform to the [specification for IRIs](https://www.google.com/search?q=iri+spec&oq=IRI+spec&aqs=chrome.0.69i59j0i512j0i22i30l5.2040j0j7&sourceid=chrome&ie=UTF-8) after expansion.

### Prefixed IRIs

Prefixed IRIs in Flint are written as keywords of the form `:prefix/name`, where the prefix is optional. When translating to SPARQL, prefixed IRIs are transformed into the form `prefix:name`.

Prefix IRIs accept word characters and hyphens, with the exception of the first prefix char (which does not allow digits); periods can also be included in the middle of the prefix.

**NOTE:** Prefixed IRIs must have the prefix be an entry in the prefixes map; otherwise, validation will fail (unless `:validate?` is set to `false`).

### `a`

There is a third IRI representation allowed in Flint for predicates: the keyword `:a`, which can also be written as the symbol `a`. This is provided as syntactic sugar to compactly represent the IRI:
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

Variables are written as symbols of the form `?var`. The characters after the question mark can be any word character. Translating to SPARQL does not change the variable other than stringifying it.

## Blank Nodes

Blank nodes are written as symbols of the form `_bnode`; an underscore without any trailing chars, `_`, is also a valid blank node. When translating to SPARQL, blank nodes are rewritten as `_:bnode`, i.e. a colon is added after the underscore; the exception is with `_`, which is rewritten as `[]`. The characters after the underscore can be written as any word character; periods are also allowed in the middle.

## Literals

Flint supports the following literals: simple strings, language-tagged strings, numbers, booleans, and dateTime timestamps.

**NOTE:** For simplicity, Flint does not allow for user-defined RDF types despite it being allowed on the SPARQL spec.

### Numbers

Numbers cover both integers and doubles, which are represented as integer and double literals in SPARQL. Neither are transformed during SPARQL translation beyond stringification

### Booleans

Booleans cover the values `true` and `false` in Flint, and are not transformed during SPARQL translation beyond stringification

### Simple strings

In Flint, string literals can contain any characters **except** unescaped line breaks, carriage returns, backslashes, or double quotes; this is in order to prevent SPARQL injection attacks. Strings are not transformed during SPARQL translation.

### Language-tagged strings

In Flint, strings with language tags are represented by a singleton map between a language tag keyword and the string literal (which follows the same restrictions as above). The following is an example of a language-tagged string in Flint:
```clojure
{:en "Hajime Isayama"}
```
Which during SPARQL translation is transformed as so:
```sparql
"Hajime Isayama"@en
```

As the map **must** only contain a single entry; to query or update strings in multiple languages, one must use multiple [triples](triple.md):
```clojure
{?x {dc:creator #{{:en "Hajime Isayama"} {:jp "諫山 創"}}}}
```
which becomes
```sparql
?x dc:creator "Hajime Isayama"@en , "諫山 創"@jp
```

### dateTime timestamps

In Flint, dateTime timestamps include any values for which `inst?` is `true`. The following is an example of a timestamp pre-SPARQL translation:
```clojure
#inst "2022-01-01T10:10:10Z"
```
and post-SPARQL translation:
```sparql
"2022-01-01T10:10:10Z"^^<http://www.w3.org/2001/XMLSchema#dateTime>
```
However, if one includes an entry for the above IRI in their prefixes map, they can shorten the resulting string considerably; for example, if `xsd` is the prefix, then the string becomes:
```sparql
"2022-01-01T10:10:10Z"^^xsd:dateTime
```

# Triples

The fundamental building block of RDF and SPARQL is the _triple_, a tuple consisting of a subject, predicate, and object. In Flint, there are two ways to write a series of triples. The first is as a vector of three-element vectors; for example:
```clojure
[:where [[?x :dc/title "Attack on Titan"]
         [?x :dc/author "Hajime Isayama"]]]
```
is represented in SPARQL as:
```sparql
WHERE {
    ?x dc:title "Attack on Titan" .
    ?x dc:author "Hajime Isayama" .
}
```

The other way to write a triple block in Flint is as a [normal form map](https://github.com/ont-app/igraph#normal-form) of the [IGraph protocol](https://github.com/ont-app/igraph). Each normal form map associates subjects with predicate-object maps, and each predicate-object map associates predicates with object sets. For example:
```clojure
[:where [{?x {:dc/title  #{"Attack on Titan"}
              :dc/author #{"Hajime Isayama"}}}]]
```
is represented in SPARQL as:
```sparql
WHERE {
    ?x dc:title "Attack on Titan" ;
       dc:author "Hajime Isayama" .
}
```

Note that only when triples written as normal form maps in Flint will they share subjects in the SPARQL representation. Since objects are represented as sets, a similar grouping occurs if more than one object exists in a set:
```clojure
[:where [{?h {:foaf/name  #{"Historia Reiss", "Christa Lenz"}}}]]
```
becomes
```sparql
WHERE {
    ?h foaf:name "Historia Reiss" , "Christa Lenz"
}
```
which is the same as
```sparql
WHERE {
    ?h foaf:name "Historia Reiss" .
    ?h foaf:name "Christa Lenz" .
}
```

Both forms can be combined to form a graph pattern:
```clojure
[:where [{?e {:foaf/name #{"Eren Jaeger"}
              :foaf/nick #{"Attack Titan"}}}
         {?a {:foaf/name #{"Armin Arlet"}}}
         [?m :foaf/name "Mikasa Ackerman"]]]
```
which becomes
```sparql
WHERE {
    ?e foaf:name "Eren Jaeger" ;
       foaf:nick "Attack Titan" .
    ?a foaf:name "Armin Arlet" .
    ?m foaf:name "Mikasa Ackerman" .
}
```

## Restrictions

Subjects can be one of the following:
- Variables
- IRIs or prefixed IRIs
- Blank nodes

Predicates can be one of the following:
- Variables
- IRIs or prefixed IRIs
- `:a` or `a`, the `rdf:type` shorthand
- Property paths

Objects can be one of the following:
- Variables
- IRIs or prefixed IRIs
- Blank nodes
- Literals

**NOTE:** Property paths are not allowed in triples in `CONSTRUCT` clauses, nor in `DELETE` and `INSERT DATA` clauses.

**NOTE:** Blank nodes are not allowed in triples in `DELETE` clauses (including `DELETE WHERE` and `DELETE DATA`).

**NOTE:** Variables are not allowed in triples in `DELETE DATA` OR `INSERT DATA` clauses.

**NOTE:** Technically literals are allowed in subject position according to the SPARQL spec, but no RDF implementation accepts that, so Flint does not allow for subject literals either.

## Property Paths

Reference: [9. Property Paths](https://www.w3.org/TR/sparql11-query/#propertypaths)

Property paths act as syntactic sugar for predicate sequences in RDF graphs. A property path in Flint is an IRI (including `:a`) or a list of the form `(op path ...)`; this is similar to Clojure functions or [Flint expressions](expr.md). The following is a table of path operations:

| Flint | SPARQL form | Arglist
| --- | --- | ---
| `alt` | `(path \| path \| ...)` | `[& paths]`
| `cat` | `(path / path / ...)` | `[& paths]`
| `inv` | `^(path)` | `[path]`
| `?` | `(path)?` | `[path]`
| `*` | `(path)*` | `[path]`
| `+` | `(path)+` | `[path]`
| `not` | `!neg-path` | `[neg-path]`

A `neg-path` is a subset of paths that can only include `alt` operations or IRIs.

<!-- TODO: example -->
**NOTE:** Variables are not allowed in property paths.

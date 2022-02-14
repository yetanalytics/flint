# Triples

The fundamental building block of RDF and SPARQL is the _triple_, a tuple consisting of a subject, predicate, and object. In Flint, there are two ways to write a series of triples.

The first is as a vector of three-element vectors. For example, the triples in the `:where` clause here are represented as vectors:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?name ?age]
 :where    [[?x :foaf/name ?name]
            [?x :foaf/age ?age]]}
```
which after SPARQL translation becomes:
```sparql
PREFIX foaf:<http://xmlns.com/foaf/0.1/>
SELECT ?name ?age
WHERE {
    ?x foaf:name ?name .
    ?x foaf:age ?age .
}
```

The other way to write a triple block in Flint is as a [normal form map](https://github.com/ont-app/igraph#normal-form) of the [IGraph protocol](https://github.com/ont-app/igraph). Each normal form map associates subjects with predicate-object maps, and each predicate-object map associates predicates with object sets. The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?name ?age]
 :where    [{?x {:foaf/name #{?name}
                 :foaf/age  #{?age}}}]}
```
becomes:
```sparql
WHERE {
    ?x foaf:name ?name ;
       foaf:age  ?age .
}
```

Note that only when triples written as normal form maps in Flint will they share subjects in the SPARQL representation. Since objects are represented as sets, a similar grouping occurs if more than one object exists in a set:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?y]
 :where    [{?y {:foaf/givenName #{"Ymir", "_Ymir"}}}]}
```
which becomes
```sparql
PREFIX foaf:<http://xmlns.com/foaf/0.1/>
SELECT ?y
WHERE {
    ?y foaf:givenName "Ymir" , "_Ymir"
}
```
which is the same as
```sparql
PREFIX foaf:<http://xmlns.com/foaf/0.1/>
SELECT ?y
WHERE {
    ?y foaf:givenName "Ymir" .
    ?y foaf:givenName "_Ymir" .
}
```

Both forms can be combined to form a graph pattern:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"
            :dc   "<http://purl.org/dc/elements/1.1/>"}
 :select   [?name ?age ?title]
 :where    [{?x {:foaf/name #{?name}
                 :foaf/age  #{?age}}}
            [?y :dc/creator ?name]
            [?y :dc/title ?title]]}
```
which become:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX dc:   <http://purl.org/dc/elements/1.1/>
SELECT ?name ?age ?title
WHERE {
    ?x foaf:name ?name ;
       foaf:age ?age .
    ?y dc:creator ?name .
    ?y dc:title ?title .
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

A `neg-path` is a subset of paths that can only include `alt` operations or IRIs. No other operations are allowed, even as args to `alt` ops.

An example of a query with a simple property path:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?friendName]
 :where    [{?x {:foaf/name #{"Eren Jaeger"}
                (cat (* :foaf/knows) :foaf/name) #{?friendName}}}]}
```
which becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?friendName
WHERE {
    ?x foaf:name "Eren Jaeger" ;
       (foaf:knows* / foaf:name) ?friendName .
}
```

**NOTE:** Variables are not allowed in property paths.

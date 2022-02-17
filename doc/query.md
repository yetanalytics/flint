# SPARQL Queries

A SPARQL query is used to find or test for values in an RDF graph database. There are four types of SPARQL queries:
- [`:select`](query.md#select) (incl. [`:select-distinct`](query.md#select-distinct) and [`:select-reduced`](query.md#select-reduced))
- [`:construct`](query.md#construct)
- [`:ask`](query.md#ask)
- [`:describe`](query.md#describe)

Each SPARQL query in Flint is a map that includes one of the four aforementioned clauses, as well as any of the following clauses:

- [Prologue clauses](prologue.md)
  - `:base`
  - `:prefixes`
- [Graph IRI clauses](graph.md)
  - `:from`
  - `:from-named`
- [`:where`](where.md) (optional in `:describe` queries, required in others)
- [Modifiers](modifier.md)
  - `:group-by`
  - `:order-by`
  - `:having`
  - `:limit`
  - `:offset`
  - `:values` (`:select` queries only)

**NOTE:** Any key other than the above keywords is not allowed in a SPARQL query map.

## Query Clauses

### `:select`

Reference: [16.1 SELECT](https://www.w3.org/TR/sparql11-query/#select)

A `:select` query is used to select and return specific variables in a query. It can be one of two things:
- A wildcard: `*` or `:*`
- A collection of variables or `[expr var]` forms.

Example of a wildcard `:select`:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   *
 :where    [[?x :foaf/familyName "Jaeger"]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT *
WHERE {
    ?x foaf:familyName "Jaeger" .
}
```

Example of a `:select` with variables and `[expr var]` forms:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?fullName [(<= 18 ?age) ?isAdult]]
 :where    [{?x {:foaf/familyName #{"Jaeger"}
                 :foaf/name       #{?fullName}
                 :foaf/age        #{?age}}}]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?fullName ((18 <= ?age) AS ?isAdult)
WHERE {
    ?x foaf:familyName "Jaeger" ;
       foaf:name ?fullName ;
       foaf:age ?age .
}
```

**NOTE:** Flint does not allow for duplicate projected variables in `:select`. SPARQL implementations do allow for duplicates in certain situations but not others, so Flint implements this blanket restriction for simplicity.

**NOTE:** `:group-by` cannot be used with a wildcard `:select`.

**NOTE:** Aggregate expressions introduce restrictions on variables in a `:select` clause, namely all variables must be projected from a `:group-by` clause, a `[expr var]` form, or be part of an aggregate.

#### `:select-distinct`

Reference: [15.3 Duplicate Solutions](https://www.w3.org/TR/sparql11-query/#modDuplicates)

The `:select-distinct` variant of `:select` eliminates duplicate values of a selected variable from the result set. The syntax and restrictions of a `:select-distinct` clause are exactly the same as those of a `:select` clause.

The example:
```clojure
{:prefixes        {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select-distinct [?givenName]
 :where           [[?x :foaf/familyName "Jaeger"]
                   [?x :foaf/givenName ?givenName]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT DISTINCT ?givenName
WHERE {
    ?x foaf:familyName "Jaeger" .
    ?x foaf:givenName ?givenName .
}
```

#### `:select-reduced`

Reference: [15.3 Duplicate Solutions](https://www.w3.org/TR/sparql11-query/#modDuplicates)

The `:select-reduced` variant of `:select` allows elimination of duplicate values from the result set. The syntax and restrictions of a `:select-reduced` clause are exactly the same as those of a `:select` clause.

The example:
```clojure
{:prefixes       {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select-reduced [?givenName]
 :where          [[?x :foaf/familyName "Jaeger"]
                  [?x :foaf/givenName ?givenName]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT REDUCED ?givenName
WHERE {
    ?x foaf:familyName "Jaeger" .
    ?x foaf:givenName ?givenName .
}
```

### `:construct`

Reference: [16.2 CONSTRUCT](https://www.w3.org/TR/sparql11-query/#construct)

A `:construct` query returns an RDF graph. Syntactically, the `:construct` clause is comprised of a series of [triples](triple.md), which can be written as vectors or IGraph normal form maps.

The example:
```clojure
{:prefixes  {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :construct [[?x :foaf/familyName "Jaeger"]
             {?y {:foaf/familyName #{"Ackerman"}
                  :foaf/givenName #{?givenName}}}]
 :where     [[?x :foaf/familyName "Jaeger"]
             {?y {:foaf/familyName #{"Ackerman"}}}]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
CONSTRUCT {
    ?x foaf:familyName "Jaeger" .
    ?y foaf:familyName "Ackerman" ;
       foaf:givenName ?givenName .
}
WHERE {
    ?x foaf:familyName "Jaeger" .
    ?y foaf:familyName "Ackerman" .
}
```

If the `:construct` clause is an empty collection or `nil`, then Flint will the query as the `CONSTRUCT WHERE` shorthand in SPARQL.

The example:
```clojure
{:prefixes  {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :construct []
 :where     [[?x :foaf/familyName "Jaeger"]
             {?y {:foaf/familyName #{"Ackerman"}}}]}
```
becomes:
```
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
CONSTRUCT
WHERE {
    ?x foaf:familyName "Jaeger" .
    ?y foaf:familyName "Ackerman" .
}
```

**NOTE:** Because of the above, it is not possible to use `:construct` queries to construct an empty model in Flint, even though it is valid in SPARQL.

**NOTE:** Property paths are not allowed in the `:construct` clause (though they are still allowed in the `:where` clause).

### `:ask`

Reference: [16.3 ASK](https://www.w3.org/TR/sparql11-query/#ask)

A `:ask` query tests for existence, i.e. returns `true` if the data described by the `:where` clause exists. The `:ask` clause in Flint, to match SPARQL syntax, must be present and be either `nil` or an empty collection.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :ask      []
 :where    [[?x :foaf/familyName "Jaeger"]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
ASK
WHERE {
    ?x foaf:familyName "Jaeger" .
}
```

### `:describe`

Reference: [16.4 DESCRIBE](https://www.w3.org/TR/sparql11-query/#describe)

A `:describe` query is similar to `:construct` in that it returns an RDF graph; unlike a `:construct` query, however, what is returns is implementation-specific. A `:describe` clause can be one of two things:
- A wildcard: `*` or `:*`
- A collection of variables, IRIs, or prefixed IRIs.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :describe [?x "<http://example.org>"]
 :where    [[?x :foaf/familyName "Jaeger"]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
DESCRIBE ?x <http://example.org>
WHERE {
    ?x foaf:familyName "Jaeger" .
}
```

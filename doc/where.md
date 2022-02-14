# Graph Patterns

SPARQL/Flint `:where` clauses are written as _graph patterns_, which describe a selection of RDF triples. The simplest graph pattern, known as a _basic graph pattern_ (or BGP) consists of a series of RDF triples.* However, there are a number of clauses that can be used to create more advanced graph patterns:
- [`:where`](where.md#where)
- [`:optional`](where.md#optional)
- [`:union`](where.md#union)
- [`:filter`](where.md#filter)
- [`:minus`](where.md#minus)
- [`:bind`](where.md#bind)
- [`:graph`](where.md#graph)
- [`:service`](where.md#serviceservice-silent) (or [`:service-silent`](where.md#serviceservice-silent))
- [`:values`](where.md#values)

\* Technically a BGP also includes `:filter` clauses (though not the graph patterns described in the `:filter`).

## Graph pattern clauses

### `:where`

References: [5. Graph Patterns](https://www.w3.org/TR/sparql11-query/#GraphPattern) and [12. Subqueries](https://www.w3.org/TR/sparql11-query/#subqueries)

A `:where` clause in Flint is one of two things:
- A sub-query, which is written as a `:select` query map.
- A vector of graph patterns.

Each graph pattern is written as either a [triple](triple.md) (either as a vector or IGraph normal form map), or as a vector of the form `[:keyword & args]`.

Example of a `:where` clause containing a subquery in a nested `:where`:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"
            :dc   "<http://purl.org/dc/elements/1.1/>"}
 :select   [?person]
 :where    [[?person :foaf/interest ?doc]
            [:where {:select   [?doc [(max ?date) ?pubDate]]
                     :where    [[?h :foaf/name "Hange Zoe"]
                                [?h :foaf/publications ?doc]
                                [?doc :dc/date ?date]]
                     :group-by [?doc]}]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX dc:   <http://purl.org/dc/elements/1.1/>
SELECT ?person
WHERE {
    ?person foaf:interest ?doc .
    {
        SELECT ?doc (MAX(?date) AS ?pubDate)
        WHERE {
            ?h foaf:name "Hange Zoe" .
            ?h foaf:publications ?doc .
            ?doc dc:date ?date .
        }
        GROUP BY ?doc
    }
}
```

### `:optional`

Reference: [6. Including Optional Values](https://www.w3.org/TR/sparql11-query/#optionals)

The `:optional` keyword specifies patterns that do not need to exist in the solution. In Flint, an `:optional` graph pattern has the form `[:optional sub-where]`.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?firstName ?lastName]
 :where    [[?x :foaf/givenName ?firstName]
            [:optional [[?x :foaf/familyName ?lastName]]]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?firstName ?lastName
WHERE {
    ?x foaf:firstName ?firstName .
    OPTIONAL {
        ?x foaf:familyName ?lastName .
    }
}
```

### `:union`

Reference: [7. Matching Alternatives](https://www.w3.org/TR/sparql11-query/#alternatives)

The `:union` keyword takes the union of the results of multiple graph patterns. In Flint, a `:union` graph pattern has the form `[:union sub-where & sub-wheres]`.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?x]
 :where    [[:union [[?x :foaf/name "Historia Reiss"]]
                    [[?x :foaf/name "Christa Lenz"]]]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name ?nickName
WHERE {
    {
        ?x foaf:name "Historia Reiss" .
    }
    UNION
    {
        ?x foaf:name "Christa Lenz" .
    }
}
```

### `:filter`

Reference: [8.1 Filtering Using Graph Patterns](https://www.w3.org/TR/sparql11-query/#neg-pattern)

The `:filter` keyword is used to exclude results using [expressions](expr.md). In Flint, the `:filter` keyword is used in the form `[:filter expr]`.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?name ?age]
 :where    [[?x :foaf/name ?name]
            [?x :foaf/age ?age]
            [:filter (<= 18 ?age)]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name ?age
WHERE {
    ?x foaf:name ?name .
    ?x foaf:age ?age .
    FILTER (18 <= ?age)
}
```

### `:minus`

Reference: [8.2 Removing Possible Solutions](https://www.w3.org/TR/sparql11-query/#neg-minus)

The `:minus` keyword is used to remove the specified graph pattern from the result set. In Flint, the `:minus` graph pattern has the form `[:minus sub-where]`.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?name ?age]
 :where    [[?x :foaf/name ?name]
            [?x :foaf/name ?age]
            [:minus [[?x :foaf/name "Jean Kirstein"]]]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name ?age
WHERE {
    ?x foaf:name ?name .
    ?x foaf:age ?age
    MINUS {
        ?x foaf:name "Jean Kirstein" .
    }
}
```

### `:bind`

Reference: [10.1 BIND: Assigning to Variables](https://www.w3.org/TR/sparql11-query/#bind)

The `:bind` keyword is used to bind the result of an expression to a variable. In Flint, the `:bind` keyword is used with an `[expr var]` form in the form `[:bind [expr var]]`.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?name ?isAdult]
 :where    [[?x :foaf/name ?name]
            [?x :foaf/age ?age]
            [:bind [(<= 18 ?age) ?isAdult]]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name ?isAdult
WHERE {
    ?x foaf:name ?name .
    ?x foaf:age ?age .
    BIND ((18 <= ?age) AS ?isAdult)
}
```

### `:values`

Reference: [10.2 VALUES: Providing inline data](https://www.w3.org/TR/sparql11-query/#inline-data)

The `:values` keyword is used to inline values. In Flint, the `:values` keyword is used in the form `[:values values-map]`.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"
            :food "<http://example.org/food/>"}
 :select   [?name ?age ?favoriteFood]
 :where    [{?x {:foaf/name #{?name}
                 :foaf/age  #{?age}}}
            [:values {?name         ["Sasha Blause" "Connie Springer"]
                      ?favoriteFood [:food/potato nil]}]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX food: <http://example.org/food/>
SELECT ?name ?age ?org ?favoriteFood
WHERE {
    ?x foaf:name ?name ;
       foaf:age ?age .
    VALUES (?name ?favoriteFood) {
        ("Sasha Blause" food:potato)
        ("Connie Springer" UNDEF)
    }
}
```

See the [Modifiers](modifier.md) document for more information on `:values` clauses, including more examples.

### `:graph`

Reference: [13.3 Querying the Dataset](https://www.w3.org/TR/sparql11-query/#queryDataset)

The `:graph` keyword is used to specify the named graph that the graph pattern exists in. In Flint, the `:graph` graph pattern has the form `[:graph iri-or-var sub-where]`.

The example:
```clojure
{:prefixes   {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select     [?name]
 :from-named ["<http://survey-corps.com/graph-data/>"]
 :where      [[:graph "<http://survey-corps.com/graph-data/>"
                      [[?x :foaf/name ?name]]]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name
WHERE {
    GRAPH <http://survey-corps.com/graph-data/> {
        ?x foaf:name ?name .
    }
}
```

### `:service`/`:service-silent`

Reference: [SPARQL 1.1 Federated Query](https://www.w3.org/TR/2013/REC-sparql11-federated-query-20130321/)

The `:service` keyword is used for federated queries, i.e. queries across networks. The `:service-silent` variant is used for queries to fail silently. In Flint, the `:service` graph pattern has the form `[:service iri-or-var sub-where]`.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?name]
 :where    [[:service "<http://survey-corps.com/graph-data/remote/>"
                      [[?x :foaf/name ?name]]]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name
WHERE {
    SERVICE <http://survey-corps.com/graph-data/remote> {
        ?x foaf:name ?name .
    }
}
```

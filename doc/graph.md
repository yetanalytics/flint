# Graph IRIs

SPARQL contains a number of clauses that can be used to specify default and named graphs in the query via IRIs.

## Graph IRI Clauses

### `:from`

Used in queries, `:from` denotes the _default_ graph that the query operates on. Syntactically, the `:from` clause can be an IRI or prefixed, or a vector of **one** such IRI.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?x]
 :from     ["<http://my-default-graph.org/>"]
 :where    [[?x :foaf/name "Armin Arlet"]]}
```
which becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?x
FROM <http://my-default-graph.org/>
WHERE {
    ?x foaf:name "Armin Arlet"
}
```

### `:from-named`

Used in updates, `:from-named` denotes the _named_ graphs that the query can operate on. Syntactically, the `:from-named` clause is a vector of one or more IRIs or prefixed IRIs.

The example:
```clojure
{:prefixes   {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select     [?x]
 :from-named ["<http://my-named-graph.org/v1>"
              "<http://my-named-graph.org/v2>"]
 :where      [[:union [[:graph "<http://my-named-graph.org/v1>"
                               [[?x :foaf/name "Armin Arlet"]]]]
                      [[:graph "<http://my-named-graph.org/v2>"
                               [[?x :foaf/name "Armin Arlet"]]]]]]}
```
which becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?x
FROM NAMED <http://my-named-graph.org/v1>
FROM NAMED <http://my-named-graph.org/v2>
WHERE {
    {
        GRAPH <http://my-named-graph.org/v1> {
            ?x foaf:name "Armin Arlet" .
        }
    }
    UNION
    {
        GRAPH <http://my-named-graph.org/v2> {
            ?x foaf:name "Armin Arlet" .
        }
    }
}
```

### `:using`

Used only in `:delete`/`:insert` updates, `:using` specifies the graph used in the `:where` clause. Syntactically, it consists of either:
- An IRI or prefixed IRI, to specify the default graph.
- A `[:named iri]` tuple, to specify a named graph.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :delete   [[:graph "<http://census.marley/districts/liberio>"
                  [[?x :foaf/familyName "Brown"]]]]
 :insert   [[:graph "<http://census.marley/districts/liberio>"
                  [[?x :foaf/familyName "Braun"]]]]
 :using    "<http://census.marley/districts/liberio>"
 :where    [[?x :foaf/familyName "Brown"]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
DELETE {
    GRAPH <http://census.marley/districts/liberio> {
        ?x foaf:familyName "Brown" .
    }
}
INSERT {
    GRAPH <http://census.marley/districts/liberio> {
        ?x foaf:familyName "Braun" .
    }
}
USING <http://census.marley/districts/liberio>
WHERE {
    ?x foaf:familyName "Brown" .
}
```

### `:with`

Used only in `:delete`/`:insert` updates, `:with` specifies the graph for the query. Syntactically, it consists an IRI or prefixed IRI that specifies the graph.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :with     "<http://census.marley/districts/liberio>"
 :delete   [[?x :foaf/familyName "Brown"]]
 :insert   [[?x :foaf/familyName "Braun"]]
 :where    [[?x :foaf/familyName "Brown"]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
WITH <http://census.marley/districts/liberio>
DELETE {
    ?x foaf:familyName "Brown" .
}
INSERT {
    ?x foaf:familyName "Braun" .
}
WHERE {
    ?x foaf:familyName "Brown" .
}
```

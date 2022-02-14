# flint
Flint is a Clojure(Script) DSL for creating SPARQL Query and Update strings.

## API

Three functions exist in the Flint API: `format-query`, `format-update`, and `format-updates`. The first two functions format a single SPARQL Query or Update, respectively, while the third formats a collection of SPARQL Updates into a single Update Request.

Each function takes in the following keyword arguments:
| Argument | Description |
| --- | --- |
| `:pretty?` | If `true`, adds line breaks and indentation to the resulting SPARQL string. Default `false`.
| `:validate?` | If `true`, validates that prefixed IRIs are expandable and that certain restrictions on variables and blank nodes are met. Default `true`.

## Examples

<!-- TODO: Check that examples are correct Clojure and are paresable -->

The following is a simple query that queries the name of the author who wrote the popular manga series [Attack on Titan](https://en.wikipedia.org/wiki/Attack_on_Titan):
```clojure
(def query
  '{:prefixes {:dc "<http://purl.org/dc/elements/1.1/>"}
    :select   [?author]
    :where    [[?aot :dc/title "Attack on Titan"]
               [?aot :dc/creator ?author]]})
```
Note that the map needs to be quoted due to the presence of symbols in the map. We can then pass the query to the function `format-query`:
```clojure
(require '[com.yetanalytics.flint :as f])

(f/format-query query :pretty? true)
```
and it will return a SPARQL string:
```sparql
PREFIX dc: <http://purl.org/dc/elements/1.1/>
SELECT ?author
WHERE {
    ?aot dc:title "Attack on Titan" .
    ?aot dc:creator ?author .
}
```
One can then pass this query string to a Resource Description Framework (RDF) database and, depending on the data in the system, should return that `?author` is [Hajime Isayama](https://en.wikipedia.org/wiki/Hajime_Isayama).

The following is a more comprehensive example - a query that returns the titles of all the works published by the publisher of Attack on Titan in or after 2010:
```clojure
(def query-2
  '{:prefixes {:dc  "<http://purl.org/dc/elements/1.1/>"
               :xsd "<http://www.w3.org/2001/XMLSchema#>"}
    :select   [?title]
    :from     ["<http://my-anime-rdf-graph.com>"]
    :where    [[:union [{_b1 {:dc/title     #{{:en "Attack on Titan"}}
                              :dc/publisher #{?publisher}}}]
                       [{_b2 {:dc/title     #{{:jp "進撃の巨人"}}
                              :dc/publisher #{?publisher}}}]]
               {?work {:dc/publisher #{?publisher}
                       :dc/title     #{?title}
                       :dc/date      #{?date}}}
               [:filter (<= #inst "2010-01-01T00:00:00Z" ?date)]]})
```
which demonstrates several additional features, such as a alternate triple syntax based on maps instead of vectors, blank nodes, language tags, and the `:union`, `:filter` and `:from` clauses. When passed to `format-query`, it is translated to:
```sparql
PREFIX dc:  <http://purl.org/dc/elements/1.1/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?title
FROM <http://my-anime-rdf-graph.com>
WHERE {
    {
        _:b1 dc:title "Attack on Titan"@en ;
             dc:publisher ?publisher .
    }
    UNION
    {
        _:b2 dc:title "進撃の巨人"@jp ;
             dc:publisher ?publisher .
    }
    ?work dc:publisher ?publisher ;
          dc:title ?title ;
          dc:date ?date .
    FILTER ("2010-01-01T00:00:00Z"^^xsd:dateTime <= ?date)
}
```

## Prior Art
- Flint is based off of the grammar of [SPARQL 1.1](https://www.w3.org/TR/sparql11-query/).
- The idea of a SPARQL DSL was inspired by [HoneySQL](https://github.com/seancorfield/honeysql), a DSL for creating SQL queries.
- Flint borrows certain syntactic conventions from the [Datalog grammar](https://docs.datomic.com/on-prem/query/query.html), including for terms, expressions, and variable binding.
- The map-based triples syntax is based on the normal form used in the [IGraph protocol](https://github.com/ont-app/igraph).

## License

Copyright © 2022 Yet Analytics, Inc.

Distributed under the Apache License version 2.0.

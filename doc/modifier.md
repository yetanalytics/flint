# Modifiers

The following clauses are used at the top level of SPARQL query maps and are used to modify the solution or introduce new values. These modifiers include:

- Inline values
  - [`:values`](modifier.md#values)
- Grouping
  - [`:group-by`](modifier.md#group-by)
  - [`:having`](modifier.md#having)
- Result modification
  - [`:order-by`](modifier.md#order-by)
  - [`:offset`](modifier.md#offset)
  - [`:limit`](modifier.md#limit)

## Modifier clauses

### `:values`

Reference: [10.2 VALUES: Providing inline data](https://www.w3.org/TR/sparql11-query/#inline-data)

The `:values` clause associates values to variables in an inline fashion; it can be used in a graph pattern or a query. In Flint, each value can be one of the following:
- An IRI or prefixed IRI
- A literal value (number, string, etc.)
- `nil`, which then becomes `UNDEF` during SPARQL translation.

In Flint, the clause can be written in two ways. The first format follows how they are written in SPARQL, as a mapping of a variable vector to a collection of value vectors. For The example:
```clojure
{:values {[?x ?y] [[:uri1 1] [:uri2 nil]]}}
```
associates `?x` with values `:uri1` and `:uri2`, while `?y` is associated with `1` and `nil`. When translated to SPARQL, the `:values` clause becomes:
```sparql
VALUES (?x ?y) {
    (:uri1 1)
    (:uri2 UNDEF)
}
```
which closely matches the original Clojure.

However, there is a second format that is more idiomatic to Clojure - instead of having a singleton map between colls, each variable is a key to its own collection of values. In this way, the equivalent `:values` clause is:
```clojure
{:values {?x [:uri1 :uri2] ?y [1 nil]}}
```
which then gets translated into the same SPARQL string.

A value can be one of the following:

**NOTE:** In the first format, the number of variables must be equal to the number of value vectors. In addition, in both format the length of each value vector (including `nil` entries) must be the same.

The The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?name ?age],
 :where    [{?x {:foaf/name  #{?name}
                 :foaf/title #{?title}
                 :foaf/age   #{?age}}}]
 :values   {[?name ?title] [["Levi Ackerman" "Captain"]
                            ["Erwin Smith" "Commander"]]}}
```
which can also be written as:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [?name ?age],
 :where    [{?x {:foaf/name  #{?name}
                 :foaf/title #{?title}
                 :foaf/age   #{?age}}}]
 :values   {?name  ["Levi Ackerman" "Erwin Smith"]
            ?title ["Captain" "Commander"]}}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name ?age
WHERE {
    ?x foaf:name ?name ;
       foaf:title ?title ;
       foaf:age ?age .
}
VALUES (?name ?title) {
    ("Levi Ackerman" "Captain")
    ("Erwin Smith" "Commander")
}
```

### `:group-by`

Reference: [11.2 GROUP BY](https://www.w3.org/TR/sparql11-query/#groupby)

A `:group-by` clause is used to group results by variables or expressions. Syntactically, it consists of a vector of one or more of the following:
- Variables
- Expressions
- `[expr var]` forms.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :select   [[(sample ?n) ?name]]
 :where    [[?org :foaf/member ?x]
            [?x :foaf/name ?n]]
 :group-by [?org]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT (SAMPLE(?n) AS ?name)
WHERE {
    ?org foaf:member ?x .
    ?x foaf:name ?n .
}
GROUP BY ?org
```

**NOTE:** A `:group-by` clause cannot be used with a wildcard `:select`.

**NOTE:** Adding a `:group-by` clause to a query introduces aggregate variable restrictions to the `:select` clause.

### `:having`

Reference: [11.3 HAVING](https://www.w3.org/TR/sparql11-query/#having)

A `:having` clause filters out grouped results. Syntactically, it consists of a vector of one or more expressions (including aggregates.)

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"},
 :select   [[(sample ?n) ?name]]
 :where    [[?org :foaf/member ?x]
            [?x :foaf/name ?n]]
 :group-by [?org],
 :having   [(contains str(?org) "survey-corps")]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT (SAMPLE(?n) AS ?name)
WHERE {
    ?org foaf:member ?x .
    ?x foaf:name ?n .
}
GROUP BY ?org
HAVING CONTAINS(STR(?org), "survey-corps")
```

### `:order-by`

Reference: [15.1 ORDER BY](https://www.w3.org/TR/sparql11-query/#modOrderBy)

An `:order-by` clause orders the result set. Syntactically, it consists of a vector of one or more of the following:
- Variables
- Expressions (including aggregates)
- `(asc expr)` or `(desc expr)` forms.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"},
 :select   [?age ?name]
 :where    [[?x :foaf/name ?name]
            [?x :foaf/age ?age]]
 :order-by [(asc ?age)]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name ?age
WHERE {
    ?x foaf:name ?name .
    ?x foaf:age ?age .
}
ORDER BY ASC(?age)
```

### `:offset`

Reference: [15.4 OFFSET](https://www.w3.org/TR/sparql11-query/#modOffset)

The `:offset` clause adds a pagination offset to the result set. Syntactically, it must be an integer.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"},
 :select   [?name]
 :where    [[?x :foaf/name ?name]]
 :offset   2}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?org ?name
WHERE {
    ?x foaf:name ?name .
}
OFFSET 2
```

### `:limit`

Reference: [15.5 LIMIT](https://www.w3.org/TR/sparql11-query/#modResultLimit)

The `:limit` clause limits the size of the result set. Syntactically, it must be a non-negative integer.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"},
 :select   [?name]
 :where    [[?x :foaf/name ?name]]
 :limit    10}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?org ?name
WHERE {
    ?x foaf:name ?name .
}
LIMIT 10
```

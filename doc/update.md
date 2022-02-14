# SPARQL Updates

A SPARQL update is used to edit RDF data. There are several types of updates, which follow under two categories:
- Graph Update
  - `:insert-data`
  - `:delete-data`
  - `:delete`/`:insert`
  - `:delete-where`
  - `:load`
  - `:clear`
- Graph Management
  - `:create`
  - `:drop`
  - `:copy`
  - `:move`
  - `:add`

Graph Update updates change existing RDF graphs without adding or deleting them, while Graph Management updates are free to add, delete, or move graphs.

All Graph Management update clauses, as well as `:load` and `:clear`, have `-silent` versions, e.g. `:load-silent`, which will return success in all cases and only silently fail.

Each SPARQL update in Flint is a map that includes one of the aforementioned clauses (or two in the case of having both `:delete` and `:insert` clauses), as well as one or more of the following clauses:

- Prologue clauses
  - `:base`
  - `:prefixes`
- `:delete`/`:insert`-specific clauses:
  - `:where` (required)
  - `:with`
  - `:using`
- `:load`-specific clauses:
  - `:into`
- `:add`, `:move`, and `:copy`-specific clauses:
  - `:to`

The triple insertion and deletion clauses accept both [triples](triple.md) and _quads_, which have the form `[:graph iri triples]`. This is similar to the `:graph` clause in [graph patterns](where.md), except that variables cannot be substituted in the graph IRI position.

## Update clauses

### `:insert-data`

Reference: [3.1.1 INSERT DATA](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#insertData)

The `:insert-data` clause inserts triples in an RDF graph. Syntactically, it consists of triples or quads.

The example:
```clojure
{:prefixes    {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :insert-data [[:graph "<http://census.marley/districts/liberio>"
                       [["<http://census.marley/entry#11402>" :foaf/givenName "Gabi"]
                        ["<http://census.marley/entry/11402>" :foaf/familyName "Braun"]]]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
INSERT DATA {
    GRAPH <http://census.marley/districts/liberio> {
        <http://census.marley/entry#11402> foaf:givenName "Gabi" .
        <http://census.marley/entry#11402> foaf:familyName "Braun" .
    }
}
```

**NOTE:** Property paths and variables are not allowed in an `:insert-data` clause.

### `:delete-data`

Reference: [3.1.2 DELETE DATA](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#deleteData)

The `:delete-data` clause deletes triples in an RDF graph. Syntactically, it consists of triples or quads.

The example:
```clojure
{:prefixes    {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :delete-data [[:graph "<http://census.marley/districts/liberio>"
                       [["<http://census.marley/entry#11397>" :foaf/givenName "Bertolt"]
                        ["<http://census.marley/entry#11397>" :foaf/familyName "Hoover"]]]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
DELETE DATA {
    GRAPH <http://census.marley/districts/liberio> {
        <http://census.marley/entry#11397> foaf:givenName "Bertlot" .
        <http://census.marley/entry#11397> foaf:familyName "Hoover" .
    }
}
```

**NOTE:** Property paths, variables, and blank nodes are not allowed in a `:delete-data` clause.

### `:delete` and `:insert`

Reference: [3.1.3 DELETE/INSERT](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#delete)

A `:delete` or `:insert` clause deletes or inserts triples, respectively, in an RDF graph with variables specified by a `:where` clause. Syntactically, both clauses consists of triples or quads. An update may contain either a `:delete` clause, an `:insert` clause, or both.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :delete   [[:graph "<http://census.marley/districts/liberio>"
                    [[?x :foaf/familyName "Brown"]]]]
 :insert   [[:graph "<http://census.marley/districts/liberio>"
                    [[?x :foaf/familyName "Braun"]]]]
 :where    [[:graph "<http://census.marley/districts/liberio>"
                    [[?x :foaf/familyName "Brown"]]]]}
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
WHERE {
    GRAPH <http://census.marley/districts/liberio> {
        ?x foaf:familyName "Brown" .
    }
}
```

**NOTE:** Blank nodes are not allowed in the `:delete` clause.

For information about `:using` and `:with` clauses, see [Graph IRIs](graph.md).

### `:delete-where`

Reference: [3.1.3.3 DELETE WHERE](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#deleteWhere)

The `:delete-where` clause is a shorthand for the combination `:delete` and `:where`, without `:insert` in between. Syntactically, the `:delete-where` clause consists of triples or quads.

The example:
```clojure
{:prefixes     {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :delete-where [[:graph "<http://census.marley/districts/liberio>"
                       [[?x :foaf/givenName "Annie"]
                        [?x :foaf/familyName "Leonhart"]]]]}
```
becomes
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
DELETE WHERE {
    GRAPH <http://census.marley/districts/liberio> {
        ?x foaf:givenName "Annie" .
        ?x foaf:familyName "Leonhart" .
    }
}
```

**NOTE:** Blank nodes are not allowed in the `:delete-where` clause.

### `:load`/`:load-silent` and `:into`

Reference: [3.1.4 LOAD](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#load)

The `:load` update loads triples from a source specified by an IRI. The `:load` clause is followed by an optional `:into` clause (if the latter is omitted the data is loaded into the default graph). Syntactically, both the `:load` and `:into` clauses consist of an IRI or prefixed IRI.

The example:
```clojure
{:load "<file:census/data/liberio.rdf>"
 :into "<http://census.marley/districts/liberio>"}
```
becomes:
```sparql
LOAD <file:census/data/liberio.rdf>
INTO <http://census.marley/districts/liberio>
```

Example of silent mode:
```clojure
{:load-silent "<file:census/data/liberio.rdf>"
 :into        "<http://census.marley/districts/liberio>"}
```
becomes:
```sparql
LOAD SILENT <file:census/data/liberio.rdf>
INTO <http://census.marley/districts/liberio>
```

### `:clear`/`:clear-silent`

Reference: [3.1.5 CLEAR](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#clear)

The `:clear` update is used to clear all data from one or more RDF graphs. Syntactically, the `:clear` clause consists of either an explicit IRI or prefixed IRI, `:default`, `:named`, or `:all`.

The example:
```clojure
{:clear :default}
```
becomes:
```sparql
CLEAR DEFAULT
```

### `:create`/`:create-silent`

Reference: [3.2.1 CREATE](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#create)

The `:create` update creates a new empty graph in an RDF store. The `:create` clause consists of the IRI or prefixed of the new graph.

The example:
```clojure
{:prefixes {:dist "<http://census.marley/districts/>"
 :create   :dist/liberio}}
```
becomes:
```sparql
PREFIX myld: <http://census.marley/districts/>
CREATE dist:liberio
```

### `:drop`/`:drop-silent`

Reference: [3.2.2 DROP](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#drop)

The `:drop` update deletes one or more graphs in an RDF store. The `:drop` clause consists of either an explicit IRI or prefixed IRI, `:default`, `:named`, or `:all`.

The example:
```clojure
{:drop :default}
```
becomes:
```sparql
DROP DEFAULT
```

### `:copy`/`:copy-silent` and `:to`

Reference: [3.2.3 COPY](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#copy)

The `:copy` update transfers data from a source to a target graph, overwriting the latter in the process. Both the `:copy` and `:to` clauses consist of either an IRI or prefixed IRI or the `:default` keyword.

The example:
```clojure
{:copy "<http://census.marley/districts/liberio>"
 :to   :default}
```
becomes:
```sparql
COPY <http://census.marley/districts/liberio>
TO DEFAULT
```

### `:move`/`:move-silent` and `:to`

Reference: [3.2.4 MOVE](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#move)

The `:move` update moves data from a source to a target graph, deleting the former and overwriting the latter. Both the `:move` and `:to` clauses consist of either an IRI or prefixed IRI or the `:default` keyword.

The example:
```clojure
{:move "<http://census.marley/districts/liberio>"
 :to   :default}
```
becomes:
```sparql
MOVE <http://census.marley/districts/liberio>
TO DEFAULT
```

### `:add`/`:add-silent` and `:to`

Reference: [3.2.5 ADD](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#add)

The `:add` update appends data from a source to a target graph. Both the `:add` and `:to` clauses consist of either an IRI or prefixed IRI or the `:default` keyword.

The example:
```clojure
{:add "<http://census.marley/districts/liberio>"
 :to  :default}
```
becomes:
```sparql
ADD <http://census.marley/districts/liberio>
TO DEFAULT
```

## Update request sequences

Unlike SPARQL queries, SPARQL update requests can be chained together into sequences. In Flint, this is supported by the `format-updates` function, which accepts a collection of update maps instead of a single one.

The example:
```clojure
[{:prefixes    {:foaf "<http://xmlns.com/foaf/0.1/>"}
  :delete-data [[:graph "<http://census.marley/districts/liberio>"
                        [["<http://census.marley/entry#11325>" :foaf/familyName "Brown"]]]]}
 {:insert-data [[:graph "<http://census.marley/districts/liberio>"
                        [["<http://census.marley/entry#11325>" :foaf/familyName "Braun"]]]]}]
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
DELETE DATA {
    GRAPH <http://census.marley/districts/liberio> {
        <http://census.marley/entry#11325> foaf:familyName "Brown" .
    }
};
INSERT DATA {
    GRAPH <http://census.marley/districts/liberio> {
        <http://census.marley/entry#11325> foaf:familyName "Braun" .
    }
}
```

Note that only the first update map has a `:prefixes` clause. In Flint, prefixes in subsequent update maps are merged and overwrite duplicate prefixes.

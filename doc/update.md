# SPARQL Updates

A SPARQL update is used to edit RDF data. There are several types of updates, which follow under two categories:
- Graph Update
  - [`:insert-data`](update.md#insert-data)
  - [`:delete-data`](update.md#delete-data)
  - [`:delete`/`:insert`](update.md#delete-and-insert)
  - [`:delete-where`](update.md#delete-where)
  - [`:load`](update.md#loadload-silent-and-into)
  - [`:clear`](update.md#clearclear-silent)
- Graph Management
  - [`:create`](update.md#createcreate-silent)
  - [`:drop`](update.md#dropdrop-silent)
  - [`:copy`](update.md#copycopy-silent-and-to)
  - [`:move`](update.md#movemove-silent-and-to)
  - [`:add`](update.md#addadd-silent-and-to)

Graph Update updates change existing RDF graphs without adding or deleting them, while Graph Management updates are free to add, delete, or move graphs.

All Graph Management update clauses, as well as `:load` and `:clear`, have `-silent` versions, e.g. `:load-silent`, which will return success in all cases and only silently fail.

Each SPARQL update in Flint is a map that includes one of the aforementioned clauses (or two in the case of having both `:delete` and `:insert` clauses), as well as one or more of the following clauses:

- [Prologue clauses](prologue.md)
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

Insertion and deletion clauses accept both [triples](triple.md) and _quads_, which have the form `[:graph iri triples]`. This is similar to the `:graph` clause in [graph patterns](where.md), except that variables cannot be substituted in the graph IRI position.

**NOTE:** Any key other than the above keywords is not allowed in a SPARQL update map.

## Update clauses

### `:insert-data`

Reference: [3.1.1 INSERT DATA](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#insertData)

The `:insert-data` clause inserts triples in an RDF graph. Syntactically, it consists of triples or quads.

The example:
```clojure
{:prefixes    {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :insert-data [[:graph "<http://census.marley/data>"
                       [["<http://census.marley/entry#211402>" :foaf/givenName "Reiner"]
                        ["<http://census.marley/entry/211402>" :foaf/familyName "Braun"]]]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
INSERT DATA {
    GRAPH <http://census.marley/data> {
        <http://census.marley/entry#211402> foaf:givenName "Reiner" .
        <http://census.marley/entry#211402> foaf:familyName "Braun" .
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
 :delete-data [[:graph "<http://census.marley/data>"
                       [["<http://census.marley/entry#211397>" :foaf/givenName "Bertolt"]
                        ["<http://census.marley/entry#211397>" :foaf/familyName "Hoover"]]]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
DELETE DATA {
    GRAPH <http://census.marley/data> {
        <http://census.marley/entry#211397> foaf:givenName "Bertlot" .
        <http://census.marley/entry#211397> foaf:familyName "Hoover" .
    }
}
```

**NOTE:** Property paths, variables, and blank nodes are not allowed in a `:delete-data` clause.

### `:delete` and `:insert`

Reference: [3.1.3 DELETE/INSERT](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#delete)

A `:delete` or `:insert` clause deletes or inserts triples, respectively, in an RDF graph with variables specified by a `:where` clause. Syntactically, both clauses consist of triples or quads. An update may contain either a `:delete` clause, an `:insert` clause, or both.

The example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"}
 :delete   [[:graph "<http://census.marley/data>"
                    [[?x :foaf/familyName "Brown"]]]]
 :insert   [[:graph "<http://census.marley/data>"
                    [[?x :foaf/familyName "Braun"]]]]
 :where    [[:graph "<http://census.marley/data>"
                    [[?x :foaf/familyName "Brown"]]]]}
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
DELETE {
    GRAPH <http://census.marley/data> {
        ?x foaf:familyName "Brown" .
    }
}
INSERT {
    GRAPH <http://census.marley/data> {
        ?x foaf:familyName "Braun" .
    }
}
WHERE {
    GRAPH <http://census.marley/data> {
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
 :delete-where [[:graph "<http://census.marley/data>"
                       [[?x :foaf/givenName "Annie"]
                        [?x :foaf/familyName "Leonhart"]]]]}
```
becomes
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
DELETE WHERE {
    GRAPH <http://census.marley/data> {
        ?x foaf:givenName "Annie" .
        ?x foaf:familyName "Leonhart" .
    }
}
```

**NOTE:** Blank nodes are not allowed in the `:delete-where` clause.

### `:load`/`:load-silent` and `:into`

Reference: [3.1.4 LOAD](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#load)

The `:load` update loads triples from a source specified by an IRI. The `:load` clause is followed by an optional `:into` clause (if the latter is omitted the data is loaded into the default graph). Syntactically, the `:load` clause consists of an IRI, while the `:into` clause consists of a `[:graph iri]` pair.

The example:
```clojure
{:load "<file:marleycensus/data.rdf>"
 :into [:graph "<http://census.marley/data>"]}
```
becomes:
```sparql
LOAD <file:marleycensus/data.rdf>
INTO GRAPH <http://census.marley/data>
```

Example of silent mode:
```clojure
{:load-silent "<file:marleycensus/data.rdf>"
 :into        "<http://census.marley/data>"}
```
becomes:
```sparql
LOAD SILENT <file:marleycensus/data.rdf>
INTO <http://census.marley/data>
```

### `:clear`/`:clear-silent`

Reference: [3.1.5 CLEAR](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#clear)

The `:clear` update is used to clear all data from one or more RDF graphs. Syntactically, the `:clear` clause consists of either a `[:graph iri]` pair, `:default`, `:named`, or `:all`.

The example sequence of `:clear` updates:
```clojure
[{:clear [:graph "<http://census.marley/data>"]}
 {:clear :default}
 {:clear :named}
 {:clear :all}]
```
becomes:
```sparql
CLEAR GRAPH <http://census.marley/data>;
CLEAR DEFAULT;
CLEAR NAMED;
CLEAR ALL
```

### `:create`/`:create-silent`

Reference: [3.2.1 CREATE](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#create)

The `:create` update creates a new empty graph in an RDF store. The `:create` clause consists of a `[:graph iri]`.

The example:
```clojure
{:prefixes {:census "<http://census.marley/>"
 :create   [:graph :census/data]}}
```
becomes:
```sparql
PREFIX census: <http://census.marley/>
CREATE GRAPH census:data
```

### `:drop`/`:drop-silent`

Reference: [3.2.2 DROP](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#drop)

The `:drop` update deletes one or more graphs in an RDF store. The `:drop` clause consists of either a `[:graph iri]` pair, `:default`, `:named`, or `:all`.

The example sequence of `:drop` updates:
```clojure
[{:drop [:graph "<http://census.marley/data>"]}
 {:drop :default}
 {:drop :named}
 {:drop :all}]
```
becomes:
```sparql
DROP GRAPH <http://census.marley/data>;
DROP DEFAULT;
DROP NAMED;
DROP ALL
```

### `:copy`/`:copy-silent` and `:to`

Reference: [3.2.3 COPY](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#copy)

The `:copy` update transfers data from a source to a target graph, overwriting the latter in the process. Both the `:copy` and `:to` clauses consist of either an IRI or the `:default` keyword.

The example:
```clojure
{:copy [:graph "<http://census.marley/data>"]
 :to   :default}
```
becomes:
```sparql
COPY GRAPH <http://census.marley/data>
TO DEFAULT
```

Note that the `:graph` keyword is optional. The example:
```clojure
{:copy "<http://census.marley/data>"
 :to   :default}
```
becomes:
```sparql
COPY <http://census.marley/data>
TO DEFAULT
```

### `:move`/`:move-silent` and `:to`

Reference: [3.2.4 MOVE](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#move)

The `:move` update moves data from a source to a target graph, deleting the former and overwriting the latter. Both the `:move` and `:to` clauses consist of either an IRI or the `:default` keyword.

The example:
```clojure
{:move [:graph "<http://census.marley/data>"]
 :to   :default}
```
becomes:
```sparql
MOVE GRAPH <http://census.marley/data>
TO DEFAULT
```

Note that the `:graph` keyword is optional. The example:
```clojure
{:move "<http://census.marley/data>"
 :to   :default}
```
becomes:
```sparql
MOVE <http://census.marley/data>
TO DEFAULT
```

### `:add`/`:add-silent` and `:to`

Reference: [3.2.5 ADD](https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#add)

The `:add` update appends data from a source to a target graph. Both the `:add` and `:to` clauses consist of either an IRI or the `:default` keyword.

The example:
```clojure
{:move [:graph "<http://census.marley/data>"]
 :to   :default}
```
becomes:
```sparql
MOVE GRAPH <http://census.marley/data>
TO DEFAULT
```

Note that the `:graph` keyword is optional. The example:
```clojure
{:add "<http://census.marley/data>"
 :to  :default}
```
becomes:
```sparql
ADD <http://census.marley/data>
TO DEFAULT
```

## Update request sequences

Unlike SPARQL queries, SPARQL update requests can be chained together into sequences. In Flint, this is supported by the `format-updates` function, which accepts a collection of update maps instead of a single one.

The example:
```clojure
[{:prefixes    {:foaf "<http://xmlns.com/foaf/0.1/>"}
  :delete-data [[:graph "<http://census.marley/data>"
                        [["<http://census.marley/entry#221325>" :foaf/familyName "Brown"]]]]}
 {:insert-data [[:graph "<http://census.marley/data>"
                        [["<http://census.marley/entry#221325>" :foaf/familyName "Braun"]]]]}]
```
becomes:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
DELETE DATA {
    GRAPH <http://census.marley/data> {
        <http://census.marley/entry#212325> foaf:familyName "Brown" .
    }
};
INSERT DATA {
    GRAPH <http://census.marley/data> {
        <http://census.marley/entry#212325> foaf:familyName "Braun" .
    }
}
```

Note that only the first update map has a `:prefixes` clause. In Flint, prefixes in subsequent update maps are merged and overwrite duplicate prefixes.

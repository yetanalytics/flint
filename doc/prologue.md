# Prologue

The SPARQL prologue allows users to list IRI prefixes and base IRIs, which is very useful for shortening IRIs in the query or update body. In Flint, there are two elements in the prologue, both of which are optional: the base IRI and the prefix map.

## Prologue clauses

### `:base`

The `:base` IRI sets the base IRI that subsequent IRIs are relative to. In the following example:
```clojure
{:base   "<http://foo.org/>"
 :select [?x]
 :where  [[?x "<bar>" ?y]]}
```
`<bar>` expands to `<http://foo.org/>`. During SPARQL translation, the above query becomes:
```sparql
BASE <http://foo.org/>
SELECT ?x
WHERE {
    ?x <bar> ?y
}
```

**NOTE:** The SPARQL spec allows for multiple base IRIs to be defined, but this is not allowed in Flint. Having multiple base IRIs means that each set of IRIs after a base will use a different base IRI. This is mainly useful for defining different bases for IRI prefixes, which clashes with Flint's approach of one `:prefixes` map per query or update.

### `:prefixes`

The `:prefixes` map associates prefix keywords to IRI prefixes. In the following example:
```clojure
{:prefixes {:foaf "<http://xmlns.com/foaf/0.1/>"
            :$    "<http://foo.org/>"}
 :select   [?x]
 :where    [[?x :foaf/name "Dr. X"]
            [?y :bar ?z]]}
```
both `:foaf/name` and `:bar` are prefixed IRIs that are resolvable thanks to prefixes defined in the `:prefixes` map. Note the special prefix `:$`, which represents the "null" prefix; `:$` allows for prefixed IRIs like `:bar` to be written without a namespace, while ensuring that they are still expandable. The above query translates to:
```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX :     <http://foo.org/>
SELECT ?x
WHERE {
    ?x foaf:name "Dr. X"
    ?y :bar ?z
}
```
While in SPARQL each prefix-IRI pair is separately preceded by `PREFIX`, the `:prefixes` map approach is more idiomatic to Clojure.

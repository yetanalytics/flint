# Expressions

Reference: [17. Expressions and Testing Values](https://www.w3.org/TR/sparql11-query/#expressions)

SPARQL supports expressions, which can be used to compute values and filter query results. In particular, expressions can be used in the following circumstances:
- As part of a [`:filter`](where.md#filter) clause.
- As part of a [`:bind`](where.md#bind) clause, in an `[expr var]` form.
- As part of a [`:group-by`](modifier.md#group-by) clause, either as a freestanding expression or in an `[expr var]` form.
- To aggregate or compute values in a [`:select`](query.md#select), [`:order-by`](modifier.md#order-by) or [`:having`](modifier.md#having) clause.

In Flint, an expression is either a list of the form `(op expr...)`, similar to Clojure functions, or a terminal, which can be a [variable](axiom.md#variables), an [IRI](axiom.md#iris) or a [literal](axiom.md#literals).

Other than certain exceptions, like `exists` and `not-exists`, non-terminal expressions only accept other expressions as arguments.

The following is an example of an expression in Flint:
```clojure
(if (< ?x ?y) (str ?x) (* 2 (+ 3 4)))
```
which is translated into SPARQL as:
```sparql
IF((?x < ?y), STR(?x), (2 * (3 + 4)))
```

**NOTE:** Due to the complexity of SPARQL type semantics, Flint does not make any attempt to typecheck or validate expression arguments or return values.

## Boolean and Arithmetic Expressions

SPARQL supports boolean and arithmetic operations, with accept one or more expression arguments and return boolean or numeric values. Like all expressions, boolean and arithmetic operations are written in Clojure's prefix order in Flint, but are translated to SPARQL's infix order. (The exceptions are the unary `not` operator, as well as SPARQL's unary `+` and `-` that are not supported in Flint.)

The `in` and `not-in` operations are special boolean operations that are equivalent to `(or (= expr expr1) (= expr expr2) ...)` and `(and (not= expr expr1) (not= expr expr2) ...)`, respectively.

| Flint | SPARQL Form | Arglist | Reference |
| --- | --- | --- | --- |
| `not` | `!(expr)` | `[expr]`
| `or` | `(expr \|\| expr)` | `[expr & exprs]` | [17.4.1.5](https://www.w3.org/TR/sparql11-query/#func-logical-or)
| `and` | `(expr && expr)` | `[expr & exprs]` | [17.4.1.6](https://www.w3.org/TR/sparql11-query/#func-logical-and)
| `=` | `(expr = expr)` | `[expr expr]` | [17.4.1.7](https://www.w3.org/TR/sparql11-query/#func-RDFterm-equal)
| `not=` | `(expr != expr)` | `[expr expr]`
| `<` | `(expr < expr)` | `[expr expr]`
| `>` | `(expr > expr)` | `[expr expr]`
| `<=` | `(expr <= expr)` | `[expr expr]`
| `>=` | `(expr >= expr)` | `[expr expr]`
| `+` | `(expr + expr + ...)` | `[expr & exprs]`
| `-` | `(expr - expr - ...)` | `[expr & exprs]`
| `*` | `(expr * expr * ...)` | `[expr & exprs]`
| `/` | `(expr / expr / ...)` | `[expr & exprs]`
| `in` | `(expr IN (expr, ...))` | `[expr & exprs]` | [17.4.1.9](https://www.w3.org/TR/sparql11-query/#func-in)
| `not-in` | `(expr NOT IN (expr, ...))` | `[expr & exprs]` | [17.4.1.10](https://www.w3.org/TR/sparql11-query/#func-not-in)

## Built-in Function Expressions

SPARQL accepts a number of built-in functions, which are translated from Flint's `(fname expr expr ...)` form to SPARQL's `FNAME(expr, expr, ...)` form. This section contains a list of all non-aggregate built-in expressions, grouped together by argument and return types.

| Flint | SPARQL | Arglist | Reference
| --- | --- | --- | --- |
| `if` | `IF` | `[expr expr expr]` | [17.4.1.2](https://www.w3.org/TR/sparql11-query/#func-if)
| `coalesce` | `COALESCE` | `[& expr]` | [17.4.1.3](https://www.w3.org/TR/sparql11-query/#func-coalesce)
| `datatype` | `DATATYPE` | `[expr]` | [17.4.2.7](https://www.w3.org/TR/sparql11-query/#func-datatype)

### Graph Patterns

Unlike most expressions, `exists` and `not-exists` only accept graph patterns as arguments. The following example:
```clojure
[:filter (exists [[?person :foaf/name ?name]])]
```
becomes:
```sparql
FILTER EXISTS {
    ?person foaf:name ?name .
}
```

| Flint | SPARQL | Arglist | Reference
| --- | --- | --- | --- |
| `exists` | `EXISTS` | `[graph-pattern]` | [17.4.1.4](https://www.w3.org/TR/sparql11-query/#func-filter-exists)
| `not-exists` | `NOT EXISTS` | `[graph-pattern]` | [17.4.1.4](https://www.w3.org/TR/sparql11-query/#func-filter-exists)

### URIs and IRIs

| Flint | SPARQL name | Arglist | Reference
| --- | --- | --- | --- |
| `iri` | `IRI` | `[expr]` | [17.4.2.8](https://www.w3.org/TR/sparql11-query/#func-iri)
| `uri` | `URI` | `[expr]` | [17.4.2.8](https://www.w3.org/TR/sparql11-query/#func-iri)

### Blank Nodes

| Flint | SPARQL | Arglist | Reference
| --- | --- | --- | --- |
| `bnode` | `BNODE` | `[]` or `[expr]` | [17.4.2.9](https://www.w3.org/TR/sparql11-query/#func-bnode)

### UUIDs

| Flint | SPARQL | Arglist | Reference
| --- | --- | --- | --- |
| `uuid` | `UUID` | `[]` | [17.4.2.12](https://www.w3.org/TR/sparql11-query/#func-uuid)
| `struuid` | `STRUUID` | `[]` | [17.4.2.13](https://www.w3.org/TR/sparql11-query/#func-struuid)

### Predicates

String-specific predicates are listed under [Strings and Language Maps](expr.md#strings-and-language-maps). Note that `bound` takes a variable instead of another expression as its argument.

| Flint | SPARQL name | Arglist | Reference
| --- | --- | --- | --- |
| `bound` | `BOUND` | `[var]` | [17.4.1.1](https://www.w3.org/TR/sparql11-query/#func-bound)
| `sameterm` | `SAMETERM` | `[expr expr]` | [17.4.1.8](https://www.w3.org/TR/sparql11-query/#func-sameTerm)
| `iri?` | `isIRI` | `[expr]` | [17.4.2.1](https://www.w3.org/TR/sparql11-query/#func-isIRI)
| `uri?` | `isURI` | `[expr]` | [17.4.2.1](https://www.w3.org/TR/sparql11-query/#func-isIRI)
| `blank?` | `isBLANK` | `[expr]` | [17.4.2.2](https://www.w3.org/TR/sparql11-query/#func-isBlank)
| `literal?` | `isLITERAL` | `[expr]` | [17.4.2.3](https://www.w3.org/TR/sparql11-query/#func-isLiteral)
| `numeric?` | `isNUMERIC` | `[expr]` | [17.4.2.4](https://www.w3.org/TR/sparql11-query/#func-isNumeric)

### Strings and Language Maps

| Flint | SPARQL | Arglist | Reference
| --- | --- | --- | --- |
| `str` | `STR` | `[expr]` | [17.4.2.5](https://www.w3.org/TR/sparql11-query/#func-str)
| `lang` | `LANG` | `[expr]` | [17.4.2.6](https://www.w3.org/TR/sparql11-query/#func-lang)
| `strdt` | `STRDT` | `[expr expr]` | [17.4.2.10](https://www.w3.org/TR/sparql11-query/#func-strdt)
| `strlang` | `STRLANG` | `[expr expr]` | [17.4.2.11](https://www.w3.org/TR/sparql11-query/#func-strlang)
| `strlen` | `STRLEN` | `[expr]` | [17.4.3.2](https://www.w3.org/TR/sparql11-query/#func-strlen)
| `substr` | `SUBSTR` | `[expr expr]` or `[expr expr expr]` | [17.4.3.3](https://www.w3.org/TR/sparql11-query/#func-substr)
| `ucase` | `UCASE` | `[expr]` | [17.4.3.4](https://www.w3.org/TR/sparql11-query/#func-ucase)
| `lcase` | `LCASE` | `[expr]` | [17.4.3.5](https://www.w3.org/TR/sparql11-query/#func-lcase)
| `strstarts` | `STRSTARTS` | `[expr expr]` | [17.4.3.6](https://www.w3.org/TR/sparql11-query/#func-strstarts)
| `strends` | `STRENDS` | `[expr expr]`| [17.4.3.7](https://www.w3.org/TR/sparql11-query/#func-strends)
| `contains` | `CONTAINS` | `[expr expr]`| [17.4.3.8](https://www.w3.org/TR/sparql11-query/#func-contains)
| `strbefore` | `STRBEFORE` | `[expr expr]`| [17.4.3.9](https://www.w3.org/TR/sparql11-query/#func-strbefore)
| `strafter` | `STRAFTER` | `[expr expr]`| [17.4.3.10](https://www.w3.org/TR/sparql11-query/#func-strafter)
| `encode-for-uri` | `ENCODE_FOR_URI` | `[expr]`| [17.4.3.11](https://www.w3.org/TR/sparql11-query/#func-encode)
| `concat` | `CONCAT` | `[& exprs]` | [17.4.3.12](https://www.w3.org/TR/sparql11-query/#func-concat)
| `lang-matches` | `LANGMATCHES` | `[expr expr]` | [17.4.3.13](https://www.w3.org/TR/sparql11-query/#func-langMatches)
| `regex` | `REGEX` | `[expr expr]` or `[expr expr expr]` | [17.4.3.14](https://www.w3.org/TR/sparql11-query/#func-regex)
| `replace` | `REPLACE` | `[expr expr expr]` or `[expr expr expr expr]` | [17.4.3.15](https://www.w3.org/TR/sparql11-query/#func-replace)

### Numerics

| Flint | SPARQL | Arglist | Reference
| --- | --- | --- | --- |
| `abs` | `ABS` | `[expr]` | [17.4.4.1](https://www.w3.org/TR/sparql11-query/#func-abs)
| `round` | `ROUND` | `[expr]` | [17.4.4.2](https://www.w3.org/TR/sparql11-query/#func-round)
| `ceil` | `CEIL` | `[expr]` | [17.4.4.3](https://www.w3.org/TR/sparql11-query/#func-ceil)
| `floor` | `FLOOR` | `[expr]` | [17.4.4.4](https://www.w3.org/TR/sparql11-query/#func-floor)
| `rand` | `RAND` | `[]` | [17.4.4.5](https://www.w3.org/TR/sparql11-query/#idp2130040)

### Dates and Times

| Flint | SPARQL | Arglist | Reference
| --- | --- | --- | --- |
| `now` | `NOW` | `[]` | [17.4.5.1](https://www.w3.org/TR/sparql11-query/#func-now)
| `year` | `YEAR` | `[expr]` | [17.4.5.2](https://www.w3.org/TR/sparql11-query/#func-year)
| `month` | `MONTH` | `[expr]` | [17.4.5.3](https://www.w3.org/TR/sparql11-query/#func-month)
| `day` | `DAY` | `[expr]` | [17.4.5.4](https://www.w3.org/TR/sparql11-query/#func-day)
| `hours` | `HOURS` | `[expr]` | [17.4.5.5](https://www.w3.org/TR/sparql11-query/#func-hours)
| `minutes` | `MINUTES` | `[expr]` | [17.4.5.6](https://www.w3.org/TR/sparql11-query/#func-minutes)
| `seconds` | `SECONDS` | `[expr]` | [17.4.5.7](https://www.w3.org/TR/sparql11-query/#func-seconds)
| `timezone` | `TIMEZONE` | `[expr]` | [17.4.5.8](https://www.w3.org/TR/sparql11-query/#func-timezone)
| `tz` | `TZ` | `[expr]` | [17.4.5.9](https://www.w3.org/TR/sparql11-query/#func-tz)

### Hash Functions

| Flint | SPARQL | Arglist | Reference
| --- | --- | --- | --- |
| `md5` | `MD5` | `[expr]` | [17.4.6.1](https://www.w3.org/TR/sparql11-query/#func-md5)
| `sha1` | `SHA1` | `[expr]` | [17.4.6.2](https://www.w3.org/TR/sparql11-query/#func-sha1)
| `sha256` | `SHA256` | `[expr]` | [17.4.6.3](https://www.w3.org/TR/sparql11-query/#func-sha256)
| `sha384` | `SHA384` | `[expr]` | [17.4.6.4](https://www.w3.org/TR/sparql11-query/#func-sha384)
| `sha512` | `SHA512` | `[expr]` | [17.4.6.5](https://www.w3.org/TR/sparql11-query/#func-sha512)

## Aggregate Expressions

Aggregates are special expressions that can only be used in `SELECT` (and its `DISTINCT` and `REDUCED` variants), `ORDER BY` and `HAVING` clauses.

| Flint | SPARQL | Arglist
| --- | --- | --- |
| `sum` | `SUM` | `[expr & {:keys [distinct?]}]`
| `min` | `MIN` | `[expr & {:keys [distinct?]}]`
| `max` | `MAX` | `[expr & {:keys [distinct?]}]`
| `avg` | `AVG` | `[expr & {:keys [distinct?]}]`
| `sample` | `SAMPLE` | `[expr & {:keys [distinct?]}]`
| `count` | `COUNT` | `[expr-or-wildcard & {:keys [distinct?]}]`
| `group-concat` | `GROUP_CONCAT` | `[expr & {:keys [distinct? separator]}]`

The `count` aggregate can accept either an expression or a wildcard, e.g. both `(count ?x)` and `(count *)` are valid.

Unlike other expressions, aggregates in Flint support keyword arguments. Every aggregate function supports the `:distinct?` keyword arg, which accepts a boolean value. If `:distinct?` is `true`, then `DISTINCT` is added to the arg list in SPARQL. The example,
```clojure
(sum ?x :distinct? true)
```
becomes
```sparql
SUM(DISTINCT ?x)
```

`group-concat` also accepts the `:separator` keyword arg whose value is a separator string. Both `:distinct?` and `:separator` can be supported in the same expression, as so:
```clojure
(group-concat ?y :distinct? true :separator ";")
```
which becomes
```sparql
GROUP_CONCAT(DISTINCT ?y; SEPARATOR = ";")
```

**NOTE:** Using aggregates in an invalid clause, e.g. a `FILTER` clause, will cause a spec error.

**NOTE:** Using aggregates inside another aggregate will cause a spec error. (Nesting aggregates inside non-aggregate expressions, or vice versa, is perfectly fine, however.)

**NOTE:** Using aggregates in a `SELECT` query, or including a `GROUP BY` in the query, introduces aggregate restrictions on the `SELECT` clause. See the [SPARQL Queries](query.md) page for more details.

## Custom Functions

Users can write their own custom functions, which consist of using an IRI or prefixed IRI instead of a symbol. The example:
```clojure
[:filter (:func/isEven ?x)]
```
becomes
```sparql
FILTER func:isEven(?x)
```

**NOTE:** Flint makes no attempt to validate that custom function IRIs resolve to valid resources, nor does it to attempt to validate input or output.

In `:select`, `:order-by` and `:having` clauses, custom aggregates are allowed, and any custom function can accept the `:distinct?` keyword arg. During aggregate validation, all custom functions in these clauses are treated as aggregates.

## Variable Binding

Variables can be bound to the result of expressions in `:bind`, `:select`, and `:group-by` clauses. In Flint, they are written in as the vector `[expr var]`, such as in this example:
```clojure
[(+ 2 2) ?four]
```
which then becomes:
```sparql
(2 + 2) AS ?four
```

**NOTE:** In a `:select` or `:bind` clause, the variable being bound to cannot already be in-scope. In a `:select` clause, the var cannot be already defined in the `:where` clause, nor be previously projected in that same clause. In the `:bind` case, it cannot be already defined in previously-listed graph patterns.

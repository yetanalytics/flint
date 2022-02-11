# Expressions

### Boolean and Arithmetic Expressions

| Flint | SPARQL | Arglist | Reference |
| --- | --- | --- | --- |
| `not` | `!` | `[expr]`
| `and` | `&&` | `[expr & exprs]` | [17.4.1.6](https://www.w3.org/TR/sparql11-query/#func-logical-and)
| `or` | `\|\|` | `[expr & exprs]` | [17.4.1.5](https://www.w3.org/TR/sparql11-query/#func-logical-or)
| `=` | `=` | `[expr expr]` | [17.4.1.7](https://www.w3.org/TR/sparql11-query/#func-RDFterm-equal)
| `not=` | `!=` | `[expr expr]`
| `<` | `<` | `[expr expr]`
| `>` | `>` | `[expr expr]`
| `<=` | `<=` | `[expr expr]`
| `?=` | `>=` | `[expr expr]`
| `+` | `+` | `[expr & exprs]`
| `-` | `-` | `[expr & exprs]`
| `*` | `*` | `[expr & exprs]`
| `/` | `/` | `[expr & exprs]`
| `in` | `IN` | `[expr & exprs]` | [17.4.1.9](https://www.w3.org/TR/sparql11-query/#func-in)
| `not-in` | `NOT IN` | `[expr & exprs]` | [17.4.1.10](https://www.w3.org/TR/sparql11-query/#func-not-in)

### Built-in Function Expressions

| Flint | SPARQL | Arglist | Reference
| --- | --- | --- | --- |
| `rand` | `RAND` | `[]` | [17.4.4.5](https://www.w3.org/TR/sparql11-query/#func-rand)
| `now` | `NOW` | `[]` | [17.4.5.1](https://www.w3.org/TR/sparql11-query/#func-now)
| `uuid` | `UUID` | `[]` | [17.4.2.12](https://www.w3.org/TR/sparql11-query/#func-uuid)
| `struuid` | `STRUUID` | `[]` | [17.4.2.13](https://www.w3.org/TR/sparql11-query/#func-struuid)
| `bnode` | `BNODE` | `[]` or `[expr]` | [17.4.2.9](https://www.w3.org/TR/sparql11-query/#func-bnode)
| `bound` | `BOUND` | `[var]` | [17.4.1.1](https://www.w3.org/TR/sparql11-query/#func-bound)
| `exists` | `EXISTS` | `[where-clause]` | [17.4.1.4](https://www.w3.org/TR/sparql11-query/#func-filter-exists)
| `not-exists` | `NOT EXISTS` | `[where-clause]` | [17.4.1.4](https://www.w3.org/TR/sparql11-query/#func-filter-exists)
| `str` | `STR` | `[expr]` | [17.4.2.5](https://www.w3.org/TR/sparql11-query/#func-str)
| `strlen` | `STRLEN` | `[expr]` | [17.4.3.2](https://www.w3.org/TR/sparql11-query/#func-strlen)
| `ucase` | `UCASE` | `[expr]` | [17.4.3.4](https://www.w3.org/TR/sparql11-query/#func-ucase)
| `lcase` | `LCASE` | `[expr]` | [17.4.3.5](https://www.w3.org/TR/sparql11-query/#func-lcase)
| `lang` | `LANG` | `[expr]` | [17.4.2.6](https://www.w3.org/TR/sparql11-query/#func-lang)
| `datatype` | `DATATYPE` | `[expr]` | [17.4.2.7](https://www.w3.org/TR/sparql11-query/#func-lang)
| `blank?` | `isBlank` | `[expr]` | [17.4.2.2](https://www.w3.org/TR/sparql11-query/#func-isBlank)
| `literal?` | `isLiteral` | `[expr]` | [17.4.2.3](https://www.w3.org/TR/sparql11-query/#func-isLiteral)
| `numeric` | `isNumeric` | `[expr]` | [17.4.2.4](https://www.w3.org/TR/sparql11-query/#func-isNumeric)
| `iri` | `IRI` | `[expr]` | [17.4.2.8](https://www.w3.org/TR/sparql11-query/#func-iri)
| `uri` | `URI` | `[expr]` | [17.4.2.8](https://www.w3.org/TR/sparql11-query/#func-iri)
| `iri?` | `isIRI` | `[expr]` | [17.4.2.1](https://www.w3.org/TR/sparql11-query/#func-isIRI)
| `uri?` | `isURI` | `[expr]` | [17.4.2.1](https://www.w3.org/TR/sparql11-query/#func-isIRI)
| `encode-for-uri` | `ENCODE_FOR_URI` | `[expr]`| [17.4.3.11](https://www.w3.org/TR/sparql11-query/#func-encode)
| `abs` | `ABS` | `[expr]` | [17.4.4.1](https://www.w3.org/TR/sparql11-query/#func-abs)
| `ceil` | `CEIL` | `[expr]` | [17.4.4.3](https://www.w3.org/TR/sparql11-query/#func-ceil)
| `floor` | `FLOOR` | `[expr]` | [17.4.4.4](https://www.w3.org/TR/sparql11-query/#func-floor)
| `round` | `ROUND` | `[expr]` | [17.4.4.2](https://www.w3.org/TR/sparql11-query/#func-round)
| `year` | `YEAR` | `[expr]` | [17.4.5.2](https://www.w3.org/TR/sparql11-query/#func-year)
| `month` | `MONTH` | `[expr]` | [17.4.5.3](https://www.w3.org/TR/sparql11-query/#func-month)
| `day` | `DAY` | `[expr]` | [17.4.5.4](https://www.w3.org/TR/sparql11-query/#func-day)
| `hours` | `HOURS` | `[expr]` | [17.4.5.5](https://www.w3.org/TR/sparql11-query/#func-hours)
| `minutes` | `MINUTES` | `[expr]` | [17.4.5.6](https://www.w3.org/TR/sparql11-query/#func-minutes)
| `seconds` | `SECONDS` | `[expr]` | [17.4.5.7](https://www.w3.org/TR/sparql11-query/#func-seconds)
| `timezone` | `TIMEZONE` | `[expr]` | [17.4.5.8](https://www.w3.org/TR/sparql11-query/#func-timezone)
| `tz` | `TZ` | `[expr]` | [17.4.5.9](https://www.w3.org/TR/sparql11-query/#func-tz)
| `md5` | `MD5` | `[expr]` | [17.4.6.1](https://www.w3.org/TR/sparql11-query/#func-md5)
| `sha1` | `SHA1` | `[expr]` | [17.4.6.2](https://www.w3.org/TR/sparql11-query/#func-sha1)
| `sha256` | `SHA256` | `[expr]` | [17.4.6.3](https://www.w3.org/TR/sparql11-query/#func-sha256)
| `SHA384` | `SHA384` | `[expr]` | [17.4.6.4](https://www.w3.org/TR/sparql11-query/#func-sha384)
| `sha512` | `SHA512` | `[expr]` | [17.4.6.5](https://www.w3.org/TR/sparql11-query/#func-sha512)
| `lang-matches` | `LANGMATCHES` | `[expr expr]` | [17.4.3.13](https://www.w3.org/TR/sparql11-query/#func-langMatches)
| `contains` | `CONTAINS` | `[expr expr]`| [17.4.3.8](https://www.w3.org/TR/sparql11-query/#func-contains)
| `strlang` | `STRLANG` | `[expr expr]` | [17.4.2.11](https://www.w3.org/TR/sparql11-query/#func-strlang)
| `strdt` | `STRDT` | `[expr expr]` | [17.4.2.10](https://www.w3.org/TR/sparql11-query/#func-strdt)
| `strstarts` | `STRSTARTS` | `[expr expr]` | [17.4.3.6](https://www.w3.org/TR/sparql11-query/#func-strstarts)
| `strends` | `STRENDS` | `[expr expr]`| [17.4.3.7](https://www.w3.org/TR/sparql11-query/#func-strends)
| `strbefore` | `STRBEFORE` | `[expr expr]`| [17.4.3.9](https://www.w3.org/TR/sparql11-query/#func-strbefore)
| `strafter` | `STRAFTER` | `[expr expr]`| [17.4.3.10](https://www.w3.org/TR/sparql11-query/#func-strafter)
| `sameterm` | `SAMETERM` | `[expr expr]` | [17.4.1.8](https://www.w3.org/TR/sparql11-query/#func-sameTerm)
| `if` | `IF` | `[expr expr expr]` | [17.4.1.2](https://www.w3.org/TR/sparql11-query/#func-if)
| `regex` | `REGEX` | `[expr expr]` or `[expr expr expr]` | [17.4.3.14](https://www.w3.org/TR/sparql11-query/#func-regex)
| `substr` | `SUBSTR` | `[expr expr]` or `[expr expr expr]` | [17.4.3.3](https://www.w3.org/TR/sparql11-query/#func-substr)
| `replace` | `REPLACE` | `[expr expr expr]` or `[expr expr expr expr]` | [17.4.3.15](https://www.w3.org/TR/sparql11-query/#func-replace)
| `concat` | `CONCAT` | `[& exprs]` | [17.4.3.12](https://www.w3.org/TR/sparql11-query/#func-concat)
| `coalesce` | `COALESCE` | `[& expr]` | [17.4.1.3](https://www.w3.org/TR/sparql11-query/#func-coalesce)

### Aggregate Expressions

| Flint | SPARQL | Arglist
| --- | --- | --- |
| `sum` | `SUM` | `[expr & {:keys [distinct?]}]`
| `min` | `MIN` | `[expr & {:keys [distinct?]}]`
| `max` | `MAX` | `[expr & {:keys [distinct?]}]`
| `avg` | `AVG` | `[expr & {:keys [distinct?]}]`
| `sample` | `SAMPLE` | `[expr & {:keys [distinct?]}]`
| `count` | `COUNT` | `[expr-or-wildcard & {:keys [distinct?]}]`
| `group-concat` | `GROUP` | `[expr & {:keys [distinct? separator]}]`

The `count` aggregate can accept either a normal expression or a wildcard, so both (for example) `(count ?x)` and `(count *)` are valid.

Unlike other expressions, aggregates in Flint support keyword arguments. Every aggregate function supports the `:distinct?` keyword arg, which accepts a boolean value. If `:distinct?` is `true`, then `DISTINCT` is added to the arg list in SPARQL. For example,
```clojure
(sum ?x :distinct? true)
```
becomes
```sparql
SUM(DISTINCT ?x)
```

`group-concat` has an additional `:separator` keyword arg that takes a separator string. Both `:distinct?` and `:separator` can be supported in the same expression, so:
```clojure
(group-concat ?y :distinct? true :separator ";")
```
becomes
```sparql
GROUP_CONCAT(DISTINCT ?y SEPARATOR = ";")
```

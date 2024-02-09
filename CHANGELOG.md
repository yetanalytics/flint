# Changelog

## v0.3.0

- Add support for RDF List syntactic sugar.
- Add support for blank node vector syntactic sugar.
- Modify the AST tree for triples to support the new features and to remove redundant nodes in the tree.
- Rework blank node validation to make the implementation simpler (this results in minor changes to the error output).
- Disallow syntax-quoting for symbols.
- Fix bug where timestamps with zeroed-out seconds have the seconds omitted.

## v0.2.1

- Update GitHub Actions CI and CD to remove deprecation warnings.

## v0.2.0

- Rework IRI, variable, blank node and literal implementations and add support for additional Java(Script) types.
  - Implement protocols to define validation and formatting behavior IRIs, variables, blank nodes and literals, and apply `extend-type`/`extend-protocol` to default types.
  - Add support for `java.net.URI` and `js/URL` IRI instances.
  - Add support for `java.time.Temporal` timestamps (e.g. `LocalDateTime` and `ZonedDateTime`).
  - Refine datatypes for numeric literals, e.g. Clojure integers are associated with `xsd:long` by default.
  - Refine datatypes for date- and time-only timestamps, e.g. `java.sql.Date` and `java.sql.Time` are now associated with `xsd:date` and `xsd:time`, respectively. **(Breaking!)**
- Add `:force-iris?` optional arg in order to force datatype IRIs to be appended when formatting literals (with the exception of language-tagged strings).
- Add support for Unicode characters and percent encoding.
  - Unicode characters are now supported in symbols and keywords.
  - Percent encoding is allowed in prefixed IRI keyword names.
  - (Clojure-only) Optimize string validation.
- Replace certain uses of `s/or` with multi-specs in order to simplify error messages.

## v0.1.2

- Fix a bug where certain SPARQL Update clauses - `LOAD`, `CLEAR`, `CREATE`, and `DROP` - were not being correctly formatted.
  - The form `[:graph iri]` is now mandatory for the aforementioned Update clauses and optional for others (`ADD`, `MOVE`, AND `COPY`).
- Fix validation of strings containing escaped char sequences such as `\\n` or `\\r`.
  - Special thanks to [@quoll](https://github.com/quoll) for their assistance with this bugfix.

## v0.1.1

Fix a number of bugs discovered in v0.1.0 (see [Pull Request #20](https://github.com/yetanalytics/flint/pull/20)):
- Allow IRIs and prefixed IRIs to be used in expressions.
- Fix zipper traversal not working correctly with deletion or insertion quads.
- Fix `java.time.Instant` instances not being formatted correctly.
- Fix `a` not being valid in DELETE or INSERT queries.
- Fix incorrect predicate specs for triples that restrict both blank nodes and variables.
- Ensure parentheses are properly added around negated property paths.
- Ensure parentheses around expressions are added for certain clauses (e.g. `FILTER`).

Apply updates to the documentation:
- Fix incorrect documentation on the `CONSTRUCT WHERE` query.
- General grammar and cleanup fixes.

## v0.1.0

Initial release of Flint!

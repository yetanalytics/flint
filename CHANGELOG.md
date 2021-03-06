# Changelog

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

# flint
Flint is a Clojure(Script) DSL for creating SPARQL Query and Update strings.

## API

Three functions exist in the Flint API: `format-query`, `format-update`, and `format-updates`. The first two functions format a single SPARQL Query or Update, respectively, while the third formats a collection of SPARQL Updates into a single Update Request.

Each function takes in the following keyword arguments:
| Argument | Description |
| --- | --- |
| `pretty?` | If `true`, adds line breaks and indentation to the resulting SPARQL string. Default `false`.
| `validate?` | If `true`, performs advanced validation. Currently validates prefixed IRIs and scoped vars in `expr AS var` clauses. Default `true`.

## Prior Art
- Flint is based off of the grammar of [SPARQL 1.1](https://www.w3.org/TR/sparql11-query/).
- The idea of a SPARQL DSL was inspired by [HoneySQL](https://github.com/seancorfield/honeysql), a DSL for creating SQL queries.
- Flint borrows certain syntactic conventions from the [Datalog grammar](https://docs.datomic.com/on-prem/query/query.html).

## License

Copyright © 2022 Yet Analytics, Inc.

Distributed under the Apache License version 2.0.

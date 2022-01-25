(ns syrup.sparql.format.axiom-test
  (:require [clojure.test :refer [deftest testing is]]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]))

(deftest axiom-format
  (testing "Formatting terminal AST nodes"
    (is (= "<http://example.org>"
           (f/format-ast [:iri "<http://example.org>"])))
    (is (= "foo:bar"
           (f/format-ast [:prefix-iri :foo/bar])))
    (is (= "?xyz"
           (f/format-ast [:var '?xyz])))
    (is (= "_:b0"
           (f/format-ast [:bnode '_b0])))
    (is (= "[]"
           (f/format-ast [:bnode '_])))
    (is (= "*"
           (f/format-ast [:wildcard '*])))
    (is (= "*"
           (f/format-ast [:wildcard :*])))
    (is (= "a"
           (f/format-ast [:rdf-type 'a])))
    (is (= "a"
           (f/format-ast [:rdf-type :a])))
    (is (= "\"My String\""
           (f/format-ast [:str-lit "My String"])))
    (is (= "\"My String\"@en"
           (f/format-ast [:lmap-lit {:en "My String"}])))
    (is (= "123"
           (f/format-ast [:num-lit 123])))
    (is (= "123.4"
           (f/format-ast [:num-lit 123.4])))
    (is (= "true"
           (f/format-ast [:bool-lit true])))
    (is (= "2022-01-20T16:22:19Z^^<http://www.w3.org/2001/XMLSchema#dateTime>"
           (f/format-ast [:dt-lit #inst "2022-01-20T16:22:19Z"])))))

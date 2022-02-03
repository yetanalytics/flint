(ns com.yetanalytics.flint.format.axiom-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]))

(deftest axiom-format
  (testing "Formatting terminal AST nodes"
    (is (= "<http://example.org>"
           (f/format-ast-node {} [:iri "<http://example.org>"])))
    (is (= "foo:bar"
           (f/format-ast-node {} [:prefix-iri :foo/bar])))
    (is (= "?xyz"
           (f/format-ast-node {} [:var '?xyz])))
    (is (= "_:b0"
           (f/format-ast-node {} [:bnode '_b0])))
    (is (= "[]"
           (f/format-ast-node {} [:bnode '_])))
    (is (= "*"
           (f/format-ast-node {} [:wildcard '*])))
    (is (= "*"
           (f/format-ast-node {} [:wildcard :*])))
    (is (= "a"
           (f/format-ast-node {} [:rdf-type 'a])))
    (is (= "a"
           (f/format-ast-node {} [:rdf-type :a])))
    (is (= "\"My String\""
           (f/format-ast-node {} [:str-lit "My String"])))
    (is (= "\"My String\"@en"
           (f/format-ast-node {} [:lmap-lit {:en "My String"}])))
    (is (= "123"
           (f/format-ast-node {} [:num-lit 123])))
    (is (= "123.4"
           (f/format-ast-node {} [:num-lit 123.4])))
    (is (= "true"
           (f/format-ast-node {} [:bool-lit true])))
    (is (= "\"2022-01-20T16:22:19Z\"^^<http://www.w3.org/2001/XMLSchema#dateTime>"
           (f/format-ast-node {} [:dt-lit #inst "2022-01-20T16:22:19Z"])))
    (is (= "\"2022-01-20T16:22:19Z\"^^xsd:dateTime"
           (f/format-ast-node {:xsd-prefix "xsd"}
                              [:dt-lit #inst "2022-01-20T16:22:19Z"])))))

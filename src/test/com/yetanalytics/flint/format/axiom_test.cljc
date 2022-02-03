(ns com.yetanalytics.flint.format.axiom-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]))

(deftest axiom-format
  (testing "Formatting terminal AST nodes"
    (is (= "<http://example.org>"
           (f/format-ast-node {} [:ax/iri "<http://example.org>"])))
    (is (= "foo:bar"
           (f/format-ast-node {} [:ax/prefix-iri :foo/bar])))
    (is (= "?xyz"
           (f/format-ast-node {} [:ax/var '?xyz])))
    (is (= "_:b0"
           (f/format-ast-node {} [:ax/bnode '_b0])))
    (is (= "[]"
           (f/format-ast-node {} [:ax/bnode '_])))
    (is (= "*"
           (f/format-ast-node {} [:ax/wildcard '*])))
    (is (= "*"
           (f/format-ast-node {} [:ax/wildcard :*])))
    (is (= "a"
           (f/format-ast-node {} [:ax/rdf-type 'a])))
    (is (= "a"
           (f/format-ast-node {} [:ax/rdf-type :a])))
    (is (= "NULL"
           (f/format-ast-node {} [:ax/nil :a])))
    (is (= "\"My String\""
           (f/format-ast-node {} [:ax/str-lit "My String"])))
    (is (= "\"My String\"@en"
           (f/format-ast-node {} [:ax/lmap-lit {:en "My String"}])))
    (is (= "123"
           (f/format-ast-node {} [:ax/num-lit 123])))
    (is (= "123.4"
           (f/format-ast-node {} [:ax/num-lit 123.4])))
    (is (= "true"
           (f/format-ast-node {} [:ax/bool-lit true])))
    (is (= (str #?(:clj "\"2022-01-20T16:22:19Z\""
                   :cljs "\"2022-01-20T16:22:19.000Z\"")
                "^^<http://www.w3.org/2001/XMLSchema#dateTime>")
           (f/format-ast-node {} [:ax/dt-lit #inst "2022-01-20T16:22:19Z"])))
    (is (= (str #?(:clj "\"2022-01-20T16:22:19Z\""
                   :cljs "\"2022-01-20T16:22:19.000Z\"")
                "^^xsd:dateTime")
           (f/format-ast-node {:xsd-prefix "xsd"}
                              [:ax/dt-lit #inst "2022-01-20T16:22:19Z"])))))

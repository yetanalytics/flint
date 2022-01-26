(ns syrup.sparql.format.query-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [clojure.walk :as w]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.query]))

(deftest format-test
  (testing "format SELECT query"
    (is (= (cstr/join "\n" ["PREFIX foo: <http://example.org/foo/>"
                            "SELECT ?x"
                            "FROM <http://example.org/my-graph/>"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "}"
                            "ORDER BY ASC(?y)"
                            "VALUES ?z {"
                            "    1"
                            "}"])
           (->> '[:select-query
                  [[:prefixes [[:prefix [:foo [:iri "<http://example.org/foo/>"]]]]]
                   [:select [:select/var-or-exprs [[:var ?x]]]]
                   [:from [:iri "<http://example.org/my-graph/>"]]
                   [:where [:where-sub/where [[:triple/vec [[:var ?x] [:var ?y] [:var ?z]]]]]]
                   [:order-by [[:mod/asc-desc
                                [[:mod/op asc]
                                 [:mod/expr [:expr/terminal [:var ?y]]]]]]]
                   [:values [:values/map
                             [[[:var ?z]]
                              [[[:num-lit 1]]]]]]]]
                (w/postwalk f/format-ast)))))
  (testing "format CONSTRUCT query"
    (is (= (cstr/join "\n" ["CONSTRUCT {"
                            "    ?x ?y ?z ."
                            "}"
                            "FROM <http://example.org/my-graph/>"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "}"])
           (->> '[:construct-query
                  [[:construct [[:triple/vec [[:var ?x] [:var ?y] [:var ?z]]]]]
                   [:from [:iri "<http://example.org/my-graph/>"]]
                   [:where [:where-sub/where [[:triple/vec [[:var ?x] [:var ?y] [:var ?z]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= (cstr/join "\n" ["CONSTRUCT"
                            "FROM <http://example.org/my-graph/>"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "}"])
           (->> '[:construct-query
                  [[:construct []]
                   [:from [:iri "<http://example.org/my-graph/>"]]
                   [:where [:where-sub/where [[:triple/vec [[:var ?x] [:var ?y] [:var ?z]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= (cstr/join "\n" ["CONSTRUCT"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "}"])
           (->> '[:construct-query
                  [[:construct []]
                   [:where [:where-sub/where [[:triple/vec [[:var ?x] [:var ?y] [:var ?z]]]]]]]]
                (w/postwalk f/format-ast)))))
  (testing "format DESCRIBE query"
    (is (= (cstr/join "\n" ["DESCRIBE ?x ?y"
                            "FROM NAMED <http://example.org/my-graph/>"
                            "FROM NAMED <http://example.org/my-graph-2/>"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "}"])
           (->> '[:describe-query
                  [[:describe [:describe/vars-or-iris [[:var ?x] [:var ?y]]]]
                   [:from-named [[:iri "<http://example.org/my-graph/>"]
                                 [:iri "<http://example.org/my-graph-2/>"]]]
                   [:where [:where-sub/where [[:triple/vec [[:var ?x] [:var ?y] [:var ?z]]]]]]]]
                (w/postwalk f/format-ast)))))
  (testing "format ASK query"
    (is (= (cstr/join "\n" ["ASK"
                            "FROM NAMED <http://example.org/my-graph/>"
                            "FROM NAMED <http://example.org/my-graph-2/>"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "}"])
           (->> '[:ask-query
                  [[:ask []]
                   [:from-named [[:iri "<http://example.org/my-graph/>"]
                                 [:iri "<http://example.org/my-graph-2/>"]]]
                   [:where [:where-sub/where [[:triple/vec [[:var ?x] [:var ?y] [:var ?z]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= (cstr/join "\n" ["ASK"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "}"])
           (->> '[:ask-query
                  [[:ask []]
                   [:where [:where-sub/where [[:triple/vec [[:var ?x] [:var ?y] [:var ?z]]]]]]]]
                (w/postwalk f/format-ast))))))

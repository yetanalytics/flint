(ns com.yetanalytics.flint.format.query-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.query]))

(defn- format-ast [ast]
  (f/format-ast ast {:pretty? true}))

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
           (->> '[:query/select
                  [[:prefixes [[:prologue/prefix [:foo [:ax/iri "<http://example.org/foo/>"]]]]]
                   [:select [:select/var-or-exprs [[:ax/var ?x]]]]
                   [:from [:ax/iri "<http://example.org/my-graph/>"]]
                   [:where [:where-sub/where [[:triple/vec [[:ax/var ?x]
                                                            [:ax/var ?y]
                                                            [:ax/var ?z]]]]]]
                   [:order-by [[:mod/asc-desc
                                [[:mod/op asc]
                                 [:mod/expr [:expr/terminal [:ax/var ?y]]]]]]]
                   [:values [:values/map
                             [[[:ax/var ?z]]
                              [[[:ax/num-lit 1]]]]]]]]
                format-ast))))
  (testing "format CONSTRUCT query"
    (is (= (cstr/join "\n" ["CONSTRUCT {"
                            "    ?x ?y ?z ."
                            "}"
                            "FROM <http://example.org/my-graph/>"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "}"])
           (->> '[:query/construct
                  [[:construct [[:triple/vec [[:ax/var ?x]
                                              [:ax/var ?y]
                                              [:ax/var ?z]]]]]
                   [:from [:ax/iri "<http://example.org/my-graph/>"]]
                   [:where [:where-sub/where [[:triple/vec [[:ax/var ?x]
                                                            [:ax/var ?y]
                                                            [:ax/var ?z]]]]]]]]
                format-ast)))
    (is (= (cstr/join "\n" ["CONSTRUCT"
                            "FROM <http://example.org/my-graph/>"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "}"])
           (->> '[:query/construct
                  [[:construct []]
                   [:from [:ax/iri "<http://example.org/my-graph/>"]]
                   [:where [:where-sub/where [[:triple/vec [[:ax/var ?x]
                                                            [:ax/var ?y]
                                                            [:ax/var ?z]]]]]]]]
                format-ast)))
    (is (= (cstr/join "\n" ["CONSTRUCT"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "}"])
           (->> '[:query/construct
                  [[:construct []]
                   [:where [:where-sub/where [[:triple/vec [[:ax/var ?x]
                                                            [:ax/var ?y]
                                                            [:ax/var ?z]]]]]]]]
                format-ast))))
  (testing "format DESCRIBE query"
    (is (= (cstr/join "\n" ["DESCRIBE ?x ?y"
                            "FROM NAMED <http://example.org/my-graph/>"
                            "FROM NAMED <http://example.org/my-graph-2/>"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "}"])
           (->> '[:query/describe
                  [[:describe [:describe/vars-or-iris [[:ax/var ?x] [:ax/var ?y]]]]
                   [:from-named [[:ax/iri "<http://example.org/my-graph/>"]
                                 [:ax/iri "<http://example.org/my-graph-2/>"]]]
                   [:where [:where-sub/where [[:triple/vec [[:ax/var ?x]
                                                            [:ax/var ?y]
                                                            [:ax/var ?z]]]]]]]]
                format-ast))))
  (testing "format ASK query"
    (is (= (cstr/join "\n" ["ASK"
                            "FROM NAMED <http://example.org/my-graph/>"
                            "FROM NAMED <http://example.org/my-graph-2/>"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "}"])
           (->> '[:query/ask
                  [[:ask []]
                   [:from-named [[:ax/iri "<http://example.org/my-graph/>"]
                                 [:ax/iri "<http://example.org/my-graph-2/>"]]]
                   [:where [:where-sub/where [[:triple/vec [[:ax/var ?x]
                                                            [:ax/var ?y]
                                                            [:ax/var ?z]]]]]]]]
                format-ast)))
    (is (= (cstr/join "\n" ["ASK"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "}"])
           (->> '[:query/ask
                  [[:ask []]
                   [:where [:where-sub/where [[:triple/vec [[:ax/var ?x]
                                                            [:ax/var ?y]
                                                            [:ax/var ?z]]]]]]]]
                format-ast)))))

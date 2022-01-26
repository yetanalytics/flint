(ns syrup.sparql.spec.where-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.where :as ws]))

(deftest conform-where-test
  (testing "Conform WHERE clause"
    (is (= '[:where-sub/select [[:select [:select/var-or-exprs [[:var ?s]]]]
                                [:where [:where-sub/where [[:triple/vec [[:var ?s] [:var ?p] [:var ?o]]]]]]]]
           (s/conform ::ws/where '{:select [?s] :where [[?s ?p ?o]]})))
    (is (= '[:where-sub/select [[:select [:select/var-or-exprs [[:var ?s]]]]
                                [:where [:where-sub/where [[:triple/vec [[:var ?s] [:var ?p] [:var ?o]]]]]]
                                [:group-by [[:mod/expr-as-var
                                             [:expr/as-var
                                              [[:expr/branch
                                                [[:expr/op +]
                                                 [:expr/args ([:expr/terminal [:num-lit 2]]
                                                              [:expr/terminal [:num-lit 2]])]]]
                                               [:var ?foo]]]]]]]]
           (s/conform ::ws/where '{:select   [?s]
                                   :where    [[?s ?p ?o]]
                                   :group-by [[(+ 2 2) ?foo]]})))
    (is (= '[:where-sub/where
             [[:where/union
               [[:where-sub/where
                 [[:triple/vec [[:var ?s] [:var ?p] [:var ?o]]]
                  [:triple/vec [[:var ?s] [:var ?p] [:var ?o]]]]]
                [:where-sub/where [[:triple/vec [[:var ?s] [:var ?p] [:var ?o]]]]]]]]]
           (s/conform ::ws/where '[[:union [[?s ?p ?o] [?s ?p ?o]] [[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/optional
               [:where-sub/where [[:triple/vec [[:var ?s] [:var ?p] [:var ?o]]]]]]]]
           (s/conform ::ws/where [[:optional '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/minus
               [:where-sub/where [[:triple/vec [[:var ?s] [:var ?p] [:var ?o]]]]]]]]
           (s/conform ::ws/where [[:minus '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/graph
               [[:prefix-iri :foo/my-graph]
                [:where-sub/where [[:triple/vec [[:var ?s] [:var ?p] [:var ?o]]]]]]]]]
           (s/conform ::ws/where [[:graph :foo/my-graph '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/service
               [[:prefix-iri :foo/my-uri]
                [:where-sub/where [[:triple/vec [[:var ?s] [:var ?p] [:var ?o]]]]]]]]]
           (s/conform ::ws/where [[:service :foo/my-uri '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/service-silent
               [[:prefix-iri :foo/my-uri]
                [:where-sub/where [[:triple/vec [[:var ?s] [:var ?p] [:var ?o]]]]]]]]]
           (s/conform ::ws/where [[:service-silent :foo/my-uri '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/bind
               [:expr/as-var
                [[:expr/branch
                  [[:expr/op +]
                   [:expr/args
                    ([:expr/terminal [:num-lit 2]]
                     [:expr/terminal [:num-lit 2]])]]]
                 [:var ?foo]]]]]]
           (s/conform ::ws/where [[:bind '[(+ 2 2) ?foo]]])))
    (is (= '[:where-sub/where
             [[:where/filter
               [:expr/branch
                [[:expr/op =]
                 [:expr/args
                  ([:expr/terminal [:num-lit 2]]
                   [:expr/terminal [:var ?foo]])]]]]]]
           (s/conform ::ws/where [[:filter '(= 2 ?foo)]])))
    (is (= '[:where-sub/where
             [[:where/values [:values/map [[[:var ?bar] [:var ?qux]]
                                           [[[:num-lit 1] [:num-lit 2]]]]]]]]
           (s/conform ::ws/where [[:values '{?bar [1] ?qux [2]}]])))))

(deftest invalid-where-test
  (testing "invalid WHERE clauses"
    (is (not (s/valid? ::ws/where '[[:optional [[?s ?p]]]])))
    (is (not (s/valid? ::ws/where '[[[[?s ?p]]]])))
    (is (not (s/valid? ::ws/where '[[:where [{:select   [?s]
                                              :where    [[?s ?p ?o]]
                                              :group-by [[(+ 2 2) ?foo]]}
                                             {:select   [?s]
                                              :where    [[?s ?p ?o]]
                                              :group-by [[(+ 2 2) ?foo]]}]]])))))

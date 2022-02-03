(ns com.yetanalytics.flint.spec.where-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.where :as ws]))

(deftest conform-where-test
  (testing "Conform WHERE clause"
    (is (= '[:where-sub/select
             [[:select [:select/var-or-exprs [[:ax/var ?s]]]]
              [:where [:where-sub/where [[:triple/vec [[:ax/var ?s]
                                                       [:ax/var ?p]
                                                       [:ax/var ?o]]]]]]]]
           (s/conform ::ws/where '{:where [[?s ?p ?o]]
                                   :select [?s]})))
    (is (= '[:where-sub/select
             [[:select [:select/var-or-exprs [[:ax/var ?s]]]]
              [:where [:where-sub/where [[:triple/vec [[:ax/var ?s]
                                                       [:ax/var ?p]
                                                       [:ax/var ?o]]]]]]
              [:group-by [[:mod/expr-as-var
                           [:expr/as-var
                            [[:expr/branch
                              [[:expr/op +]
                               [:expr/args ([:expr/terminal [:ax/num-lit 2]]
                                            [:expr/terminal [:ax/num-lit 2]])]]]
                             [:ax/var ?foo]]]]]]]]
           (s/conform ::ws/where '{:select   [?s]
                                   :where    [[?s ?p ?o]]
                                   :group-by [[(+ 2 2) ?foo]]})))
    (is (= '[:where-sub/where
             [[:where/union
               [[:where-sub/where
                 [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]
                  [:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]
                [:where-sub/where [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]]]]]
           (s/conform ::ws/where '[[:union [[?s ?p ?o] [?s ?p ?o]] [[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/optional
               [:where-sub/where [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]]]]
           (s/conform ::ws/where [[:optional '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/minus
               [:where-sub/where [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]]]]
           (s/conform ::ws/where [[:minus '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/graph
               [[:ax/prefix-iri :foo/my-graph]
                [:where-sub/where [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]]]]]
           (s/conform ::ws/where [[:graph :foo/my-graph '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/service
               [[:ax/prefix-iri :foo/my-uri]
                [:where-sub/where [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]]]]]
           (s/conform ::ws/where [[:service :foo/my-uri '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/service-silent
               [[:ax/prefix-iri :foo/my-uri]
                [:where-sub/where [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]]]]]
           (s/conform ::ws/where [[:service-silent :foo/my-uri '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/bind
               [:expr/as-var
                [[:expr/branch
                  [[:expr/op +]
                   [:expr/args
                    ([:expr/terminal [:ax/num-lit 2]]
                     [:expr/terminal [:ax/num-lit 2]])]]]
                 [:ax/var ?foo]]]]]]
           (s/conform ::ws/where [[:bind '[(+ 2 2) ?foo]]])))
    (is (= '[:where-sub/where
             [[:where/filter
               [:expr/branch
                [[:expr/op =]
                 [:expr/args
                  ([:expr/terminal [:ax/num-lit 2]]
                   [:expr/terminal [:ax/var ?foo]])]]]]]]
           (s/conform ::ws/where [[:filter '(= 2 ?foo)]])))
    (is (= '[:where-sub/where
             [[:where/values [:values/map [[[:ax/var ?bar] [:ax/var ?qux]]
                                           [[[:ax/num-lit 1] [:ax/num-lit 2]]]]]]]]
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

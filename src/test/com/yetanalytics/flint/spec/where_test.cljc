(ns com.yetanalytics.flint.spec.where-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.where :as ws]))

(deftest conform-where-test
  (testing "Conforming WHERE clauses"
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
             [[:where/special
               [:where/union
                [[:where-sub/where
                  [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]
                   [:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]
                 [:where-sub/where [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]]]]]]
           (s/conform ::ws/where '[[:union [[?s ?p ?o] [?s ?p ?o]] [[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/special
               [:where/optional
                [:where-sub/where [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]]]]]
           (s/conform ::ws/where [[:optional '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/special
               [:where/minus
                [:where-sub/where [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]]]]]
           (s/conform ::ws/where [[:minus '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/special
               [:where/graph
                [[:ax/prefix-iri :foo/my-graph]
                 [:where-sub/where [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]]]]]]
           (s/conform ::ws/where [[:graph :foo/my-graph '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/special
               [:where/service
                [[:ax/prefix-iri :foo/my-uri]
                 [:where-sub/where [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]]]]]]
           (s/conform ::ws/where [[:service :foo/my-uri '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/special
               [:where/service-silent
                [[:ax/prefix-iri :foo/my-uri]
                 [:where-sub/where [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]]]]]]
           (s/conform ::ws/where [[:service-silent :foo/my-uri '[[?s ?p ?o]]]])))
    (is (= '[:where-sub/where
             [[:where/special
               [:where/bind
                [:expr/as-var
                 [[:expr/branch
                   [[:expr/op +]
                    [:expr/args
                     ([:expr/terminal [:ax/num-lit 2]]
                      [:expr/terminal [:ax/num-lit 2]])]]]
                  [:ax/var ?foo]]]]]]]
           (s/conform ::ws/where [[:bind '[(+ 2 2) ?foo]]])))
    (is (= '[:where-sub/where
             [[:where/special
               [:where/filter
                [:expr/branch
                 [[:expr/op =]
                  [:expr/args
                   ([:expr/terminal [:ax/num-lit 2]]
                    [:expr/terminal [:ax/var ?foo]])]]]]]]]
           (s/conform ::ws/where [[:filter '(= 2 ?foo)]])))
    (is (= '[:where-sub/where
             [[:where/special
               [:where/values
                [:values/map [[[:ax/var ?bar] [:ax/var ?qux]]
                              [[[:ax/num-lit 1] [:ax/num-lit 2]]]]]]]]]
           (s/conform ::ws/where [[:values '{?bar [1] ?qux [2]}]])))
    ;; This is not a special form since it does not conform to the UNION
    ;; spec (or any other special form spec really)
    (is (= '[:where-sub/where
             [[:triple/vec [[:ax/prefix-iri :union]
                            [:ax/prefix-iri :foo/bar]
                            [:ax/prefix-iri :baz/qux]]]]]
           (s/conform ::ws/where '[[:union :foo/bar :baz/qux]])))))

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

(ns syrup.sparql.spec.where-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.where :as ws]))

(deftest where-conform-test
  (testing "Conform WHERE clause"
    (is (= '[:sub-select {:select [:var-or-exprs [[:var ?s]]]
                          :where  [:sub-where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]}]
           (s/conform ::ws/where '{:select [?s] :where [[?s ?p ?o]]})))
    (is (= '[:sub-select {:select [:var-or-exprs [[:var ?s]]]
                          :where  [:sub-where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]
                          :group-by [[:expr-as-var
                                      [[:expr-branch
                                        {:op   +
                                         :args ([:expr-terminal [:num-lit 2]]
                                                [:expr-terminal [:num-lit 2]])}]
                                       [:var ?foo]]]]}]
           (s/conform ::ws/where '{:select [?s]
                                   :where [[?s ?p ?o]]
                                   :group-by [[(+ 2 2) ?foo]]})))
    (is (= '[:sub-where
             [[:union
               [[:sub-where
                 [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]
                  [:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]
                [:sub-where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]]]]]
           (s/conform ::ws/where '[[:union [[?s ?p ?o] [?s ?p ?o]] [[?s ?p ?o]]]])))
    (is (= '[:sub-where
             [[:optional
               [:sub-where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]]]]
           (s/conform ::ws/where [[:optional '[[?s ?p ?o]]]])))
    (is (= '[:sub-where
             [[:minus
               [:sub-where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]]]]
           (s/conform ::ws/where [[:minus '[[?s ?p ?o]]]])))
    (is (= '[:sub-where
             [[:graph
               [[:prefix-iri :foo/my-graph]
                [:sub-where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]]]]]
           (s/conform ::ws/where [[:graph :foo/my-graph '[[?s ?p ?o]]]])))
    (is (= '[:sub-where
             [[:service
               [[:prefix-iri :foo/my-uri]
                [:sub-where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]]]]]
           (s/conform ::ws/where [[:service :foo/my-uri '[[?s ?p ?o]]]])))
    (is (= '[:sub-where
             [[:service-silent
               [[:prefix-iri :foo/my-uri]
                [:sub-where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]]]]]
           (s/conform ::ws/where [[:service-silent :foo/my-uri '[[?s ?p ?o]]]])))
    (is (= '[:sub-where
             [[:bind
               [:expr-as-var
                [[:expr-branch
                  {:op +
                   :args
                   ([:expr-terminal [:num-lit 2]]
                    [:expr-terminal [:num-lit 2]])}]
                 [:var ?foo]]]]]]
           (s/conform ::ws/where [[:bind '[(+ 2 2) ?foo]]])))
    (is (= '[:sub-where
             [[:filter
               [:expr-branch
                {:op =
                 :args
                 ([:expr-terminal [:num-lit 2]]
                  [:expr-terminal [:var ?foo]])}]]]]
           (s/conform ::ws/where [[:filter '(= 2 ?foo)]])))
    (is (= '[:sub-where
             [[:values [:values-map {[?bar ?qux] [[1 2]]}]]]]
           (s/conform ::ws/where [[:values '{?bar [1] ?qux [2]}]])))))

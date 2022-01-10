(ns syrup.sparql.spec.where-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.expr :as es]
            [syrup.sparql.spec.where :as ws]))

(deftest where-conform-test
  (testing "Conform WHERE clause"
    (is (= '[:select {:select [:var-or-exprs [[:var ?s]]]
                      :where  [:where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]}]
           (s/conform ::ws/where '{:select [?s] :where [[?s ?p ?o]]})))
    (is (= '[:select {:select [:var-or-exprs [[:var ?s]]]
                      :where  [:where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]
                      :group-by [[:expr-as-var
                                  {:expr [::es/branch
                                          {:op   +
                                           :args ([::es/terminal [:num-lit 2]]
                                                  [::es/terminal [:num-lit 2]])}]
                                   :var  ?foo}]]}]
           (s/conform ::ws/where '{:select [?s]
                                   :where [[?s ?p ?o]]
                                   :group-by [[(+ 2 2) ?foo]]})))
    (is (= '[:where
             [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]
              [:nform [:spo [[[:var ?s] [:po [[[:var ?p] [:o [[:var ?o]]]]]]]]]]
              [:recurse
               [:where
                [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]
                 [:nform
                  [:spo [[[:var ?s] [:po [[[:var ?p] [:o [[:var ?o]]]]]]]]]]]]]
              [:recurse
               [:select
                {:select [:var-or-exprs [[:var ?s]]]
                 :where [:where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]}]]
              [:union
               {:k :union
                :v
                [[:where
                  [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]
                   [:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]
                 [:where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]]}]
              [:optional
               {:k :optional
                :v [:where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]}]
              [:minus
               {:k :minus, :v [:where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]}]
              [:graph
               {:k :graph
                :v1 [:iri :foo/my-graph]
                :v2 [:where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]}]
              [:service
               {:k :service
                :v1 [:iri :foo/my-url]
                :v2 [:where [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]}]
              [:bind
               {:k :bind
                :v {:expr [::es/branch {:op +
                                        :args
                                        ([::es/terminal [:num-lit 2]]
                                         [::es/terminal [:num-lit 2]])}]
                    :var ?foo}}]
              [:values {:k :values, :v [:clojure {?bar [1], ?qux [2]}]}]]]
           (s/conform ::ws/where
                      '[[?s ?p ?o]
                        {?s {?p #{?o}}}
                        [[?s ?p ?o]
                         {?s {?p #{?o}}}]
                        {:select [?s] :where [[?s ?p ?o]]}
                        [:union [[?s ?p ?o] [?s ?p ?o]] [[?s ?p ?o]]]
                        [:optional [[?s ?p ?o]]]
                        [:minus [[?s ?p ?o]]]
                        [:graph :foo/my-graph [[?s ?p ?o]]]
                        [:service :foo/my-url [[?s ?p ?o]]]
                        [:bind [(+ 2 2) ?foo]]
                        [:values {?bar [1] ?qux [2]}]])))))

(ns com.yetanalytics.flint.validate.scope-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.validate       :as v]
            [com.yetanalytics.flint.validate.scope :as vs]
            [com.yetanalytics.flint.spec.query     :as qs]))

(deftest get-scope-vars-test
  (testing "Searching for in-scope variables"
    (is (nil? (not-empty
               (vs/get-scope-vars
                '[:ax/num-lit 1]))))
    (is (= '[?x]
           (vs/get-scope-vars
            '[:ax/var ?x])))
    (is (= '[?z]
           (vs/get-scope-vars
            '[:expr/as-var
              [[:expr/terminal [:ax/num-lit 2]]
               [:ax/var ?z]]])))
    (is (= '[]
           (vs/get-scope-vars
            '[:where-sub/empty []])))
    (testing "in basic graph patterns"
      (is (= '[?x ?y ?z]
             (vs/get-scope-vars
              '[:triple/vec [[:ax/var ?x]
                             [:ax/var ?y]
                             [:ax/var ?z]]])))
      (is (= '[?s ?p ?o]
             (vs/get-scope-vars
              '[:triple/nform
                [:triple/spo
                 [[[:ax/var ?s]
                   [:triple/po [[[:ax/var ?p]
                                 [:triple/o [[:ax/var ?o]]]]]]]]]])))
      (is (= '[?x ?y ?z]
             (vs/get-scope-vars
              '[:where-sub/where
                [[:triple/vec [[:ax/var ?x]
                               [:ax/var ?y]
                               [:ax/var ?z]]]]])))
      (is (= '[?x ?y ?z]
             (vs/get-scope-vars
              '[:where-sub/where
                [[:triple/vec [[:ax/var ?x]
                               [:ax/var ?y]
                               [:ax/var ?z]]]]])))
      (is (= '[?x ?ya ?yb ?z]
             (vs/get-scope-vars
              '[:triple/vec
                [[:ax/var ?x]
                 [:triple/path
                  [:path/branch [[:path/op cat]
                                 [:path/paths
                                  [[:path/terminal [:ax/var ?ya]]
                                   [:path/terminal [:ax/var ?yb]]]]]]]
                 [:ax/var ?z]]]))))
    (testing "in sub-SELECT queries"
      (is (= '[?x ?y ?z]
             (vs/get-scope-vars
              '[:where-sub/select
                [[:select [:ax/wildcard '*]]
                 [:where [:where-sub/where
                          [[:triple/vec [[:ax/var ?x]
                                         [:ax/var ?y]
                                         [:ax/var ?z]]]]]]]])))
      (is (= '[?a ?b ?c]
             (vs/get-scope-vars
              '[:where-sub/select
                [[:select [:select/var-or-exprs
                           [[:ax/var ?a]
                            [:ax/var ?b]
                            [:select/expr-as-var
                             [:expr/as-var [[:expr/terminal [:ax/num-lit 2]]
                                            [:ax/var ?c]]]]]]]
                 [:where [:where-sub/where
                          [[:triple/vec [[:ax/var ?x]
                                         [:ax/var ?y]
                                         [:ax/var ?z]]]]]]]]))))
    (is (= '[?s1 ?p1 ?o1
             ?s2 ?p2 ?o2
             ?s3 ?p3 ?o3
             ?s4 ?p4 ?o4
             ?graphTerm ?s5 ?p5 ?o5
             ?serviceTerm ?s6 ?p6 ?o6
             ?serviceSilentTerm ?s7 ?p7 ?o7
             ?foo
             ?v1 ?v2]
           (vs/get-scope-vars
            '[:where-sub/where
              [[:where/recurse
                [:where-sub/where
                 [[:triple/vec [[:ax/var ?s1] [:ax/var ?p1] [:ax/var ?o1]]]]]]
               [:where/union
                [[:where-sub/where
                  [[:triple/vec
                    [[:ax/var ?s2] [:ax/var ?p2] [:ax/var ?o2]]]]]
                 [:where-sub/where
                  [[:triple/vec
                    [[:ax/var ?s3] [:ax/var ?p3] [:ax/var ?o3]]]]]]]
               [:where/optional
                [:where-sub/where
                 [[:triple/vec
                   [[:ax/var ?s4] [:ax/var ?p4] [:ax/var ?o4]]]]]]
               [:where/graph
                [[:ax/var ?graphTerm]
                 [:where-sub/where
                  [[:triple/vec
                    [[:ax/var ?s5] [:ax/var ?p5] [:ax/var ?o5]]]]]]]
               [:where/service
                [[:ax/var ?serviceTerm]
                 [:where-sub/where
                  [[:triple/vec
                    [[:ax/var ?s6] [:ax/var ?p6] [:ax/var ?o6]]]]]]]
               [:where/service-silent
                [[:ax/var ?serviceSilentTerm]
                 [:where-sub/where
                  [[:triple/vec
                    [[:ax/var ?s7] [:ax/var ?p7] [:ax/var ?o7]]]]]]]
               [:where/bind
                [:expr/as-var
                 [[:expr/branch
                   [[:expr/op +]
                    [:expr/args
                     ([:expr/terminal [:ax/num-lit 2]]
                      [:expr/terminal [:ax/num-lit 2]])]]]
                  [:ax/var ?foo]]]]
               [:where/values
                [:values/map [[[:ax/var ?v1] [:ax/var ?v2]]
                              [[[:ax/num-lit 1] [:ax/num-lit 2]]]]]]
               [:where/filter
                [:expr/branch
                 [[:expr/op =]
                  [:expr/args
                   ([:expr/terminal [:ax/num-lit 2]]
                    [:expr/terminal [:ax/var ?bar]])]]]]
               [:where/minus
                [:where-sub/where
                 [[:triple/vec
                   [[:ax/var ?s5] [:ax/var ?p5] [:ax/var ?o5]]]]]]]])))))


(deftest scope-validation-test
  (testing "Bind var scope validation"
    (is (nil? (->> '{:select [?x]
                     :where  [[?x ?y ?z]]}
                   (s/conform qs/query-spec)
                   v/collect-nodes
                   vs/validate-scoped-vars)))
    (is (nil? (->> '{:select [?x [2 ?new]]
                     :where  [[?x ?y ?z]]}
                   (s/conform qs/query-spec)
                   v/collect-nodes
                   vs/validate-scoped-vars)))
    (is (nil? (->> '{:select [?x]
                     :where  [[?x ?y ?z]
                              [:bind [3 ?new]]]}
                   (s/conform qs/query-spec)
                   v/collect-nodes
                   vs/validate-scoped-vars)))
    (is (= [{:variable   '?x
             :scope-vars #{'?x '?y '?z}
             :path       [:query/select :select :select/var-or-exprs :select/expr-as-var]}]
           (->> '{:select [[2 ?x]]
                  :where [[?x ?y ?z]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vs/validate-scoped-vars)))
    (is (= [{:variable   '?z
             :scope-vars #{'?x '?y '?z}
             :path       [:query/select :select :select/var-or-exprs :select/expr-as-var]}]
           (->> '{:select [[2 ?z]]
                  :where [{?x {?y #{?z}}}]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vs/validate-scoped-vars)))
    (is (= [{:variable   '?x
             :scope-vars #{'?x '?y '?z '?w}
             :path       [:query/select :select :select/var-or-exprs :select/expr-as-var]}]
           (->> '{:select [?w [2 ?x]]
                  :where [[?x ?y ?z]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vs/validate-scoped-vars)))
    (is (= [{:variable   '?y
             :scope-vars #{'?x '?y '?z}
             :path       [:query/select :where :where-sub/where :where/bind]}]
           (->> '{:select [?x]
                  :where [[?x ?y ?z]
                          [:bind [3 ?y]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vs/validate-scoped-vars)))
    (is (= #{{:variable   '?x
              :scope-vars #{'?x '?y '?z '?w}
              :path       [:query/select :select :select/var-or-exprs :select/expr-as-var]}
             {:variable   '?y
              :scope-vars #{'?x '?y '?z}
              :path       [:query/select :where :where-sub/where :where/bind]}}
           (->> '{:select [?w [2 ?x]]
                  :where [[?x ?y ?z]
                          [:bind [3 ?y]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vs/validate-scoped-vars
                set)))))

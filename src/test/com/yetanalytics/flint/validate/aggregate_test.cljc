(ns com.yetanalytics.flint.validate.aggregate-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.validate           :as v]
            [com.yetanalytics.flint.validate.aggregate :as va]
            [com.yetanalytics.flint.spec.query         :as qs]))

(deftest helper-test
  (testing "helper multimethods"
    (testing "to find GROUP BY projected vars"
      (is (= []
             (va/group-by-projected-vars
              '[:group-by
                [[:mod/expr [:expr/terminal [:ax/var ?x]]]]])))
      (is (= '[?x ?y ?z]
             (va/group-by-projected-vars
              '[:group-by
                [[:ax/var ?x] [:ax/var ?y] [:ax/var ?z]]])))
      (is (= '[?y]
             (va/group-by-projected-vars
              '[:group-by
                [[:mod/expr-as-var
                  [:expr/as-var [[:expr/terminal [:ax/var ?x]]
                                 [:ax/var ?y]]]]]]))))
    (testing "to find invalid vars in aggregate SELECT exprs"
      (is (= '[?x]
             (va/invalid-agg-expr-vars
              #{}
              '[:expr/terminal [:ax/var ?x]])))
      (is (= []
             (va/invalid-agg-expr-vars
              #{'?x}
              '[:expr/terminal [:ax/var ?x]])))
      (is (= ['?x]
             (va/invalid-agg-expr-vars
              #{}
              '[:expr/branch [[:expr/op str]
                              [:expr/args [[:expr/terminal [:ax/var ?x]]]]]])))
      (is (= []
             (va/invalid-agg-expr-vars
              #{}
              '[:expr/branch [[:expr/op count]
                              [:expr/args [[:expr/terminal [:ax/var ?x]]]]]])))
      (is (= []
             (va/invalid-agg-expr-vars
              #{'?x}
              '[:expr/branch [[:expr/op str]
                              [:expr/args [[:expr/terminal [:ax/var ?x]]]]]]))))))

(deftest aggregates-validation-test
  (testing "Validating SELECTs with aggregates"
    (is (= []
           (->> '{:select [[2 ?sum]]
                  :where  [[?x ?y ?z]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects)))
    (is (= []
           (->> '{:select [[(str ?z) ?str]]
                  :where  [[?x ?y ?z]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects)))
    (is (= []
           (->> '{:select [[(sum ?x) ?sum]]
                  :where  [[?x ?y ?z]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects)))
    (is (= []
           (->> '{:select [[(sum ?x :distinct? true) ?sum]]
                  :where  [[?x ?y ?z]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects)))
    ;; All custom fns in SELECT queries are treated as custom aggregates
    (is (= []
           (->> '{:select [[("<http://custom.agg>" ?x) ?sum]]
                  :where  [[?x ?y ?z]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects)))
    (is (= []
           (->> '{:select   [?x]
                  :where    [[?x ?y ?z]]
                  :group-by [?x]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects)))
    (is (= []
           (->> '{:select   [[(sum ?x) ?x2] [(str ?x2) ?x3]]
                  :where    [[?x ?y ?z]]
                  :group-by [?x]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects)))
    (is (= []
           (->> '{:select   [?x]
                  :where    [{:select [[(sum ?y) ?sum]]
                              :where [?x ?y ?z]}]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects)))
    (is (= [{:kind ::va/invalid-aggregate-var
             :variables ['?z]}]
           (->> '{:select [[(sum ?x) ?sum] [(str ?z) ?str]]
                  :where  [[?x ?y ?z]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects
                (map #(dissoc % :loc)))))
    (is (= [{:kind ::va/invalid-aggregate-var
             :variables ['?z]}]
           (->> '{:select [[("<http://custom.agg>" ?x) ?sum] [(str ?z) ?str]]
                  :where  [[?x ?y ?z]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects
                (map #(dissoc % :loc)))))
    (is (= [{:kind ::va/invalid-aggregate-var
             :variables ['?z]}]
           (->> '{:select   [?z]
                  :where    [[?x ?y ?z]]
                  :group-by [?x [2 ?y]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects
                (map #(dissoc % :loc)))))
    (is (= [{:kind ::va/invalid-aggregate-var
             :variables ['?y '?z]}]
           (->> '{:select   [?y ?z]
                  :where    [[?x ?y ?z]]
                  :group-by [?x]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects
                (map #(dissoc % :loc)))))
    (is (= [{:kind ::va/invalid-aggregate-var
             :variables ['?z]}]
           (->> '{:select [?x]
                  :where  {:select   [?z]
                           :where    [[?x ?y ?z]]
                           :group-by [?x [2 ?y]]}}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects
                (map #(dissoc % :loc)))))
    (is (= [{:kind ::va/wildcard-group-by}]
           (->> '{:select   :*
                  :where    [[?x ?y ?z]]
                  :group-by [?x]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects
                (map #(dissoc % :loc)))))
    (is (= [{:kind ::va/wildcard-group-by}]
           (->> '{:select :*
                  :where  {:select   :*
                           :where    [[?x ?y ?z]]
                           :group-by [?x]}}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects
                (map #(dissoc % :loc)))))))

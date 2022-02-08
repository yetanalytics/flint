(ns com.yetanalytics.flint.bnode-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.bnode :as bnode]
            [com.yetanalytics.flint.spec.query :as qs]))

(deftest bnode-test
  (testing "valid blank nodes"
    (is (= []
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]]}
                (s/conform qs/query-spec)
                bnode/validate-bnodes
                :errors)))
    (is (= []
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]]}
                (s/conform qs/query-spec)
                bnode/validate-bnodes
                :errors)))
    (is (= []
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]
                           [?x :foo/bar _1]]}
                (s/conform qs/query-spec)
                bnode/validate-bnodes
                :errors)))
    (is (= []
           (->> '{:select [?x]
                  :where  [{?x {:foo/bar #{_1}
                                :baz/qux #{_1}}}
                           {?y {:fii/fie #{_1}
                                :foe/fum #{_1}}}]}
                (s/conform qs/query-spec)
                bnode/validate-bnodes
                :errors)))
    (is (= []
           (->> '{:select [?x]
                  :where  [[:where [[?x :foo/bar _1]]]
                           [:where [[?y :baz/qux _2]]]]}
                (s/conform qs/query-spec)
                bnode/validate-bnodes
                :errors)))
    (is (= []
           (->> '{:select [?x]
                  :where  [[:where [[?x :foo/bar _]]]
                           [:where [[?y :baz/qux _]]]]}
                (s/conform qs/query-spec)
                bnode/validate-bnodes
                :errors))))
  (testing "invalid blank nodes"
    (is (= [{:bnode '_1, :bgp-count 2}]
           (->> '{:select [?x]
                  :where  [[:where [[?x :foo/bar _1]]]
                           [:where [[?y :baz/qux _1]]]]}
                (s/conform qs/query-spec)
                bnode/validate-bnodes
                :errors)))
    (is (= [{:bnode '_1, :bgp-count 2}]
           (->> '{:select [?x]
                  :where  [[:where [{?x {:foo/bar #{_1}
                                         :baz/qux #{_1}}}]]
                           [:where [{?y {:fii/fie #{_1}
                                         :foe/fum #{_1}}}]]]}
                (s/conform qs/query-spec)
                bnode/validate-bnodes
                :errors)))
    (is (= [{:bnode '_1, :bgp-count 2}]
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]
                           [:optional [[?y :baz/qux _1]]]]}
                (s/conform qs/query-spec)
                bnode/validate-bnodes
                :errors)))
    (is (= [{:bnode '_1, :bgp-count 2}]
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]
                           [?y :baz/qux _1]
                           [:optional [[?z :far/lands _1]]]]}
                (s/conform qs/query-spec)
                bnode/validate-bnodes
                :errors)))
    (is (= [{:bnode '_1, :bgp-count 3}]
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]
                           [:optional [[?z :far/lands _1]]]
                           [?y :baz/qux _1]]}
                (s/conform qs/query-spec)
                bnode/validate-bnodes
                :errors)))
    (is (= [{:bnode '_1, :bgp-count 2}]
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]
                           [:filter (not-exists [[?z :far/lands _1]])]
                           [?y :baz/qux _1]]}
                (s/conform qs/query-spec)
                bnode/validate-bnodes
                :errors)))))

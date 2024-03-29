(ns com.yetanalytics.flint.validate.bnode-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.validate       :as v]
            [com.yetanalytics.flint.validate.bnode :as vb]
            [com.yetanalytics.flint.spec.query     :as qs]
            [com.yetanalytics.flint.spec.update    :as us]))

(deftest bnode-test
  (testing "valid blank nodes"
    (is (= [#{'_1} nil]
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{'_1} nil]
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{'_1} nil]
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]
                           [?x :foo/bar _1]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{'_1} nil]
           (->> '{:select [?x]
                  :where  [{?x {:foo/bar #{_1}
                                :baz/qux #{_1}}}
                           {?y {:fii/fie #{_1}
                                :foe/fum #{_1}}}]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{'_1 '_2} nil]
           (->> '{:select [?x]
                  :where  [[:where [[?x :foo/bar _1]]]
                           [:where [[?y :baz/qux _2]]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{} nil]
           (->> '{:select [?x]
                  :where  [[:where [[?x :foo/bar _]]]
                           [:where [[?y :baz/qux _]]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{} nil]
           (->> '{:select [?p]
                  :where  [[?x ?y] ?p (?z ?w)]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes))))
  (testing "invalid blank nodes"
    (is (= [#{'_1} {:kind   ::vb/dupe-bnodes-bgp
                    :errors [{:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.vec/spo]}
                             {:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.vec/spo]}]}]
           (->> '{:select [?x]
                  :where  [[:where [[?x :foo/bar _1]]]
                           [:where [[?y :baz/qux _1]]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{'_1} {:kind ::vb/dupe-bnodes-bgp
                    :errors [{:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.nform/spo :triple.nform/po :triple.nform/o]}
                             {:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.nform/spo :triple.nform/po :triple.nform/o]}
                             {:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.nform/spo :triple.nform/po :triple.nform/o]}
                             {:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.nform/spo :triple.nform/po :triple.nform/o]}]}]
           (->> '{:select [?x]
                  :where  [[:where [{?x {:foo/bar #{_1}
                                         :baz/qux #{_1}}}]]
                           [:where [{?y {:fii/fie #{_1}
                                         :foe/fum #{_1}}}]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{'_1 '_2} {:kind ::vb/dupe-bnodes-bgp
                        :errors [{:bnode '_2
                                  :path [:query/select :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.nform/spo]}
                                 {:bnode '_2
                                  :path [:query/select :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.nform/spo]}
                                 {:bnode '_1
                                  :path [:query/select :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.nform/spo :triple.nform/po :triple.nform/o]}
                                 {:bnode '_1
                                  :path [:query/select :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.nform/spo :triple.nform/po :triple.nform/o]}
                                 {:bnode '_1
                                  :path [:query/select :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.nform/spo :triple.nform/po :triple.nform/o]}
                                 {:bnode '_1
                                  :path [:query/select :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.nform/spo :triple.nform/po :triple.nform/o]}]}]
           (->> '{:select [?x]
                  :where  [[:where [{_2 {:foo/bar #{_1}
                                         :baz/qux #{_1}}}]]
                           [:where [{_2 {:fii/fie #{_1}
                                         :foe/fum #{_1}}}]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{'_1} {:kind ::vb/dupe-bnodes-bgp
                    :errors [{:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/triple :triple.vec/spo]}
                             {:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/special :where/optional :where-sub/where :where/triple :triple.vec/spo]}]}]
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]
                           [:optional [[?y :baz/qux _1]]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{'_1} {:kind ::vb/dupe-bnodes-bgp
                    :errors [{:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/triple :triple.vec/spo]}
                             {:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/triple :triple.vec/spo]}
                             {:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/special :where/optional :where-sub/where :where/triple :triple.vec/spo]}]}]
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]
                           [?y :baz/qux _1]
                           [:optional [[?z :far/lands _1]]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{'_1} {:kind ::vb/dupe-bnodes-bgp
                    :errors [{:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/triple :triple.vec/spo]}
                             {:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/special :where/optional :where-sub/where :where/triple :triple.vec/spo]}
                             {:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/triple :triple.vec/spo]}]}]
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]
                           [:optional [[?z :far/lands _1]]]
                           [?y :baz/qux _1]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{'_1 '_2} {:kind ::vb/dupe-bnodes-bgp
                        :errors [{:bnode '_2
                                  :path  [:query/select :where :where-sub/where :where/special :where/optional :where-sub/where :where/triple :triple.vec/spo]}
                                 {:bnode '_2
                                  :path  [:query/select :where :where-sub/where :where/triple :triple.vec/spo]}]}]
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]
                           [:optional [[?z :far/lands _2]]]
                           [?y :baz/qux _2]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{'_1} {:kind ::vb/dupe-bnodes-bgp
                    :errors [{:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/triple :triple.vec/spo]}
                             {:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/triple :triple.vec/spo]}
                             {:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/special :where/filter :expr/branch :expr/args :where-sub/where :where/triple :triple.vec/spo]}]}]
           (->> '{:select [?x]
                  :where  [[?x :foo/bar _1]
                           [:filter (not-exists [[?z :far/lands _1]])]
                           [?y :baz/qux _1]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{'_1} {:kind   ::vb/dupe-bnodes-update
                    :errors [{:bnode '_1
                              :path  [:query/select :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.vec/spo]}]
                    :prev-bnodes #{'_1}}]
           (->> '{:select [?x]
                  :where  [[:where [[?x :foo/bar _1]]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                (vb/validate-bnodes #{'_1}))))
    (is (= [[#{'_1} {:kind   ::vb/dupe-bnodes-bgp
                     :errors [{:bnode '_1
                               :path  [:update/modify :insert :triple.vec/spo]}
                              {:bnode '_1
                               :path  [:update/modify :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.vec/spo]}]}]]
           (->> '[{:insert [[?x :foo/bar _1]]
                   :where  [[:where [[?x :foo/bar _1]]]]}]
                (map (partial s/conform us/update-spec))
                (map v/collect-nodes)
                (map vb/validate-bnodes))))
    (is (= [#{'_1 '_2} nil]
           (->> '[{:insert [[?x :foo/bar _1]]
                   :where  [[:where [[?x :foo/bar _2]]]]}
                  {:insert [[?x :foo/bar _1]]
                   :where  [[:where [[?x :foo/bar _2]]]]}]
                first
                (s/conform us/update-spec)
                v/collect-nodes
                vb/validate-bnodes)))
    (is (= [#{'_1 '_2 '_3}
            {:kind
             ::vb/dupe-bnodes-update
             :errors
             [{:bnode '_1
               :path  [:update/modify :insert :triple.vec/spo]}
              {:bnode '_2
               :path  [:update/modify :where :where-sub/where :where/special :where/recurse :where-sub/where :where/triple :triple.vec/spo]}]
             :prev-bnodes
             #{'_1 '_2}}]
           (->> '[{:insert [[?x :foo/bar _1]]
                   :where  [[:where [[?x :foo/bar _2]]]]}
                  {:insert [[?x :foo/bar _1]]
                   :where  [[:where [[?x :foo/bar _2]
                                     [?y :baz/qux _3]]]]}]
                second
                (s/conform us/update-spec)
                v/collect-nodes
                (vb/validate-bnodes #{'_1 '_2}))))))

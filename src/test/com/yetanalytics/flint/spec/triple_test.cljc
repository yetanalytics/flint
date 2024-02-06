(ns com.yetanalytics.flint.spec.triple-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.triple :as ts]))

(deftest conform-triple-test
  (testing "Conforming triples"
    (is (= '[:triple.nform/spo
             [[[:ax/var ?s]
               [:triple.nform/po
                [[[:ax/var ?p]
                  [:triple.nform/o [[:ax/var ?o]]]]]]]]]
           (s/conform ts/triple-spec '{?s {?p #{?o}}})))
    (is (= '[:triple.nform/spo
             [[[:ax/var ?s1]
               [:triple.nform/po
                [[[:ax/var ?p1]
                  [:triple.nform/o [[:ax/var ?o1]
                                    [:ax/var ?o2]]]]
                 [[:ax/var ?p2]
                  [:triple.nform/o [[:ax/var ?o1]
                                    [:ax/var ?o2]]]]]]]
              [[:ax/var ?s2]
               [:triple.nform/po
                [[[:ax/var ?p1]
                  [:triple.nform/o [[:ax/var ?o1]
                                    [:ax/var ?o2]]]]
                 [[:ax/var ?p2]
                  [:triple.nform/o [[:ax/var ?o1]
                                    [:ax/var ?o2]]]]]]]]]
           (s/conform ts/triple-spec '{?s1 {?p1 #{?o1 ?o2}
                                            ?p2 #{?o1 ?o2}}
                                       ?s2 {?p1 #{?o1 ?o2}
                                            ?p2 #{?o1 ?o2}}})))
    (is (= '[:triple.vec/spo [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]
           (s/conform ts/triple-spec '[?s ?p ?o])))
    (testing "with and without paths"
      (is (= '[:triple.nform/spo
               [[[:ax/var ?s]
                 [:triple.nform/po
                  [[[:triple/path
                     [:path/branch [[:path/op cat]
                                    [:path/paths
                                     [[:path/terminal [:ax/prefix-iri :x/one]]
                                      [:path/terminal [:ax/prefix-iri :x/two]]]]]]]
                    [:triple.nform/o [[:ax/var ?o]]]]]]]]]
             (s/conform ts/triple-spec
                        '{?s {(cat :x/one :x/two) #{?o}}})))
      (is (= '[:triple.vec/spo
               [[:ax/var ?s]
                [:triple/path
                 [:path/branch [[:path/op cat]
                                [:path/paths
                                 [[:path/terminal [:ax/prefix-iri :x/one]]
                                  [:path/terminal [:ax/prefix-iri :x/two]]]]]]]
                [:ax/var ?o]]]
             (s/conform ts/triple-spec
                        '[?s (cat :x/one :x/two) ?o])))
      (is (not (s/valid? ts/triple-nopath-spec
                         '{?s {(cat :x/one :x/two) #{?o}}})))
      (is (->> '{?s {(cat :x/one :x/two) #{?o}}}
               (s/explain-data ts/triple-nopath-spec)
               ::s/problems
               (filter #(-> % :path first (= :triple.nform/spo)))
               (map :val)
               (every? (partial = '(cat :x/one :x/two))))))
    (testing "with list and blank nodes"
      (is (= '[:triple.vec/spo
               [[:triple/list [[:ax/literal 1]
                               [:ax/var ?x]
                               [:ax/literal 3]
                               [:ax/literal 4]]]
                [:ax/prefix-iri :p]
                [:ax/literal "w"]]]
             (s/conform ts/triple-spec
                        '[(1 ?x 3 4) :p "w"])))
      (is (= '[:triple.vec/spo
               [[:ax/var ?s]
                [:ax/var ?p]
                [:triple/list [[:ax/literal 100] [:ax/literal 200] [:ax/literal 300]]]]]
             (s/conform ts/triple-spec
                        '[?s ?p (100 200 300)])))
      (is (= '[:triple.nform/spo
               [[[:triple/list [[:ax/literal 1]
                                [:ax/var ?x]
                                [:ax/literal 3]
                                [:ax/literal 4]]]
                 [:triple.nform/po
                  [[[:ax/prefix-iri :p]
                    [:triple.nform/o [[:ax/literal "w"]]]]]]]]]
             (s/conform ts/triple-spec
                        '{(1 ?x 3 4) {:p #{"w"}}})))
      (is (= '[:triple.nform/spo
               [[[:ax/var ?x]
                 [:triple.nform/po
                  [[[:ax/prefix-iri :p]
                    [:triple.nform/o
                     [[:triple/list [[:ax/literal 100]
                                     [:ax/literal 200]
                                     [:ax/literal 300]]]]]]]]]]]
             (s/conform ts/triple-spec
                        '{?x {:p #{(100 200 300)}}})))
      (is (= '[:triple.vec/s
               [[:triple/list [[:ax/literal 1]
                               [:ax/var ?x]
                               [:triple/list [[:ax/literal 2]]]]]]]
             (s/conform ts/triple-spec
                        '[(1 ?x (2))])))
      (is (= '[:triple.nform/s
               [[[:triple/list [[:ax/literal 1]
                                [:ax/var ?x]
                                [:triple/list [[:ax/literal 2]]]]]
                 []]]]
             (s/conform ts/triple-spec
                        '{(1 ?x (2)) {}})))
      (is (= '[:triple.vec/s
               [[:triple/list [[:ax/literal 1]
                               [:triple/bnodes [[[:ax/prefix-iri :p]
                                                 [:ax/prefix-iri :q]]]]
                               [:triple/list [[:ax/literal 2]]]]]]]
             (s/conform ts/triple-spec
                        '[(1 [:p :q] (2))])))
      (is (= '[:triple.vec/spo
               [[:triple/bnodes [[[:ax/prefix-iri :p]
                                  [:ax/literal "v"]]]]
                [:ax/prefix-iri :q]
                [:ax/literal "w"]]]
             (s/conform ts/triple-spec
                        '[[:p "v"] :q "w"])))
      (is (= '[:triple.vec/spo
               [[:ax/prefix-iri :x]
                [:ax/prefix-iri :q]
                [:triple/bnodes [[[:ax/prefix-iri :p]
                                  [:ax/literal "v"]]]]]]
             (s/conform ts/triple-spec
                        '[:x :q [:p "v"]])))
      (is (= '[:triple.nform/spo
               [[[:triple/bnodes
                  [[[:ax/prefix-iri :p]
                    [:ax/literal "v"]]]]
                 [:triple.nform/po
                  [[[:ax/prefix-iri :q]
                    [:triple.nform/o [[:ax/literal "w"]]]]]]]]]
             (s/conform ts/triple-spec
                        '{[:p "v"] {:q #{"w"}}})))
      (is (= '[:triple.nform/spo
               [[[:ax/prefix-iri :x]
                 [:triple.nform/po
                  [[[:ax/prefix-iri :q]
                    [:triple.nform/o
                     [[:triple/bnodes [[[:ax/prefix-iri :p]
                                        [:ax/literal "v"]]]]]]]]]]]]
             (s/conform ts/triple-spec
                        '{:x {:q #{[:p "v"]}}})))
      (is (= '[:triple.vec/spo
               [[:triple/bnodes []]
                [:ax/prefix-iri :q]
                [:ax/literal "w"]]]
             (s/conform ts/triple-spec
                        '[[] :q "w"])))
      (is (= '[:triple.vec/spo
               [[:triple/bnodes
                 [[[:ax/prefix-iri :p0]
                   [:triple/bnodes
                    [[[:ax/prefix-iri :p1]
                      [:triple/bnodes [[[:ax/prefix-iri :p2]
                                        [:ax/literal "v"]]]]]]]]]]
                [:ax/prefix-iri :q]
                [:ax/literal "w"]]]
             (s/conform ts/triple-spec
                        '[[:p0 [:p1 [:p2 "v"]]] :q "w"])))
      (is (= '[:triple.vec/spo
               [[:triple/bnodes
                 [[[:ax/prefix-iri :p1]
                   [:ax/var ?x1]]
                  [[:ax/prefix-iri :p2]
                   [:ax/var ?x2]]]]
                [:ax/prefix-iri :q]
                [:ax/literal "w"]]]
             (s/conform ts/triple-spec
                        '[[:p1 ?x1 :p2 ?x2] :q "w"]))))))

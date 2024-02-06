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
    (is (= '[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]
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
      (is (= '[:triple/vec
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
               (every? (partial = '(cat :x/one :x/two))))))))

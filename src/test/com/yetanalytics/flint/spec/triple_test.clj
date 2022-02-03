(ns com.yetanalytics.flint.spec.triple-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.triple :as ts]))

(deftest foo
  (testing "Triples"
    (is (= '[:triple/spo [[[:ax/var ?s]
                    [:triple/po [[[:ax/var ?p]
                           [:triple/o [[:ax/var ?o]]]]]]]]]
           (s/conform ts/normal-form-spec '{?s {?p #{?o}}})))
    (is (= '[:triple/spo [[[:ax/var ?s1]
                    [:triple/po [[[:ax/var ?p1]
                           [:triple/o [[:ax/var ?o1] [:ax/var ?o2]]]]
                          [[:ax/var ?p2]
                           [:triple/o [[:ax/var ?o1] [:ax/var ?o2]]]]]]]
                   [[:ax/var ?s2]
                    [:triple/po [[[:ax/var ?p1]
                           [:triple/o [[:ax/var ?o1] [:ax/var ?o2]]]]
                          [[:ax/var ?p2]
                           [:triple/o [[:ax/var ?o1] [:ax/var ?o2]]]]]]]]]
           (s/conform ts/normal-form-spec '{?s1 {?p1 #{?o1 ?o2}
                                                 ?p2 #{?o1 ?o2}}
                                            ?s2 {?p1 #{?o1 ?o2}
                                                 ?p2 #{?o1 ?o2}}})))
    (is (= '[[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]
           (s/conform ts/triple-vec-spec '[?s ?p ?o])))
    (testing "with and without paths"
      (is (= '[:triple/spo
               [[[:ax/var ?s]
                 [:triple/po
                  [[[:triple/path
                     [:path/branch [[:path/op cat]
                                    [:path/paths
                                     [[:path/terminal [:ax/prefix-iri :x/one]]
                                      [:path/terminal [:ax/prefix-iri :x/two]]]]]]]
                    [:triple/o [[:ax/var ?o]]]]]]]]]
             (s/conform ts/normal-form-spec
                        '{?s {(cat :x/one :x/two) #{?o}}})))
      (is (= '[[:ax/var ?s]
               [:triple/path
                [:path/branch [[:path/op cat]
                               [:path/paths
                                [[:path/terminal [:ax/prefix-iri :x/one]]
                                 [:path/terminal [:ax/prefix-iri :x/two]]]]]]]
               [:ax/var ?o]]
             (s/conform ts/triple-vec-spec
                        '[?s (cat :x/one :x/two) ?o])))
      (is (not (s/valid? ts/normal-form-nopath-spec
                         '{?s {(cat :x/one :x/two) #{?o}}})))
      (is (->> '{?s {(cat :x/one :x/two) #{?o}}}
               (s/explain-data ts/normal-form-nopath-spec)
               ::s/problems
               (map :val)
               (every? (partial = '(cat :x/one :x/two))))))))

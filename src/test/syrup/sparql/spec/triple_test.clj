(ns syrup.sparql.spec.triple-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.path :as ps]
            [syrup.sparql.spec.triple :as ts]))

(deftest foo
  (testing "Triples"
    (is (= '[:spo [[[:var ?s]
                    [:po [[[:var ?p]
                           [:o [[:var ?o]]]]]]]]]
           (s/conform ts/normal-form-spec '{?s {?p #{?o}}})))
    (is (= '[:spo [[[:var ?s1]
                    [:po [[[:var ?p1]
                           [:o [[:var ?o1] [:var ?o2]]]]
                          [[:var ?p2]
                           [:o [[:var ?o1] [:var ?o2]]]]]]]
                   [[:var ?s2]
                    [:po [[[:var ?p1]
                           [:o [[:var ?o1] [:var ?o2]]]]
                          [[:var ?p2]
                           [:o [[:var ?o1] [:var ?o2]]]]]]]]]
           (s/conform ts/normal-form-spec '{?s1 {?p1 #{?o1 ?o2}
                                                 ?p2 #{?o1 ?o2}}
                                            ?s2 {?p1 #{?o1 ?o2}
                                                 ?p2 #{?o1 ?o2}}})))
    (is (= '[[:var ?s] [:var ?p] [:var ?o]]
           (s/conform ts/triple-vec-spec '[?s ?p ?o])))
    (testing "with and without paths"
      (is (= '[:spo [[[:var ?s]
                      [:po [[[:path [::ps/branch {:op       cat
                                                  :paths [[::ps/terminal [:iri :x/one]]
                                                          [::ps/terminal [:iri :x/two]]]}]]
                             [:o [[:var ?o]]]]]]]]]
             (s/conform ts/normal-form-spec
                        '{?s {(cat :x/one :x/two) #{?o}}})))
      (is (s/invalid?
           (s/conform ts/normal-form-nopath-spec
                      '{?s {(cat :x/one :x/two) #{?o}}})))
      (is (= '[[:var ?s]
               [:path [::ps/branch {:op       cat
                                    :paths [[::ps/terminal [:iri :x/one]]
                                            [::ps/terminal [:iri :x/two]]]}]]
               [:var ?o]]
             (s/conform ts/triple-vec-spec
                        '[?s (cat :x/one :x/two) ?o]))))))

(ns syrup.sparql.spec.values-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.values :as vs]))

(deftest conform-values-test
  (testing "Conform VALUES clause"
    (is (= '[:values-map {[[:var ?foo] [:var ?bar]]
                          [[[:num-lit 1] [:prefix-iri :x]]
                           [[:num-lit 2] [:prefix-iri :y]]
                           [[:num-lit 3] [:prefix-iri :z]]]}]
           (s/conform ::vs/values '{[?foo ?bar] [[1 :x] [2 :y] [3 :z]]})
           (s/conform ::vs/values '{?foo [1 2 3]
                                    ?bar [:x :y :z]})))
    (is (= '[:values-map {[[:var ?foo] [:var ?bar]]
                          [[[:undef nil] [:prefix-iri :x]]
                           [[:num-lit 2] [:undef nil]]]}]
           (s/conform ::vs/values '{[?foo ?bar] [[nil :x] [2 nil]]})
           (s/conform ::vs/values '{?foo [nil 2]
                                    ?bar [:x nil]})))))

(deftest invalid-values-test
  (testing "Invalid VALUES clause"
    (is (= {::s/problems [{:path [:values-map :sparql-format]
                           :pred `map?
                           :val  2
                           :via  [::vs/values]
                           :in   []}
                          {:path [:values-map :clojure-format]
                           :pred `map?
                           :val  2
                           :via  [::vs/values]
                           :in   []}]
            ::s/spec     ::vs/values
            ::s/value    2}
         (s/explain-data ::vs/values 2)))
    (is (= {::s/problems [{:path [:values-map :sparql-format]
                           :pred `(<= 1 (count ~'%) 1)
                           :val  '{?foo [1 2]
                                   ?bar [:x :y :z]}
                           :via  [::vs/values]
                           :in   []}
                          {:path [:values-map :clojure-format]
                           :pred `vs/matching-val-lengths
                           :val  '{?foo [[:num-lit 1] [:num-lit 2]]
                                   ?bar [[:prefix-iri :x]
                                         [:prefix-iri :y]
                                         [:prefix-iri :z]]}
                           :via  [::vs/values]
                           :in   []}]
            ::s/spec     ::vs/values
            ::s/value    '{?foo [1 2]
                           ?bar [:x :y :z]}}
         (s/explain-data ::vs/values '{?foo [1 2]
                                       ?bar [:x :y :z]})))))

(ns com.yetanalytics.flint.spec.values-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.values :as vs]))

(deftest conform-values-test
  (testing "Conforming VALUES clauses"
    (is (= '[:values/map [[[:ax/var ?foo] [:ax/var ?bar]]
                          [[[:ax/literal 1] [:ax/prefix-iri :x]]
                           [[:ax/literal 2] [:ax/prefix-iri :y]]
                           [[:ax/literal 3] [:ax/prefix-iri :z]]]]]
           (s/conform ::vs/values '{[?foo ?bar] [[1 :x] [2 :y] [3 :z]]})
           (s/conform ::vs/values '{?foo [1 2 3]
                                    ?bar [:x :y :z]})))
    (is (= '[:values/map [[[:ax/var ?foo] [:ax/var ?bar]]
                          [[[:values/undef nil] [:ax/prefix-iri :x]]
                           [[:ax/literal 2] [:values/undef nil]]]]]
           (s/conform ::vs/values '{[?foo ?bar] [[nil :x] [2 nil]]})
           (s/conform ::vs/values '{?foo [nil 2]
                                    ?bar [:x nil]})))))

(deftest invalid-values-test
  (testing "Invalid VALUES clauses"
    (is (= {::s/problems [{:path [:values/map :values/sparql-format]
                           :pred `map?
                           :val  2
                           :via  [::vs/values]
                           :in   []}
                          {:path [:values/map :values/clojure-format]
                           :pred `map?
                           :val  2
                           :via  [::vs/values]
                           :in   []}]
            ::s/spec     ::vs/values
            ::s/value    2}
         (s/explain-data ::vs/values 2)))
    (is (= {::s/problems [{:path [:values/map :values/sparql-format]
                           :pred `(<= 1 (count ~'%) 1)
                           :val  '{?foo [1 2]
                                   ?bar [:x :y :z]}
                           :via  [::vs/values]
                           :in   []}
                          {:path [:values/map :values/clojure-format]
                           :pred `vs/matching-val-lengths
                           :val  '{?foo [[:ax/literal 1]
                                         [:ax/literal 2]]
                                   ?bar [[:ax/prefix-iri :x]
                                         [:ax/prefix-iri :y]
                                         [:ax/prefix-iri :z]]}
                           :via  [::vs/values]
                           :in   []}]
            ::s/spec     ::vs/values
            ::s/value    '{?foo [1 2]
                           ?bar [:x :y :z]}}
         (s/explain-data ::vs/values '{?foo [1 2]
                                       ?bar [:x :y :z]})))))

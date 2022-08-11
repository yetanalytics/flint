(ns com.yetanalytics.flint.spec.select-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.select :as ss]))

(deftest conform-select-test
  (testing "Conform SELECT clauses"
    (is (= [:ax/wildcard '*]
           (s/conform ss/select-spec '*)))
    (is (= '[:select/var-or-exprs
             [[:ax/var ?x]
              [:ax/var ?y]
              [:select/expr-as-var [:expr/as-var
                                    [[:expr/terminal [:ax/literal 2]]
                                     [:ax/var ?z]]]]]]
           (s/conform ss/select-spec '[?x ?y [2 ?z]])))))

(deftest invalid-select-test
  (testing "Invalid SELECT clauses"
    (is (not (s/valid? ss/select-spec '[?x ?x])))
    (is (not (s/valid? ss/select-spec '[?x ?y [2 ?y]])))
    (is (not (s/valid? ss/select-spec '[?x [2 ?y] ?y])))
    (is (not (s/valid? ss/select-spec '[[2 ?y] [3 ?y]])))))

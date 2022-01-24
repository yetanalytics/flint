(ns syrup.sparql.spec.modifier-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.modifier :as ms]))

(deftest modifier-conform-test
  (testing "Conforming solution modifiers"
    (is (= [[:mod/expr [:expr/terminal [:var '?foo]]]]
           (s/conform ::ms/group-by ['?foo])))
    (is (= '[[:mod/expr-as-var
              [:expr/as-var
               [[:expr/branch [[:expr/op +]
                               [:expr/args ([:expr/terminal [:num-lit 2]]
                                            [:expr/terminal [:num-lit 2]])]]]
                [:var ?foo]]]]]
           (s/conform ::ms/group-by ['[(+ 2 2) ?foo]])))
    (is (= '[[:mod/asc-desc
              [[:mod/op asc]
               [:mod/expr [:expr/terminal [:var ?bar]]]]]]
           (s/conform ::ms/order-by '[(asc ?bar)])))
    (is (= '[[:var ?foo]
             [:mod/asc-desc
              [[:mod/op asc]
               [:mod/expr [:expr/terminal [:var ?bar]]]]]]
           (s/conform ::ms/order-by '[?foo (asc ?bar)])))
    (is (= '[[:expr/terminal [:num-lit 1]]
             [:expr/terminal [:num-lit 2]]
             [:expr/terminal [:num-lit 3]]]
           (s/conform ::ms/having '[1 2 3])))
    (is (= [:num-lit 10]
           (s/conform ::ms/limit 10)))
    (is (= [:num-lit 2]
           (s/conform ::ms/offset 2)))))

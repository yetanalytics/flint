(ns com.yetanalytics.flint.spec.modifier-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.modifier :as ms]))

(deftest conform-modifier-test
  (testing "Conforming solution modifiers"
    (is (= [[:mod/expr [:expr/terminal [:ax/var '?foo]]]]
           (s/conform ::ms/group-by ['?foo])))
    (is (= '[[:mod/expr-as-var
              [:expr/as-var
               [[:expr/branch [[:expr/op +]
                               [:expr/args ([:expr/terminal [:ax/num-lit 2]]
                                            [:expr/terminal [:ax/num-lit 2]])]]]
                [:ax/var ?foo]]]]]
           (s/conform ::ms/group-by ['[(+ 2 2) ?foo]])))
    (is (= '[[:mod/asc-desc
              [[:mod/op asc]
               [:mod/expr [:expr/terminal [:ax/var ?bar]]]]]]
           (s/conform ::ms/order-by '[(asc ?bar)])))
    (is (= '[[:ax/var ?foo]
             [:mod/asc-desc
              [[:mod/op asc]
               [:mod/expr [:expr/terminal [:ax/var ?bar]]]]]]
           (s/conform ::ms/order-by '[?foo (asc ?bar)])))
    (is (= '[[:expr/terminal [:ax/num-lit 1]]
             [:expr/terminal [:ax/num-lit 2]]
             [:expr/terminal [:ax/num-lit 3]]]
           (s/conform ::ms/having '[1 2 3])))
    (is (= [:ax/num-lit 10]
           (s/conform ::ms/limit 10)))
    (is (= [:ax/num-lit 2]
           (s/conform ::ms/offset 2)))))

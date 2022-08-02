(ns com.yetanalytics.flint.spec.modifier-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.modifier :as ms]))

(deftest conform-modifier-test
  (testing "Conforming solution modifiers"
    (is (= [[:ax/var '?foo]]
           (s/conform ::ms/group-by ['?foo])))
    (is (= '[[:mod/expr-as-var
              [:expr/as-var
               [[:expr/branch [[:expr/op +]
                               [:expr/args ([:expr/terminal [:ax/literal 2]]
                                            [:expr/terminal [:ax/literal 2]])]]]
                [:ax/var ?foo]]]]]
           (s/conform ::ms/group-by ['[(+ 2 2) ?foo]])))
    (is (= '[[:ax/var ?foo]]
           (s/conform ::ms/order-by '[?foo])))
    (is (= '[[:mod/asc-desc
              [[:mod/op asc]
               [:mod/asc-desc-expr [:expr/terminal [:ax/var ?bar]]]]]]
           (s/conform ::ms/order-by '[(asc ?bar)])))
    (is (= '[[:ax/var ?foo]
             [:mod/asc-desc
              [[:mod/op asc]
               [:mod/asc-desc-expr [:expr/terminal [:ax/var ?bar]]]]]]
           (s/conform ::ms/order-by '[?foo (asc ?bar)])))
    (is (= '[[:expr/terminal [:ax/literal 1]]
             [:expr/terminal [:ax/literal 2]]
             [:expr/terminal [:ax/literal 3]]]
           (s/conform ::ms/having '[1 2 3])))
    (is (= [:ax/numeric 10]
           (s/conform ::ms/limit 10)))
    (is (= [:ax/numeric 0]
           (s/conform ::ms/limit 0)))
    (is (s/invalid?
         (s/conform ::ms/limit -10)))
    (is (= [:ax/numeric 2]
           (s/conform ::ms/offset 2)))
    (is (= [:ax/numeric 0]
           (s/conform ::ms/offset 0)))
    (is (= [:ax/numeric -2]
           (s/conform ::ms/offset -2)))))

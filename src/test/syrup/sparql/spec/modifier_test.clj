(ns syrup.sparql.spec.modifier-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.expr :as es]
            [syrup.sparql.spec.modifier :as ms]))

(deftest modifier-conform-test
  (testing "Conforming solution modifiers"
    (is (= [[:expr [::es/terminal [:var '?foo]]]]
           (s/conform ::ms/group-by ['?foo])))
    (is (= '[[:expr-as-var
             {:expr [::es/branch {:op   +
                                  :args ([::es/terminal [:num-lit 2]]
                                         [::es/terminal [:num-lit 2]])}]
              :var ?foo}]]
           (s/conform ::ms/group-by ['[(+ 2 2) ?foo]])))
    (is (= '[[:asc-desc
              {:op asc
               :expr [::es/terminal [:var ?bar]]}]]
           (s/conform ::ms/order-by ['(asc ?bar)])))
    (is (= '[[::es/terminal [:num-lit 1]]
             [::es/terminal [:num-lit 2]]
             [::es/terminal [:num-lit 3]]]
           (s/conform ::ms/having '[1 2 3])))))

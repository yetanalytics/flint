(ns syrup.sparql.spec.expr-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.expr :as es]))

(deftest expr-conform-test
  (testing "Conforming expressions"
    (is (= [::es/branch {:op   'rand
                         :args []}]
           (s/conform ::es/expr '(rand))))
    (is (= [::es/branch {:op   'bnode
                         :args []}]
           (s/conform ::es/expr '(bnode))))
    (is (= [::es/branch {:op   'bnode
                         :args [[::es/branch {:op   'rand
                                              :args []}]]}]
           (s/conform ::es/expr '(bnode (rand)))))
    (is (= [::es/branch {:op   'count
                         :args [[::es/terminal [:var '?foo]]]}]
           (s/conform ::es/expr '(count ?foo))))
    (is (= [::es/branch {:op   'count
                         :args [[::es/terminal [:wildcard '*]]]}]
           (s/conform ::es/expr '(count *))))
    (is (= [::es/branch {:op   'bound
                         :args [[::es/terminal [:var '?foo]]]}]
           (s/conform ::es/expr '(bound ?foo))))
    (is (= [::es/branch {:op   'exists
                         :args [[:group [[:tvec '[[:var ?s]
                                                  [:var ?p]
                                                  [:var ?o]]]] ]]}]
           (s/conform ::es/expr '(exists [[?s ?p ?o]]))))
    (is (= [::es/branch {:op   'contains
                         :args [[::es/terminal [:str-lit "foo"]]
                                [::es/terminal [:str-lit "foobar"]]]}]
           (s/conform ::es/expr '(contains "foo" "foobar"))))
    (is (= [::es/branch {:op   'regex
                         :args [[::es/terminal [:var '?foo]]
                                [::es/terminal [:str-lit "bar"]]
                                [::es/terminal [:str-lit "i"]]]}]
           (s/conform ::es/expr '(regex ?foo "bar" "i"))))
    (is (= [::es/branch {:op   'group-concat
                         :args [[::es/terminal [:var '?foo]]
                                [::es/terminal [:kwarg {:k :separator
                                                        :v ";"}]]]}]
           (s/conform ::es/expr '(group-concat ?foo :separator ";"))))
    (is (= [::es/branch {:op   'if
                         :args [[::es/terminal [:bool-lit true]]
                                [::es/terminal [:num-lit 1]]
                                [::es/terminal [:num-lit 0]]]}]
           (s/conform ::es/expr '(if true 1 0))))
    (is (= [::es/branch {:op   '+
                         :args [[::es/terminal [:num-lit 1]]
                                [::es/terminal [:num-lit 2]]
                                [::es/branch {:op   '*
                                              :args [[::es/terminal [:num-lit 3]]
                                                     [::es/terminal [:num-lit 4]]]}]]}]
           (s/conform ::es/expr '(+ 1 2 (* 3 4)))))
    (is (= [::es/branch {:op   :foo/my-custom-fn
                         :args [[::es/terminal [:num-lit 2]]
                                [::es/terminal [:num-lit 2]]]}]
           (s/conform ::es/expr '(:foo/my-custom-fn 2 2))))))

(deftest invalid-test
  (testing "Invalid values"
    (is (s/invalid? (s/conform ::es/expr '(rand 1))))
    (is (s/invalid? (s/conform ::es/expr '(not false true))))
    (is (s/invalid? (s/conform ::es/expr '(contains "foo"))))
    (is (s/invalid? (s/conform ::es/expr '(+))))))

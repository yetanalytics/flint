(ns syrup.sparql.format.expr-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.walk :as w]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.expr]
            [syrup.sparql.format.where]))

(deftest format-test
  (testing "expression formatting"
    (is (= ["2" "3"]
           (->> [[:expr/terminal [:num-lit 2]]
                 [:expr/terminal [:num-lit 3]]]
                (w/postwalk f/format-ast))))
    (is (= "foo:myCustomFunction(2, 3)"
           (->> [:expr/branch [[:expr/op [:prefix-iri :foo/myCustomFunction]]
                               [:expr/args [[:expr/terminal [:num-lit 2]]
                                            [:expr/terminal [:num-lit 3]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "!false"
           (->> '[:expr/branch [[:expr/op not]
                                [:expr/args [[:expr/terminal [:bool-lit false]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "!(!false)"
           (->> '[:expr/branch [[:expr/op not]
                                [:expr/args [[:expr/branch [[:expr/op not]
                                                            [:expr/args [[:expr/terminal [:bool-lit false]]]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "(1 IN (1, 2, 3))"
           (->> '[:expr/branch [[:expr/op in]
                                [:expr/args [[:expr/terminal [:num-lit 1]]
                                             [:expr/terminal [:num-lit 1]]
                                             [:expr/terminal [:num-lit 2]]
                                             [:expr/terminal [:num-lit 3]]
                                             ]]]]
                (w/postwalk f/format-ast))))
    (is (= "(1 NOT IN (2, 3, 4))"
           (->> '[:expr/branch [[:expr/op not-in]
                                [:expr/args [[:expr/terminal [:num-lit 1]]
                                             [:expr/terminal [:num-lit 2]]
                                             [:expr/terminal [:num-lit 3]]
                                             [:expr/terminal [:num-lit 4]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "(2 = 2)"
           (->> '[:expr/branch [[:expr/op =]
                                [:expr/args [[:expr/terminal [:num-lit 2]]
                                             [:expr/terminal [:num-lit 2]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "(2 != 3)"
           (->> '[:expr/branch [[:expr/op not=]
                                [:expr/args [[:expr/terminal [:num-lit 2]]
                                             [:expr/terminal [:num-lit 3]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "(2 + 3)"
           (->> [:expr/branch [[:expr/op '+]
                               [:expr/args [[:expr/terminal [:num-lit 2]]
                                            [:expr/terminal [:num-lit 3]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "(2 * (3 + 3))"
           (->> [:expr/branch
                 [[:expr/op '*]
                  [:expr/args [[:expr/terminal [:num-lit 2]]
                               [:expr/branch
                                [[:expr/op '+]
                                 [:expr/args [[:expr/terminal [:num-lit 3]]
                                              [:expr/terminal [:num-lit 3]]]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "(2 - (6 - 2))" ; => -2
           (->> [:expr/branch
                 [[:expr/op '-]
                  [:expr/args [[:expr/terminal [:num-lit 2]]
                               [:expr/branch
                                [[:expr/op '-]
                                 [:expr/args [[:expr/terminal [:num-lit 6]]
                                              [:expr/terminal [:num-lit 2]]]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "((2 - 6) - 2)" ; => -6
           (->> [:expr/branch
                 [[:expr/op '-]
                  [:expr/args [[:expr/branch
                                [[:expr/op '-]
                                 [:expr/args [[:expr/terminal [:num-lit 2]]
                                              [:expr/terminal [:num-lit 6]]]]]]
                               [:expr/terminal [:num-lit 2]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "(((2 * 3) + 4) / 2)" ; (/ (+ (* 2 3) 4) 2)
           (->> [:expr/branch
                 [[:expr/op '/]
                  [:expr/args [[:expr/branch
                                [[:expr/op '+]
                                 [:expr/args [[:expr/branch
                                               [[:expr/op '*]
                                                [:expr/args [[:expr/terminal [:num-lit 2]]
                                                             [:expr/terminal [:num-lit 3]]]]]]
                                              [:expr/terminal [:num-lit 4]]]]]]
                               [:expr/terminal [:num-lit 2]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "((2 * 3) + (4 / 2))" ; (+ (* 2 3) (/ 4 2))
           (->> [:expr/branch
                 [[:expr/op '+]
                  [:expr/args [[:expr/branch
                                [[:expr/op '*]
                                 [:expr/args [[:expr/terminal [:num-lit 2]]
                                              [:expr/terminal [:num-lit 3]]]]]]
                               [:expr/branch
                                [[:expr/op '/]
                                 [:expr/args [[:expr/terminal [:num-lit 4]]
                                              [:expr/terminal [:num-lit 2]]]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "SUM(2, (3 - 3))"
           (->> [:expr/branch
                 [[:expr/op 'sum]
                  [:expr/args [[:expr/terminal [:num-lit 2]]
                               [:expr/branch
                                [[:expr/op '-]
                                 [:expr/args [[:expr/terminal [:num-lit 3]]
                                              [:expr/terminal [:num-lit 3]]]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "((true || false) && !true)"
           (->> [:expr/branch
                 [[:expr/op 'and]
                  [:expr/args [[:expr/branch
                                [[:expr/op 'or]
                                 [:expr/args [[:expr/terminal [:bool-lit true]]
                                              [:expr/terminal [:bool-lit false]]]]]]
                               [:expr/branch
                                [[:expr/op 'not]
                                 [:expr/args [[:expr/terminal [:bool-lit true]]]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "(true || (false && !true))"
           (->> [:expr/branch
                 [[:expr/op 'or]
                  [:expr/args [[:expr/terminal [:bool-lit true]]
                               [:expr/branch
                                [[:expr/op 'and]
                                 [:expr/args [[:expr/terminal [:bool-lit false]]
                                              [:expr/branch
                                               [[:expr/op 'not]
                                                [:expr/args [[:expr/terminal [:bool-lit true]]]]]]]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "GROUP_CONCAT(?foo; SEPARATOR = ';')"
           (->> [:expr/branch [[:expr/op 'group-concat]
                               [:expr/args [[:expr/terminal [:var '?foo]]
                                            [:expr/terminal [:expr/kwarg
                                                             [[:expr/k :separator]
                                                              [:expr/v ";"]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "GROUP_CONCAT(DISTINCT ?foo; SEPARATOR = ';')"
           (->> [:expr/branch [[:expr/op 'group-concat-distinct]
                               [:expr/args [[:expr/terminal [:var '?foo]]
                                            [:expr/terminal [:expr/kwarg
                                                             [[:expr/k :separator]
                                                              [:expr/v ";"]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "ENCODE_FOR_URI(?foo)"
           (->> [:expr/branch [[:expr/op 'encode-for-uri]
                               [:expr/args [[:expr/terminal [:var '?foo]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "isIRI(?foo)"
           (->> [:expr/branch [[:expr/op 'iri?]
                               [:expr/args [[:expr/terminal [:var '?foo]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "EXISTS {\n    ?x ?y ?z .\n}"
           (->> '[:expr/branch [[:expr/op exists]
                                [:expr/args [[:where-sub/where
                                              [[:triple/vec [[:var ?x]
                                                             [:var ?y]
                                                             [:var ?z]]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "NOT EXISTS {\n    ?x ?y ?z .\n}"
           (->> '[:expr/branch [[:expr/op not-exists]
                                [:expr/args [[:where-sub/where
                                              [[:triple/vec [[:var ?x]
                                                             [:var ?y]
                                                             [:var ?z]]]]]]]]]
                (w/postwalk f/format-ast)))))
  (testing "expr AS var formatting"
    (is (= "(2 + 2) AS ?foo"
           (->> '[:expr/as-var
                  [[:expr/branch
                    [[:expr/op +]
                     [:expr/args
                      [[:expr/terminal [:num-lit 2]]
                       [:expr/terminal [:num-lit 2]]]]]]
                   [:var ?foo]]]
                (w/postwalk f/format-ast))))))

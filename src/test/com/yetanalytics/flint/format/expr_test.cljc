(ns com.yetanalytics.flint.format.expr-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.expr]
            [com.yetanalytics.flint.format.where]))

(defn- format-ast [expr-ast]
  (f/format-ast expr-ast {:pretty? true}))

(deftest format-expr-test
  (testing "Formatting expressions"
    (is (= ["2" "3"]
           (->> [[:expr/terminal [:ax/literal 2]]
                 [:expr/terminal [:ax/literal 3]]]
                format-ast)))
    (is (= "foo:myCustomFunction(2, 3)"
           (->> [:expr/branch [[:expr/op [:ax/prefix-iri :foo/myCustomFunction]]
                               [:expr/args [[:expr/terminal [:ax/literal 2]]
                                            [:expr/terminal [:ax/literal 3]]]]]]
                format-ast)))
    (is (= "!false"
           (->> '[:expr/branch [[:expr/op not]
                                [:expr/args [[:expr/terminal [:ax/literal false]]]]]]
                format-ast)))
    (is (= "!(!false)"
           (->> '[:expr/branch [[:expr/op not]
                                [:expr/args [[:expr/branch [[:expr/op not]
                                                            [:expr/args [[:expr/terminal [:ax/literal false]]]]]]]]]]
                format-ast)))
    (is (= "!(-2)"
           (->> '[:expr/branch [[:expr/op not]
                                [:expr/args [[:expr/terminal [:ax/literal -2]]]]]]
                format-ast)))
    (is (= "(1 IN (1, 2, 3))"
           (->> '[:expr/branch [[:expr/op in]
                                [:expr/args [[:expr/terminal [:ax/literal 1]]
                                             [:expr/terminal [:ax/literal 1]]
                                             [:expr/terminal [:ax/literal 2]]
                                             [:expr/terminal [:ax/literal 3]]]]]]
                format-ast)))
    (is (= "(1 NOT IN (2, 3, 4))"
           (->> '[:expr/branch [[:expr/op not-in]
                                [:expr/args [[:expr/terminal [:ax/literal 1]]
                                             [:expr/terminal [:ax/literal 2]]
                                             [:expr/terminal [:ax/literal 3]]
                                             [:expr/terminal [:ax/literal 4]]]]]]
                format-ast)))
    (is (= "(2 = 2)"
           (->> '[:expr/branch [[:expr/op =]
                                [:expr/args [[:expr/terminal [:ax/literal 2]]
                                             [:expr/terminal [:ax/literal 2]]]]]]
                format-ast)))
    (is (= "(2 != 3)"
           (->> '[:expr/branch [[:expr/op not=]
                                [:expr/args [[:expr/terminal [:ax/literal 2]]
                                             [:expr/terminal [:ax/literal 3]]]]]]
                format-ast)))
    (is (= "(2 + 3)"
           (->> [:expr/branch [[:expr/op '+]
                               [:expr/args [[:expr/terminal [:ax/literal 2]]
                                            [:expr/terminal [:ax/literal 3]]]]]]
                format-ast)))
    (is (= "(2 * (3 + 3))"
           (->> [:expr/branch
                 [[:expr/op '*]
                  [:expr/args [[:expr/terminal [:ax/literal 2]]
                               [:expr/branch
                                [[:expr/op '+]
                                 [:expr/args [[:expr/terminal [:ax/literal 3]]
                                              [:expr/terminal [:ax/literal 3]]]]]]]]]]
                format-ast)))
    (is (= "(2 - (6 - 2))" ; => -2
           (->> [:expr/branch
                 [[:expr/op '-]
                  [:expr/args [[:expr/terminal [:ax/literal 2]]
                               [:expr/branch
                                [[:expr/op '-]
                                 [:expr/args [[:expr/terminal [:ax/literal 6]]
                                              [:expr/terminal [:ax/literal 2]]]]]]]]]]
                format-ast)))
    (is (= "((2 - 6) - 2)" ; => -6
           (->> [:expr/branch
                 [[:expr/op '-]
                  [:expr/args [[:expr/branch
                                [[:expr/op '-]
                                 [:expr/args [[:expr/terminal [:ax/literal 2]]
                                              [:expr/terminal [:ax/literal 6]]]]]]
                               [:expr/terminal [:ax/literal 2]]]]]]
                format-ast)))
    (is (= "(((2 * 3) + 4) / 2)" ; (/ (+ (* 2 3) 4) 2)
           (->> [:expr/branch
                 [[:expr/op '/]
                  [:expr/args [[:expr/branch
                                [[:expr/op '+]
                                 [:expr/args [[:expr/branch
                                               [[:expr/op '*]
                                                [:expr/args [[:expr/terminal [:ax/literal 2]]
                                                             [:expr/terminal [:ax/literal 3]]]]]]
                                              [:expr/terminal [:ax/literal 4]]]]]]
                               [:expr/terminal [:ax/literal 2]]]]]]
                format-ast)))
    (is (= "((2 * 3) + (4 / 2))" ; (+ (* 2 3) (/ 4 2))
           (->> [:expr/branch
                 [[:expr/op '+]
                  [:expr/args [[:expr/branch
                                [[:expr/op '*]
                                 [:expr/args [[:expr/terminal [:ax/literal 2]]
                                              [:expr/terminal [:ax/literal 3]]]]]]
                               [:expr/branch
                                [[:expr/op '/]
                                 [:expr/args [[:expr/terminal [:ax/literal 4]]
                                              [:expr/terminal [:ax/literal 2]]]]]]]]]]
                format-ast)))
    (is (= "SUM(2, (3 - 3))"
           (->> [:expr/branch
                 [[:expr/op 'sum]
                  [:expr/args [[:expr/terminal [:ax/literal 2]]
                               [:expr/branch
                                [[:expr/op '-]
                                 [:expr/args [[:expr/terminal [:ax/literal 3]]
                                              [:expr/terminal [:ax/literal 3]]]]]]]]]]
                format-ast)))
    (is (= "((true || false) && !true)"
           (->> [:expr/branch
                 [[:expr/op 'and]
                  [:expr/args [[:expr/branch
                                [[:expr/op 'or]
                                 [:expr/args [[:expr/terminal [:ax/literal true]]
                                              [:expr/terminal [:ax/literal false]]]]]]
                               [:expr/branch
                                [[:expr/op 'not]
                                 [:expr/args [[:expr/terminal [:ax/literal true]]]]]]]]]]
                format-ast)))
    (is (= "(true || (false && !true))"
           (->> [:expr/branch
                 [[:expr/op 'or]
                  [:expr/args [[:expr/terminal [:ax/literal true]]
                               [:expr/branch
                                [[:expr/op 'and]
                                 [:expr/args [[:expr/terminal [:ax/literal false]]
                                              [:expr/branch
                                               [[:expr/op 'not]
                                                [:expr/args [[:expr/terminal [:ax/literal true]]]]]]]]]]]]]]
                format-ast)))
    (is (= "GROUP_CONCAT(?foo)"
           (->> [:expr/branch [[:expr/op 'group-concat]
                               [:expr/args [[:expr/terminal [:ax/var '?foo]]]]]]
                format-ast)))
    (is (= "GROUP_CONCAT(DISTINCT ?foo)"
           (->> [:expr/branch [[:expr/op 'group-concat]
                               [:expr/args [[:expr/terminal [:ax/var '?foo]]]]
                               [:expr/kwargs [[:distinct? true]]]]]
                format-ast)))
    (is (= "GROUP_CONCAT(?foo; SEPARATOR = \";\")"
           (->> [:expr/branch [[:expr/op 'group-concat]
                               [:expr/args [[:expr/terminal [:ax/var '?foo]]]]
                               [:expr/kwargs [[:separator ";"]]]]]
                format-ast)))
    (is (= "GROUP_CONCAT(DISTINCT ?foo; SEPARATOR = \";\")"
           (->> [:expr/branch [[:expr/op 'group-concat]
                               [:expr/args [[:expr/terminal [:ax/var '?foo]]]]
                               [:expr/kwargs [[:distinct? true]
                                              [:separator ";"]]]]]
                format-ast)))
    (is (= "GROUP_CONCAT(DISTINCT ?foo; SEPARATOR = \";\")"
           (->> [:expr/branch [[:expr/op 'group-concat]
                               [:expr/args [[:expr/terminal [:ax/var '?foo]]]]
                               [:expr/kwargs [[:separator ";"]
                                              [:distinct? true]]]]]
                format-ast)))
    (is (= "ENCODE_FOR_URI(?foo)"
           (->> [:expr/branch [[:expr/op 'encode-for-uri]
                               [:expr/args [[:expr/terminal [:ax/var '?foo]]]]]]
                format-ast)))
    (is (= "isIRI(?foo)"
           (->> [:expr/branch [[:expr/op 'iri?]
                               [:expr/args [[:expr/terminal [:ax/var '?foo]]]]]]
                format-ast)))
    (is (= "EXISTS {\n    ?x ?y ?z .\n}"
           (->> '[:expr/branch [[:expr/op exists]
                                [:expr/args [[:where-sub/where
                                              [:where/triple
                                               [[:triple.vec/spo [[:ax/var ?x]
                                                                  [:ax/var ?y]
                                                                  [:ax/var ?z]]]]]]]]]]
                format-ast)))
    (is (= "NOT EXISTS {\n    ?x ?y ?z .\n}"
           (->> '[:expr/branch [[:expr/op not-exists]
                                [:expr/args [[:where-sub/where
                                              [[:where/triple
                                                [:triple.vec/spo [[:ax/var ?x]
                                                                  [:ax/var ?y]
                                                                  [:ax/var ?z]]]]]]]]]]
                format-ast)))))

(deftest format-expr-as-var-test
  (testing "Formatting expr AS var clauses"
    (is (= "(2 + 2) AS ?foo"
           (->> '[:expr/as-var
                  [[:expr/branch
                    [[:expr/op +]
                     [:expr/args
                      [[:expr/terminal [:ax/literal 2]]
                       [:expr/terminal [:ax/literal 2]]]]]]
                   [:ax/var ?foo]]]
                format-ast)))))

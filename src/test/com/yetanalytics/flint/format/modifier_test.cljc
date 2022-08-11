(ns com.yetanalytics.flint.format.modifier-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.modifier]))

(defn- format-ast [ast]
  (f/format-ast ast {}))

(deftest format-modifier-test
  (testing "Formatting ORDER BY clauses"
    (is (= "ORDER BY (?foo)"
           (->> '[:order-by [[:mod/order-expr
                              [:expr/terminal [:ax/var ?foo]]]]]
                format-ast)))
    (is (= "ORDER BY (?foo + ?bar)"
           (->> '[:order-by [[:mod/order-expr
                              [:expr/branch
                               [[:expr/op +]
                                [:expr/args [[:expr/terminal [:ax/var ?foo]]
                                             [:expr/terminal [:ax/var ?bar]]]]]]]]]
                format-ast)))
    (is (= "ORDER BY ASC(?bar)"
           (->> '[:order-by [[:mod/asc-desc [[:mod/op asc]
                                             [:mod/asc-desc-expr [:expr/terminal [:ax/var ?bar]]]]]]]
                format-ast)))
    (is (= "ORDER BY ASC(?x + ?y)"
           (->> '[:order-by [[:mod/asc-desc [[:mod/op asc]
                                             [:mod/asc-desc-expr
                                              [:expr/branch [[:expr/op +]
                                                             [:expr/args [[:expr/terminal [:ax/var ?x]]
                                                                          [:expr/terminal [:ax/var ?y]]]]]]]]]]]
                format-ast)))
    (is (= "ORDER BY DESC(?bar)"
           (->> '[:order-by [[:mod/asc-desc [[:mod/op desc]
                                             [:mod/asc-desc-expr [:expr/terminal [:ax/var ?bar]]]]]]]
                format-ast)))
    (is (= "ORDER BY (?a + ?b) ASC(?bar)"
           (->> '[:order-by [[:mod/order-expr
                              [:expr/branch
                               [[:expr/op +]
                                [:expr/args [[:expr/terminal [:ax/var ?a]]
                                             [:expr/terminal [:ax/var ?b]]]]]]]
                             [:mod/asc-desc [[:mod/op asc]
                                             [:mod/asc-desc-expr [:expr/terminal [:ax/var ?bar]]]]]]]
                format-ast)))
    (is (= "ORDER BY ?foo ASC(?bar)"
           (->> '[:order-by [[:ax/var ?foo]
                             [:mod/asc-desc [[:mod/op asc]
                                             [:mod/asc-desc-expr [:expr/terminal [:ax/var ?bar]]]]]]]
                format-ast))))
  (testing "Formatting GROUP BY clauses"
    (is (= "GROUP BY ?foo"
           (->> '[:group-by [[:ax/var ?foo]]]
                format-ast)))
    (is (= "GROUP BY ?foo ?bar"
           (->> '[:group-by [[:ax/var ?foo]
                             [:ax/var ?bar]]]
                format-ast)))
    ;; This is technically invalid since (?a + ?b) is not a fn call
    (is (= "GROUP BY (?a + ?b) (1 AS ?foo) ?bar"
           (->> '[:group-by [[:mod/order-expr
                              [:expr/branch
                               [[:expr/op +]
                                [:expr/args [[:expr/terminal [:ax/var ?a]]
                                             [:expr/terminal [:ax/var ?b]]]]]]]
                             [:mod/expr-as-var [:expr/as-var [[:expr/terminal [:ax/literal 1]]
                                                              [:ax/var ?foo]]]]
                             [:ax/var ?bar]]]
                format-ast))))
  (testing "Formatting HAVING clauses"
    (is (= "HAVING (1) (!true)"
           (->> '[:having [[:expr/terminal [:ax/literal 1]]
                           [:expr/branch
                            [[:expr/op not]
                             [:expr/args [[:expr/terminal [:ax/literal true]]]]]]
                           ]]
                format-ast))))
  (testing "Formatting LIMIT clauses"
    (is (= "LIMIT 10"
           (->> '[:limit [:ax/literal 10]]
                format-ast))))
  (testing "Formatting OFFSET clauses"
    (is (= "OFFSET 2"
           (->> '[:offset [:ax/literal 2]]
                format-ast)))))

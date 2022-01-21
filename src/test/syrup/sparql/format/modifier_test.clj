(ns syrup.sparql.format.modifier-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.walk :as w]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.modifier]))

(deftest format-test
  (testing "ORDER BY formatting"
    (is (= "ORDER BY (?foo + ?bar)"
           (->> '[:order-by [[:expr [:expr-branch {:op   +
                                                   :args [[:expr-terminal [:var ?foo]]
                                                          [:expr-terminal [:var ?bar]]]}]]]]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))
    (is (= "ORDER BY ASC(?bar)"
           (->> '[:order-by [[:asc-desc {:op       asc
                                         :sub-expr [:expr-terminal [:var ?bar]]}]]]
                (w/postwalk f/format-ast))))
    (is (= "ORDER BY DESC(?bar)"
           (->> '[:order-by [[:asc-desc {:op       desc
                                         :sub-expr [:expr-terminal [:var ?bar]]}]]]
                (w/postwalk f/format-ast))))
    (is (= "ORDER BY (?a + ?b) ASC(?bar)"
           (->> '[:order-by [[:expr [:expr-branch {:op   +
                                                   :args [[:expr-terminal [:var ?a]]
                                                          [:expr-terminal [:var ?b]]]}]]
                             [:asc-desc {:op       asc
                                         :sub-expr [:expr-terminal [:var ?bar]]}]]]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast)))))
  (is (= "ORDER BY ?foo ASC(?bar)"
         (->> '[:order-by [[:var ?foo]
                           [:asc-desc {:op       asc
                                       :sub-expr [:expr-terminal [:var ?bar]]}]]]
              (w/postwalk f/format-ast))))
  (testing "GROUP BY formatting"
    (is (= "GROUP BY ?foo"
           (->> '[:group-by [[:var ?foo]]]
                (w/postwalk f/format-ast))))
    (is (= "GROUP BY ?foo ?bar"
           (->> '[:group-by [[:var ?foo]
                             [:var ?bar]]]
                (w/postwalk f/format-ast))))
    (is (= "GROUP BY (?a + ?b) (1 AS ?foo) ?bar"
           (->> '[:group-by [[:expr [:expr-branch {:op   +
                                                   :args [[:expr-terminal [:var ?a]]
                                                          [:expr-terminal [:var ?b]]]}]]
                             [:expr-var [:expr-as-var [[:expr-terminal [:num-lit 1]]
                                                       [:var ?foo]]]]
                             [:var ?bar]]]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast)))))
  (testing "HAVING formatting"
    (is (= "HAVING 1 2 3"
           (->> '[:having [[:expr-terminal [:num-lit 1]]
                           [:expr-terminal [:num-lit 2]]
                           [:expr-terminal [:num-lit 3]]]]
                (w/postwalk f/format-ast)))))
  (testing "LIMIT formatting"
    (is (= "LIMIT 10"
           (->> '[:limit [:num-lit 10]]
                (w/postwalk f/format-ast)))))
  (testing "OFFSET formatting"
    (is (= "OFFSET 2"
           (->> '[:offset [:num-lit 2]]
                (w/postwalk f/format-ast))))))


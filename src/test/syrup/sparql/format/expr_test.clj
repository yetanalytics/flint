(ns syrup.sparql.format.expr-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.walk :as w]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.expr]))

(deftest format-test
  (testing "expression formatting"
    (is (= ["2" "3"]
           (->> [[:expr-terminal [:num-lit 2]]
                 [:expr-terminal [:num-lit 3]]]
                (w/postwalk f/format-ast))))
    (is (= "foo:myCustomFunction(2, 3)"
           (->> [:expr-branch {:op   [:prefix-iri :foo/myCustomFunction]
                               :args [[:expr-terminal [:num-lit 2]]
                                      [:expr-terminal [:num-lit 3]]]}]
                (w/postwalk f/format-ast))))
    (is (= "(2) + (3)"
           (->> [:expr-branch {:op   '+
                               :args [[:expr-terminal [:num-lit 2]]
                                      [:expr-terminal [:num-lit 3]]]}]
                (w/postwalk f/format-ast))))
    (is (= "2 + 3"
           (->> [:expr-branch {:op   '+
                               :args [[:expr-terminal [:num-lit 2]]
                                      [:expr-terminal [:num-lit 3]]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))
    (is (= "2 * (3 + 3)"
           (->> [:expr-branch
                 {:op   '*
                  :args [[:expr-terminal [:num-lit 2]]
                         [:expr-branch
                          {:op   '+
                           :args [[:expr-terminal [:num-lit 3]]
                                  [:expr-terminal [:num-lit 3]]]}]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))
    (is (= "2 - (6 - 2)" ; => -2
           (->> [:expr-branch
                 {:op   '-
                  :args [[:expr-terminal [:num-lit 2]]
                         [:expr-branch
                          {:op   '-
                           :args [[:expr-terminal [:num-lit 6]]
                                  [:expr-terminal [:num-lit 2]]]}]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))
    (is (= "(2 - 6) - 2" ; => -6
           (->> [:expr-branch
                 {:op   '-
                  :args [[:expr-branch
                          {:op   '-
                           :args [[:expr-terminal [:num-lit 2]]
                                  [:expr-terminal [:num-lit 6]]]}]
                         [:expr-terminal [:num-lit 2]]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))
    (is (= "(2 * 3 + 4) / 2" ; (/ (+ (* 2 3) 4) 2)
           (->> [:expr-branch
                 {:op   '/
                  :args [[:expr-branch
                          {:op   '+
                           :args [[:expr-branch
                                   {:op   '*
                                    :args [[:expr-terminal [:num-lit 2]]
                                           [:expr-terminal [:num-lit 3]]]}]
                                  [:expr-terminal [:num-lit 4]]]}]
                         [:expr-terminal [:num-lit 2]]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))
    (is (= "2 * 3 + 4 / 2" ; (+ (* 2 3) (/ 4 2))
           (->> [:expr-branch
                 {:op   '+
                  :args [[:expr-branch
                          {:op   '*
                           :args [[:expr-terminal [:num-lit 2]]
                                  [:expr-terminal [:num-lit 3]]]}]
                         [:expr-branch
                          {:op   '/
                           :args [[:expr-terminal [:num-lit 4]]
                                  [:expr-terminal [:num-lit 2]]]}]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))
    (is (= "SUM(2, 3 - 3)"
           (->> [:expr-branch
                 {:op   'sum
                  :args [[:expr-terminal [:num-lit 2]]
                         [:expr-branch
                          {:op   '-
                           :args [[:expr-terminal [:num-lit 3]]
                                  [:expr-terminal [:num-lit 3]]]}]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))
    (is (= "(true || false) && !true"
           (->> [:expr-branch
                 {:op   'and
                  :args [[:expr-branch
                          {:op 'or
                           :args [[:expr-terminal [:bool-lit true]]
                                  [:expr-terminal [:bool-lit false]]]}]
                         [:expr-branch
                          {:op   'not
                           :args [[:expr-terminal [:bool-lit true]]]}]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))
    (is (= "true || false && !true"
           (->> [:expr-branch
                 {:op   'or
                  :args [[:expr-terminal [:bool-lit true]]
                         [:expr-branch
                          {:op 'and
                           :args [[:expr-terminal [:bool-lit false]]
                                  [:expr-branch
                                   {:op   'not
                                    :args [[:expr-terminal [:bool-lit true]]]}]]}]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))
    (is (= "GROUP_CONCAT(?foo; SEPARATOR = ';')"
           (->> [:expr-branch {:op   'group-concat
                               :args [[:expr-terminal [:var '?foo]]
                                      [:expr-terminal [:kwarg {:k :separator
                                                               :v ";"}]]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))
    (is (= "GROUP_CONCAT(DISTINCT ?foo; SEPARATOR = ';')"
           (->> [:expr-branch {:op   'group-concat-distinct
                               :args [[:expr-terminal [:var '?foo]]
                                      [:expr-terminal [:kwarg {:k :separator
                                                               :v ";"}]]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))
    (is (= "ENCODE_FOR_URI(?foo)"
           (->> [:expr-branch {:op   'encode-for-uri
                               :args [[:expr-terminal [:var '?foo]]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))
    (is (= "isIRI(?foo)"
           (->> [:expr-branch {:op   'iri?
                               :args [[:expr-terminal [:var '?foo]]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast)))))
  (testing "expr AS var formatting"
    (is (= "2 + 2 AS ?foo"
           (->> '[:expr-as-var
                  [[:expr-branch
                    {:op +
                     :args
                     [[:expr-terminal [:num-lit 2]]
                      [:expr-terminal [:num-lit 2]]]}]
                   [:var ?foo]]]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))))

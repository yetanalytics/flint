(ns syrup.sparql.format.path-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.walk :as w]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.path]))

(deftest format-test
  (testing "formatting paths"
    (is (= "(!foo:bar | (^baz:qux / quu:bee))"
           (->> '[:path/branch
                  [[:path/op alt]
                   [:path/args [[:path/branch
                                 [[:path/op not]
                                  [:path/args [[:path/terminal [:prefix-iri :foo/bar]]]]]]
                                [:path/branch
                                 [[:path/op cat]
                                  [:path/args [[:path/branch
                                                [[:path/op inv]
                                                 [:path/args [[:path/terminal
                                                               [:prefix-iri :baz/qux]]]]]]
                                               [:path/terminal
                                                [:prefix-iri :quu/bee]]]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "(!foo:bar / (^baz:qux | quu:bee))"
           (->> '[:path/branch
                  [[:path/op cat]
                   [:path/args [[:path/branch
                                 [[:path/op not]
                                  [:path/args [[:path/terminal [:prefix-iri :foo/bar]]]]]]
                                [:path/branch
                                 [[:path/op alt]
                                  [:path/args [[:path/branch
                                                [[:path/op inv]
                                                 [:path/args [[:path/terminal
                                                               [:prefix-iri :baz/qux]]]]]]
                                               [:path/terminal
                                                [:prefix-iri :quu/bee]]]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "!(foo:bar | baz:qux)"
           (->> '[:path/branch
                  [[:path/op not]
                   [:path/args [[:path/branch
                                 [[:path/op alt]
                                  [:path/args [[:path/terminal [:prefix-iri :foo/bar]]
                                               [:path/terminal [:prefix-iri :baz/qux]]]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "^a"
           (->> '[:path/branch
                  [[:path/op inv]
                   [:path/args [[:path/terminal [:rdf-type 'a]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "a?"
           (->> '[:path/branch
                  [[:path/op ?]
                   [:path/args [[:path/terminal [:rdf-type 'a]]]]]]
                (w/postwalk f/format-ast))))
    (is (= "((a?)*)+"
           (->> '[:path/branch
                  [[:path/op +]
                   [:path/args [[:path/branch
                                 [[:path/op *]
                                  [:path/args [[:path/branch
                                                [[:path/op ?]
                                                 [:path/args [[:path/terminal [:rdf-type 'a]]]]]]]]]]]]]]
                (w/postwalk f/format-ast))))))

(deftest invalid-test
  (testing "attempting to format an invalid path"
    (is (try (w/postwalk f/format-ast
                         '[:path/branch
                           [[:path/op oh-no]
                            [:path/args [[:path/terminal [:rdf-type 'a]]]]]])
             (catch IllegalArgumentException _ true)))))

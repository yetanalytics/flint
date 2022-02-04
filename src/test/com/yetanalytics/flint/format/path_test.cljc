(ns com.yetanalytics.flint.format.path-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.path]))

(defn- format-ast [ast]
  (f/format-ast ast {}))

(deftest format-path-test
  (testing "Formatting paths"
    (is (= "(!foo:bar | (^baz:qux / quu:bee))"
           (->> '[:path/branch
                  [[:path/op alt]
                   [:path/paths [[:path/branch
                                  [[:path/op not]
                                   [:path/paths [[:path/terminal [:ax/prefix-iri :foo/bar]]]]]]
                                 [:path/branch
                                  [[:path/op cat]
                                   [:path/paths [[:path/branch
                                                  [[:path/op inv]
                                                   [:path/paths [[:path/terminal
                                                                  [:ax/prefix-iri :baz/qux]]]]]]
                                                 [:path/terminal
                                                  [:ax/prefix-iri :quu/bee]]]]]]]]]]
                format-ast)))
    (is (= "(!foo:bar / (^baz:qux | quu:bee))"
           (->> '[:path/branch
                  [[:path/op cat]
                   [:path/paths [[:path/branch
                                  [[:path/op not]
                                   [:path/paths [[:path/terminal [:ax/prefix-iri :foo/bar]]]]]]
                                 [:path/branch
                                  [[:path/op alt]
                                   [:path/paths [[:path/branch
                                                  [[:path/op inv]
                                                   [:path/paths [[:path/terminal
                                                                  [:ax/prefix-iri :baz/qux]]]]]]
                                                 [:path/terminal
                                                  [:ax/prefix-iri :quu/bee]]]]]]]]]]
                format-ast)))
    (is (= "!(foo:bar | baz:qux)"
           (->> '[:path/branch
                  [[:path/op not]
                   [:path/paths [[:path/branch
                                  [[:path/op alt]
                                   [:path/paths [[:path/terminal [:ax/prefix-iri :foo/bar]]
                                                 [:path/terminal [:ax/prefix-iri :baz/qux]]]]]]]]]]
                format-ast)))
    (is (= "^a"
           (->> '[:path/branch
                  [[:path/op inv]
                   [:path/paths [[:path/terminal [:ax/rdf-type 'a]]]]]]
                format-ast)))
    (is (= "a?"
           (->> '[:path/branch
                  [[:path/op ?]
                   [:path/paths [[:path/terminal [:ax/rdf-type 'a]]]]]]
                format-ast)))
    (is (= "((a?)*)+"
           (->> '[:path/branch
                  [[:path/op +]
                   [:path/paths [[:path/branch
                                  [[:path/op *]
                                   [:path/paths [[:path/branch
                                                  [[:path/op ?]
                                                   [:path/paths [[:path/terminal [:ax/rdf-type 'a]]]]]]]]]]]]]]
                format-ast)))))

(deftest format-invalid-test
  (testing "Formatting an invalid path"
    (is (try (f/format-ast
              '[:path/branch
                [[:path/op oh-no]
                 [:path/paths [[:path/terminal [:ax/rdf-type 'a]]]]]]
              {})
             (catch #?(:clj IllegalArgumentException
                       :cljs js/Error) _
               true)))))

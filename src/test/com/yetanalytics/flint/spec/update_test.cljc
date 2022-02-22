(ns com.yetanalytics.flint.spec.update-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.update :as us]))

(deftest conform-update-test
  (testing "Conforming updates"
    (is (= '[[:insert-data [[:triple/vec [[:ax/prefix-iri :foo/x]
                                          [:ax/prefix-iri :dc/title]
                                          [:ax/str-lit "Title"]]]
                            [:triple/vec [[:ax/prefix-iri :foo/y]
                                          [:ax/rdf-type :a]
                                          [:ax/str-lit "MyType"]]]]]]
           (s/conform us/insert-data-update-spec
                      '{:insert-data [[:foo/x :dc/title "Title"]
                                      [:foo/y :a "MyType"]]})))
    (is (= '[[:delete-data [[:triple/quads
                             [[:ax/iri "<http://example.org>"]
                              [:triple/quad-triples
                               [[:triple/vec [[:ax/prefix-iri :foo/x]
                                              [:ax/prefix-iri :dc/title]
                                              [:ax/str-lit "Title"]]]
                                [:triple/vec [[:ax/prefix-iri :foo/y]
                                              [:ax/rdf-type :a]
                                              [:ax/str-lit "MyType"]]]]]]]]]]
           (s/conform us/delete-data-update-spec
                      '{:delete-data [[:graph
                                       "<http://example.org>"
                                       [[:foo/x :dc/title "Title"]
                                        [:foo/y :a "MyType"]]]]})))
    (is (= '[[:move [:update/default-graph :default]]
             [:to [:update/named-graph [:ax/iri "<http://example.org>"]]]]
           (s/conform us/move-update-spec
                      '{:to "<http://example.org>"
                        :move :default})))))

(deftest invalid-update-test
  (testing "Invalid updates"
    (is (not (s/valid? us/insert-data-update-spec
                       '{:insert-data [[?x ?y ?z]]})))
    (is (not (s/valid? us/insert-data-update-spec
                       '{:insert-data [{:foo/x {:bar/y #{?z}}}]})))
    (is (not (s/valid? us/insert-data-update-spec
                       '{:delete-data [[:foo/x :bar/y _1]]})))
    (is (not (s/valid? us/insert-data-update-spec
                       '{:delete-data [{:foo/x {:bar/y #{_1}}}]})))))

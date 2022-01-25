(ns syrup.sparql.spec.update-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.update :as us]))

(deftest conform-update-test
  (testing "conforming updates"
    (is (= '[[:insert-data [[:tvec [[:prefix-iri :foo/x]
                                    [:prefix-iri :dc/title]
                                    [:str-lit "Title"]]]]]]
           (s/conform us/insert-data-update-spec
                      '{:insert-data [[:foo/x :dc/title "Title"]]})))
    (is (= '[[:delete-data [[:quads [:graph
                                     [:iri "<http://example.org>"]
                                     [[:tvec [[:prefix-iri :foo/x]
                                              [:prefix-iri :dc/title]
                                              [:str-lit "Title"]]]]]]]]]
           (s/conform us/delete-data-update-spec
                      '{:delete-data [[:graph
                                       "<http://example.org>"
                                       [[:foo/x :dc/title "Title"]]]]})))
    (is (= '[[:move [:update/default-graph :default]]
             [:to [:update/named-graph [:iri "<http://example.org>"]]]]
           (s/conform us/move-update-spec
                      '{:move :default
                        :to "<http://example.org>"})))))

(deftest invalid-update-test
  (testing "invalid updates"
    (is (not (s/valid? us/insert-data-update-spec
                       '{:insert-data [[?x ?y ?z]]})))))
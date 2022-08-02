(ns com.yetanalytics.flint.spec.update-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.update :as us]))

(deftest conform-update-test
  (testing "Conforming updates"
    (is (= '[[:insert-data [[:triple/vec [[:ax/prefix-iri :foo/x]
                                          [:ax/prefix-iri :dc/title]
                                          [:ax/literal "Title"]]]
                            [:triple/vec [[:ax/prefix-iri :foo/y]
                                          [:ax/rdf-type :a]
                                          [:ax/literal "MyType"]]]]]]
           (s/conform us/insert-data-update-spec
                      '{:insert-data [[:foo/x :dc/title "Title"]
                                      [:foo/y :a "MyType"]]})))
    (is (= '[[:delete-data [[:triple/quads
                             [[:ax/iri "<http://example.org>"]
                              [:triple/quad-triples
                               [[:triple/vec [[:ax/prefix-iri :foo/x]
                                              [:ax/prefix-iri :dc/title]
                                              [:ax/literal "Title"]]]
                                [:triple/vec [[:ax/prefix-iri :foo/y]
                                              [:ax/rdf-type :a]
                                              [:ax/literal "MyType"]]]]]]]]]]
           (s/conform us/delete-data-update-spec
                      '{:delete-data [[:graph
                                       "<http://example.org>"
                                       [[:foo/x :dc/title "Title"]
                                        [:foo/y :a "MyType"]]]]})))
    (testing "- graph management"
      (is (= '[[:load [:ax/iri "<http://example.org>"]]
               [:into [:update/graph [:graph [:ax/iri "<http://graph.com/1>"]]]]]
             (s/conform us/load-update-spec
                        '{:load "<http://example.org>"
                          :into [:graph "<http://graph.com/1>"]})))
      (is (= '[[:clear [:update/default :default]]]
             (s/conform us/clear-update-spec
                        '{:clear :default})))
      (is (= '[[:clear [:update/named :named]]]
             (s/conform us/clear-update-spec
                        '{:clear :named})))
      (is (= '[[:clear [:update/all :all]]]
             (s/conform us/clear-update-spec
                        '{:clear :all})))
      (is (= '[[:create-silent
                [:update/graph [:graph [:ax/iri "<http://graph.com/1>"]]]]]
             (s/conform us/create-update-spec
                        '{:create-silent [:graph "<http://graph.com/1>"]})))
      (is (= '[[:drop-silent [:update/all :all]]]
             (s/conform us/drop-update-spec
                        '{:drop-silent :all})))
      (is (= '[[:copy [:update/default :default]]
               [:to [:update/graph [:graph [:ax/iri "<http://example.org>"]]]]]
             (s/conform us/copy-update-spec
                        '{:copy :default
                          :to [:graph "<http://example.org>"]})))
      (is (= '[[:move [:update/default :default]]
               [:to [:update/graph-notag [:ax/iri "<http://example.org>"]]]]
             (s/conform us/move-update-spec
                        '{:to "<http://example.org>"
                          :move :default})))
      (is (= '[[:add [:update/default :default]]
               [:to [:update/default :default]]]
             (s/conform us/add-update-spec
                        '{:to :default
                          :add :default}))))))

(deftest invalid-update-test
  (testing "Invalid updates"
    (is (not (s/valid? us/insert-data-update-spec
                       '{:insert-data [[?x ?y ?z]]})))
    (is (not (s/valid? us/insert-data-update-spec
                       '{:insert-data [{:foo/x {:bar/y #{?z}}}]})))
    (is (not (s/valid? us/insert-data-update-spec
                       '{:delete-data [[:foo/x :bar/y _1]]})))
    (is (not (s/valid? us/insert-data-update-spec
                       '{:delete-data [{:foo/x {:bar/y #{_1}}}]})))
    (testing "- naked graph IRIs aren't allowed in some updates"
      (is (not (s/valid? us/load-update-spec
                         '{:load "<http://source.org>"
                           :into "<http://dest.org>"})))
      (is (not (s/valid? us/clear-update-spec
                         '{:clear "<http://foo.org>"})))
      (is (not (s/valid? us/create-update-spec
                         '{:create "<http://foo.org>"})))
      (is (not (s/valid? us/drop-update-spec
                         '{:drop "<http://foo.org>"}))))))

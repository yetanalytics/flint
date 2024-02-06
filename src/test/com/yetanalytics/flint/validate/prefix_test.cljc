(ns com.yetanalytics.flint.validate.prefix-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.validate :as v]
            [com.yetanalytics.flint.validate.prefix :as vp]
            [com.yetanalytics.flint.spec.query :as qs]
            [com.yetanalytics.flint.spec.update :as us]))

(def query '{:prefixes {:foo "<http://foo.org/>"}
             :select [?x]
             :where [[:foo/bar :a ?y]
                     [:fee/bar :a ?x]
                     [:union [[:fii/bar :a ?z]] [[:fum/bar :a ?w]]]]})

(deftest validate-prefixes-test
  (testing "validate-prefixes function"
    (is (nil? (->> (assoc query :where '[[:foo/bar :a ?y]])
                   (s/conform qs/query-spec)
                   v/collect-nodes
                   (vp/validate-prefixes (:prefixes query)))))
    (is (nil? (->> (assoc query :where '[[:bar :a ?y]])
                   (s/conform qs/query-spec)
                   v/collect-nodes
                   (vp/validate-prefixes (assoc (:prefixes query)
                                                :$ "<http://default.org>")))))
    (is (= [{:iri :bar
             :prefix :$
             :prefixes {:foo "<http://foo.org/>"}
             :path [:query/select :where :where-sub/where :where/triple :triple.vec/spo :ax/prefix-iri]}]
           (->> (assoc query :where '[[:bar :a ?y]])
                (s/conform qs/query-spec)
                v/collect-nodes
                (vp/validate-prefixes (:prefixes query)))))
    (is (= [{:iri      :fee/bar
             :prefix   :fee
             :prefixes {:foo "<http://foo.org/>"}
             :path     [:query/select :where :where-sub/where :where/triple :triple.vec/spo :ax/prefix-iri]}
            {:iri      :fii/bar
             :prefix   :fii
             :prefixes {:foo "<http://foo.org/>"}
             :path     [:query/select :where :where-sub/where :where/special :where/union :where-sub/where :where/triple :triple.vec/spo :ax/prefix-iri]}
            {:iri      :fum/bar
             :prefix   :fum
             :prefixes {:foo "<http://foo.org/>"}
             :path     [:query/select :where :where-sub/where :where/special :where/union :where-sub/where :where/triple :triple.vec/spo :ax/prefix-iri]}]
           (->> query
                (s/conform qs/query-spec)
                v/collect-nodes
                (vp/validate-prefixes (:prefixes query)))))
    (is (= [{:iri :baz/Qux
             :prefix :baz
             :prefixes {:rdf "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"}
             :path [:update/insert-data :insert-data :triple/quads :triple/quad-triples :triple.nform/spo :triple.nform/po :triple.nform/o :ax/prefix-iri]}
            {:iri :baz/Quu
             :prefix :baz
             :prefixes {:rdf "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"}
             :path [:update/insert-data :insert-data :triple/quads :triple/quad-triples :triple.vec/spo :ax/prefix-iri]}]
           (->> {:prefixes {:rdf "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"}
                 :insert-data [[:graph "<http://foo.org>"
                                [{"<http://bar.org>" {:rdf/type #{:baz/Qux}}}
                                 ["<http://bar.org>" :rdf/type :baz/Quu]]]]}
                (s/conform us/update-spec)
                v/collect-nodes
                (vp/validate-prefixes {:rdf "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"}))))))

(ns com.yetanalytics.flint.validate.prefix-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.validate :as v]
            [com.yetanalytics.flint.validate.prefix :as vp]
            [com.yetanalytics.flint.spec.query :as qs]))

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
             :path [:query/select :where :where-sub/where :triple/vec :ax/prefix-iri]}]
           (->> (assoc query :where '[[:bar :a ?y]])
                (s/conform qs/query-spec)
                v/collect-nodes
                (vp/validate-prefixes (:prefixes query)))))
    (is (= [{:iri      :fee/bar
             :prefix   :fee
             :prefixes {:foo "<http://foo.org/>"}
             :path     [:query/select :where :where-sub/where :triple/vec :ax/prefix-iri]}
            {:iri      :fii/bar
             :prefix   :fii
             :prefixes {:foo "<http://foo.org/>"}
             :path     [:query/select :where :where-sub/where :where/union :where-sub/where :triple/vec :ax/prefix-iri]}
            {:iri      :fum/bar
             :prefix   :fum
             :prefixes {:foo "<http://foo.org/>"}
             :path     [:query/select :where :where-sub/where :where/union :where-sub/where :triple/vec :ax/prefix-iri]}]
           (->> query
                (s/conform qs/query-spec)
                v/collect-nodes
                (vp/validate-prefixes (:prefixes query)))))))

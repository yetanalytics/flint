(ns com.yetanalytics.flint.prefix-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.prefix     :as pre]
            [com.yetanalytics.flint.spec.query :as qs]))

(def query '{:prefixes {:foo "<http://foo.org/>"}
             :select [?x]
             :where [[:foo/bar :a ?y]
                     [:fee/bar :a ?x]
                     [:union [[:fii/bar :a ?z]] [[:fum/bar :a ?w]]]]})

(deftest validate-prefixes-test
  (testing "validate-prefixes"
    (is (nil? (->> (assoc query :where '[[:foo/bar :a ?y]])
                   (s/conform qs/query-spec)
                   (pre/validate-prefixes (:prefixes query)))))
    (is (nil? (->> (assoc query :where '[[:bar :a ?y]])
                   (s/conform qs/query-spec)
                   (pre/validate-prefixes (assoc (:prefixes query)
                                                 :$ "<http://default.org>")))))
    (is (= [{:iri :bar
             :prefix :$
             :prefixes {:foo "<http://foo.org/>"}
             :path [:query/select :where :where-sub/where :triple/vec]}]
           (->> (assoc query :where '[[:bar :a ?y]])
                (s/conform qs/query-spec)
                (pre/validate-prefixes (:prefixes query)))))
    (is (= [{:iri      :fee/bar
             :prefix   :fee
             :prefixes {:foo "<http://foo.org/>"}
             :path     [:query/select :where :where-sub/where :triple/vec]}
            {:iri      :fii/bar
             :prefix   :fii
             :prefixes {:foo "<http://foo.org/>"}
             :path     [:query/select :where :where-sub/where :where/union :where-sub/where :triple/vec]}
            {:iri      :fum/bar
             :prefix   :fum
             :prefixes {:foo "<http://foo.org/>"}
             :path     [:query/select :where :where-sub/where :where/union :where-sub/where :triple/vec]}]
           (->> query
                (s/conform qs/query-spec)
                (pre/validate-prefixes (:prefixes query)))))))

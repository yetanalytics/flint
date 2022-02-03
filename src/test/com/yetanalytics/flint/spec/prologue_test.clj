(ns com.yetanalytics.flint.spec.prologue-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.prologue :as ps]))

(deftest prologue-axiom-test
  (testing "Valid base IRIs"
    (is (s/valid? ::ps/base "<http://foo.org>"))
    (is (not (s/valid? ::ps/base "http://foo.org"))))
  (testing "Valid prefixes"
    (is (s/valid? ::ps/prefixes {:$ "<http://foo.org>"}))
    (is (s/valid? ::ps/prefixes {:foo "<http://foo.org>"}))
    (is (not (s/valid? ::ps/prefixes {:foo "http://foo.org"})))
    (is (not (s/valid? ::ps/prefixes {:foo/bar "<http://foo.org>"})))
    (is (not (s/valid? ::ps/prefixes {:& "<http://foo.org>"})))))

(deftest conform-prologue-test
  (testing "Conforming the prologue"
    (is (= [:iri "<http://foo.org>"]
           (s/conform ::ps/base "<http://foo.org>")))
    (is (= [[:prefix [:$   [:iri "<http://default.org>"]]]
            [:prefix [:foo [:iri "<http://foo.org>"]]]
            [:prefix [:bar [:iri "<http://bar.org>"]]]]
           (s/conform ::ps/prefixes
                      {:$   "<http://default.org>"
                       :foo "<http://foo.org>"
                       :bar "<http://bar.org>"})))))

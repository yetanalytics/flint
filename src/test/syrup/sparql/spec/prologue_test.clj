(ns syrup.sparql.spec.prologue-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.prologue :as sp]))

(deftest conform-prologue-test
  (testing "Conforming the prologue"
    (is (= [[:base [:iri "<http://foo.org>"]]
            [:base [:iri "<http://bar.org>"]]]
           (s/conform ::sp/bases
                      ["<http://foo.org>" "<http://bar.org>"])))
    (is (= [[:prefix [:$   [:iri "<http://default.org>"]]]
            [:prefix [:foo [:iri "<http://foo.org>"]]]
            [:prefix [:bar [:iri "<http://bar.org>"]]]]
           (s/conform ::sp/prefixes
                      {:$   "<http://default.org>"
                       :foo "<http://foo.org>"
                       :bar "<http://bar.org>"})))))
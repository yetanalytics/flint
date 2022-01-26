(ns syrup.sparql.format.prologue-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [clojure.walk :as w]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.prologue]))

(deftest format-test
  (testing "Formatting prologues"
    (is (= (cstr/join "\n" ["BASE <http://foo.org>"
                            "BASE <http://bar.org>"])
           (->> [[:base [:iri "<http://foo.org>"]]
                 [:base [:iri "<http://bar.org>"]]]
                (w/postwalk f/format-ast)
                (cstr/join "\n"))))
    (is (= (cstr/join "\n" ["PREFIX : <http://default.org>"
                            "PREFIX foo: <http://foo.org>"
                            "PREFIX bar: <http://bar.org>"])
           (->> [[:prefix [:$   [:iri "<http://default.org>"]]]
                 [:prefix [:foo [:iri "<http://foo.org>"]]]
                 [:prefix [:bar [:iri "<http://bar.org>"]]]]
                (w/postwalk f/format-ast)
                (cstr/join "\n"))))))

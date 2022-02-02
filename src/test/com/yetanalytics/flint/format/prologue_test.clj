(ns com.yetanalytics.flint.format.prologue-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [clojure.walk   :as w]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.prologue]))

(defn- format-ast [ast]
  (w/postwalk (partial f/format-ast {:pretty? true}) ast))

(deftest format-test
  (testing "Formatting prologues"
    (is (= (cstr/join "\n" ["BASE <http://foo.org>"
                            "BASE <http://bar.org>"])
           (->> [:bases [[:base [:iri "<http://foo.org>"]]
                         [:base [:iri "<http://bar.org>"]]]]
                format-ast)))
    (is (= (cstr/join "\n" ["PREFIX :     <http://default.org>"
                            "PREFIX foo:  <http://foo.org>"
                            "PREFIX barb: <http://bar.org>"])
           (->> [:prefixes [[:prefix [:$    [:iri "<http://default.org>"]]]
                            [:prefix [:foo  [:iri "<http://foo.org>"]]]
                            [:prefix [:barb [:iri "<http://bar.org>"]]]]]
                format-ast)))))

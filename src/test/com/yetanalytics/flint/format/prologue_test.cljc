(ns com.yetanalytics.flint.format.prologue-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.prologue]))

(defn- format-ast [ast]
  (f/format-ast ast {:pretty? true}))

(deftest format-prologue-test
  (testing "Formatting prologues"
    (is (= "BASE <http://foo.org>"
           (->> [:base [:ax/iri "<http://foo.org>"]]
                format-ast)))
    (is (= (cstr/join "\n" ["PREFIX :     <http://default.org>"
                            "PREFIX foo:  <http://foo.org>"
                            "PREFIX barb: <http://bar.org>"])
           (->> [:prefixes [[:prologue/prefix [:$    [:ax/iri "<http://default.org>"]]]
                            [:prologue/prefix [:foo  [:ax/iri "<http://foo.org>"]]]
                            [:prologue/prefix [:barb [:ax/iri "<http://bar.org>"]]]]]
                format-ast)))))

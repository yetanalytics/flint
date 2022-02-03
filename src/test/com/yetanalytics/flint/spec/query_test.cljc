(ns com.yetanalytics.flint.spec.query-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.query :as qs]))

(deftest conform-query-test
  (testing "conform query"
    ;; Ensure that re-ordering works
    (is (= '[:query/select
               [[:prefixes [[:prologue/prefix [:foo [:ax/iri "<http://example.org/foo/>"]]]]]
                [:select [:select/var-or-exprs [[:ax/var ?x]]]]
                [:from [:ax/iri "<http://example.org/my-graph/>"]]
                [:where [:where-sub/where [[:triple/vec [[:ax/var ?x]
                                                         [:ax/var ?y]
                                                         [:ax/var ?z]]]]]]
                [:order-by [[:mod/asc-desc
                             [[:mod/op asc]
                              [:mod/expr [:expr/terminal [:ax/var ?y]]]]]]]
                [:values [:values/map
                          [[[:ax/var ?z]]
                           [[[:ax/num-lit 1]]]]]]]]
           (s/conform
            qs/query-spec
            '{:values   {[?z] [[1]]}
              :order-by [(asc ?y)]
              :where    [[?x ?y ?z]]
              :from     ["<http://example.org/my-graph/>"]
              :select   [?x]
              :prefixes {:foo "<http://example.org/foo/>"}})))))

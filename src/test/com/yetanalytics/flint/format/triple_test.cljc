(ns com.yetanalytics.flint.format.triple-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.triple]))

(deftest format-triples-test
  (testing "Formatting triples"
    (is (= (cstr/join
            "\n"
            ["<http://example.org/supercalifragilisticexpialidocious> ?p1 ?o1 , ?o2 ;"
             "                                                        ?p2 ?o1 , ?o2 ."
             "?s2 ?p1 ?o1 , ?o2 ;"
             "    ?p2 ?o1 , ?o2 ."])
           (f/format-ast
            '[:triple/nform
              [:triple/spo [[[:ax/iri "<http://example.org/supercalifragilisticexpialidocious>"]
                             [:triple/po
                              [[[:ax/var ?p1]
                                [:triple/o [[:triple/object [:ax/var ?o1]]
                                            [:triple/object [:ax/var ?o2]]]]]
                               [[:ax/var ?p2]
                                [:triple/o [[:triple/object [:ax/var ?o1]]
                                            [:triple/object [:ax/var ?o2]]]]]]]]
                            [[:ax/var ?s2]
                             [:triple/po
                              [[[:ax/var ?p1]
                                [:triple/o [[:triple/object [:ax/var ?o1]]
                                            [:triple/object [:ax/var ?o2]]]]]
                               [[:ax/var ?p2]
                                [:triple/o [[:triple/object [:ax/var ?o1]]
                                            [:triple/object [:ax/var ?o2]]]]]]]]]]]
            {:pretty? true})))
    (is (= "?s ?p ?o ."
           (f/format-ast
            '[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]
            {:pretty? true})))))

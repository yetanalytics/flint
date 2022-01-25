(ns syrup.sparql.format.triple-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [clojure.walk :as w]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.triple]))

(deftest format-test
  (testing "formatting triples"
    (is (= (cstr/join
            "\n"
            ["<http://example.org/supercalifragilisticexpialidocious> ?p1 ?o1 , ?o2 ;"
             "                                                        ?p2 ?o1 , ?o2 ."
             "?s2 ?p1 ?o1 , ?o2 ;"
             "    ?p2 ?o1 , ?o2 ."])
           (w/postwalk
            f/format-ast
            '[:nform [:spo [[[:iri "<http://example.org/supercalifragilisticexpialidocious>"]
                             [:po [[[:var ?p1]
                                    [:o [[:var ?o1] [:var ?o2]]]]
                                   [[:var ?p2]
                                    [:o [[:var ?o1] [:var ?o2]]]]]]]
                            [[:var ?s2]
                             [:po [[[:var ?p1]
                                    [:o [[:var ?o1] [:var ?o2]]]]
                                   [[:var ?p2]
                                    [:o [[:var ?o1] [:var ?o2]]]]]]]]]])))
    (is (= "?s ?p ?o ."
           (w/postwalk
            f/format-ast
            '[:tvec [[:var ?s] [:var ?p] [:var ?o]]])))))

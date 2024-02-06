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
            '[:triple.nform/spo
              [[[:ax/iri "<http://example.org/supercalifragilisticexpialidocious>"]
                [:triple.nform/po
                 [[[:ax/var ?p1]
                   [:triple.nform/o [[:ax/var ?o1]
                                     [:ax/var ?o2]]]]
                  [[:ax/var ?p2]
                   [:triple.nform/o [[:ax/var ?o1]
                                     [:ax/var ?o2]]]]]]]
               [[:ax/var ?s2]
                [:triple.nform/po
                 [[[:ax/var ?p1]
                   [:triple.nform/o [[:ax/var ?o1]
                                     [:ax/var ?o2]]]]
                  [[:ax/var ?p2]
                   [:triple.nform/o [[:ax/var ?o1]
                                     [:ax/var ?o2]]]]]]]]]
            {:pretty? true})))
    (is (= "?s ?p ?o ."
           (f/format-ast
            '[:triple.vec/spo [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]
            {:pretty? true})))
    (is (= "( 1 2 ) :p \"w\" ."
           (f/format-ast
            '[:triple.vec/spo
              [[:triple/list [[:ax/literal 1] [:ax/literal 2]]]
               [:ax/prefix-iri :p]
               [:ax/literal "w"]]]
            {:pretty? true})))
    (is (= "( ?x ?y ) ."
           (f/format-ast
            '[:triple.vec/s
              [[:triple/list [[:ax/var ?x] [:ax/var ?y]]]]]
            {:pretty? true})))
    (is (= "( ?x ?y ) ."
           (f/format-ast
            '[:triple.nform/s
              [[[:triple/list [[:ax/var ?x] [:ax/var ?y]]] []]]]
            {:pretty? true})))
    (is (= "\"v\" :p ( 1 2 ( 3 ) ) ."
           (f/format-ast
            '[:triple.vec/spo
              [[:ax/literal "v"]
               [:ax/prefix-iri :p]
               [:triple/list [[:ax/literal 1]
                              [:ax/literal 2]
                              [:triple/list [[:ax/literal 3]]]]]]]
            {:pretty? true})))
    (is (= "[ foaf:name ?name ;\n  foaf:mbox <mailto:foo@example.com> ] :q \"w\" ."
           (f/format-ast
            '[:triple.vec/spo
              [[:triple/bnodes [[[:ax/prefix-iri :foaf/name]
                                 [:ax/var ?name]]
                                [[:ax/prefix-iri :foaf/mbox]
                                 [:ax/iri "<mailto:foo@example.com>"]]]]
               [:ax/prefix-iri :q]
               [:ax/literal "w"]]]
            {:pretty? true})))
    (is (= "[ foaf:name ?name ; foaf:mbox <mailto:foo@example.com> ] :q \"w\" ."
           (f/format-ast
            '[:triple.vec/spo
              [[:triple/bnodes [[[:ax/prefix-iri :foaf/name]
                                 [:ax/var ?name]]
                                [[:ax/prefix-iri :foaf/mbox]
                                 [:ax/iri "<mailto:foo@example.com>"]]]]
               [:ax/prefix-iri :q]
               [:ax/literal "w"]]]
            {:pretty? false})))
    (is (= "[ foaf:name ?name ; foaf:mbox <mailto:foo@example.com> ] ."
           (f/format-ast
            '[:triple.vec/s
              [[:triple/bnodes [[[:ax/prefix-iri :foaf/name]
                                 [:ax/var ?name]]
                                [[:ax/prefix-iri :foaf/mbox]
                                 [:ax/iri "<mailto:foo@example.com>"]]]]]]
            {:pretty? false})))
    (is (= "[ foo:bar [ foaf:mbox <mailto:foo@example.com> ] ] ."
           (f/format-ast
            '[:triple.vec/s
              [[:triple/bnodes [[[:ax/prefix-iri :foo/bar]
                                 [:triple/bnodes
                                  [[[:ax/prefix-iri :foaf/mbox]
                                    [:ax/iri "<mailto:foo@example.com>"]]]]]]]]]
            {:pretty? true})))
    (is (= "[ foo:bar [ foaf:mbox <mailto:foo@example.com> ] ] ."
           (f/format-ast
            '[:triple.vec/s
              [[:triple/bnodes [[[:ax/prefix-iri :foo/bar]
                                 [:triple/bnodes
                                  [[[:ax/prefix-iri :foaf/mbox]
                                    [:ax/iri "<mailto:foo@example.com>"]]]]]]]]]
            {:pretty? false})))
    (is (= "[] ?p ?o ."
           (f/format-ast
            '[:triple.vec/spo
              [[:triple/bnodes []]
               [:ax/var ?p]
               [:ax/var ?o]]]
            {:pretty? false})))))

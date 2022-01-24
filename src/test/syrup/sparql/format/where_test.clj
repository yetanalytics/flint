(ns syrup.sparql.format.where-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [clojure.walk :as w]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.where]))

(deftest format-test
  (testing "WHERE formatting"
    (is (= (cstr/join "\n" ["{"
                            "    ?s ?p ?o ."
                            "    ?s ?p ?o ."
                            "}"])
           (->> '[:where-sub/where
                  [[:tvec [[:var ?s] [:var ?p] [:var ?o]]]
                   [:tvec [[:var ?s] [:var ?p] [:var ?o]]]]]
                (w/postwalk f/format-ast))))
    (is (= (cstr/join "\n" ["{"
                            "    ?s1 ?p1 ?o1 ."
                            "    ?s2 ?p2 ?o2a, ?o2b"
                            "    {"
                            "        ?s3 ?p3 ?o3 ."
                            "    }"
                            "    {"
                            "        ?s4 ?p4 ?o4 ."
                            "    } UNION {"
                            "        ?s5 ?p5 ?o5 ."
                            "    }"
                            "    OPTIONAL {"
                            "        ?s6 ?p6 ?o6 ."
                            "    }"
                            "    MINUS {"
                            "        ?s7 ?p7 ?o7 ."
                            "    }"
                            "    GRAPH ns:my-graph {"
                            "        ?s8 ?p8 ?o8 ."
                            "    }"
                            "    SERVICE ns:my-uri {"
                            "        ?s9 ?p9 ?o9 ."
                            "    }"
                            "    BIND ((2 + 2) AS ?foo)"
                            "    FILTER (2 = ?bar)"
                            "    FILTER ns:myfn(2, ?baz)"
                            "    VALUES (?x ?y) {"
                            "      (1 2)"
                            "    }"
                            "}"])
           (->> '[:where-sub/where
                  [[:tvec [[:var ?s1] [:var ?p1] [:var ?o1]]]
                   [:nform [:spo [[[:var ?s2]
                                   [:po [[[:var ?p2]
                                          [:o [[:var ?o2a] [:var ?o2b]]]]]]]]]]
                   [:where/recurse
                    [:where-sub/where [[:tvec [[:var ?s3] [:var ?p3] [:var ?o3]]]]]]
                   [:where/union
                    [[:where-sub/where
                      [[:tvec [[:var ?s4] [:var ?p4] [:var ?o4]]]]]
                     [:where-sub/where [[:tvec [[:var ?s5] [:var ?p5] [:var ?o5]]]]]]]
                   [:where/optional
                    [:where-sub/where [[:tvec [[:var ?s6] [:var ?p6] [:var ?o6]]]]]]
                   [:where/minus
                    [:where-sub/where [[:tvec [[:var ?s7] [:var ?p7] [:var ?o7]]]]]]
                   [:where/graph
                    [[:prefix-iri :ns/my-graph]
                     [:where-sub/where [[:tvec [[:var ?s8] [:var ?p8] [:var ?o8]]]]]]]
                   [:where/service
                    [[:prefix-iri :ns/my-uri]
                     [:where-sub/where [[:tvec [[:var ?s9] [:var ?p9] [:var ?o9]]]]]]]
                   [:where/bind
                    [:expr/as-var
                     [[:expr/branch
                       [[:expr/op +]
                        [:expr/args
                         ([:expr/terminal [:num-lit 2]]
                          [:expr/terminal [:num-lit 2]])]]]
                      [:var ?foo]]]]
                   [:where/filter
                    [:expr/branch
                     [[:expr/op =]
                      [:expr/args
                       ([:expr/terminal [:num-lit 2]]
                        [:expr/terminal [:var ?bar]])]]]]
                   [:filter
                    [:expr/branch
                     [[:expr/op ns:myfn]
                      [:expr/args
                       ([:expr/terminal [:num-lit 2]]
                        [:expr/terminal [:var ?baz]])]]]]
                   [:values [:values/map [[[:var ?x] [:var ?y]]
                                          [[[:num-lit 1] [:num-lit 2]]]]]]]]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))))
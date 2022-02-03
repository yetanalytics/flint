(ns com.yetanalytics.flint.format.where-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.where]))

(deftest format-test
  (testing "WHERE formatting"
    (is (= (cstr/join "\n" ["{"
                            "    ?s ?p ?o ."
                            "    ?s ?p ?o ."
                            "}"])
           (-> '[:where-sub/where
                 [[:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]
                  [:triple/vec [[:ax/var ?s] [:ax/var ?p] [:ax/var ?o]]]]]
               (f/format-ast {:pretty? true}))))
    (is (= (cstr/join "\n" ["{"
                            "    ?s1 ?p1 ?o1 ."
                            "    ?s2 ?p2 ?o2a , ?o2b ."
                            "    {"
                            "        ?s3 ?p3 ?o3 ."
                            "    }"
                            "    {"
                            "        ?s4 ?p4 ?o4 ."
                            "    }"
                            "    UNION"
                            "    {"
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
                            "    SERVICE SILENT ns:my-uri {"
                            "        ?s10 ?p10 ?o10 ."
                            "    }"
                            "    BIND ((2 + 2) AS ?foo)"
                            "    FILTER (2 = ?bar)"
                            "    FILTER ns:myfn(2, ?baz)"
                            "    VALUES (?x ?y) {"
                            "        (1 2)"
                            "    }"
                            "}"])
           (-> '[:where-sub/where
                 [[:triple/vec
                   [[:ax/var ?s1] [:ax/var ?p1] [:ax/var ?o1]]]
                  [:triple/nform
                   [:triple/spo [[[:ax/var ?s2]
                                  [:triple/po [[[:ax/var ?p2]
                                                [:triple/o [[:ax/var ?o2a]
                                                            [:ax/var ?o2b]]]]]]]]]]
                  [:where/recurse
                   [:where-sub/where
                    [[:triple/vec [[:ax/var ?s3] [:ax/var ?p3] [:ax/var ?o3]]]]]]
                  [:where/union
                   [[:where-sub/where
                     [[:triple/vec
                       [[:ax/var ?s4] [:ax/var ?p4] [:ax/var ?o4]]]]]
                    [:where-sub/where
                     [[:triple/vec [[:ax/var ?s5] [:ax/var ?p5] [:ax/var ?o5]]]]]]]
                  [:where/optional
                   [:where-sub/where
                    [[:triple/vec [[:ax/var ?s6] [:ax/var ?p6] [:ax/var ?o6]]]]]]
                  [:where/minus
                   [:where-sub/where
                    [[:triple/vec [[:ax/var ?s7] [:ax/var ?p7] [:ax/var ?o7]]]]]]
                  [:where/graph
                   [[:ax/prefix-iri :ns/my-graph]
                    [:where-sub/where
                     [[:triple/vec [[:ax/var ?s8] [:ax/var ?p8] [:ax/var ?o8]]]]]]]
                  [:where/service
                   [[:ax/prefix-iri :ns/my-uri]
                    [:where-sub/where
                     [[:triple/vec [[:ax/var ?s9] [:ax/var ?p9] [:ax/var ?o9]]]]]]]
                  [:where/service-silent
                   [[:ax/prefix-iri :ns/my-uri]
                    [:where-sub/where
                     [[:triple/vec [[:ax/var ?s10] [:ax/var ?p10] [:ax/var ?o10]]]]]]]
                  [:where/bind
                   [:expr/as-var
                    [[:expr/branch
                      [[:expr/op +]
                       [:expr/args
                        ([:expr/terminal [:ax/num-lit 2]]
                         [:expr/terminal [:ax/num-lit 2]])]]]
                     [:ax/var ?foo]]]]
                  [:where/filter
                   [:expr/branch
                    [[:expr/op =]
                     [:expr/args
                      ([:expr/terminal [:ax/num-lit 2]]
                       [:expr/terminal [:ax/var ?bar]])]]]]
                  [:where/filter
                   [:expr/branch
                    [[:expr/op ns:myfn]
                     [:expr/args
                      ([:expr/terminal [:ax/num-lit 2]]
                       [:expr/terminal [:ax/var ?baz]])]]]]
                  [:where/values
                   [:values/map [[[:ax/var ?x] [:ax/var ?y]]
                                 [[[:ax/num-lit 1] [:ax/num-lit 2]]]]]]]]
               (f/format-ast {:pretty? true}))))))

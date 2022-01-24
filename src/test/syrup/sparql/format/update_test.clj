(ns syrup.sparql.format.update-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [clojure.walk :as w]
            [syrup.sparql.format :as f]))

;; TODO: Ban variables from INSERT/DELETE DATA

(deftest format-test
  (testing "format INSERT DATA"
    (is (= (cstr/join "\n" ["INSERT DATA {"
                            "    ?x ?y ?z ."
                            "    ?i ?j ?k ."
                            "}"])
           (->> '[:insert-data-update
                  [:insert-data [[:tvec [[:var ?x] [:var ?y] [:var ?z]]]
                                 [:tvec [[:var ?i] [:var ?j] [:var ?k]]]]]]
                (w/postwalk f/format-ast))))
    (is (= (cstr/join "\n" ["INSERT DATA {"
                            "    ?x ?y ?z ."
                            "    ?i ?j ?k ."
                            "    ?s ?p ?o"
                            "    GRAPH <http://example.org> {"
                            "        ?q ?r ?s ."
                            "    }"
                            "}"])
           (->> '[:insert-data-update
                  [:insert-data
                   [[:tvec [[:var ?x] [:var ?y] [:var ?z]]]
                    [:nform [:spo [[[:var ?i]
                                    [:po [[[:var ?j]
                                           [:o [[:var ?k]]]]]]]
                                   [[:var ?s]
                                    [:po [[[:var ?p]
                                           [:o [[:var ?o]]]]]]]]]]
                    [:quads [:graph
                             [:iri "<http://example.org>"]
                             [:tvec [[:var ?q] [:var ?r] [:var ?s]]]]]]]]
                (w/postwalk f/format-ast)))))
  (testing "format DELETE DATA"
    (is (= (cstr/join "\n" ["INSERT DATA {"
                            "    ?x ?y ?z ."
                            "    ?i ?j ?k ."
                            "}"])
           (->> '[:insert-data-update
                  [:insert-data [[:tvec [[:var ?x] [:var ?y] [:var ?z]]]
                                 [:tvec [[:var ?i] [:var ?j] [:var ?k]]]]]]
                (w/postwalk f/format-ast))))
    (is (= (cstr/join "\n" ["DELETE DATA {"
                            "    ?x ?y ?z ."
                            "    ?i ?j ?k ."
                            "    ?s ?p ?o"
                            "    GRAPH <http://example.org> {"
                            "        ?q ?r ?s ."
                            "    }"
                            "}"])
           (->> '[:delete-data-update
                  [:delete-data
                   [[:tvec [[:var ?x] [:var ?y] [:var ?z]]]
                    [:nform [:spo [[[:var ?i]
                                    [:po [[[:var ?j]
                                           [:o [[:var ?k]]]]]]]
                                   [[:var ?s]
                                    [:po [[[:var ?p]
                                           [:o [[:var ?o]]]]]]]]]]
                    [:quads [:graph
                             [:iri "<http://example.org>"]
                             [:tvec [[:var ?q] [:var ?r] [:var ?s]]]]]]]]
                (w/postwalk f/format-ast)))))
  (testing "format DELETE WHERE"
    (is (= (cstr/join "\n" ["DELETE WHERE {"
                            "    ?x ?y ?z ."
                            "    ?i ?j ?k ."
                            "}"])
           (->> '[:delete-where-update
                  [:delete-where [[:tvec [[:var ?x] [:var ?y] [:var ?z]]]
                                  [:tvec [[:var ?i] [:var ?j] [:var ?k]]]]]]
                (w/postwalk f/format-ast))))
    (is (= (cstr/join "\n" ["DELETE WHERE {"
                            "    ?x ?y ?z ."
                            "    ?i ?j ?k ."
                            "    ?s ?p ?o"
                            "    GRAPH <http://example.org> {"
                            "        ?q ?r ?s ."
                            "    }"
                            "}"])
           (->> '[:delete-where-update
                  [:delete-where
                   [[:tvec [[:var ?x] [:var ?y] [:var ?z]]]
                    [:nform [:spo [[[:var ?i]
                                    [:po [[[:var ?j]
                                           [:o [[:var ?k]]]]]]]
                                   [[:var ?s]
                                    [:po [[[:var ?p]
                                           [:o [[:var ?o]]]]]]]]]]
                    [:quads [:graph
                             [:iri "<http://example.org>"]
                             [:tvec [[:var ?q] [:var ?r] [:var ?s]]]]]]]]
                (w/postwalk f/format-ast)))))
  (testing "format INSERT...DELETE"
    (is (= (cstr/join "\n" ["WITH <http://example.org>"
                            "DELETE {"
                            "    ?x ?y ?z ."
                            "}"
                            "INSERT {"
                            "    ?a ?b ?c ."
                            "}"
                            "USING <http://example.org/2>"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "    ?a ?b ?c"
                            "}"])
           (->> '[:modify-update
                  [[:with [:iri "<http://example.org>"]]
                   [:delete [[:tvec [[:var ?x] [:var ?y] [:var ?z]]]]]
                   [:insert [[:tvec [[:var ?a] [:var ?b] [:var ?c]]]]]
                   [:using [:iri "<http://example.org/2>"]]
                   [:where [:where-sub/where
                            [[:nform
                              [:spo [[[:var ?x]
                                      [:po [[[:var ?y]
                                             [:o [[:var ?z]]]]]]]
                                     [[:var ?a]
                                      [:po [[[:var ?b]
                                             [:o [[:var ?c]]]]]]]]]]]]]]]
                (w/postwalk f/format-ast)))))
  (testing "format graph management updates"
    (is (= "LOAD <http://example.org/1> INTO <http://example.org/2>"
           (->> '[:load-update
                  [[:load [:iri "<http://example.org/1>"]]
                   [:into [:iri "<http://example.org/2>"]]]]
                (w/postwalk f/format-ast))))
    (is (= "CLEAR DEFAULT"
           (->> '[:clear-update
                  [[:clear [:update/kw :default]]]]
                (w/postwalk f/format-ast))))
    (is (= "MOVE <http://example.org/1> TO <http://example.org/2>"
           (->> '[:move-update
                  [[:move [:iri "<http://example.org/1>"]]
                   [:to [:iri "<http://example.org/2>"]]]]
                (w/postwalk f/format-ast))))))
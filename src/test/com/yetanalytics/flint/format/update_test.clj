(ns com.yetanalytics.flint.format.update-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [clojure.walk   :as w]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.update]))

(defn- format-ast [ast]
  (w/postwalk (partial f/format-ast {:pretty? true}) ast))

(deftest format-test
  (testing "format INSERT DATA"
    (is (= (cstr/join "\n" ["INSERT DATA {"
                            "    foo:x dc:title \"Title\" ."
                            "}"])
           (->> '[:insert-data-update
                  [[:insert-data [[:triple/vec [[:prefix-iri :foo/x]
                                                [:prefix-iri :dc/title]
                                                [:str-lit "Title"]]]]]]]
                format-ast))))
  (testing "format DELETE DATA"
    (is (= (cstr/join "\n" ["DELETE DATA {"
                            "    GRAPH <http://example.org> {"
                            "        foo:x dc:title \"Title\" ."
                            "    }"
                            "}"])
           (->> '[:delete-data-update
                  [[:delete-data
                    [[:triple/quads [:graph
                                     [:iri "<http://example.org>"]
                                     [[:triple/vec [[:prefix-iri :foo/x]
                                                    [:prefix-iri :dc/title]
                                                    [:str-lit "Title"]]]]]]]]]]
                format-ast))))
  (testing "format DELETE WHERE"
    (is (= (cstr/join "\n" ["DELETE WHERE {"
                            "    ?x ?y ?z ."
                            "    ?i ?j ?k ."
                            "}"])
           (->> '[:delete-where-update
                  [[:delete-where [[:triple/vec [[:var ?x] [:var ?y] [:var ?z]]]
                                   [:triple/vec [[:var ?i] [:var ?j] [:var ?k]]]]]]]
                format-ast)))
    (is (= (cstr/join "\n" ["DELETE WHERE {"
                            "    ?x ?y ?z ."
                            "    ?i ?j ?k ."
                            "    ?s ?p ?o ."
                            "    GRAPH <http://example.org> {"
                            "        ?q ?r ?s ."
                            "    }"
                            "}"])
           (->> '[:delete-where-update
                  [[:delete-where
                    [[:triple/vec [[:var ?x] [:var ?y] [:var ?z]]]
                     [:triple/nform [:spo [[[:var ?i]
                                     [:po [[[:var ?j]
                                            [:o [[:var ?k]]]]]]]
                                    [[:var ?s]
                                     [:po [[[:var ?p]
                                            [:o [[:var ?o]]]]]]]]]]
                     [:triple/quads [:graph
                                     [:iri "<http://example.org>"]
                                     [[:triple/vec [[:var ?q] [:var ?r] [:var ?s]]]]]]]]]]
                format-ast))))
  (testing "format DELETE...INSERT"
    (is (= (cstr/join "\n" ["INSERT {"
                            "    ?a ?b ?c ."
                            "}"
                            "USING NAMED <http://example.org/2>"
                            "WHERE {"
                            "    ?a ?b ?c ."
                            "}"])
           (->> '[:modify-update
                  [[:insert [[:triple/vec [[:var ?a] [:var ?b] [:var ?c]]]]]
                   [:using [:update/named-iri [:named [:iri "<http://example.org/2>"]]]]
                   [:where [:where-sub/where
                            [[:triple/vec [[:var ?a] [:var ?b] [:var ?c]]]]]]]]
                format-ast)))
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
                            "    ?a ?b ?c ."
                            "}"])
           (->> '[:modify-update
                  [[:with [:iri "<http://example.org>"]]
                   [:delete [[:triple/vec [[:var ?x] [:var ?y] [:var ?z]]]]]
                   [:insert [[:triple/vec [[:var ?a] [:var ?b] [:var ?c]]]]]
                   [:using [:update/iri [:iri "<http://example.org/2>"]]]
                   [:where [:where-sub/where
                            [[:triple/nform
                              [:spo [[[:var ?x]
                                      [:po [[[:var ?y]
                                             [:o [[:var ?z]]]]]]]
                                     [[:var ?a]
                                      [:po [[[:var ?b]
                                             [:o [[:var ?c]]]]]]]]]]]]]]]
                format-ast))))
  (testing "format graph management updates"
    (testing "- LOAD"
      (is (= "LOAD <http://example.org/1>\nINTO <http://example.org/2>"
             (->> '[:load-update
                    [[:load [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:into [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  format-ast)))
      (is (= "LOAD SILENT <http://example.org/1>\nINTO <http://example.org/2>"
             (->> '[:load-update
                    [[:load-silent [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:into [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  format-ast))))
    (testing "- CLEAR"
      (is (= "CLEAR DEFAULT"
             (->> '[:clear-update
                    [[:clear [:update/kw :default]]]]
                  format-ast)))
      (is (= "CLEAR NAMED"
             (->> '[:clear-update
                    [[:clear [:update/kw :named]]]]
                  format-ast)))
      (is (= "CLEAR ALL"
             (->> '[:clear-update
                    [[:clear [:update/kw :all]]]]
                  format-ast)))
      (is (= "CLEAR <http://example.org>"
             (->> '[:clear-update
                    [[:clear [:iri "<http://example.org>"]]]]
                  format-ast)))
      (is (= "CLEAR SILENT <http://example.org>"
             (->> '[:clear-update
                    [[:clear-silent [:iri "<http://example.org>"]]]]
                  format-ast)))
      (is (try (->> '[:clear-update
                      [[:clear [:update/kw :bad]]]]
                    format-ast)
               (catch IllegalArgumentException _ true))))
    (testing "- DROP"
      (is (= "DROP DEFAULT"
             (->> '[:drop-update
                    [[:drop [:update/kw :default]]]]
                  format-ast)))
      (is (= "DROP NAMED"
             (->> '[:drop-update
                    [[:drop [:update/kw :named]]]]
                  format-ast)))
      (is (= "DROP ALL"
             (->> '[:drop-update
                    [[:drop [:update/kw :all]]]]
                  format-ast)))
      (is (= "DROP <http://example.org>"
             (->> '[:drop-update
                    [[:drop [:iri "<http://example.org>"]]]]
                  format-ast)))
      (is (= "DROP SILENT <http://example.org>"
             (->> '[:drop-update
                    [[:drop-silent [:iri "<http://example.org>"]]]]
                  format-ast))))
    (testing "- CREATE"
      (is (= "CREATE <http://example.org>"
             (->> '[:create-update
                    [[:create [:iri "<http://example.org>"]]]]
                  format-ast)))
      (is (= "CREATE SILENT <http://example.org>"
             (->> '[:create-update
                    [[:create-silent [:iri "<http://example.org>"]]]]
                  format-ast))))
    (testing "- ADD"
      (is (= "ADD <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:add-update
                    [[:add [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:to [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  format-ast)))
      (is (= "ADD SILENT <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:add-update
                    [[:add-silent [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:to [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  format-ast))))
    (testing "- COPY"
      (is (= "COPY <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:copy-update
                    [[:copy [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:to [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  format-ast)))
      (is (= "COPY SILENT <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:copy-update
                    [[:copy-silent [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:to [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  format-ast))))
    (testing "- MOVE"
      (is (= "MOVE <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:move-update
                    [[:move [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:to [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  format-ast)))
      (is (= "MOVE SILENT <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:move-update
                    [[:move-silent [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:to [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  format-ast))))))

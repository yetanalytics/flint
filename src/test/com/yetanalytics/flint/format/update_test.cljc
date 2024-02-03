(ns com.yetanalytics.flint.format.update-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.update]))

(defn- format-ast [ast]
  (f/format-ast ast {:pretty? true}))

(deftest format-update-test
  (testing "Formatting INSERT DATA clauses"
    (is (= (cstr/join "\n" ["INSERT DATA {"
                            "    foo:x dc:title \"Title\" ."
                            "}"])
           (->> '[:update/insert-data
                  [[:insert-data [[:triple/vec [[:ax/prefix-iri :foo/x]
                                                [:ax/prefix-iri :dc/title]
                                                [:ax/literal "Title"]]]]]]]
                format-ast))))
  (testing "Formatting DELETE DATA clauses"
    (is (= (cstr/join "\n" ["DELETE DATA {"
                            "    GRAPH <http://example.org> {"
                            "        foo:x dc:title \"Title\" ."
                            "    }"
                            "}"])
           (->> '[:update/delete-data
                  [[:delete-data
                    [[:triple/quads
                      [[:ax/iri "<http://example.org>"]
                       [:triple/quad-triples
                        [[:triple/vec [[:ax/prefix-iri :foo/x]
                                       [:ax/prefix-iri :dc/title]
                                       [:ax/literal "Title"]]]]]]]]]]]
                format-ast))))
  (testing "Formatting DELETE WHERE clauses"
    (is (= (cstr/join "\n" ["DELETE WHERE {"
                            "    ?x ?y ?z ."
                            "    ?i ?j ?k ."
                            "}"])
           (->> '[:update/delete-where
                  [[:delete-where
                    [[:triple/vec [[:ax/var ?x] [:ax/var ?y] [:ax/var ?z]]]
                     [:triple/vec [[:ax/var ?i] [:ax/var ?j] [:ax/var ?k]]]]]]]
                format-ast)))
    (is (= (cstr/join "\n" ["DELETE WHERE {"
                            "    ?x ?y ?z ."
                            "    ?i ?j ?k ."
                            "    ?s ?p ?o ."
                            "    GRAPH <http://example.org> {"
                            "        ?q ?r ?s ."
                            "    }"
                            "}"])
           (->> '[:update/delete-where
                  [[:delete-where
                    [[:triple/vec
                      [[:ax/var ?x] [:ax/var ?y] [:ax/var ?z]]]
                     [:triple/nform
                      [:triple/spo [[[:ax/var ?i]
                                     [:triple/po [[[:ax/var ?j]
                                                   [:triple/o [[:triple/object [:ax/var ?k]]]]]]]]
                                    [[:ax/var ?s]
                                     [:triple/po [[[:ax/var ?p]
                                                   [:triple/o [[:triple/object [:ax/var ?o]]]]]]]]]]]
                     [:triple/quads
                      [[:ax/iri "<http://example.org>"]
                       [:triple/quad-triples
                        [[:triple/vec [[:ax/var ?q] [:ax/var ?r] [:ax/var ?s]]]]]]]]]]]
                format-ast))))
  (testing "Formatting DELETE...INSERT clauses"
    (is (= (cstr/join "\n" ["INSERT {"
                            "    ?a ?b ?c ."
                            "}"
                            "USING NAMED <http://example.org/2>"
                            "WHERE {"
                            "    ?a ?b ?c ."
                            "}"])
           (->> '[:update/modify
                  [[:insert
                    [[:triple/vec [[:ax/var ?a] [:ax/var ?b] [:ax/var ?c]]]]]
                   [:using
                    [:update/named-iri [:named [:ax/iri "<http://example.org/2>"]]]]
                   [:where
                    [:where-sub/where
                     [[:where/triple
                       [:triple/vec [[:ax/var ?a] [:ax/var ?b] [:ax/var ?c]]]]]]]]]
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
           (->> '[:update/modify
                  [[:with [:ax/iri "<http://example.org>"]]
                   [:delete [[:triple/vec [[:ax/var ?x] [:ax/var ?y] [:ax/var ?z]]]]]
                   [:insert [[:triple/vec [[:ax/var ?a] [:ax/var ?b] [:ax/var ?c]]]]]
                   [:using [:update/iri [:ax/iri "<http://example.org/2>"]]]
                   [:where [:where-sub/where
                            [[:where/triple
                              [:triple/nform
                               [:triple/spo
                                [[[:ax/var ?x]
                                  [:triple/po [[[:ax/var ?y]
                                                [:triple/o [[:triple/object [:ax/var ?z]]]]]]]]
                                 [[:ax/var ?a]
                                  [:triple/po [[[:ax/var ?b]
                                                [:triple/o [[:triple/object [:ax/var ?c]]]]]]]]]]]]]]]]]
                format-ast))))
  (testing "Formatting graph management updates"
    (testing "- LOAD"
      (is (= "LOAD <http://example.org/1>\nINTO GRAPH <http://example.org/2>"
             (->> '[:update/load
                    [[:load [:ax/iri "<http://example.org/1>"]]
                     [:into [:update/graph [:graph [:ax/iri "<http://example.org/2>"]]]]]]
                  format-ast)))
      (is (= "LOAD SILENT <http://example.org/1>\nINTO GRAPH <http://example.org/2>"
             (->> '[:update/load
                    [[:load-silent [:ax/iri "<http://example.org/1>"]]
                     [:into [:update/graph [:graph [:ax/iri "<http://example.org/2>"]]]]]]
                  format-ast))))
    (testing "- CLEAR"
      (is (= "CLEAR DEFAULT"
             (->> '[:update/clear
                    [[:clear [:update/default :default]]]]
                  format-ast)))
      (is (= "CLEAR NAMED"
             (->> '[:update/clear
                    [[:clear [:update/named :named]]]]
                  format-ast)))
      (is (= "CLEAR ALL"
             (->> '[:update/clear
                    [[:clear [:update/all :all]]]]
                  format-ast)))
      (is (= "CLEAR GRAPH <http://example.org>"
             (->> '[:update/clear
                    [[:clear [:update/graph [:graph [:ax/iri "<http://example.org>"]]]]]]
                  format-ast)))
      (is (= "CLEAR SILENT GRAPH <http://example.org>"
             (->> '[:update/clear
                    [[:clear-silent [:update/graph [:graph [:ax/iri "<http://example.org>"]]]]]]
                  format-ast))))
    (testing "- DROP"
      (is (= "DROP DEFAULT"
             (->> '[:update/drop
                    [[:drop [:update/default :default]]]]
                  format-ast)))
      (is (= "DROP NAMED"
             (->> '[:update/drop
                    [[:drop [:update/named :named]]]]
                  format-ast)))
      (is (= "DROP ALL"
             (->> '[:update/drop
                    [[:drop [:update/all :all]]]]
                  format-ast)))
      (is (= "DROP GRAPH <http://example.org>"
             (->> '[:update/drop
                    [[:drop [:update/graph [:graph [:ax/iri "<http://example.org>"]]]]]]
                  format-ast)))
      (is (= "DROP SILENT GRAPH <http://example.org>"
             (->> '[:update/drop
                    [[:drop-silent [:update/graph [:graph [:ax/iri "<http://example.org>"]]]]]]
                  format-ast))))
    (testing "- CREATE"
      (is (= "CREATE GRAPH <http://example.org>"
             (->> '[:update/create
                    [[:create [:update/graph [:graph [:ax/iri "<http://example.org>"]]]]]]
                  format-ast)))
      (is (= "CREATE SILENT GRAPH <http://example.org>"
             (->> '[:update/create
                    [[:create-silent [:update/graph [:graph [:ax/iri "<http://example.org>"]]]]]]
                  format-ast))))
    (testing "- ADD"
      (is (= "ADD <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:update/add
                    [[:add [:update/graph-notag [:ax/iri "<http://example.org/1>"]]]
                     [:to [:update/graph-notag [:ax/iri "<http://example.org/2>"]]]]]
                  format-ast)))
      (is (= "ADD SILENT <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:update/add
                    [[:add-silent [:update/graph-notag [:ax/iri "<http://example.org/1>"]]]
                     [:to [:update/graph-notag [:ax/iri "<http://example.org/2>"]]]]]
                  format-ast))))
    (testing "- COPY"
      (is (= "COPY <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:update/copy
                    [[:copy [:update/graph-notag [:ax/iri "<http://example.org/1>"]]]
                     [:to [:update/graph-notag [:ax/iri "<http://example.org/2>"]]]]]
                  format-ast)))
      (is (= "COPY SILENT <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:update/copy
                    [[:copy-silent [:update/graph-notag [:ax/iri "<http://example.org/1>"]]]
                     [:to [:update/graph-notag [:ax/iri "<http://example.org/2>"]]]]]
                  format-ast))))
    (testing "- MOVE"
      (is (= "MOVE <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:update/move
                    [[:move [:update/graph-notag [:ax/iri "<http://example.org/1>"]]]
                     [:to [:update/graph-notag [:ax/iri "<http://example.org/2>"]]]]]
                  format-ast)))
      (is (= "MOVE SILENT <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:update/move
                    [[:move-silent [:update/graph-notag [:ax/iri "<http://example.org/1>"]]]
                     [:to [:update/graph-notag [:ax/iri "<http://example.org/2>"]]]]]
                  format-ast))))))

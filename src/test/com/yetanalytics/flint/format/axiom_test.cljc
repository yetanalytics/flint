(ns com.yetanalytics.flint.format.axiom-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]))

(deftest axiom-format
  (testing "Formatting terminal AST nodes"
    (is (= "<http://example.org>"
           (f/format-ast-node {} [:ax/iri "<http://example.org>"])))
    (is (= "foo:bar"
           (f/format-ast-node {} [:ax/prefix-iri :foo/bar])))
    (is (= "?xyz"
           (f/format-ast-node {} [:ax/var '?xyz])))
    (is (= "_:b0"
           (f/format-ast-node {} [:ax/bnode '_b0])))
    (is (= "[]"
           (f/format-ast-node {} [:ax/bnode '_])))
    (is (= "*"
           (f/format-ast-node {} [:ax/wildcard '*])))
    (is (= "*"
           (f/format-ast-node {} [:ax/wildcard :*])))
    (is (= "a"
           (f/format-ast-node {} [:ax/rdf-type 'a])))
    (is (= "a"
           (f/format-ast-node {} [:ax/rdf-type :a])))
    (is (= "\"My String\""
           (f/format-ast-node {} [:ax/str-lit "My String"])))
    (is (= "\"foo\\nbar\\rbaz\""
           (f/format-ast-node {} [:ax/str-lit "foo\\nbar\\rbaz"])))
    (is (= "\"My String\"@en"
           (f/format-ast-node {} [:ax/lmap-lit {:en "My String"}])))
    (is (= "123"
           (f/format-ast-node {} [:ax/num-lit 123])))
    (is (= "123.4"
           (f/format-ast-node {} [:ax/num-lit 123.4])))
    (is (= "true"
           (f/format-ast-node {} [:ax/bool-lit true])))
    (is (= (str #?(:clj "\"2022-01-20T16:22:19Z\""
                   :cljs "\"2022-01-20T16:22:19.000Z\"")
                "^^<http://www.w3.org/2001/XMLSchema#dateTime>")
           (f/format-ast-node {} [:ax/dt-lit #inst "2022-01-20T16:22:19Z"])))
    (is (= (str #?(:clj "\"2022-01-20T16:22:19Z\""
                   :cljs "\"2022-01-20T16:22:19.000Z\"")
                "^^xsd:dateTime")
           (f/format-ast-node {:xsd-prefix "xsd"}
                              [:ax/dt-lit #inst "2022-01-20T16:22:19Z"]))))
  (testing "DateTime formatting works on all `inst?` timestamps"
    #?(:clj (let [ts-str-1 "\"2022-01-20T16:22:19Z\"^^xsd:dateTime"
                  ts-str-2 "\"1970-01-01T00:00:00Z\"^^xsd:dateTime"
                  literal  #inst "2022-01-20T16:22:19Z"
                  instant  (java.time.Instant/parse "2022-01-20T16:22:19Z")
                  date     (java.util.Date/from instant)
                  fmt-ts   (fn [ts] (f/format-ast-node {:xsd-prefix "xsd"}
                                                       [:ax/dt-lit ts]))]
              (is (= ts-str-1 (fmt-ts literal)))
              (is (= ts-str-1 (fmt-ts instant)))
              (is (= ts-str-1 (fmt-ts date)))
              (is (= ts-str-2 (fmt-ts (java.sql.Timestamp. 0))))
              (is (= ts-str-2 (fmt-ts (java.sql.Time. 0))))
              (is (= ts-str-2 (fmt-ts (java.sql.Date. 0)))))
       :cljs (is (string?
                  (f/format-ast-node {:xsd-prefix "xsd"}
                                     [:ax/dt-lit (js/Date.)]))))))

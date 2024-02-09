(ns com.yetanalytics.flint.format.axiom-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.yetanalytics.flint.axiom.iri :as iri]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom])
  #?(:clj (:import [java.time
                    Instant
                    LocalDate LocalTime LocalDateTime
                    OffsetTime OffsetDateTime
                    ZonedDateTime])))

(deftest axiom-format
  (testing "Formatting terminal AST nodes"
    (is (= "<http://example.org>"
           (f/format-ast-node {} [:ax/iri "<http://example.org>"])))
    (is (= "foo"
           (f/format-ast-node {} [:ax/prefix :foo])))
    (is (= "foo:bar"
           (f/format-ast-node {} [:ax/prefix-iri :foo/bar])))
    (is (= ":bar"
           (f/format-ast-node {} [:ax/prefix-iri :bar])))
    (is (= "?xyz"
           (f/format-ast-node {} [:ax/var '?xyz])))
    (is (= "_:b0"
           (f/format-ast-node {} [:ax/bnode '_b0])))
    (is (= "[]"
           (f/format-ast-node {} [:ax/bnode '_])))
    (is (= "_:___"
           (f/format-ast-node {} [:ax/bnode '____])))
    (is (= "*"
           (f/format-ast-node {} [:ax/wildcard '*])))
    (is (= "*"
           (f/format-ast-node {} [:ax/wildcard :*])))
    (is (= "a"
           (f/format-ast-node {} [:ax/rdf-type 'a])))
    (is (= "a"
           (f/format-ast-node {} [:ax/rdf-type :a])))
    (is (= "\"My String\""
           (f/format-ast-node {} [:ax/literal "My String"])))
    (is (= "\"foo\\nbar\\rbaz\""
           (f/format-ast-node {} [:ax/literal "foo\\nbar\\rbaz"])))
    (is (= "\"My String\"@en"
           (f/format-ast-node {} [:ax/literal {:en "My String"}])))
    (is (= "123"
           (f/format-ast-node {} [:ax/literal 123])))
    (is (= "123.4"
           (f/format-ast-node {} [:ax/literal 123.4])))
    (is (= "true"
           (f/format-ast-node {} [:ax/literal true])))
    (is (= (str #?(:clj "\"2022-01-20T16:22:19Z\""
                   :cljs "\"2022-01-20T16:22:19.000Z\"")
                "^^<http://www.w3.org/2001/XMLSchema#dateTime>")
           (f/format-ast-node {} [:ax/literal #inst "2022-01-20T16:22:19Z"])))
    (is (= (str #?(:clj "\"2022-01-20T16:22:19Z\""
                   :cljs "\"2022-01-20T16:22:19.000Z\"")
                "^^xsd:dateTime")
           (f/format-ast-node {:iri-prefix-m {iri/xsd-iri-prefix "xsd"}}
                              [:ax/literal #inst "2022-01-20T16:22:19Z"]))))
  (testing "DateTime formatting works on all `inst?` timestamps"
    #?(:clj (let [ts-str-1 "\"2022-01-20T16:22:19Z\"^^xsd:dateTime"
                  ts-str-2 "\"1970-01-01T00:00:00Z\"^^xsd:dateTime"
                  ts-str-3 "\"1970-01-01\"^^xsd:date"
                  ts-str-4 "\"00:00:00Z\"^^xsd:time"
                  literal-1 #inst "2022-01-20T16:22:19Z"
                  literal-2 #inst "1970-01-01T00:00:00Z"
                  instant-1 (Instant/parse "2022-01-20T16:22:19Z")
                  instant-2 (Instant/parse "1970-01-01T00:00:00Z")
                  date     (java.util.Date/from instant-1)
                  fmt-ts   (fn [ts] (f/format-ast-node
                                     {:iri-prefix-m {iri/xsd-iri-prefix "xsd"}}
                                     [:ax/literal ts]))]
              (is (= ts-str-1 (fmt-ts literal-1)))
              (is (= ts-str-2 (fmt-ts literal-2)))
              (is (= ts-str-1 (fmt-ts instant-1)))
              (is (= ts-str-2 (fmt-ts instant-2)))
              (is (= ts-str-1 (fmt-ts date)))
              (is (= ts-str-2 (fmt-ts (java.sql.Timestamp. 0))))
              (is (= ts-str-3 (fmt-ts (java.sql.Date. 0))))
              (is (= ts-str-4 (fmt-ts (java.sql.Time. 0)))))
       :cljs (is (string?
                  (f/format-ast-node {:iri-prefix-m {iri/xsd-iri-prefix "xsd"}}
                                     [:ax/literal (js/Date.)])))))
  #?(:clj
     (testing "DateTime formatting work on all java.time instances"
       ;; Note that here, zeroed-out seconds are preserved
       (is (= "\"2022-01-20T16:22:00\"^^xsd:dateTime"
              (f/format-ast-node
               {:iri-prefix-m {iri/xsd-iri-prefix "xsd"}}
               [:ax/literal (LocalDateTime/parse "2022-01-20T16:22:00")])))
       (is (= "\"2022-01-20T16:22:00-05:00\"^^xsd:dateTime"
              (f/format-ast-node
               {:iri-prefix-m {iri/xsd-iri-prefix "xsd"}}
               [:ax/literal (ZonedDateTime/parse "2022-01-20T16:22:00-05:00")])))
       (is (= "\"2022-01-20T16:22:00-05:00\"^^xsd:dateTime"
              (f/format-ast-node
               {:iri-prefix-m {iri/xsd-iri-prefix "xsd"}}
               [:ax/literal (OffsetDateTime/parse "2022-01-20T16:22:00-05:00")])))
       (is (= "\"16:22:00\"^^xsd:time"
              (f/format-ast-node
               {:iri-prefix-m {iri/xsd-iri-prefix "xsd"}}
               [:ax/literal (LocalTime/parse "16:22:00")])))
       (is (= "\"16:22:00-05:00\"^^xsd:time"
              (f/format-ast-node
               {:iri-prefix-m {iri/xsd-iri-prefix "xsd"}}
               [:ax/literal (OffsetTime/parse "16:22:00-05:00")])))
       (is (= "\"2022-01-20\"^^xsd:date"
              (f/format-ast-node
               {:iri-prefix-m {iri/xsd-iri-prefix "xsd"}}
               [:ax/literal (LocalDate/parse "2022-01-20")]))))))

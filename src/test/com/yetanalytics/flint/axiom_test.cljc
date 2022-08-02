(ns com.yetanalytics.flint.axiom-test
  "Tests for axiom protocol implementations.
   This is mainly to test basic protocol functionality. For more detailed
   validation and formatting tests, see the `flint.spec.axiom-test` and
   `flint.format.axiom-test` namespaces, respectively."
  (:require [clojure.test :refer [deftest testing is are]]
            [com.yetanalytics.flint.axiom.iri :as iri]
            [com.yetanalytics.flint.axiom.protocol :as p]
            [com.yetanalytics.flint.axiom.impl]
            [com.yetanalytics.flint :as flint]))

(deftest axiom-protocol-test
  (testing "IRIs"
    (is (not (p/-valid-iri? "http://foo.org")))
    (is (= "<http://foo.org>"
           (p/-format-iri "<http://foo.org>")))
    #?(:clj
       (is (= true
              (p/-valid-iri? "<http://foo.org>")
              (p/-valid-iri? (java.net.URL. "http://foo.org"))
              (p/-valid-iri? (java.net.URI. "http://foo.org"))))
       :cljs
       (is (= true
              (p/-valid-iri? "<http://foo.org/>")
              (p/-valid-iri? (js/URL. "http://foo.org")))))
    #?(:clj
       (is (= "<http://foo.org>"
              (p/-format-iri "<http://foo.org>")
              (p/-format-iri (java.net.URL. "http://foo.org"))
              (p/-format-iri (java.net.URI. "http://foo.org"))))
       :cljs
       (is (= "<http://foo.org/>" ; js/URL auto-adds the final slash
              (p/-format-iri "<http://foo.org/>")
              (p/-format-iri (js/URL. "http://foo.org")))))
    #?(:clj
       (is (= "http://foo.org"
              (p/-unwrap-iri "<http://foo.org>")
              (p/-unwrap-iri (java.net.URL. "http://foo.org"))
              (p/-unwrap-iri (java.net.URI. "http://foo.org"))))
       :cljs
       (is (= "http://foo.org/bar#"
              (p/-unwrap-iri "<http://foo.org/bar#>")
              (p/-unwrap-iri (js/URL. "http://foo.org/bar#"))))))
  (testing "Prefixes"
    (is (p/-valid-prefix? :foo))
    (is (p/-valid-prefix? :$))
    (is (= "foo" (p/-format-prefix :foo)))
    (is (= "" (p/-format-prefix :$))))
  (testing "Prefixed IRIs"
    (is (p/-valid-prefix-iri? :foo/bar))
    (is (= "foo:bar" (p/-format-prefix-iri :foo/bar))))
  (testing "Variables"
    (is (p/-valid-variable? '?foo))
    (is (= "?foo" (p/-format-variable '?foo))))
  (testing "Blank Nodes"
    (is (p/-valid-bnode? '_bar))
    (is (= "_:bar" (p/-format-bnode '_bar))))
  (testing "Wildcards"
    (is (p/-valid-wildcard? :*))
    (is (p/-valid-wildcard? '*))
    (is (= "*"
           (p/-format-wildcard :*)
           (p/-format-wildcard '*))))
  (testing "RDF Type Shorthands"
    (is (p/-valid-rdf-type? :a))
    (is (p/-valid-rdf-type? 'a))
    (is (= "a"
           (p/-format-rdf-type :a)
           (p/-format-rdf-type 'a))))
  (testing "String Literals"
    (is (p/-valid-literal? "http://foo.org"))
    ;; As this is a valid IRI string, it would be validated as an IRI before
    ;; as a literal, depending on the order in `s/or`.
    (is (p/-valid-literal? "<http://foo.org>"))
    (is (= "\"blue\"" (p/-format-literal "blue")))
    (is (= "black" (p/-format-literal-strval "black")))
    (is (= "\"yellow\"^^xsd:string"
           (p/-format-literal "yellow"
                              {:force-iri?   true
                               :iri-prefix-m {iri/xsd-iri-prefix :xsd}})))
    (is (= "<http://www.w3.org/2001/XMLSchema#string>"
           (p/-format-literal-url "green")))
    (is (= "xsd:string"
           (p/-format-literal-url "white" {:iri-prefix-m {iri/xsd-iri-prefix :xsd}})))
    (is (nil? (p/-format-literal-lang-tag "red"))))
  (testing "Lang Map Literals"
    (is (p/-valid-literal? {:en "Foo"}))
    (is (= "\"Bar\"@en" (p/-format-literal {:en "Bar"})))
    (is (= "<http://www.w3.org/1999/02/22-rdf-syntax-ns#langString>"
           (p/-format-literal-url {:en "Baz"})))
    (is (= "rdf:langString"
           (p/-format-literal-url {:en "Bazz"}
                                  {:iri-prefix-m {iri/rdf-iri-prefix :rdf}})))
    (is (= "en" (p/-format-literal-lang-tag {:en "Qux"})))
    (is (= "Boo" (p/-format-literal-strval {:en "Boo"})))) 
  (testing "Numeric Literals"
    (is (p/-valid-literal? 2))
    (is (p/-valid-literal? 2.0))
    (is (= "2" (p/-format-literal 2)))
    (is (= #?(:clj "2.0" :cljs "2") (p/-format-literal 2.0)))
    (is (nil? (p/-format-literal-lang-tag 2)))
    (is (nil? (p/-format-literal-lang-tag 2.0)))
    (is (= "\"2\"^^xsd:integer"
           (p/-format-literal (int 2)
                              {:force-iri?   true
                               :iri-prefix-m {iri/xsd-iri-prefix :xsd}})))
    #?(:clj
       (is (= "2.0"
              (p/-format-literal 2.0)
              (p/-format-literal (float 2.0)))))
    #?(:clj
       (is (= "2"
              (p/-format-literal 2)
              (p/-format-literal (int 2))
              (p/-format-literal (short 2))
              (p/-format-literal (byte 2))
              (p/-format-literal java.math.BigInteger/TWO)
              (p/-format-literal (java.math.BigDecimal. 2.0)))))
    #?(:cljs
       (is (= "2"
              (p/-format-literal 2.0)
              (p/-format-literal 2))))
    #?(:clj
       (are [s n]
            (= s (p/-format-literal n {:force-iri?   true
                                       :iri-prefix-m {iri/xsd-iri-prefix :xsd}}))
         "\"2.0\"^^xsd:double" 2.0
         "\"2.0\"^^xsd:float" (float 2.0)
         "\"2\"^^xsd:long" 2
         "\"2\"^^xsd:integer" (int 2)
         "\"2\"^^xsd:short" (short 2)
         "\"2\"^^xsd:byte" (byte 2)
         "\"2\"^^xsd:decimal" (java.math.BigDecimal. 2.0)
         "\"2\"^^xsd:integer" java.math.BigInteger/TWO)
       :cljs
       (are [s n]
            (= s (p/-format-literal n {:force-iri?   true
                                       :iri-prefix-m {iri/xsd-iri-prefix :xsd}}))
         "\"2\"^^xsd:integer" 2
         "\"2\"^^xsd:integer" 2.0
         "\"2.1\"^^xsd:double" 2.1))
    #?(:clj
       (are [url n]
            (= url (p/-format-literal-url n))
         "<http://www.w3.org/2001/XMLSchema#double>" 2.0
         "<http://www.w3.org/2001/XMLSchema#float>" (float 2.0)
         "<http://www.w3.org/2001/XMLSchema#long>" 2
         "<http://www.w3.org/2001/XMLSchema#integer>" (int 2)
         "<http://www.w3.org/2001/XMLSchema#short>" (short 2)
         "<http://www.w3.org/2001/XMLSchema#byte>" (byte 2)
         "<http://www.w3.org/2001/XMLSchema#decimal>" (java.math.BigDecimal. 2.0)
         "<http://www.w3.org/2001/XMLSchema#integer>" java.math.BigInteger/TWO)
       :cljs
       (are [url n]
            (= url (p/-format-literal-url n))
         "<http://www.w3.org/2001/XMLSchema#integer>" 2
         "<http://www.w3.org/2001/XMLSchema#integer>" 2.0
         "<http://www.w3.org/2001/XMLSchema#double>" 3.14)))
  (testing "Boolean Literals"
    (is (p/-valid-literal? true))
    (is (= "true" (p/-format-literal true)))
    (is (= "\"true\"^^xsd:boolean"
           (p/-format-literal true
                              {:force-iri?   true
                               :iri-prefix-m {iri/xsd-iri-prefix :xsd}})))
    (is (= "<http://www.w3.org/2001/XMLSchema#boolean>"
           (p/-format-literal-url true)))
    (is (nil? (p/-format-literal-lang-tag true))))
  (testing "Date/Time Literals:"
    (testing " -valid-literal?"
      #?(:clj
         (are [ts]
              (p/-valid-literal? ts)
           (java.time.Instant/EPOCH)
           (java.util.Date. 0)
           (java.sql.Date. 0)
           (java.sql.Time. 0))
         :cljs
         (is (p/-valid-literal? (js/Date. 0)))))
    (testing "p/-format-literal"
      #?(:clj
         (is (= "\"1970-01-01T00:00:00Z\"^^<http://www.w3.org/2001/XMLSchema#dateTime>"
                (p/-format-literal (java.time.Instant/EPOCH))
                (p/-format-literal (java.util.Date. 0))
                (p/-format-literal (java.sql.Date. 0))
                (p/-format-literal (java.sql.Time. 0))))
         :cljs
         (is (= "\"1970-01-01T00:00:00.000Z\"^^<http://www.w3.org/2001/XMLSchema#dateTime>"
                (p/-format-literal (js/Date. 0))))))
    (testing "p/-format-literal with prefix"
      #?(:clj
         (is (= "\"1970-01-01T00:00:00Z\"^^xsd:dateTime"
                (p/-format-literal (java.time.Instant/EPOCH)
                                   {:iri-prefix-m
                                    {"http://www.w3.org/2001/XMLSchema#" :xsd}})
                (p/-format-literal (java.util.Date. 0)
                                   {:iri-prefix-m
                                    {"http://www.w3.org/2001/XMLSchema#" :xsd}})
                (p/-format-literal (java.sql.Date. 0)
                                   {:iri-prefix-m
                                    {"http://www.w3.org/2001/XMLSchema#" :xsd}})
                (p/-format-literal (java.sql.Time. 0)
                                   {:iri-prefix-m
                                    {"http://www.w3.org/2001/XMLSchema#" :xsd}})))
         :cljs
         (is (= "\"1970-01-01T00:00:00.000Z\"^^xsd:dateTime"
                (p/-format-literal (js/Date. 0)
                                   {:iri-prefix-m
                                    {"http://www.w3.org/2001/XMLSchema#" :xsd}})))))
    (testing "p/-format-literal-strval"
      #?(:clj
         (is (= "1970-01-01T00:00:00Z"
                (p/-format-literal-strval (java.time.Instant/EPOCH))
                (p/-format-literal-strval (java.util.Date. 0))
                (p/-format-literal-strval (java.sql.Date. 0))
                (p/-format-literal-strval (java.sql.Time. 0))))
         :cljs
         (is (= "1970-01-01T00:00:00.000Z"
                (p/-format-literal-strval (js/Date. 0))))))
    (testing "p/-format-literal-lang-tag"
      #?(:clj
         (is (= nil
                (p/-format-literal-lang-tag (java.time.Instant/EPOCH))
                (p/-format-literal-lang-tag (java.util.Date. 0))
                (p/-format-literal-lang-tag (java.sql.Date. 0))
                (p/-format-literal-lang-tag (java.sql.Time. 0))))
         :cljs
         (is (nil? (p/-format-literal-lang-tag (js/Date. 0))))))
    (testing "p/-format-literal-url"
      #?(:clj
         (is (= "<http://www.w3.org/2001/XMLSchema#dateTime>"
                (p/-format-literal-url (java.time.Instant/EPOCH))
                (p/-format-literal-url (java.util.Date. 0))
                (p/-format-literal-url (java.sql.Date. 0))
                (p/-format-literal-url (java.sql.Time. 0))))
         :cljs
         (is (= "<http://www.w3.org/2001/XMLSchema#dateTime>"
                (p/-format-literal-url (js/Date. 0))))))))

(deftest integration-tests
  (testing "Queries with non-string IRIs"
    (is (= "SELECT ?x ?z WHERE { ?x <http://foo.org/> ?z . }"
           #?(:clj (flint/format-query
                    {:select ['?x '?z]
                     :where  [['?x (java.net.URL. "http://foo.org/") '?z]]}))
           #?(:clj (flint/format-query
                    {:select ['?x '?z]
                     :where  [['?x (java.net.URI. "http://foo.org/") '?z]]}))
           #?(:cljs (flint/format-query
                     {:select ['?x '?z]
                      :where  [['?x (js/URL. "http://foo.org/") '?z]]}))))
    #?(:clj (is (= "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> PREFIX foo: <http://foo.org/> SELECT ?x WHERE { ?x foo:time \"1970-01-01T00:00:00Z\"^^xsd:dateTime . }"
                   (flint/format-query
                    {:prefixes {:xsd (java.net.URL. "http://www.w3.org/2001/XMLSchema#")
                                :foo (java.net.URL. "http://foo.org/")}
                     :select   ['?x]
                     :where    [['?x :foo/time (java.time.Instant/EPOCH)]]})
                   (flint/format-query
                    {:prefixes {:xsd (java.net.URI. "http://www.w3.org/2001/XMLSchema#")
                                :foo (java.net.URI. "http://foo.org/")}
                     :select   ['?x]
                     :where    [['?x :foo/time (java.util.Date. 0)]]})))
       :cljs (is (= "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> PREFIX foo: <http://foo.org/> SELECT ?x WHERE { ?x foo:time \"1970-01-01T00:00:00.000Z\"^^xsd:dateTime . }"
                    (flint/format-query
                     {:prefixes {:xsd (js/URL. "http://www.w3.org/2001/XMLSchema#")
                                 :foo (js/URL. "http://foo.org/")}
                      :select ['?x]
                      :where  [['?x :foo/time (js/Date. 0)]]})))))
  (testing "Queries with forced literal IRIs"
    (is (= "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?x WHERE { ?x ?y \"Blah Blah Blah\"^^xsd:string . }"
           (flint/format-query
            {:prefixes {:xsd "<http://www.w3.org/2001/XMLSchema#>"}
             :select   ['?x]
             :where    [['?x '?y "Blah Blah Blah"]]}
            :force-iris? true)))
    ;; long vs integer
    (is (= #?(:clj "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?x WHERE { ?x ?y \"2\"^^xsd:long . }"
              :cljs "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?x WHERE { ?x ?y \"2\"^^xsd:integer . }")
           (flint/format-query
            {:prefixes {:xsd "<http://www.w3.org/2001/XMLSchema#>"}
             :select   ['?x]
             :where    [['?x '?y 2]]}
            :force-iris? true)))
    (testing "- does not affect lang maps"
      (is (= "SELECT ?x WHERE { ?x ?y \"Lorem Ipsum\"@lat . }"
             (flint/format-query
              {:select   ['?x]
               :where    [['?x '?y {:lat "Lorem Ipsum"}]]}
              :force-iris? true))))
    (testing "- does not affect timestamps"
      #?(:clj
         (is (= "SELECT ?x WHERE { ?x ?y \"1970-01-01T00:00:00Z\"^^<http://www.w3.org/2001/XMLSchema#dateTime> . }"
                (flint/format-query
                 {:select   ['?x]
                  :where    [['?x '?y java.time.Instant/EPOCH]]}
                 :force-iris? true)
                (flint/format-query
                 {:select   ['?x]
                  :where    [['?x '?y java.time.Instant/EPOCH]]}
                 :force-iris? false)))
         :cljs
         (is (= "SELECT ?x WHERE { ?x ?y \"1970-01-01T00:00:00.000Z\"^^<http://www.w3.org/2001/XMLSchema#dateTime> . }"
                (flint/format-query
                 {:select   ['?x]
                  :where    [['?x '?y (js/Date. 0)]]}
                 :force-iris? true)
                (flint/format-query
                 {:select   ['?x]
                  :where    [['?x '?y (js/Date. 0)]]}
                 :force-iris? false)))))))

(def custom-literal (reify p/Literal
                      (-valid-literal? [_] true)
                      (-format-literal [_] "\"custom\"^^<http://foo.org>")
                      (-format-literal [_ _] "\"custom\"^^<http://foo.org>")
                      (-format-literal-strval [_] "custom")
                      (-format-literal-lang-tag [_] nil)
                      (-format-literal-url [_] "<http://foo.org>")
                      (-format-literal-url [_ _] "<http://foo.org>")))

(defrecord Rational [numerator denominator]
  p/Literal
  (p/-valid-literal? [_rational]
    (and (int? numerator)
         (int? denominator)
         (not (zero? denominator))))
  (p/-format-literal [rational]
    (p/-format-literal rational {}))
  (p/-format-literal [rational opts]
    (str "\"" (p/-format-literal-strval rational)
         "\"^^" (p/-format-literal-url rational opts)))
  (p/-format-literal-strval [_rational]
    (str numerator "/" denominator))
  (p/-format-literal-lang-tag [_rational]
    nil)
  (p/-format-literal-url [rational]
    (p/-format-literal-url rational {}))
  (p/-format-literal-url [_rational {:keys [iri-prefix-m]}]
    (if-some [prefix (get iri-prefix-m "http://foo.org/literals#")]
      (str (name prefix) ":rational")
      "<http://foo.org/literals#rational>")))

(deftest protocol-extension-test
  (testing "Custom literal via protocol extension"
    (is (p/-valid-literal? (->Rational 2 3)))
    (is (not (p/-valid-literal? (->Rational 2 0))))
    (is (= "\"3/4\"^^<http://foo.org/literals#rational>"
           (p/-format-literal (->Rational 3 4))))
    (is (= "SELECT ?x WHERE { ?x ?y \"5/6\"^^<http://foo.org/literals#rational> . }"
           (flint/format-query
            {:select ['?x]
             :where [['?x '?y (->Rational 5 6)]]})))
    (is (= "PREFIX foo: <http://foo.org/literals#> SELECT ?x WHERE { ?x ?y \"7/8\"^^foo:rational . }"
           (flint/format-query
            {:prefixes {:foo "<http://foo.org/literals#>"}
             :select   ['?x]
             :where    [['?x '?y (->Rational 7 8)]]})))))

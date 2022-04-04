(ns com.yetanalytics.flint.spec.axiom-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.yetanalytics.flint.spec.axiom :as ax]))

;; NOTE: Technically non-ASCII chars (e.g. hanzi, latin with diacritics) are
;; valid in variables, prefixed IRIs, and bnodes in the SPARQL spec,
;; but we exclude them in this lib for simplicity.

(deftest axiom-test
  (testing "IRIs"
    (is (ax/iri? "<http://example.org>"))
    (is (ax/iri? "<http://example.org/%20>"))
    (is (not (ax/iri? "<http://example.org")))
    (is (not (ax/iri? "http://example.org")))
    (testing "- even non-IRIs count due to BASE"
      (is (ax/iri? "<foo>")))
    (testing "- spaces and quotes are not allowed"
      (is (not (ax/iri? "<foo bar>")))
      (is (not (ax/iri? "<http://example.org/\"bar\"")))))
  (testing "prefixed IRIs"
    (is (ax/prefix-iri? :bar))
    (is (ax/prefix-iri? :foo/bar))
    (is (ax/prefix-iri? :foo/bar-baz))
    (is (ax/prefix-iri? :foo-baz/bar))
    (is (not (ax/prefix-iri? "foo:bar")))
    (testing " - char edge cases"
      (is (not (ax/prefix-iri? :foo/bar#baz)))
      (is (not (ax/prefix-iri? :foo#baz/bar)))
      (is (not (ax/prefix-iri? :foo/bar'baz)))
      (is (not (ax/prefix-iri? :foo'baz/bar)))
      (is (not (ax/prefix-iri? :你好))))
    (testing "- not reserved keywords"
      (is (not (ax/prefix-iri? :a)))
      (is (not (ax/prefix-iri? :*)))))
  (testing "variables"
    (is (ax/variable? '?foo))
    (is (not (ax/variable? "?foo")))
    (is (not (ax/variable? 'foo)))
    (testing "- only ASCII question mark is allowed"
      (is (not (ax/variable? '？foo))))
    (testing "- char edge cases"
      (is (ax/variable? '?foo0))
      (is (ax/variable? '?0000))
      (is (not (ax/variable? '?foo.bar)))
      (is (not (ax/variable? '?.foobar)))
      (is (not (ax/variable? '?foo#bar)))
      (is (not (ax/variable? '?foo'bar)))
      (is (not (ax/variable? '?你好)))))
  (testing "blank nodes"
    (is (ax/bnode? '_))
    (is (ax/bnode? '_1))
    (is (ax/bnode? '_foo))
    (is (ax/bnode? '_foo.bar))
    (is (not (ax/bnode? 'foo)))
    (is (not (ax/bnode? '_.foobar)))
    (is (not (ax/bnode? '_你好))))
  (testing "strings"
    (is (ax/valid-string? "foo bar"))
    (testing "- double quotes"
      (is (not (ax/valid-string? "\"")))
      (is (ax/valid-string? "\\\"")))
    (testing "- back slashes"
      (is (not (ax/valid-string? "\\")))
      (is (ax/valid-string? "\\\\")))
    (testing "- line breaks"
      (is (not (ax/valid-string? "\n")))
      (is (not (ax/valid-string? "\\\n")))
      (is (ax/valid-string? "\\n"))
      (is (ax/valid-string? "\\\\n")))
    (testing "- carriage returns"
      (is (not (ax/valid-string? "\r")))
      (is (not (ax/valid-string? "\\\r")))
      (is (ax/valid-string? "\\r"))
      (is (ax/valid-string? "\\\\r")))
    (testing "- other escape input"
      (is (ax/valid-string? "\\t"))
      (is (ax/valid-string? "\\b"))
      (is (ax/valid-string? "\\f"))
      (is (not (ax/valid-string? "\\a")))
      (is (not (ax/valid-string? "\\2")))))
  (testing "lang maps"
    (is (ax/lang-map? {:en "foo"}))
    (is (ax/lang-map? {:not-a-real-ltag "bar"}))
    (is (not (ax/lang-map? {:en "\"foo\""})))))

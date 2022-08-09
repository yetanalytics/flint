(ns com.yetanalytics.flint.spec.axiom-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]))

;; NOTE: Technically non-ASCII chars (e.g. hanzi, latin with diacritics) are
;; valid in variables, prefixed IRIs, and bnodes in the SPARQL spec,
;; but we exclude them in this lib for simplicity.

(deftest axiom-test
  (testing "IRIs"
    (is (s/valid? ax/iri-spec "<http://example.org>"))
    (is (s/valid? ax/iri-spec "<http://example.org/%20>"))
    (is (not (s/valid? ax/iri-spec "<http://example.org")))
    (is (not (s/valid? ax/iri-spec "http://example.org")))
    (testing "- even non-IRIs count due to BASE"
      (is (s/valid? ax/iri-spec "<foo>")))
    (testing "- spaces and quotes are not allowed"
      (is (not (s/valid? ax/iri-spec "<foo bar>")))
      (is (not (s/valid? ax/iri-spec "<http://example.org/\"bar\"")))))
  (testing "prefixed IRIs"
    (is (s/valid? ax/prefix-iri-spec :bar))
    (is (s/valid? ax/prefix-iri-spec :foo/bar))
    (is (s/valid? ax/prefix-iri-spec :foo/bar-baz))
    (is (s/valid? ax/prefix-iri-spec :foo-baz/bar))
    (is (not (s/valid? ax/prefix-iri-spec "foo:bar")))
    (testing " - char edge cases"
      (is (not (s/valid? ax/prefix-iri-spec :foo/bar#baz)))
      (is (not (s/valid? ax/prefix-iri-spec :foo#baz/bar)))
      (is (not (s/valid? ax/prefix-iri-spec :foo/bar'baz)))
      (is (not (s/valid? ax/prefix-iri-spec :foo'baz/bar)))
      ;; Unicode
      (is (s/valid? ax/prefix-iri-spec :你好)))
    (testing "- not reserved keywords"
      (is (not (s/valid? ax/prefix-iri-spec :a)))
      (is (not (s/valid? ax/prefix-iri-spec :*)))))
  (testing "variables"
    (is (s/valid? ax/variable-spec '?foo))
    (is (not (s/valid? ax/variable-spec "?foo")))
    (is (not (s/valid? ax/variable-spec 'foo)))
    (testing "- only ASCII question mark is allowed"
      (is (not (s/valid? ax/variable-spec '？foo))))
    (testing "- char edge cases"
      (is (s/valid? ax/variable-spec '?foo0))
      (is (s/valid? ax/variable-spec '?0000))
      (is (not (s/valid? ax/variable-spec '?foo.bar)))
      (is (not (s/valid? ax/variable-spec '?.foobar)))
      (is (not (s/valid? ax/variable-spec '?foo#bar)))
      (is (not (s/valid? ax/variable-spec '?foo'bar)))
      ;; Unicode
      (is (s/valid? ax/variable-spec '?你好))))
  (testing "blank nodes"
    (is (s/valid? ax/bnode-spec '_))
    (is (s/valid? ax/bnode-spec '_1))
    (is (s/valid? ax/bnode-spec '_foo))
    (is (s/valid? ax/bnode-spec '_foo.bar))
    (is (not (s/valid? ax/bnode-spec 'foo)))
    (is (not (s/valid? ax/bnode-spec '_.foobar)))
    ;; Unicode
    (is (s/valid? ax/bnode-spec '_你好)))
  (testing "strings"
    (is (s/valid? ax/literal-spec "foo bar"))
    (testing "- double quotes"
      (is (not (s/valid? ax/literal-spec "\"")))
      (is (s/valid? ax/literal-spec "\\\"")))
    (testing "- back slashes"
      (is (not (s/valid? ax/literal-spec "\\")))
      (is (s/valid? ax/literal-spec "\\\\")))
    (testing "- line breaks"
      (is (not (s/valid? ax/literal-spec "\n")))
      (is (not (s/valid? ax/literal-spec "\\\n")))
      (is (s/valid? ax/literal-spec "\\n"))
      (is (s/valid? ax/literal-spec "\\\\n")))
    (testing "- carriage returns"
      (is (not (s/valid? ax/literal-spec "\r")))
      (is (not (s/valid? ax/literal-spec "\\\r")))
      (is (s/valid? ax/literal-spec "\\r"))
      (is (s/valid? ax/literal-spec "\\\\r")))
    (testing "- other escape input"
      (is (s/valid? ax/literal-spec "\\t"))
      (is (s/valid? ax/literal-spec "\\b"))
      (is (s/valid? ax/literal-spec "\\f"))
      (is (not (s/valid? ax/literal-spec "\\a")))
      (is (not (s/valid? ax/literal-spec "\\2")))))
  (testing "lang maps"
    (is (s/valid? ax/literal-spec {:en "foo"}))
    (is (s/valid? ax/literal-spec {:not-a-real-ltag "bar"}))
    (is (not (s/valid? ax/literal-spec {:en "\"foo\""})))))

(ns syrup.sparql.spec.axiom-test
  (:require [clojure.test :refer [deftest testing is]]
            [syrup.sparql.spec.axiom :as ax]))

(deftest axiom-test
  (testing "IRIs"
    (is (ax/iri? "<http://example.org>"))
    (is (not (ax/iri? "<http://example.org")))
    (is (not (ax/iri? "http://example.org")))
    (testing "- even non-IRIs count due to BASE"
      (is (ax/iri? "<foo>"))
      (is (not (ax/iri? "<foo bar>")))))
  (testing "strings"
    (is (ax/valid-string? "foo bar"))
    (testing "- unsanitary input rejected"
      (is (not (ax/valid-string? "\"foo\"")))
      (is (not (ax/valid-string? "foo\\bar")))
      (is (not (ax/valid-string? "foo\nbar")))
      (is (not (ax/valid-string? "foo\rbar"))))
    (testing "- escaped input ok"
      (is (ax/valid-string? "\\\"foo\\\""))
      (is (ax/valid-string? "foo\\\\bar"))
      (is (ax/valid-string? "foo\\\nbar"))
      (is (ax/valid-string? "foo\\\rbar"))))
  (testing "lang maps"
    (is (ax/lang-map? {:en "foo"}))
    (is (ax/lang-map? {:not-a-real-ltag "bar"}))
    (is (not (ax/lang-map? {:en "\"foo\""})))))

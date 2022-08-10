(ns com.yetanalytics.flint.spec.axiom-test
  "Axiom validation tests, both at impl and spec level."
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.spec.alpha :as s]
            [clojure.string :as cstr]
            [com.yetanalytics.flint.axiom.impl.validation :as v]
            [com.yetanalytics.flint.spec.axiom :as ax])
  #?(:cljs (:require-macros [com.yetanalytics.flint.axiom.validation-test
                             :refer [multilingual-test]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generative Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Test to ensure that clj string validation fns have the same results as
;; pattern matching against the regexes.

;; gen/string only generates strings with ASCII-range chars
(def ^:private gen-string
  "Generator for a random Unicode string."
  (->> (gen/choose 0 0xFFFF)
       (gen/fmap char)
       gen/vector
       (gen/fmap cstr/join)))

(defn make-prop
  [str-coerce str-regex str-pred?]
  (prop/for-all [rs gen-string]
                (let [x (str-coerce rs)
                      match? (boolean (re-matches str-regex (name x)))
                      valid? (str-pred? x)]
                  (= match? valid?))))

(deftest string-validation-gentest
  (testing "variables"
    (let [var-prop (make-prop (fn [s] (symbol (str "?" s)))
                              v/var-regex
                              v/valid-var-symbol?)]
      (is (:pass? (tc/quick-check 100 var-prop)))))
  (testing "blank nodes"
    (let [bnode-prop (make-prop (fn [s] (symbol (str "_" s)))
                                v/bnode-regex
                                v/valid-bnode-symbol?)]
      (is (:pass? (tc/quick-check 100 bnode-prop)))))
  (testing "IRI strings"
    (let [iri-prop (make-prop identity
                              v/iri-body-regex
                              v/valid-iri-string?*)]
      (is (:pass? (tc/quick-check 100 iri-prop))))
    (let [iri-prop (make-prop (fn [s] (str "<" s ">"))
                              v/iri-regex
                              v/valid-iri-string?)]
      (is (:pass? (tc/quick-check 100 iri-prop)))))
  (testing "Prefix strings"
    (let [prefix-prop (make-prop keyword
                                 v/prefix-ns-regex
                                 v/valid-prefix-keyword?)]
      (is (:pass? (tc/quick-check 100 prefix-prop))))
    (let [prefix-prop (make-prop keyword
                                 v/prefix-name-regex
                                 v/valid-prefix-iri-keyword?)]
      (is (:pass? (tc/quick-check 100 prefix-prop)))))
  (testing "String literals"
    (let [literal-prop (make-prop identity
                                  v/literal-regex
                                  v/valid-string-literal?)]
      (is (:pass? (tc/quick-check 100 literal-prop))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Unit Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:clj
   (defmacro multilingual-test
     [test-fn]
     `(are [x#] (~test-fn x#)
        ;; ASCII
        "x"
        "foo_bar"
        "supercalifragilistiexpialidocious"
        "abc12345"
        ;; Latin w/ diacritics
        "Eren_Jäger"
        "Hange_Zoë"
        "ÊMIA"
        "Kevin_Nguyễn"
        ;; Chars only allowed in the middle of words
        "u·v"
        ~(str "Tie" \u0308 "sto") ; Tiësto
        ;; Other alphabets
        "Слава_Україні"
        "Ἀριστοτέλης"
        "حيفا"
        "חֵיפָה"
        "इस्रो"
        "เด็กใหม่"
        ;; CJK characters
        "進撃の巨人"
        "소녀시대"
        "少女时代"
        "少女時代"
        "しょうじょじだい")))

(def ^:private biang-biang-noodles
  "Test string that includes supplemental CJK characters."
  (str (char 0xd883) (char 0xdedd) ; Java only supports UTF-16 => surrogate pairs
       (char 0xd883) (char 0xdedd)
       "面"))

(deftest string-validation-unit-test
  (testing "variable strings"
    (multilingual-test
     (fn [x] (v/valid-var-symbol? (symbol (str "?" x)))))
    (are [x] (not (v/valid-var-symbol? (symbol (str "?" x))))
      "???"
      "foo bar"
      "foo.bar"
      ".foobar"
      "foo'bar"
      "foo#bar"
      (str "foo" (char 0x037E) "bar")
      (str \u0308)
      "·t"
      biang-biang-noodles)
    (is (v/valid-var-symbol? '?1234567890))
    (is (not (v/valid-var-symbol? 'foo)))
    (is (not (v/valid-var-symbol? '？foo))))
  (testing "blank node strings"
    (multilingual-test
     (fn [x] (v/valid-bnode-symbol? (symbol (str "_" x)))))
    (are [x] (not (v/valid-bnode-symbol? (symbol (str "_" x))))
      "foo bar"
      (str "foo" (char 0x037E) "bar")
      (str \u0308)
      "·t"
      biang-biang-noodles)
    (is (v/valid-bnode-symbol? '______))
    (is (v/valid-bnode-symbol? '______x))
    (is (v/valid-bnode-symbol? '______1.x.y))
    (is (not (v/valid-bnode-symbol? '______x.)))
    (is (not (v/valid-bnode-symbol? '_._____x)))
    (is (v/valid-bnode-symbol? '__.____x))
    (is (not (v/valid-bnode-symbol? 'x))))
  (testing "IRI strings"
    (multilingual-test
     (fn [x] (v/valid-iri-string?* x)))
    (multilingual-test
     (fn [x] (v/valid-iri-string? (str "<" x ">"))))
    (are [x] (not (v/valid-iri-string? (str "<" x ">")))
      "foo  bar"
      "foo\nbar"
      "foo\rbar"
      "foo\tbar"
      "<extra-brackets>"
      "\\\""
      "^"
      "{}"
      "|"
      "http://example.org/\"bar\"")
    (is (v/valid-iri-string? (str "<" biang-biang-noodles ">")))
    (is (v/valid-iri-string? "<http://example.org/%20>"))
    (is (s/valid? ax/iri-spec "<foo>"))
    (is (not (v/valid-iri-string? "<http://example.org")))
    (is (not (v/valid-iri-string? "http://example.org>"))))
  (testing "IRI prefixes"
    (multilingual-test
     (fn [x] (v/valid-prefix-keyword? (keyword x))))
    (multilingual-test
     (fn [x] (v/valid-prefix-iri-keyword? (keyword x))))
    (multilingual-test
     (fn [x] (v/valid-prefix-iri-keyword? (keyword x x))))
    (are [x] (v/valid-prefix-iri-keyword? x)
      :bar
      :foo/bar
      :foo/bar-baz
      :foo-baz/bar)
    (are [x] (not (v/valid-prefix-iri-keyword? x))
      :foo/bar#baz
      :foo#baz/bar
      :foo/bar'baz
      :foo'baz/bar)
    (testing "- percent encoding"
      (is (v/valid-prefix-iri-keyword? :foo%80bar))
      (is (not (v/valid-prefix-iri-keyword? :foo%80bar/qux)))
      (is (not (v/valid-prefix-iri-keyword? :foo%8)))
      (is (not (v/valid-prefix-iri-keyword? :foo%8Gbar)))))
  (testing "String literals"
    (multilingual-test
     (fn [x] (v/valid-string-literal? x)))
    (are [x y] (= y (v/valid-string-literal? x))
      "\""    false
      "\\\""  true
      "\\"    false
      "\\\\"  true
      "\n"    false
      "\\n"   true
      "\\\n"  false
      "\\\\n" true
      "\r"    false
      "\\r"   true
      "\\\r"  false
      "\\\\r" true
      "\\t"   true
      "\\b"   true
      "\\f"   true
      "\\a"   false
      "\\2"   false)
    (is (not (v/valid-string-literal? "\"\n\r\t\"")))
    (is (v/valid-string-literal? "\\\"\\n\\r\\t\\\""))))

(deftest spec-test
  (testing "IRIs"
    (is (s/valid? ax/iri-spec "<http://example.org>"))
    (is (not (s/valid? ax/iri-spec "http://example.org")))
    (is (s/valid? ax/iri-spec
                  #?(:clj (java.net.URI. "http://example.org")
                     :cljs (js/URL. "http://example.org")))))
  (testing "prefixed IRIs"
    (is (s/valid? ax/prefix-iri-spec :foo/bar))
    (is (not (s/valid? ax/prefix-iri-spec "foo:bar")))
    (testing "- not reserved keywords"
      (is (not (s/valid? ax/prefix-iri-spec :a)))
      (is (not (s/valid? ax/prefix-iri-spec :*)))))
  (testing "variables"
    (is (s/valid? ax/variable-spec '?foo))
    (is (not (s/valid? ax/variable-spec "?foo")))
    (is (not (s/valid? ax/variable-spec 'foo))))
  (testing "blank nodes"
    (is (s/valid? ax/bnode-spec '_))
    (is (s/valid? ax/bnode-spec '_foo))
    (is (not (s/valid? ax/bnode-spec 'foo))))
  (testing "string literals"
    (is (s/valid? ax/literal-spec "foo bar"))
    (is (s/valid? ax/literal-spec "\\\"\\n\\r\\t\\\""))
    (is (not (s/valid? ax/literal-spec "\"\n\r\t\""))))
  (testing "lang map literals"
    (is (s/valid? ax/literal-spec {:en "foo"}))
    (is (s/valid? ax/literal-spec {:not-a-real-ltag "bar"}))
    (is (not (s/valid? ax/literal-spec {:en "\"foo\""})))))

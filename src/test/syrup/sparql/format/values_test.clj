(ns syrup.sparql.format.values-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [clojure.walk :as w]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.values]))

(deftest format-test
  (testing "Formatting VALUES clause"
    (is (= (cstr/join "\n" ["?foo {"
                            "  1"
                            "  2"
                            "  3"
                            "}"])
           (w/postwalk f/format-ast
                       [:values/map [[[:var '?foo]]
                                     [[[:num-lit 1]]
                                      [[:num-lit 2]]
                                      [[:num-lit 3]]]]])))
    (is (= (cstr/join "\n" ["(?foo ?bar) {"
                            "  (1 'a')"
                            "  (2 'b')"
                            "  (3 'c')"
                            "}"])
           (w/postwalk f/format-ast
                       [:values/map [[[:var '?foo] [:var '?bar]]
                                     [[[:num-lit 1] [:str-lit "a"]]
                                      [[:num-lit 2] [:str-lit "b"]]
                                      [[:num-lit 3] [:str-lit "c"]]]]])))
    (is (= (cstr/join "\n" ["(?foo ?bar) {"
                            "  (UNDEF 'a')"
                            "  (2 UNDEF)"
                            "}"])
           (w/postwalk f/format-ast
                       [:values/map [[[:var '?foo] [:var '?bar]]
                                     [[[:values/undef nil] [:str-lit "a"]]
                                      [[:num-lit 2] [:values/undef nil]]]]])))))
(ns com.yetanalytics.flint.format.values-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.values]))

(defn- format-ast [ast]
  (f/format-ast ast {:pretty? true}))

(deftest format-values-test
  (testing "Formatting VALUES clauses"
    (is (= (cstr/join "\n" ["?foo {"
                            "    1"
                            "    2"
                            "    3"
                            "}"])
           (format-ast [:values/map [[[:ax/var '?foo]]
                                     [[[:ax/num-lit 1]]
                                      [[:ax/num-lit 2]]
                                      [[:ax/num-lit 3]]]]])))
    (is (= (cstr/join "\n" ["(?foo ?bar) {"
                            "    (1 \"a\")"
                            "    (2 \"b\")"
                            "    (3 \"c\")"
                            "}"])
           (format-ast [:values/map [[[:ax/var '?foo] [:ax/var '?bar]]
                                     [[[:ax/num-lit 1] [:ax/str-lit "a"]]
                                      [[:ax/num-lit 2] [:ax/str-lit "b"]]
                                      [[:ax/num-lit 3] [:ax/str-lit "c"]]]]])))
    (is (= (cstr/join "\n" ["(?foo ?bar) {"
                            "    (UNDEF \"a\")"
                            "    (2 UNDEF)"
                            "}"])
           (format-ast [:values/map [[[:ax/var '?foo] [:ax/var '?bar]]
                                     [[[:values/undef nil] [:ax/str-lit "a"]]
                                      [[:ax/num-lit 2] [:values/undef nil]]]]])))))

(ns syrup.sparql.format.path-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.walk :as w]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.path]))

(deftest format-test
  (testing "formatting paths"
    (is (= "!foo:bar | ^baz:qux / quu:bee"
         (->> '[:path-branch
                {:op    alt
                 :paths [[:path-branch
                          {:op    not
                           :paths [[:path-terminal [:prefix-iri :foo/bar]]]}]
                         [:path-branch
                          {:op    cat
                           :paths [[:path-branch
                                    {:op    inv
                                     :paths [[:path-terminal
                                              [:prefix-iri :baz/qux]]]}]
                                   [:path-terminal
                                    [:prefix-iri :quu/bee]]]}]]}]
              (w/postwalk f/annotate-ast)
              (w/postwalk f/format-ast))))
    (is (= "!foo:bar / (^baz:qux | quu:bee)"
           (->> '[:path-branch
                  {:op    cat
                   :paths [[:path-branch
                            {:op    not
                             :paths [[:path-terminal [:prefix-iri :foo/bar]]]}]
                           [:path-branch
                            {:op    alt
                             :paths [[:path-branch
                                      {:op    inv
                                       :paths [[:path-terminal
                                                [:prefix-iri :baz/qux]]]}]
                                     [:path-terminal
                                      [:prefix-iri :quu/bee]]]}]]}]
                (w/postwalk f/annotate-ast)
                (w/postwalk f/format-ast))))))
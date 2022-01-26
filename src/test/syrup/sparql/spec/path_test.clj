(ns syrup.sparql.spec.path-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]
            [syrup.sparql.spec.path :as ps]))

(deftest conform-path-test
  (testing "Path"
    (testing "terminals"
      (is (= [:path/terminal [:iri "<http://example.org>"]]
             (s/conform ::ps/path "<http://example.org>")))
      (is (= [:path/terminal [:prefix-iri :foo/bar]]
             (s/conform ::ps/path :foo/bar)))
      (is (= [:path/terminal [:rdf-type 'a]]
             (s/conform ::ps/path 'a))))
    (testing "branch structure"
      (are [path]
           (= [:path/branch [[:path/args [[:path/terminal [:prefix-iri :foo/bar]]
                                          [:path/terminal [:prefix-iri :baz/qux]]]]]]
              (update (s/conform ::ps/path path) 1 subvec 1 2))
        '(alt :foo/bar :baz/qux)
        '(cat :foo/bar :baz/qux))
      (are [path]
           (= [:path/branch [[:path/args [[:path/terminal [:prefix-iri :foo/bar]]]]]]
              (update (s/conform ::ps/path path) 1 subvec 1 2))
        '(inv :foo/bar)
        '(not :foo/bar)
        '(? :foo/bar)
        '(* :foo/bar)
        '(+ :foo/bar)))
    (testing "branch operation"
      (are [op path]
           (= op
              (get-in (s/conform ::ps/path path) [1 0 1]))
        'alt '(alt :foo/bar :baz/qux)
        'cat '(cat :foo/bar :baz/qux)
        'inv '(inv :foo/bar)
        'not '(not :foo/bar)
        '? '(? :foo/bar)
        '* '(* :foo/bar)
        '+ '(+ :foo/bar)))
    (testing "branch nesting"
      (is (= [:path/branch
              [[:path/op 'alt]
               [:path/args
                [[:path/branch
                  [[:path/op 'not]
                   [:path/args [[:path/terminal [:prefix-iri :foo/bar]]]]]]
                 [:path/branch
                  [[:path/op 'cat]
                   [:path/args [[:path/branch
                                 [[:path/op 'inv]
                                  [:path/args [[:path/terminal
                                                [:prefix-iri :baz/qux]]]]]]
                                [:path/terminal
                                 [:prefix-iri :quu/bee]]]]]]]]]]
             (s/conform
              ::ps/path
              '(alt (not :foo/bar) (cat (inv :baz/qux) :quu/bee))))))))

(deftest invalid-path-test
  (testing "Path error data"
    (is (= {::s/problems [{:path [:path/terminal :iri]
                           :pred `ax/iri?
                           :val  2
                           :via  [::ps/path]
                           :in   []}
                          {:path [:path/terminal :prefix-iri]
                           :pred `ax/prefix-iri?
                           :val  2
                           :via  [::ps/path]
                           :in   []}
                          {:path [:path/terminal :rdf-type]
                           :pred `ax/rdf-type?
                           :val  2
                           :via  [::ps/path]
                           :in   []}
                          {:path [:path/branch]
                           :pred `list?
                           :val  2
                           :via  [::ps/path]
                           :in   []}]
            ::s/spec ::ps/path
            ::s/value 2}
           (s/explain-data ::ps/path 2)))
    (is (= {::s/problems [{:path [:path/terminal]
                           :pred `(comp not list?)
                           :val  '(:foo :bar/baz)
                           :via  [::ps/path]
                           :in   []}
                          {:path [:path/branch]
                           :pred `(comp symbol? first)
                           :val  '(:foo :bar/baz)
                           :via  [::ps/path]
                           :in   []}]
            ::s/spec ::ps/path
            ::s/value '(:foo :bar/baz)}
           (s/explain-data ::ps/path '(:foo :bar/baz))))
    (is (= {::s/problems [{:path [:path/terminal]
                           :pred `(comp not list?)
                           :val  '(foo :bar/baz)
                           :via  [::ps/path]
                           :in   []}
                          {:path [:path/branch :varardic :op]
                           :pred '#{'cat 'alt}
                           :val  'foo
                           :via  [::ps/path]
                           :in   [0]}
                          {:path [:path/branch :unary :op]
                           :pred '#{'inv '+ '* '?}
                           :val  'foo
                           :via  [::ps/path]
                           :in   [0]}
                          {:path [:path/branch :unary-neg :op]
                           :pred '#{'not}
                           :val  'foo
                           :via  [::ps/path]
                           :in   [0]}]
            ::s/spec ::ps/path
            ::s/value '(foo :bar/baz)}
           (s/explain-data ::ps/path '(foo :bar/baz))))
    (is (= {::s/problems [{:path [:path/terminal]
                           :pred `(comp not list?)
                           :val  '(not (cat :foo/bar :bar/baz))
                           :via  [::ps/path]
                           :in   []}
                          {:path [:path/branch :varardic :op]
                           :pred '#{'cat 'alt}
                           :val  'not
                           :via  [::ps/path]
                           :in   [0]}
                          {:path [:path/branch :unary :op]
                           :pred '#{'inv '+ '* '?}
                           :val  'not
                           :via  [::ps/path]
                           :in   [0]}
                          {:path [:path/branch :unary-neg :path :path/terminal]
                           :pred `(comp not list?)
                           :val  '(cat :foo/bar :bar/baz)
                           :via  [::ps/path ::ps/path-neg ::ps/path-neg]
                           :in   [1]}
                          {:path [:path/branch :unary-neg :path :path/branch :varardic :op]
                           :pred '#{'alt}
                           :val  'cat
                           :via  [::ps/path ::ps/path-neg ::ps/path-neg]
                           :in   [1 0]}]
            ::s/spec ::ps/path
            ::s/value '(not (cat :foo/bar :bar/baz))}
           (s/explain-data ::ps/path '(not (cat :foo/bar :bar/baz)))))))

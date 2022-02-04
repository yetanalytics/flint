(ns com.yetanalytics.flint.spec.path-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]
            [com.yetanalytics.flint.spec.path  :as ps]))

(deftest conform-path-test
  (testing "Conform paths"
    (testing "- terminals"
      (is (= [:path/terminal [:ax/iri "<http://example.org>"]]
             (s/conform ::ps/path "<http://example.org>")))
      (is (= [:path/terminal [:ax/prefix-iri :foo/bar]]
             (s/conform ::ps/path :foo/bar)))
      (is (= [:path/terminal [:ax/rdf-type 'a]]
             (s/conform ::ps/path 'a))))
    (testing "- branch structure"
      (are [path]
           (= [:path/branch
               [[:path/paths [[:path/terminal [:ax/prefix-iri :foo/bar]]
                              [:path/terminal [:ax/prefix-iri :baz/qux]]]]]]
              (update (s/conform ::ps/path path) 1 subvec 1 2))
        '(alt :foo/bar :baz/qux)
        '(cat :foo/bar :baz/qux))
      (are [path]
           (= [:path/branch [[:path/paths [[:path/terminal [:ax/prefix-iri :foo/bar]]]]]]
              (update (s/conform ::ps/path path) 1 subvec 1 2))
        '(inv :foo/bar)
        '(not :foo/bar)
        '(? :foo/bar)
        '(* :foo/bar)
        '(+ :foo/bar)))
    (testing "- branch operation"
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
               [:path/paths
                [[:path/branch
                  [[:path/op 'not]
                   [:path/paths [[:path/terminal [:ax/prefix-iri :foo/bar]]]]]]
                 [:path/branch
                  [[:path/op 'cat]
                   [:path/paths [[:path/branch
                                  [[:path/op 'inv]
                                   [:path/paths [[:path/terminal
                                                  [:ax/prefix-iri :baz/qux]]]]]]
                                 [:path/terminal
                                  [:ax/prefix-iri :quu/bee]]]]]]]]]]
             (s/conform
              ::ps/path
              '(alt (not :foo/bar) (cat (inv :baz/qux) :quu/bee))))))))

(deftest invalid-path-test
  (testing "Invalid paths"
    (is (= {::s/problems [{:path [:path/terminal :ax/iri]
                           :pred `ax/iri?
                           :val  2
                           :via  [::ps/path]
                           :in   []}
                          {:path [:path/terminal :ax/prefix-iri]
                           :pred `ax/prefix-iri?
                           :val  2
                           :via  [::ps/path]
                           :in   []}
                          {:path [:path/terminal :ax/rdf-type]
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
                          {:path [:path/branch :path/varardic :path/op]
                           :pred '#{'cat 'alt}
                           :val  'foo
                           :via  [::ps/path]
                           :in   [0]}
                          {:path [:path/branch :path/unary :path/op]
                           :pred '#{'inv '+ '* '?}
                           :val  'foo
                           :via  [::ps/path]
                           :in   [0]}
                          {:path [:path/branch :path/unary-neg :path/op]
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
                          {:path [:path/branch :path/varardic :path/op]
                           :pred '#{'cat 'alt}
                           :val  'not
                           :via  [::ps/path]
                           :in   [0]}
                          {:path [:path/branch :path/unary :path/op]
                           :pred '#{'inv '+ '* '?}
                           :val  'not
                           :via  [::ps/path]
                           :in   [0]}
                          {:path [:path/branch :path/unary-neg :path/path :path/terminal]
                           :pred `(comp not list?)
                           :val  '(cat :foo/bar :bar/baz)
                           :via  [::ps/path ::ps/path-neg ::ps/path-neg]
                           :in   [1]}
                          {:path [:path/branch :path/unary-neg :path/path :path/branch :path/varardic :path/op]
                           :pred '#{'alt}
                           :val  'cat
                           :via  [::ps/path ::ps/path-neg ::ps/path-neg]
                           :in   [1 0]}]
            ::s/spec ::ps/path
            ::s/value '(not (cat :foo/bar :bar/baz))}
           (s/explain-data ::ps/path '(not (cat :foo/bar :bar/baz)))))))

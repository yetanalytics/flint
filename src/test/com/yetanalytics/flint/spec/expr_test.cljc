(ns com.yetanalytics.flint.spec.expr-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.expr  :as es]))

(deftest conform-expr-test
  (testing "Conforming expressions"
    (testing "(terminals)"
      (is (s/valid? ::es/expr '?foo))
      (is (s/valid? ::es/expr "bar"))
      (is (s/valid? ::es/expr 2))
      (is (s/valid? ::es/expr false))
      (is (= [:expr/terminal [:ax/iri "<http://foo.org>"]]
             (s/conform ::es/expr "<http://foo.org>")))
      (is (= [:expr/terminal [:ax/prefix-iri :foo/bar]]
             (s/conform ::es/expr :foo/bar)))
      (is (= [:expr/terminal [:ax/var '?foo]]
             (s/conform ::es/expr '?foo)))
      (is (= [:expr/terminal [:ax/literal #inst "2022-01-19T22:20:49Z"]]
             (s/conform ::es/expr #inst "2022-01-19T22:20:49Z")))
      (is (= [:expr/terminal [:ax/literal 100]]
             (s/conform ::es/expr 100)))
      (is (= [:expr/terminal [:ax/literal true]]
             (s/conform ::es/expr true)))
      (is (= [:expr/terminal [:ax/literal "ok"]]
             (s/conform ::es/expr "ok"))))
    (is (= [:expr/branch [[:expr/op 'rand]
                          [:expr/args []]]]
           (s/conform ::es/expr '(rand))))
    (is (= [:expr/branch [[:expr/op 'bnode]
                          [:expr/args []]]]
           (s/conform ::es/expr '(bnode))))
    (is (= [:expr/branch [[:expr/op 'bnode]
                          [:expr/args [[:expr/branch [[:expr/op 'rand]
                                                      [:expr/args []]]]]]]]
           (s/conform ::es/expr '(bnode (rand)))))
    (is (= [:expr/branch [[:expr/op 'bound]
                          [:expr/args [[:expr/terminal [:ax/var '?foo]]]]]]
           (s/conform ::es/expr '(bound ?foo))))
    ;; Don't forget to eval the com.yetanalytics.flint.spec.where-test ns!
    ;; (We can't `require` it due to circular dependencies.)
    (is (= [:expr/branch [[:expr/op 'exists]
                          [:expr/args [[:where-sub/where
                                        [[:triple/vec '[[:ax/var ?s]
                                                        [:ax/var ?p]
                                                        [:ax/var ?o]]]]]]]]]
           (s/conform ::es/expr '(exists [[?s ?p ?o]]))))
    (is (= [:expr/branch [[:expr/op 'contains]
                          [:expr/args [[:expr/terminal [:ax/literal "foo"]]
                                       [:expr/terminal [:ax/literal "foobar"]]]]]]
           (s/conform ::es/expr '(contains "foo" "foobar"))))
    (is (= [:expr/branch [[:expr/op 'regex]
                          [:expr/args [[:expr/terminal [:ax/var '?foo]]
                                       [:expr/terminal [:ax/literal "bar"]]
                                       [:expr/terminal [:ax/literal "i"]]]]]]
           (s/conform ::es/expr '(regex ?foo "bar" "i"))))
    (is (= [:expr/branch [[:expr/op 'if]
                          [:expr/args [[:expr/terminal [:ax/literal true]]
                                       [:expr/terminal [:ax/literal 1]]
                                       [:expr/terminal [:ax/literal 0]]]]]]
           (s/conform ::es/expr '(if true 1 0))))
    (is (= [:expr/branch [[:expr/op '+]
                          [:expr/args [[:expr/terminal [:ax/literal 1]]
                                       [:expr/terminal [:ax/literal 2]]
                                       [:expr/branch [[:expr/op '*]
                                                      [:expr/args [[:expr/terminal [:ax/literal 3]]
                                                                   [:expr/terminal [:ax/literal 4]]]]]]]]]]
           (s/conform ::es/expr '(+ 1 2 (* 3 4)))))
    (is (= [:expr/branch [[:expr/op [:ax/prefix-iri :foo/my-custom-fn]]
                          [:expr/args [[:expr/terminal [:ax/literal 2]]
                                       [:expr/terminal [:ax/literal 2]]]]]]
           (s/conform ::es/expr '(:foo/my-custom-fn 2 2))))
    (testing "aggregates"
      (is (= [:expr/branch [[:expr/op 'count]
                            [:expr/args [[:expr/terminal [:ax/var '?foo]]]]]]
             (s/conform ::es/agg-expr '(count ?foo))))
      ;; Nested aggregates banned by some impls, but is allowed here
      (is (= [:expr/branch [[:expr/op 'count]
                            [:expr/args
                             [[:expr/branch
                               [[:expr/op 'avg]
                                [:expr/args [[:expr/terminal [:ax/var '?foo]]]]]]]]]]
             (s/conform ::es/agg-expr '(count (avg ?foo)))))
      (is (= [:expr/branch [[:expr/op 'count]
                            [:expr/args [[:expr/terminal [:ax/var '?foo]]]]
                            [:expr/kwargs [[:distinct? true]]]]]
             (s/conform ::es/agg-expr '(count ?foo :distinct? true))))
      (is (= [:expr/branch [[:expr/op 'count]
                            [:expr/args [[:expr/terminal [:ax/wildcard '*]]]]]]
             (s/conform ::es/agg-expr '(count *))))
      (is (= [:expr/branch [[:expr/op 'count]
                            [:expr/args [[:expr/terminal [:ax/wildcard '*]]]]
                            [:expr/kwargs [[:distinct? true]]]]]
             (s/conform ::es/agg-expr '(count * :distinct? true))))
      (is (= [:expr/branch [[:expr/op 'group-concat]
                            [:expr/args [[:expr/terminal [:ax/var '?foo]]]]
                            [:expr/kwargs [[:separator ";"]]]]]
             (s/conform ::es/agg-expr '(group-concat ?foo :separator ";"))))
      (is (= [:expr/branch [[:expr/op 'group-concat]
                            [:expr/args [[:expr/terminal [:ax/var '?foo]]]]
                            [:expr/kwargs [[:distinct? true]
                                           [:separator ";"]]]]]
             (s/conform ::es/agg-expr '(group-concat ?foo :distinct? true :separator ";"))))
      (is (= [:expr/branch [[:expr/op 'group-concat]
                            [:expr/args [[:expr/terminal [:ax/var '?foo]]]]
                            [:expr/kwargs [[:separator ";"]
                                           [:distinct? true]]]]]
             (s/conform ::es/agg-expr '(group-concat ?foo :separator ";" :distinct? true))))
      (is (= [:expr/branch [[:expr/op [:ax/prefix-iri :my/fn]]
                            [:expr/args [[:expr/terminal [:ax/var '?foo]]]]
                            [:expr/kwargs [[:distinct? true]]]]]
             (s/conform ::es/agg-expr '(:my/fn ?foo :distinct? true))))
      (is (= [:expr/branch [[:expr/op [:ax/prefix-iri :my/fn]]
                            [:expr/args [[:expr/terminal [:ax/var '?foo]]]]]]
             (s/conform ::es/agg-expr '(:my/fn ?foo))))
      (is (s/invalid?
           (s/conform ::es/agg-expr '(count ?foo :bad? true))))
      (is (s/invalid?
           (s/conform ::es/agg-expr '(group-concat ?foo :separator ";" :bad? true))))
      (is (s/invalid?
           (s/conform ::es/expr '(count ?foo))))
      (is (s/invalid?
           (s/conform ::es/expr '(group-concat ?foo :separator ";"))))
      (is (s/invalid?
           (s/conform ::es/expr '(:my/fn ?foo :distinct? true)))))))

(deftest invalid-expr-test
  (testing "Invalid expressions"
    (is (= {::s/problems [{:path [:expr/terminal]
                           :pred `(comp not list?)
                           :val  '(rand 1)
                           :via  [::es/expr]
                           :in   []}
                          {:path   [:expr/branch 'rand]
                           :reason "Extra input"
                           :pred   `(s/cat :expr/op es/nilary-ops)
                           :val    '(1)
                           :via    [::es/expr]
                           :in     [1]}]
            ::s/spec     ::es/expr
            ::s/value    '(rand 1)}
           (s/explain-data ::es/expr '(rand 1))))
    (is (= {::s/problems [{:path [:expr/terminal]
                           :pred `(comp not list?)
                           :val  '(not false true)
                           :via  [::es/expr]
                           :in   []}
                          {:path   [:expr/branch 'not]
                           :reason "Extra input"
                           :pred   `(s/cat
                                     :expr/op es/unary-ops
                                     :expr/arg-1 ~'expr-spec)
                           :val    '(true)
                           :via    [::es/expr]
                           :in     [2]}]
            ::s/spec ::es/expr
            ::s/value '(not false true)}
           (s/explain-data ::es/expr '(not false true))))
    (is (= {::s/problems [{:path [:expr/terminal]
                           :pred `(comp not list?)
                           :val  '(contains "foo")
                           :via  [::es/expr]
                           :in   []}
                          {:path   [:expr/branch 'contains :expr/arg-2]
                           :reason "Insufficient input"
                           :pred   'expr-spec
                           :val    ()
                           :via    [::es/expr ::es/expr]
                           :in     []}]
            ::s/spec ::es/expr
            ::s/value '(contains "foo")}
           (s/explain-data ::es/expr '(contains "foo"))))
    (is (= {::s/problems [{:path [:expr/terminal]
                           :pred `(comp not list?)
                           :val  '(+)
                           :via  [::es/expr]
                           :in   []}
                          {:path   [:expr/branch '+ :expr/arg-1]
                           :reason "Insufficient input"
                           :pred   'expr-spec
                           :val    ()
                           :via    [::es/expr ::es/expr]
                           :in     []}]
            ::s/spec ::es/expr
            ::s/value '(+)}
           (s/explain-data ::es/expr '(+))))))

(deftest conform-expr-as-var-test
  (testing "Conforming expr AS var clauses"
    (is (= '[:expr/as-var
             [[:expr/branch [[:expr/op +]
                             [:expr/args [[:expr/terminal [:ax/literal 2]]
                                          [:expr/terminal [:ax/literal 2]]]]]]
              [:ax/var ?foo]]]
           (s/conform ::es/expr-as-var '[(+ 2 2) ?foo])))
    (is (= '[:expr/as-var
             [[:expr/branch [[:expr/op concat]
                             [:expr/args [[:expr/terminal [:ax/var ?G]]
                                          [:expr/terminal [:ax/literal " "]]
                                          [:expr/terminal [:ax/var ?S]]]]]]
              [:ax/var ?name]]]
           (s/conform ::es/expr-as-var '[(concat ?G " " ?S) ?name])))
    (is (= '[:expr/as-var
             [[:expr/branch [[:expr/op avg]
                             [:expr/args [[:expr/terminal [:ax/var ?x]]]]]]
              [:ax/var ?avg]]]
           (s/conform ::es/agg-expr-as-var '[(avg ?x) ?avg])))
    (is (s/invalid?
         (s/conform ::es/expr-as-var '[(avg ?x) ?avg])))))

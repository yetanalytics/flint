(ns com.yetanalytics.flint.spec.expr-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]
            [com.yetanalytics.flint.spec.expr  :as es]))

(deftest conform-expr-test
  (testing "Conforming expression"
    (testing "terminals"
      (is (s/valid? ::es/expr '?foo))
      (is (s/valid? ::es/expr "bar"))
      (is (s/valid? ::es/expr 2))
      (is (s/valid? ::es/expr false))
      (is (= [:expr/terminal [:ax/var '?foo]]
             (s/conform ::es/expr '?foo)))
      (is (= [:expr/terminal [:ax/dt-lit #inst "2022-01-19T22:20:49Z"]]
             (s/conform ::es/expr #inst "2022-01-19T22:20:49Z")))
      (is (= [:expr/terminal [:ax/num-lit 100]]
             (s/conform ::es/expr 100)))
      (is (= [:expr/terminal [:ax/bool-lit true]]
             (s/conform ::es/expr true)))
      (is (= [:expr/terminal [:ax/str-lit "ok"]]
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
    (is (= [:expr/branch [[:expr/op 'count]
                          [:expr/args [[:expr/terminal [:ax/var '?foo]]]]]]
           (s/conform ::es/expr '(count ?foo))))
    (is (= [:expr/branch [[:expr/op 'count]
                          [:expr/args [[:expr/terminal [:ax/wildcard '*]]]]]]
           (s/conform ::es/expr '(count *))))
    (is (= [:expr/branch [[:expr/op 'bound]
                          [:expr/args [[:expr/terminal [:ax/var '?foo]]]]]]
           (s/conform ::es/expr '(bound ?foo))))
    (is (= [:expr/branch [[:expr/op 'exists]
                          [:expr/args [[:where-sub/where
                                        [[:triple/vec '[[:ax/var ?s]
                                                        [:ax/var ?p]
                                                        [:ax/var ?o]]]]]]]]]
           (s/conform ::es/expr '(exists [[?s ?p ?o]]))))
    (is (= [:expr/branch [[:expr/op 'contains]
                          [:expr/args [[:expr/terminal [:ax/str-lit "foo"]]
                                       [:expr/terminal [:ax/str-lit "foobar"]]]]]]
           (s/conform ::es/expr '(contains "foo" "foobar"))))
    (is (= [:expr/branch [[:expr/op 'regex]
                          [:expr/args [[:expr/terminal [:ax/var '?foo]]
                                       [:expr/terminal [:ax/str-lit "bar"]]
                                       [:expr/terminal [:ax/str-lit "i"]]]]]]
           (s/conform ::es/expr '(regex ?foo "bar" "i"))))
    (is (= [:expr/branch [[:expr/op 'group-concat]
                          [:expr/args [[:expr/terminal [:ax/var '?foo]]
                                       [:expr/terminal [:expr/kwarg
                                                        [[:expr/k :separator]
                                                         [:expr/v ";"]]]]]]]]
           (s/conform ::es/expr '(group-concat ?foo :separator ";"))))
    (is (= [:expr/branch [[:expr/op 'if]
                          [:expr/args [[:expr/terminal [:ax/bool-lit true]]
                                       [:expr/terminal [:ax/num-lit 1]]
                                       [:expr/terminal [:ax/num-lit 0]]]]]]
           (s/conform ::es/expr '(if true 1 0))))
    (is (= [:expr/branch [[:expr/op '+]
                          [:expr/args [[:expr/terminal [:ax/num-lit 1]]
                                       [:expr/terminal [:ax/num-lit 2]]
                                       [:expr/branch [[:expr/op '*]
                                                      [:expr/args [[:expr/terminal [:ax/num-lit 3]]
                                                                   [:expr/terminal [:ax/num-lit 4]]]]]]]]]]
           (s/conform ::es/expr '(+ 1 2 (* 3 4)))))
    (is (= [:expr/branch [[:expr/op [:ax/prefix-iri :foo/my-custom-fn]]
                          [:expr/args [[:expr/terminal [:ax/num-lit 2]]
                                       [:expr/terminal [:ax/num-lit 2]]]]]]
           (s/conform ::es/expr '(:foo/my-custom-fn 2 2))))))

(deftest invalid-expr-test
  (testing "Invalid expressions"
    (is (= {::s/problems [{:path [:expr/terminal]
                           :pred `(comp not list?)
                           :val  '(rand 1)
                           :via  [::es/expr]
                           :in   []}
                          {:path   [:expr/branch :0-ary]
                           :reason "Extra input"
                           :pred   `(s/cat :op ~'#{'uuid 'now 'rand 'struuid})
                           :val    '(1)
                           :via    [::es/expr]
                           :in     [1]}
                          {:path [:expr/branch :custom :op :ax/iri]
                           :pred `ax/iri?
                           :val  'rand
                           :via  [::es/expr]
                           :in   [0]}
                          {:path [:expr/branch :custom :op :ax/prefix-iri]
                           :pred `ax/prefix-iri?
                           :val  'rand
                           :via  [::es/expr]
                           :in   [0]}]
            ::s/spec     ::es/expr
            ::s/value    '(rand 1)}
           (-> (s/explain-data ::es/expr '(rand 1))
               (update ::s/problems (partial filter (comp not set? :pred))))))
    (is (= {::s/problems [{:path [:expr/terminal]
                           :pred `(comp not list?)
                           :val  '(not false true)
                           :via  [::es/expr]
                           :in   []}
                          {:path   [:expr/branch :1-ary]
                           :reason "Extra input"
                           :pred   `(s/cat
                                     :op
                                     ~'#{'not
                                         'str 'strlen 'ucase 'lcase
                                         'lang 'datatype 'blank? 'literal?
                                         'numeric?
                                         'iri 'uri 'iri? 'uri? 'encode-for-uri
                                         'abs 'ceil 'floor 'round
                                         'year 'month 'day
                                         'hours 'minutes 'seconds
                                         'timezone 'tz
                                         'md5 'sha1 'sha256 'sha384 'sha512
                                         'sum 'sum-distinct
                                         'min 'min-distinct
                                         'max 'max-distinct
                                         'avg 'avg-distinct
                                         'sample 'sample-distinct
                                         'count 'count-distinct}
                                     :arg-1 ::es/expr)
                           :val    '(true)
                           :via    [::es/expr]
                           :in     [2]}
                           {:path [:expr/branch :custom :op :ax/iri]
                            :pred `ax/iri?
                            :val  'not
                            :via  [::es/expr]
                            :in   [0]}
                           {:path [:expr/branch :custom :op :ax/prefix-iri]
                            :pred `ax/prefix-iri?
                            :val  'not
                            :via  [::es/expr]
                            :in   [0]}]
            ::s/spec ::es/expr
            ::s/value '(not false true)}
           (-> (s/explain-data ::es/expr '(not false true))
               (update ::s/problems (partial filter (comp not set? :pred))))))
    (is (= {::s/problems [{:path [:expr/terminal]
                           :pred `(comp not list?)
                           :val  '(contains "foo")
                           :via  [::es/expr]
                           :in   []}
                          {:path   [:expr/branch :2-ary :arg-2]
                           :reason "Insufficient input"
                           :pred   ::es/expr
                           :val    ()
                           :via    [::es/expr ::es/expr]
                           :in     []}
                          {:path [:expr/branch :custom :op :ax/iri]
                           :pred `ax/iri?
                           :val  'contains
                           :via  [::es/expr]
                           :in   [0]}
                          {:path [:expr/branch :custom :op :ax/prefix-iri]
                           :pred `ax/prefix-iri?
                           :val  'contains
                           :via  [::es/expr]
                           :in   [0]}]
            ::s/spec ::es/expr
            ::s/value '(contains "foo")}
           (-> (s/explain-data ::es/expr '(contains "foo"))
               (update ::s/problems (partial filter (comp not set? :pred))))))
    (is (= {::s/problems [{:path [:expr/terminal]
                           :pred `(comp not list?)
                           :val  '(+)
                           :via  [::es/expr]
                           :in   []}
                          {:path   [:expr/branch :2-plus-ary :arg-1]
                           :reason "Insufficient input"
                           :pred   ::es/expr
                           :val    ()
                           :via    [::es/expr ::es/expr]
                           :in     []}
                          {:path [:expr/branch :custom :op :ax/iri]
                           :pred `ax/iri?
                           :val  '+
                           :via  [::es/expr]
                           :in   [0]}
                          {:path [:expr/branch :custom :op :ax/prefix-iri]
                           :pred `ax/prefix-iri?
                           :val  '+
                           :via  [::es/expr]
                           :in   [0]}]
            ::s/spec ::es/expr
            ::s/value '(+)}
           (-> (s/explain-data ::es/expr '(+))
               (update ::s/problems (partial filter (comp not set? :pred))))))))

(deftest expr-as-var-test
  (testing "expr-as-var spec"
    (is (= '[:expr/as-var
             [[:expr/branch [[:expr/op +]
                             [:expr/args [[:expr/terminal [:ax/num-lit 2]]
                                          [:expr/terminal [:ax/num-lit 2]]]]]]
              [:ax/var ?foo]]]
           (s/conform ::es/expr-as-var '[(+ 2 2) ?foo])))
    (is (= '[:expr/as-var
             [[:expr/branch [[:expr/op concat]
                             [:expr/args [[:expr/terminal [:ax/var ?G]]
                                          [:expr/terminal [:ax/str-lit " "]]
                                          [:expr/terminal [:ax/var ?S]]]]]]
              [:ax/var ?name]]]
           (s/conform ::es/expr-as-var '[(concat ?G " " ?S) ?name])))))

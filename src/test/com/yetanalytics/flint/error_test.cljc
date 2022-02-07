(ns com.yetanalytics.flint.error-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.query  :as qs]
            [com.yetanalytics.flint.spec.update :as us]
            [com.yetanalytics.flint.error       :as err]
            [com.yetanalytics.flint.prefix      :as pre]
            [com.yetanalytics.flint.scope       :as scope]))

(deftest top-level-keyword-test
  (testing "all top level keywords are accounted for"
    (is (= err/top-level-keywords
           (set (concat (keys qs/key-order-map)
                        (keys us/key-order-map)))))
    (is (= err/top-level-keywords
           (->> (s/registry)
                keys
                (filter (fn [k]
                          (#{"com.yetanalytics.flint.spec.modifier"
                             "com.yetanalytics.flint.spec.query"
                             "com.yetanalytics.flint.spec.prologue"
                             "com.yetanalytics.flint.spec.select"
                             "com.yetanalytics.flint.spec.update"
                             "com.yetanalytics.flint.spec.values"
                             "com.yetanalytics.flint.spec.where"}
                           (namespace k))))
                (map (comp keyword name))
                set)))))

(deftest spec-error-msg-test
  (testing "query spec error messages"
    (is (= "Syntax errors exist due to missing clauses!"
           (->> '{}
                (s/explain-data qs/query-spec)
                err/spec-error-msg)))
    (is (= "Syntax errors exist due to missing clauses!"
           (->> '{:select [?x]}
                (s/explain-data qs/query-spec)
                err/spec-error-msg)))
    (is (= "Syntax errors exist due to missing clauses!"
           (->> '{:select [?x] :from "<http://foo.org>"}
                (s/explain-data qs/query-spec)
                err/spec-error-msg)))
    (is (= "Syntax errors exist due to missing clauses!"
           (->> '{:where [[?x ?y ?z]]}
                (s/explain-data qs/query-spec)
                err/spec-error-msg)))
    (is (= "Syntax errors exist in the WHERE clause!"
           (->> '{:where [[?x ?y]]}
                (s/explain-data qs/query-spec)
                err/spec-error-msg)))
    (is (= "Syntax errors exist in the SELECT and WHERE clauses!"
           (->> '{:select [x] :where [[?x ?y]]}
                (s/explain-data qs/query-spec)
                err/spec-error-msg)))
    (is (= "Syntax errors exist in the SELECT, FROM and WHERE clauses!"
           (->> '{:select [x] :from "http://foo.org" :where [[?x ?y]]}
                (s/explain-data qs/query-spec)
                err/spec-error-msg)))
    (is (= "Syntax errors exist in the BASE, PREFIXES, SELECT, FROM, WHERE, GROUP BY and HAVING clauses!"
           (->> '{:base     "http://base.org"
                  :prefixes {:$ "http://prefix.org"}
                  :select   [x]
                  :from     "http://foo.org"
                  :where    [[?x ?y]]
                  :group-by 1
                  :having   2}
                (s/explain-data qs/query-spec)
                err/spec-error-msg))))
  (testing "update spec error messages"
    (is (= "Syntax errors exist due to missing clauses!"
           (->> '{}
                (s/explain-data us/update-spec)
                err/spec-error-msg)))
    (is (= ["Syntax errors exist at index 0 due to missing clauses!"
            "Syntax errors exist at index 1 due to missing clauses!"]
           (->> ['{} '{}]
                (map (partial s/explain-data us/update-spec))
                (map-indexed (fn [idx ed] (err/spec-error-msg ed idx))))))
    (is (= ["Syntax errors exist at index 0 due to missing clauses!"
            "Syntax errors exist at index 1 due to missing clauses!"]
           (->> ['{:copy "<http://example.org>"} '{}]
                (map (partial s/explain-data us/update-spec))
                (map-indexed (fn [idx ed] (err/spec-error-msg ed idx))))))
    (is (= ["Syntax errors exist at index 0 in the TO clause!"
            "Syntax errors exist at index 1 due to missing clauses!"]
           (->> ['{:copy "<http://example.org>" :to "foo.org"} '{}]
                (map (partial s/explain-data us/update-spec))
                (map-indexed (fn [idx ed] (err/spec-error-msg ed idx))))))
    (is (= ["Syntax errors exist at index 0 in the TO and COPY clauses!"
            "Syntax errors exist at index 1 due to missing clauses!"]
           (->> ['{:copy "http://example.org" :to "foo.org"} '{}]
                (map (partial s/explain-data us/update-spec))
                (map-indexed (fn [idx ed] (err/spec-error-msg ed idx))))))))

(deftest prefix-error-msg-test
  (testing "prefix error messages"
    (is (= "1 IRI cannot be expanded due to missing prefixes :dc!"
           (->> '{:select [?x]
                  :where [[?x :dc/title "Foo"]]}
                (s/conform qs/query-spec)
                (pre/validate-prefixes {})
                (err/prefix-error-msg))))
    (is (= (str "4 IRIs cannot be expanded due to missing prefixes "
                ":$, :dc and :foaf!")
           (->> '{:select [?x]
                  :where [[?x :boo "Boo"]
                          [?x :dc/title "Foo"]
                          [:union
                           [[?x :foaf/name "Bar"]]
                           [[?x :foaf/name "Buu"]]]]}
                (s/conform qs/query-spec)
                (pre/validate-prefixes {})
                (err/prefix-error-msg))))
    (is (= (str "3 IRIs cannot be expanded due to missing prefixes "
                ":$ and :dc!")
           (->> '{:delete-data [[:fish :dc/title :food]]}
                (s/conform us/update-spec)
                (pre/validate-prefixes {})
                (err/prefix-error-msg))))
    (is (= (str "3 IRIs at index 0 cannot be expanded due to missing prefixes "
                ":$ and :dc!")
           (->> ['{:delete-data [[:fish :dc/title :food]]}]
                (map (partial s/conform us/update-spec))
                (map (partial pre/validate-prefixes {}))
                (map-indexed (fn [idx err] (err/prefix-error-msg err idx)))
                first)))))

(deftest scope-error-msg-test
  (testing "scope error messages"
    (is (= "1 variable in 2 `expr AS var` clauses was already defined in scope: ?x!'"
           (->> '{:select [[2 ?x]]
                  :where  [[?x ?y ?z]
                           [:bind [3 ?x]]]}
                (s/conform qs/query-spec)
                scope/validate-scoped-vars
                err/scope-error-msg)))
    (is (= "2 variables in 2 `expr AS var` clauses were already defined in scope: ?x and ?y!'"
           (->> '{:select [[2 ?y]]
                  :where  [[?x ?y ?z]
                           [:bind [3 ?x]]]}
                (s/conform qs/query-spec)
                scope/validate-scoped-vars
                err/scope-error-msg)))
    (is (= "1 variable at index 0 in 1 `expr AS var` clause was already defined in scope: ?x!'"
           (->> '[{:delete [[?x ?y ?z]]
                   :where  [[?x ?y ?z]
                            [:bind [3 ?x]]]}]
                (map (partial s/conform us/update-spec))
                (map scope/validate-scoped-vars)
                (map-indexed (fn [idx err] (err/scope-error-msg err idx)))
                first)))))

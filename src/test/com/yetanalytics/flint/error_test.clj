(ns com.yetanalytics.flint.error-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.query  :as qs]
            [com.yetanalytics.flint.spec.update :as us]
            [com.yetanalytics.flint.error       :as err]))

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

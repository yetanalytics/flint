(ns com.yetanalytics.flint.error-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.query  :as qs]
            [com.yetanalytics.flint.spec.update :as us]
            [com.yetanalytics.flint.error       :as err]
            [com.yetanalytics.flint.validate    :as v]
            [com.yetanalytics.flint.validate.aggregate  :as va]
            [com.yetanalytics.flint.validate.bnode      :as vb]
            [com.yetanalytics.flint.validate.prefix     :as vp]
            [com.yetanalytics.flint.validate.scope      :as vs]))

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
    (is (= "Syntax errors exist due to invalid map, or invalid or extra clauses!"
           (->> '{}
                (s/explain-data qs/query-spec)
                err/spec-error-msg)))
    (is (= "Syntax errors exist due to invalid map, or invalid or extra clauses!"
           (->> '{:select [?x]}
                (s/explain-data qs/query-spec)
                err/spec-error-msg)))
    (is (= "Syntax errors exist due to invalid map, or invalid or extra clauses!"
           (->> '{:select [?x] :from "<http://foo.org>"}
                (s/explain-data qs/query-spec)
                err/spec-error-msg)))
    (is (= "Syntax errors exist due to invalid map, or invalid or extra clauses!"
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
    (is (= "Syntax errors exist due to invalid map, or invalid or extra clauses!"
           (->> '{}
                (s/explain-data us/update-spec)
                err/spec-error-msg)))
    (is (= "Syntax errors exist due to invalid map, or invalid or extra clauses!"
           (->> '[:copy "http://example.org" :to "<foo.org>"]
                (s/explain-data us/update-spec)
                err/spec-error-msg)))
    (is (= "Syntax errors exist due to invalid map, or invalid or extra clauses!"
           (->> '{:copy "http://example.org" :to "<foo.org>" :from "<bar.org>"}
                (s/explain-data us/update-spec)
                err/spec-error-msg)))
    (is (= ["Syntax errors exist at index 0 due to invalid map, or invalid or extra clauses!"
            "Syntax errors exist at index 1 due to invalid map, or invalid or extra clauses!"]
           (->> ['{} '{}]
                (map (partial s/explain-data us/update-spec))
                (map-indexed (fn [idx ed] (err/spec-error-msg ed idx))))))
    (is (= ["Syntax errors exist at index 0 due to invalid map, or invalid or extra clauses!"
            "Syntax errors exist at index 1 due to invalid map, or invalid or extra clauses!"]
           (->> ['{:copy "<http://example.org>"} '{}]
                (map (partial s/explain-data us/update-spec))
                (map-indexed (fn [idx ed] (err/spec-error-msg ed idx))))))
    (is (= ["Syntax errors exist at index 0 in the TO clause!"
            "Syntax errors exist at index 1 due to invalid map, or invalid or extra clauses!"]
           (->> ['{:copy "<http://example.org>" :to "foo.org"} '{}]
                (map (partial s/explain-data us/update-spec))
                (map-indexed (fn [idx ed] (err/spec-error-msg ed idx))))))
    (is (= ["Syntax errors exist at index 0 in the COPY and TO clauses!"
            "Syntax errors exist at index 1 due to invalid map, or invalid or extra clauses!"]
           (->> ['{:copy "http://example.org" :to "foo.org"} '{}]
                (map (partial s/explain-data us/update-spec))
                (map-indexed (fn [idx ed] (err/spec-error-msg ed idx))))))))

(deftest prefix-error-msg-test
  (testing "prefix error messages"
    (is (= "1 IRI cannot be expanded due to missing prefixes :dc!"
           (->> '{:select [?x]
                  :where [[?x :dc/title "Foo"]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                (vp/validate-prefixes {})
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
                v/collect-nodes
                (vp/validate-prefixes {})
                (err/prefix-error-msg))))
    (is (= (str "3 IRIs cannot be expanded due to missing prefixes "
                ":$ and :dc!")
           (->> '{:delete-data [[:fish :dc/title :food]]}
                (s/conform us/update-spec)
                v/collect-nodes
                (vp/validate-prefixes {})
                (err/prefix-error-msg))))
    (is (= (str "3 IRIs at index 0 cannot be expanded due to missing prefixes "
                ":$ and :dc!")
           (->> ['{:delete-data [[:fish :dc/title :food]]}]
                (map (partial s/conform us/update-spec))
                (map v/collect-nodes)
                (map (partial vp/validate-prefixes {}))
                (map-indexed (fn [idx err] (err/prefix-error-msg err idx)))
                first)))))

(deftest scope-error-msg-test
  (testing "scope error messages"
    (is (= "1 variable in 2 `expr AS var` clauses was already defined in scope: ?x!'"
           (->> '{:select [[2 ?x]]
                  :where  [[?x ?y ?z]
                           [:bind [3 ?x]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vs/validate-scoped-vars
                err/scope-error-msg)))
    (is (= "2 variables in 1 `expr AS var` clause were not defined in scope: ?u and ?v!'"
           (->> '{:select [[(+ ?u ?v) ?x]]
                  :where  [[?x ?y ?z]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vs/validate-scoped-vars
                err/scope-error-msg)))
    (is (= "2 variables in 2 `expr AS var` clauses were already defined in scope: ?x and ?y!'"
           (->> '{:select [[2 ?y]]
                  :where  [[?x ?y ?z]
                           [:bind [3 ?x]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vs/validate-scoped-vars
                err/scope-error-msg)))
    (is (= "1 variable at index 0 in 1 `expr AS var` clause was already defined in scope: ?x!'"
           (->> '[{:delete [[?x ?y ?z]]
                   :where  [[?x ?y ?z]
                            [:bind [3 ?x]]]}]
                (map (partial s/conform us/update-spec))
                (map v/collect-nodes)
                (map vs/validate-scoped-vars)
                (map-indexed (fn [idx err] (err/scope-error-msg err idx)))
                first)))))

(deftest aggregate-error-msg-test
  (testing "aggregates error messages"
    (is (= "1 SELECT clause has both wildcard and GROUP BY!"
           (->> '{:select   :*
                  :where    [[?x ?y ?z]]
                  :group-by [?x]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects
                err/aggregate-error-msg)))
    (is (= "2 SELECT clauses have both wildcard and GROUP BY!"
           (->> '{:select   :*
                  :where    {:select   :*
                             :where    [[?x ?y ?z]]
                             :group-by [?x]}
                  :group-by [?x]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects
                err/aggregate-error-msg)))
    (is (= "1 variable was illegally used in SELECTs with aggregates: ?y!"
           (->> '{:select [?x ?y]
                  :where  [[?x ?y ?z]]
                  :group-by [?x]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects
                err/aggregate-error-msg)))
    (is (= "2 variables were illegally used in SELECTs with aggregates: ?y and ?z!"
           (->> '{:select [?x ?y ?z]
                  :where  [[?x ?y ?z]]
                  :group-by [?x]}
                (s/conform qs/query-spec)
                v/collect-nodes
                va/validate-agg-selects
                err/aggregate-error-msg)))
    (is (= "2 variables at index 0 were illegally used in SELECTs with aggregates: ?y and ?z!"
           (->> '[{:delete [[?x ?y ?z]]
                   :where  {:select   [?x ?y ?z]
                            :where    [[?x ?y ?z]]
                            :group-by [?x]}}]
                (map (partial s/conform us/update-spec))
                (map v/collect-nodes)
                (map va/validate-agg-selects)
                (map-indexed (fn [i x] (err/aggregate-error-msg x i)))
                first)))))

(deftest bnode-error-msg-test
  (testing "blank node error messages"
    (is (= "1 blank node was duplicated in multiple BGPs: _1!"
           (->> '{:select [?x]
                  :where  [[:where [[?x :foo/bar _1]]]
                           [:where [[?y :baz/qux _1]]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes
                second
                err/bnode-error-msg)))
    (is (= "2 blank nodes were duplicated in multiple BGPs: _2 and _1!"
           (->> '{:select [?x]
                  :where  [[:where [{_2 {:foo/bar #{_1}
                                         :baz/qux #{_1}}}]]
                           [:where [{_2 {:fii/fie #{_1}
                                         :foe/fum #{_1}}}]]]}
                (s/conform qs/query-spec)
                v/collect-nodes
                vb/validate-bnodes
                second
                err/bnode-error-msg)))
    (is (= "2 blank nodes at index 1 were duplicated from previous updates: _1 and _2!"
           (let [emap (->> '{:insert [[?x :foo/bar _1]]
                             :where  [[:where [[?x :foo/bar _2]
                                               [?y :baz/qux _3]]]]}
                           (s/conform us/update-spec)
                           v/collect-nodes
                           (vb/validate-bnodes #{'_1 '_2})
                           second)]
             (err/bnode-error-msg emap 1))))))

(ns syrup.sparql.spec-test
  (:require [clojure.test :refer [deftest testing are]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.query :as qs]))

(deftest query-spec-test
  (testing "SELECT queries"
    (are [q] (s/valid? qs/select-query-spec q)
      '{:select [?x]
        :where  []}
      '{:select [?title]
        :where  [["http://example.org" "http://purl.org/dc/elements/1.1/title" ?title]]}
      '{:select [?v]
        :where  [[?v ?p "cat"]]}
      '{:select [?v]
        :where  [[?v ?p 42]]}
      '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
        :select   [?name ?mbox]
        :where    [[?x :foaf/name ?name]
                   [?x :foaf/name ?mbox]]}
      '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
        :select   [[(concat ?G " " ?S) ?name]]
        :where    [{?P {:foaf/givenName #{?G}
                        :foaf/surname #{?S}}}]}
      ;; with FILTER applied
      '{:prefixes {:dc "http://purl.org/dc/elements/1.1/"}
        :select   [?title]
        :where    [[?x :dc/title ?title]
                   [:filter (regex ?title "^SPARQL")]]}
      '{:prefxes {:dc "http://purl.org/dc/elements/1.1/"
                  :ns "http://example.org/ns#"}
        :select  [?title ?price]
        :where   [[?x :ns/price ?price]
                  [:filter (< ?price 30.5)]
                  [?x :dc/title ?title]]}
      '{:prefixes {:rdf  "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                   :foaf "http://xmlns.com/foaf/0.1/"}
        :select   [?person]
        :where    [[?person :rdf/type :foaf/Person]
                   [:filter (exists [[?person :foaf/name ?name]])]]}
      '{:prefixes {:rdf  "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                   :foaf "http://xmlns.com/foaf/0.1/"}
        :select   [?person]
        :where    [[?person :rdf/type :foaf/Person]
                   [:filter (not-exists [[?person :foaf/name ?name]])]]}
      '{:prefixes {:$ "http://example.com/"}
        :select   *
        :where    [[?x :p ?n]
                   [:filter (not-exists [[?x :q ?m]
                                         [:filter (= ?n ?m)]])]]}
      ;; with OPTIONAL applied
      '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
        :select   [?name ?mbox]
        :where    [[?x :foaf/name ?name]
                   [:optional [[?x :foaf/mbox ?mbox]]]]}
      '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
        :select   [?name ?mbox ?hpage]
        :where    [[?x :foaf/name ?name]
                   [:optional [[?x :foaf/mbox ?mbox]]]
                   [:optional [[?x :foaf/homepage ?hpage]]]]}
      '{:prefixes {:dc "http://purl.org/dc/elements/1.1/"
                   :ns "http://example.org/ns#"}
        :select [?title ?price]
        :where [[?x :dc/title ?title]
                [:optional [[?x :ns/price ?price]
                            [:filter (< ?price 30)]]]]}
      ;; with UNION applied
      '{:prefixes {:dc10 "http://purl.org/dc/elements/1.0/"
                   :dc11 "http://purl.org/dc/elements/1.1/"}
        :select [?title]
        :where [[:union
                 [[?book :dc10/title ?title]]
                 [[?book :dc11/title ?title]]]]}
      '{:prefixes {:dc10 "http://purl.org/dc/elements/1.0/"
                   :dc11 "http://purl.org/dc/elements/1.1/"}
        :select [?x ?y]
        :where [[:union
                 [[?book :dc10/title ?x]]
                 [[?book :dc11/title ?y]]]]}
      ;; with MINUS applied
      '{:prefix          {:$    "http://example/"
                          :foaf "http://xmlns.com/foaf/0.1/"}
        :select-distinct [?s]
        :where           [[?s ?p ?o]
                          [:minus [[?s :foaf/givenName "Bob"]]]]}
      '{:prefixes {:$ "http://example.com/"}
        :select   *
        :where    [[?x :p ?n]
                   [:minus [[?x :q ?m]
                            [:filter (= ?n ?m)]]]]}
      ;; with property paths
      '{:prefix {:$ "http://example/"}
        :select *
        :where  [[:s (cat :item :price) ?x]]}
      '{:prefix {:$ "http://example/"}
        :select [[(sum ?x) ?total]]
        :where  [[:s (cat :item :price) ?x]]}
      '{:prefix {:rdfs "http://www.w3.org/2000/01/rdf-schema#"
                 :rdf  "http://www.w3.org/1999/02/22-rdf-syntax-ns#"}
        :select [?x ?type]
        :where  [[?x (cat :rdf/type (* :rdfs/subClassOf)) ?type]]}
      '{:prefix {:$    "http://example/"
                 :foaf "http://xmlns.com/foaf/0.1/"}
        :select [?person]
        :where  [[:x (+ :foaf/knows) ?person]]}
      ;; with BIND applied
      '{:prefxes {:dc "http://purl.org/dc/elements/1.1/"
                  :ns "http://example.org/ns#"}
        :select  [?title ?price]
        :where   [[?x :ns/price ?price]
                  [?x :ns/discount ?discount]
                  [:bind [(* ?p (- 1 ?discount)) ?price]]
                  [:filter (< ?price 20)]
                  [?x :dc/title ?title]]}
      ;; TODO
      #_'{:prefxes {:dc "http://purl.org/dc/elements/1.1/"
                    :ns "http://example.org/ns#"}
          :select  [?title ?price]
          :where   [[[?x :ns/price ?price]
                     [?x :ns/discount ?discount]
                     [:bind [(* ?p (- 1 ?discount)) ?price]]]
                    [:filter (< ?price 20)]
                    [?x :dc/title ?title]]}
      ;; with VALUES applied
      '{:prefixes {:dc "http://purl.org/dc/elements/1.1/"
                   :ns "http://example.org/ns#"
                   :$  "http://example.org/ns#"}
        :select [?book ?title ?price]
        :where [[:values {?book [:book1 :book3]}]
                {?book {:dc/title #{?title}
                        :ns/price #{?price}}}]}
      '{:prefixes {:dc "http://purl.org/dc/elements/1.1/"
                   :ns "http://example.org/ns#"
                   :$  "http://example.org/ns#"}
        :select [?book ?title ?price]
        :where [[:values {?book  [nil :book2]
                          ?title ["SPARQL Tutorial" nil]}]
                {?book {:dc/title #{?title}
                        :ns/price #{?price}}}]}
      '{:prefixes {:dc "http://purl.org/dc/elements/1.1/"
                   :ns "http://example.org/ns#"
                   :$  "http://example.org/ns#"}
        :select [?book ?title ?price]
        :where [[:values {[?book ?title] [[nil "SPARQL Tutorial"]
                                          [:book2 title]]}]
                {?book {:dc/title #{?title}
                        :ns/price #{?price}}}]}
      ;; with aggregates
      '{:select   [[(avg ?y) ?avg]]
        :where    [{?a {:x #{?x}
                        :y #{?y}}}]
        :group-by [?x]}
      '{:prefix   {:$ "http://data.example/"}
        :select   [[(avg ?size) ?asize]]
        :where    [[?x :size ?size]]
        :group-by [?x]
        :having   [(> (avg ?size) 10)]}
      '{:prefix   {:$ "http://example.com/data/#"}
        :select   [?x [(* (min ?y) 2) ?min]]
        :where    [[?x :p ?y] [?x :q ?z]]
        :group-by [?x (str ?z)]}
      '{:prefix   {:$ "http://example.com/data/#"}
        :select   [?g [(avg ?p) ?avg] [(/ (+ (min ?p) (max ?p)) 2) ?c]]
        :where    [[?g :p ?p]]
        :group-by [?g]}
      '{:prefixes {:$ "http://books.example/"}
        :select   [[(sum ?lprice) ?totalPrice]]
        :where    [[?org :affiliates ?auth]
                   [?auth :writesBook ?book]
                   [?book :price ?lprice]]
        :group-by [?org]
        :having   [(> (sum ?lprice) 10)]}
      ;; with subqueries
      ;; TODO
      #_'{:prefixes {:$ "http://people.example/"}
          :select   [?y ?minName]
          :where    [[:alice :knows ?y]
                     {:select   [?y [(min ?name) ?minName]]
                      :where    [[?y :name ?name]]
                      :group-by [?y]}]}
      ))
  (testing "CONSTRUCT queries"
    (are [q] (s/valid? qs/construct-query-spec q)
      '{:prefixes  {:foaf "http://xmlns.com/foaf/0.1/"
                    :org  "http://example.com/ns#"}
        :construct [?x :foaf/name ?name]
        :where     [[?x :org/employeeName ?name]]})))

(comment
  (s/explain qs/select-query-spec
             '{:prefixes {:rdf  "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                          :foaf "http://xmlns.com/foaf/0.1/"}
               :select   [?person]
               :where    [[?person :rdf/type :foaf/Person]
                          [:filter (not-exists [[?person2 :foaf/name ?name]])]]}))

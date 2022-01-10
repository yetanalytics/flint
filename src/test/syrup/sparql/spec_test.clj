(ns syrup.sparql.spec-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.query  :as qs]
            [syrup.sparql.spec.update :as us]))

(deftest select-query-spec-test
  (testing "SELECT with"
    (testing "nothing else"
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
                     [?x :foaf/mbox ?mbox]]}
        '{:prefixes        {:foaf "http://xmlns.com/foaf/0.1/"}
          :select-distinct [?name]
          :where           [[?x :foaf/name ?name]]}
        '{:prefixes       {:foaf "http://xmlns.com/foaf/0.1/"}
          :select-reduced [?name]
          :where          [[?x :foaf/name ?name]]}
        '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
          :select   [[(concat ?G " " ?S) ?name]]
          :where    [{?P {:foaf/givenName #{?G}
                          :foaf/surname #{?S}}}]}))
    (testing "FILTER"
      (are [q] (s/valid? qs/select-query-spec q)
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
                                           [:filter (= ?n ?m)]])]]}))
    (testing "OPTIONAL"
      (are [q] (s/valid? qs/select-query-spec q)
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
          :select   [?title ?price]
          :where    [[?x :dc/title ?title]
                     [:optional [[?x :ns/price ?price]
                                 [:filter (< ?price 30)]]]]}))
    (testing "UNION"
      (are [q] (s/valid? qs/select-query-spec q)
        '{:prefixes {:dc10 "http://purl.org/dc/elements/1.0/"
                     :dc11 "http://purl.org/dc/elements/1.1/"}
          :select   [?title]
          :where    [[:union
                      [[?book :dc10/title ?title]]
                      [[?book :dc11/title ?title]]]]}
        '{:prefixes {:dc10 "http://purl.org/dc/elements/1.0/"
                     :dc11 "http://purl.org/dc/elements/1.1/"}
          :select   [?x ?y]
          :where    [[:union
                      [[?book :dc10/title ?x]]
                      [[?book :dc11/title ?y]]]]}))
    (testing "MINUS"
      (are [q] (s/valid? qs/select-query-spec q)
        '{:prefixes        {:$    "http://example/"
                            :foaf "http://xmlns.com/foaf/0.1/"}
          :select-distinct [?s]
          :where           [[?s ?p ?o]
                            [:minus [[?s :foaf/givenName "Bob"]]]]}
        '{:prefixes {:$ "http://example.com/"}
          :select   *
          :where    [[?x :p ?n]
                     [:minus [[?x :q ?m]
                              [:filter (= ?n ?m)]]]]}))
    (testing "BIND"
      (are [q] (s/valid? qs/select-query-spec q)
        '{:prefxes {:dc "http://purl.org/dc/elements/1.1/"
                    :ns "http://example.org/ns#"}
          :select  [?title ?price]
          :where   [[?x :ns/price ?price]
                    [?x :ns/discount ?discount]
                    [:bind [(* ?p (- 1 ?discount)) ?price]]
                    [:filter (< ?price 20)]
                    [?x :dc/title ?title]]}
        '{:prefxes {:dc "http://purl.org/dc/elements/1.1/"
                    :ns "http://example.org/ns#"}
          :select  [?title ?price]
          :where   [[[?x :ns/price ?price]
                     [?x :ns/discount ?discount]
                     [:bind [(* ?p (- 1 ?discount)) ?price]]]
                    [:filter (< ?price 20)]
                    [?x :dc/title ?title]]}))
    (testing "VALUES"
      (are [q] (s/valid? qs/select-query-spec q)
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
                          :ns/price #{?price}}}]}))
    (testing "ORDER BY"
      (are [q] (s/valid? qs/select-query-spec q)
        '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
          :select   [?name]
          :where    [[?x :foaf/name ?name]]
          :order-by [?name]}
        '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"
                     :$    "http://example.org/ns#"}
          :select   [?name]
          :where    [{?x {:foaf/name #{?name} :empId #{?emp}}}]
          :order-by [(desc ?name)]}
        '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"
                     :$    "http://example.org/ns#"}
          :select   [?name]
          :where    [{?x {:foaf/name #{?name} :empId #{?emp}}}]
          :order-by [?name (desc ?emp)]}))
    (testing "LIMIT and OFFSET"
      (are [q] (s/valid? qs/select-query-spec q)
        '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
          :select   [?name]
          :where    [[?x :foaf/name ?name]]
          :limit    20}
        '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
          :select   [?name]
          :where    [[?x :foaf/name ?name]]
          :order-by [?name]
          :limit    5
          :offset   10}))
    (testing "FROM and FROM NAMED"
      (are [q] (s/valid? qs/select-query-spec q)
        '{:prefixes   {:foaf "http://xmlns.com/foaf/0.1/"
                       :dc   "http://purl.org/dc/elements/1.1/"}
          :select     [?name ?mbox ?date]
          :where      [{?g {:dc/publisher #{?name}
                            :dc/date      #{?date}}}
                       [:graph ?g [{?person {:foaf/name #{?name}
                                             :foaf/mbox #{?mbox}}}]]]}
        '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
          :select   [?name]
          :from     "http://example.org/foaf/aliceFoaf"
          :where    [[?x :foaf/name ?name]]}
        '{:prefixes   {:foaf "http://xmlns.com/foaf/0.1/"}
          :select     [?src ?bobNick]
          :from-named ["http://example.org/foaf/aliceFoaf"
                       "http://example.org/foaf/bobFoaf"]
          :where      [[:graph ?src [[?x :foaf/mbox "mailto:bob@work.example"]
                                     [?x :foaf/name ?bobNick]]]]}
        '{:prefixes   {:foaf "http://xmlns.com/foaf/0.1/"
                       :data "http://example.org/foaf/"}
          :select     [?nick]
          :from-named ["http://example.org/foaf/aliceFoaf"
                       "http://example.org/foaf/bobFoaf"]
          :where      [[:graph
                        :data/bobFoaf
                        [[?x :foaf/mbox "mailto:bob@work.example"]
                         [?x :foaf/name ?nick]]]]}
        '{:prefixes   {:foaf "http://xmlns.com/foaf/0.1/"
                       :data "http://example.org/foaf/"
                       :rdfs "http://www.w3.org/2000/01/rdf-schema#"}
          :select     [?mbox ?nick ?ppd]
          :from-named ["http://example.org/foaf/aliceFoaf"
                       "http://example.org/foaf/bobFoaf"]
          :where      [[:graph
                        :data/aliceFoaf
                        [{?alice {:foaf/mbox  #{"mailto:alice@work.example"}
                                  :foaf/knows #{?whom}}
                          ?whom  {:foaf/mbox    #{?mbox}
                                  :rdfs/seeAlso #{?ppd}}
                          ?ppd   {a #{:foaf/PersonalProfileDocument}}}]]
                       [:graph
                        ?ppd
                        [{?w {:foaf/mbox #{?mbox}
                              :foaf/nick #{?nick}}}]]]}
        '{:prefixes   {:foaf "http://xmlns.com/foaf/0.1/"
                       :dc   "http://purl.org/dc/elements/1.1/"}
          :select     [?who ?g ?mbox]
          :from       "http://example.org/dft.ttl"
          :from-named ["http://example.org/alice"
                       "http://example.org/bob"]
          :where      [[?g :dc/publisher ?who]
                       [:graph ?g [[?x :foaf/mbox ?mbox]]]]}))
    (testing "aggregation"
      (are [q] (s/valid? qs/select-query-spec q)
        '{:select   [[(avg ?y) ?avg]]
          :where    [{?a {:x #{?x}
                          :y #{?y}}}]
          :group-by [?x]}
        '{:prefixes {:$ "http://data.example/"}
          :select   [[(avg ?size) ?asize]]
          :where    [[?x :size ?size]]
          :group-by [?x]
          :having   [(> (avg ?size) 10)]}
        '{:prefixes {:$ "http://example.com/data/#"}
          :select   [?x [(* (min ?y) 2) ?min]]
          :where    [[?x :p ?y] [?x :q ?z]]
          :group-by [?x (str ?z)]}
        '{:prefixes {:$ "http://example.com/data/#"}
          :select   [?g [(avg ?p) ?avg] [(/ (+ (min ?p) (max ?p)) 2) ?c]]
          :where    [[?g :p ?p]]
          :group-by [?g]}
        '{:prefixes {:$ "http://books.example/"}
          :select   [[(sum ?lprice) ?totalPrice]]
          :where    [[?org :affiliates ?auth]
                     [?auth :writesBook ?book]
                     [?book :price ?lprice]]
          :group-by [?org]
          :having   [(> (sum ?lprice) 10)]}))
    (testing "subqueries"
      (are [q] (s/valid? qs/select-query-spec q)
        '{:prefixes {:$ "http://people.example/"}
          :select   [?y ?minName]
          :where    [[:alice :knows ?y]
                     {:select   [?y [(min ?name) ?minName]]
                      :where    [[?y :name ?name]]
                      :group-by [?y]}]}))
    (testing "property paths"
      (are [q] (s/valid? qs/select-query-spec q)
        '{:prefixes {:$ "http://example/"}
          :select   *
          :where    [[:s (cat :item :price) ?x]]}
        '{:prefixes {:$ "http://example/"}
          :select   [[(sum ?x) ?total]]
          :where    [[:s (cat :item :price) ?x]]}
        '{:prefixes {:rdfs "http://www.w3.org/2000/01/rdf-schema#"
                     :rdf  "http://www.w3.org/1999/02/22-rdf-syntax-ns#"}
          :select   [?x ?type]
          :where    [[?x (cat :rdf/type (* :rdfs/subClassOf)) ?type]]}
        '{:prefixes {:$    "http://example/"
                     :foaf "http://xmlns.com/foaf/0.1/"}
          :select   [?person]
          :where    [[:x (+ :foaf/knows) ?person]]}))))

(deftest nonselect-query-spec-test
  (testing "CONSTRUCT queries"
    (are [q] (s/valid? qs/construct-query-spec q)
      '{:prefixes  {:foaf  "http://xmlns.com/foaf/0.1/"
                    :vcard "http://www.w3.org/2001/vcard-rdf/3.0#"}
        :construct [["http://example.org/person#Alice" :vcard/FN ?name]]
        :where     [[?x :foaf/name ?name]]}
      '{:prefixes  {:foaf "http://xmlns.com/foaf/0.1/"
                    :org  "http://example.com/ns#"}
        :construct [[?x :foaf/name ?name]]
        :where     [[?x :org/employeeName ?name]]}
      '{:prefixes  {:foaf  "http://xmlns.com/foaf/0.1/"
                    :vcard "http://www.w3.org/2001/vcard-rdf/3.0#"}
        :construct [[?x :vcard/N _v]
                    [_v :vcard/givenName ?gname]
                    [_v :vcard/familyName ?fname]]
        :where     [[:union [[?x :foaf/firstname ?gname]
                             [?x :foaf/givenname ?gname]]]
                    [:union [[?x :foaf/surname ?fname]
                             [?x :foaf/family_name ?fname]]]]}
      '{:prefixes  {:dc  "http://purl.org/dc/elements/1.1/"
                    :app "http://example.org/ns#"
                    :xsd "http://www.w3.org/2001/XMLSchema#"}
        :construct [[?s ?p ?o]]
        :where     [[:graph ?g [[?s ?p ?o]]]
                    [?g :dc/publisher "http://www.w3.org"]
                    [?g :dc/date ?date]
                    [:filter (> (:app/customDate ?date)
                                #inst "2005-02-28T00:00:00Z")]]}
      '{:prefixes  {:foaf "http://xmlns.com/foaf/0.1/"
                    :site "http://example.org/stats#"}
        :construct [[_ :foaf/name ?name]]
        :where     [{_ {:foaf/name #{?name}
                        :site/hits #{?hits}}}]
        :order-by  [(desc ?hits)]
        :limit     2}
      '{:prefixes        {:foaf "http://xmlns.com/foaf/0.1/"}
        :construct-where [[?x :foaf/name ?name]]}))
  (testing "ASK queries"
    (are [q] (s/valid? qs/ask-query-spec q)
      '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
        :ask      [[?x :foaf/name "Alice"]]}
      '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
        :ask      [{?x {:foaf/name #{"Alice"}
                        :foaf/mbox #{"mailto:alice@work.example"}}}]}))
  (testing "DESCRIBE queries"
    (are [q] (s/valid? qs/describe-query-spec q)
      '{:describe ["http://example.org"]}
      '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
        :describe [?x]
        :where    [[?x :foaf/mbox "mailto:alice@org"]]}
      '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
        :describe [?x]
        :where    [[?x :foaf/name "Alice"]]}
      '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
        :describe [?x ?y "http://example.org"]
        :where    [[?x :foaf/knows ?y]]}
      '{:prefixes {:ent "http://org.example.com/employees#"}
        :describe [?x]
        :where    [[?x :ent/employeeId "1234"]]})))

(comment
  (s/explain qs/select-query-spec
             '{:prefixes {:$ "http://people.example/"}
               :select   [?y ?minName]
               :where    [[:alice :knows ?y]
                          {:select   [?y [(min ?name) ?minName]]
                           :where    [[?y :name ?name]]
                           :group-by [?y]}]}))

(deftest update-spec-test
  (testing "INSERT DATA"
    (are [u] (s/valid? us/insert-data-update-spec u)
      '{:prefixes {:dc "http://purl.org/dc/elements/1.1/"}
        :insert-data [{"http://example/book1"
                       {:dc/title   #{"A new book"}
                        :dc/creator #{"A. N. Other"}}}]}
      '{:prefixes {:dc "http://purl.org/dc/elements/1.1/"
                   :ns "http://example.org/ns#"}
        :insert-data [[:graph "http://example/bookStore"
                       [["http://example/book1" :ns/price 42]]]]}))
  (testing "DELETE DATA"
    (are [u] (s/valid? us/delete-data-update-spec u)
      '{:prefixes {:dc "http://purl.org/dc/elements/1.1/"}
        :delete-data [{"http://example/book2"
                       {:dc/title   #{"David Copperfield"}
                        :dc/creator #{"Edmund Wells"}}}]}))
  (testing "DELETE/INSERT"
    (are [u] (s/valid? us/modify-update-spec u)
      '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
        :with     "http://example/addresses"
        :delete   [[?person :foaf/givenName "Bill"]
                   [?person :foaf/givenName "William"]]
        :where    [[?person :foaf/givenName "Bill"]]}
      '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
        :with     "http://example/addresses"
        :delete   [[?person ?property ?value]]
        :where    [{?person {?property       #{?value}
                             :foaf/givenName #{"Fred"}}}]}
      '{:prefixes {:dc  "http://purl.org/dc/elements/1.1/"
                   :xsd "http://www.w3.org/2001/XMLSchema#"}
        :insert   [[:graph "http://example/bookStore2" [[?book ?p ?v]]]]
        :where    [[:graph
                    "http://example/bookStore"
                    [[?book :dc/date ?date]
                     [:filter (> ?date #inst "1970-01-01T00:00:00-02:00")]
                     [?book ?p ?v]]]]}
      '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"
                   :rdf  "http://www.w3.org/1999/02/22-rdf-syntax-ns#"}
        :insert   [[:graph
                    "http://example/addresses"
                    [[?person :foaf/name ?name]
                     [?person :foaf/mbox ?email]]]]
        :where    [[:graph
                    "http://example/people"
                    [[?person :foaf/name ?name]
                     [:optional [[?person :foaf/mbox ?email]]]]]]}
      '{:prefixes {:foaf "http://xmlns.com/foaf/0.1/"}
        :with     "http://example/addresses"
        :delete   [[?person :foaf/givenName "Bill"]]
        :insert   [[?person :foaf/givenName "William"]]
        :where    [[?person :foaf/givenName "Bill"]]}))
  (testing "DELETE WHERE"
    (are [u] (s/valid? us/delete-where-update-spec u)
      '{:prefixes     {:foaf "http://xmlns.com/foaf/0.1/"}
        :delete-where [{?person {:foaf/givenName #{"Fred"}
                                 ?property       #{?value}}}]}
      '{:prefixes     {:foaf "http://xmlns.com/foaf/0.1/"}
        :delete-where [[:graph
                        "http://example.com/names"
                        [{?person {:foaf/givenName #{"Fred"}
                                   ?property1      #{?value1}}}]]
                       [:graph
                        "http://example.com/addresses"
                        [[?person ?property2 ?value2]]]]}))
  (testing "COPY"
    (is (s/valid? us/copy-update-spec
                  '{:copy :default
                    :to   "http://example.org/named"})))
  (testing "MOVE"
    (is (s/valid? us/move-update-spec
                  '{:move :default
                    :to   "http://example.org/named"})))
  (testing "MOVE"
    (is (s/valid? us/add-update-spec
                  '{:add :default
                    :to  "http://example.org/named"}))))

(comment
  (s/explain us/insert-data-update-spec
             '{:prefixes {:dc "http://purl.org/dc/elements/1.1/"
                          :ns "http://example.org/ns#"}
               :insert-data [[:graph "http://example/bookStore"
                              [["http://example/book1" :ns/price 42]]]]})
  (s/explain us/delete-data-update-spec
             '{:prefixes {:dc "http://purl.org/dc/elements/1.1/"}
               :delete-data [{"http://example/book2"
                              {:dc/title   #{"David Copperfield"}
                               :dc/creator #{"Edmund Wells"}}}]}))

(comment
  '{:select [?v]
    :where  [[?v ?p 42]]}
  
  '[?v ?p 42]
  '[:triple [?v ?p 42]]
  "?v ?p 42 ."

  '{?v {?p #{42}}}
  '[:spo [?v [:po [?p [:o 42]]]]]
  )

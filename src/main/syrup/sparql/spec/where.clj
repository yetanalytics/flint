(ns syrup.sparql.spec.where
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]
            [syrup.sparql.spec.expr :as ex]
            [syrup.sparql.spec.triple :as triple]
            [syrup.sparql.spec.modifier :as ms]
            [syrup.sparql.spec.select :as ss]
            [syrup.sparql.spec.value :as vs]))

(s/def ::select
  (s/merge
   (s/keys :req-un [(or ::ss/select ::ss/select-distinct ::ss/select-reduced)
                    ::where]
           :opt-un [::vs/values])
   ms/solution-modifier-spec))

(s/def ::where
  (s/or :sub-select ::select
        :group (s/* (s/alt
                     :triple   triple/triple-vec-spec
                     :nform    triple/normal-form-spec
                     :group    ::where
                     :union    (s/cat ::s/k #{:union}
                                      ::s/v (s/+ ::where))
                     :optional (s/cat ::s/k #{:optional}
                                      ::s/v ::where)
                     :minus    (s/cat ::s/k #{:minus}
                                      ::s/v ::where)
                     :graph    (s/cat ::s/k #{:graph}
                                      ::s/v (s/cat :iri ax/var-or-iri-spec
                                                   :pat ::where))
                     :service  (s/cat ::s/k #{:service :service-silent}
                                      ::s/v (s/cat :iri ax/var-or-iri-spec
                                                   :pat ::where))
                     :filter   (s/cat ::s/k #{:filter}
                                      ::s/v ::ex/expr)
                     :bind     (s/cat ::s/k #{:bind}
                                      ::s/v ::ex/expr-as-var)
                     :values   (s/cat ::s/k #{:values}
                                      ::s/v ::vs/values)))))

(comment
  (s/explain triple/triple-vec-spec [:?s :?p :?o])
  (s/explain-data union-spec [:union
                              [[:?book :dc10/title :?title]]
                              [[:?book :dc11/title :?title]]])
  (s/explain ::where [[:?s :?p :?o]
                          {:?s {:?p #{:?o1 :?o2}}}
                          [:union
                           [[:?book :dc10/title :?title]]
                           [[:?book :dc11/title :?title]]]
                          [:optional
                           [[:?s :?p :?o]]]
                          [:graph "http://example.org#graph"
                           [{:?s {:?p #{:?o}}}]]])
  
  (s/explain ::where '[[?x :dc/title ?title]
                           [:filter (regex ?title "^SPARQL")]]))

(comment

  (s/conform triple/triples-spec {:?s {:?p #{:?o1 :?o2}}})
  {:union [[:?book :dc10/title :?title]
           [:?book :dc11/title :?title]]}

  {:union [{:?s {:?p #{:?o1 :?o2}}}
           {:?s {:?p #{:?o3 :?o4}}}
           {:optional {:?s {:?p #{:?o}}}}]}

;;   {?book dc10:title  ?title} UNION {?book dc11:title  ?title}
  (s/explain ::where [{:?s2 {:?p2 #{:?o2}}}])
  (s/explain (s/keys :req-un [::union])
             {:union [{:?s2 {:?p2 #{:?o2}}}
                      {:?s3 {:?p3 #{:?o3}}}]})

  (s/conform ::where
             ['[?s ?p ?o]
              {:union [{:?s2 {:?p2 #{:?o2}}}
                       {:?s3 {:?p3 #{:?o3}}}]}
              {:optional []}]))

;; WHERE {
;;   ?s1 ?p1 ?o1 . ?s2 ?p2 ?2 .
;;   { ?s3 ?p3 ?o3 } UNION { ?s4 ?p4 ?o4 . OPTIONAL { ?s5 ?p5 ?o5 } }
;; }

(comment
  {:?s1 {:?p1 #{:?o1}}
   :?s2 {:?p2 #{:?o2}}
   :union [{:?s3 {:?p3 #{:?o3}}}
           {:?s4 {:?p4 #{:?o4}}
            :optional {:?s5 {:?p5 #{:?o5}}}}]}

  {:triples [[:?s1 :?p1 :?o1]
             [:?s2 :?p2 :?o2]]
   :union [{:triples [:?s3 :?p3 :?o3]}]}

  [:where
   [:?s1 :?p1 :?o1]
   [:?s2 :?p2 :?o2]
   '(:union [[:?s3 :?p3 :?o3]])])

(comment
  [{:?s1 {:?p1 #{:?o1}}
    :?s2 {:?p2 #{:?o2}}}
   :union [[{:?s3 {:?p3 #{:?o3}}}]
           [{:?s4 {:?p4 #{:?o4}}
             :optional {:?s5 {:?p5 #{:?o5}}}}]]])

(comment
  [:where
   {:?s1 {:?p1 #{:?o1}}
    :?s2 {:?p2 #{:?o2}}}
   [:union [{:?s3 {:?p3 #{:?o3}}}]]])

;; WHERE {
;;   ?s1 ?p1 ?o1 .
;;   OPTIONAL { ?s2 ?p2 ?o2 }
;;   GRAPH <http://example.org#graph> { ?s3 ?p3 ?o3 }
;;   SERVICE SILENT <http://example.org#external> { ?s4 ?p4 ?o4 }
;;   BIND ( 2 AS ?two )
;; }

(comment
  {:triples [[:?s1 :?p1 :?o1]]
   :optional {:triples [[:?s2 :?p2 :?o2]]}
   :graph {"http://example.org#graph" [[:?s3 :?p3 :?o3]]}
   :service-silent {"http://example.org#graph" [[:?s4 :?p4 :?o4]]}
   :bind [2 :as :?two]})

(comment
  [[[:?s1 :?p1 :?o1]]
   [:optional [[[:?s2 :?p2 :?o2]]]]
   [:graph {"http://example.org#graph" [[[:?s3 :?p3 :?o3]]]}]
   [:service-silent {"http://example.org#graph" [[[:?s4 :?p4 :?o4]]]}]
   [:bind [2 :as :?two]]])

(comment
  [:where
   [:?s1 :?p1 :?o1]
   [:optional [[:?s2 :?p2 :?o2]]]
   [:graph "http://example.org#graph" [[:?s3 :?p3 :?o3]]]
   [:service-silent "http://example.org#external" [[:?s4 :?p4 :?o4]]]
   [:bind [2 :?two]]])


;; WhereClause := 'WHERE'? GroupGraphPattern
;; GroupGraphPattern := '{' ( SubSelect | GroupGraphPatternSub ) '}'
;; GroupGraphPatternSub := 	TriplesBlock? (GraphPatternNotTriples '.'? TriplesBlock?) *
;; GraphPatternNotTriples ::=
;;     GroupOrUnionGraphPattern
;;   | OptionalGraphPattern
;;   | MinusGraphPattern
;;   | GraphGraphPattern
;;   | ServiceGraphPattern
;;   | Filter
;;   | Bind
;;   | InlineData

;; WHERE { 
;;   OPTIONAL { :?s1 :?p1 :?o1 } .
;;   OPTIONAL { { ?s2 ?p2 ?o2 } UNION { ?s3 ?p3 ?o3 } . 
;;              { ?s4 ?p4 ?o4 } UNION { ?s5 ?p5 ?o5 } }
;; }

(comment
  [[:optional [{:?s1 {:?p1 #{:?o1}}}]]
   [:optional [{:?s2 {:?p2 #{:?o2}}}]]])

(comment
  [:optional [{:?s1 {:?p1 #{:?o1}}}]
   :optional [:union [[{:?s1 {:?p1 #{:?o1}}}]
                      [{:?s1 {:?p1 #{:?o1}}}]]
              :union [[{:?s1 {:?p1 #{:?o1}}}]
                      [{:?s1 {:?p1 #{:?o1}}}]]]])

(comment
  {:optional [{:?s1 {:?p1 #{:?o1}}}
              {:union [{:?s1 {:?p1 #{:?o1}}}
                       {:?s1 {:?p1 #{:?o1}}}]}]})

(comment
  [:where
   [:optional [[:?s1 :?p1 :?o1]]]
   [:optional
    [[:union [[:?s2 :?p2 :?o2]] [[:?s3 :?p3 :?o3]]]
     [:union [[:?s4 :?p4 :?o4]] [[:?s5 :?p5 :?o5]]]]]])

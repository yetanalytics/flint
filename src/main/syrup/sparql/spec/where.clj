(ns syrup.sparql.spec.where
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]
            [syrup.sparql.spec.expr :as ex]
            [syrup.sparql.spec.triple :as triple]
            [syrup.sparql.spec.value :as vs]))

;; Forward declare where specs
(declare where-one-spec)
(declare where-spec)

(def union-spec
  (s/and vector?
         (s/cat ::s/k #{:union}
                ::s/v (s/+ where-spec))))

(s/def ::optional where-spec)
(def optional-spec
  (s/and vector? (s/keys* :req-un [::optional])))

(s/def ::minus where-spec)
(def minus-spec
  (s/and vector? (s/keys* :req-un [::minus])))

(def graph-spec
  (s/and vector?
         (s/cat ::s/k #{:graph}
                ::s/v (s/cat :iri ax/var-or-iri-spec
                             :pat where-spec))))

(def service-spec
  (s/and vector?
         (s/cat ::s/k #{:service :service-silent}
                ::s/v (s/cat :iri ax/var-or-iri-spec
                             :pat where-spec))))

(s/def ::filter ex/expr-spec)
(def filter-spec
  (s/and vector? (s/keys* :req-un [::filter])))

(s/def ::bind ex/expr-as-var-spec)
(def bind-spec
  (s/and vector? (s/keys* :req-un [::bind])))

(def values-spec
  (s/and vector?
         (s/cat :kword #{:values}
                :pattern vs/values-clause-spec)))

;; TODO: SolutionModifier
;; TODO: where clause
(def where-select-spec
  (s/keys :req-un [(or ::select ::select-distinct ::select-where)
                   ::values]))

(def where-keys
  #{::union
    ::optional
    ::minus
    ::graph
    ::service
    ::service-silent
    ::filter
    ::filter-exists
    ::filter-not-exists
    ::bind
    ::values})

(def where-keys-un
  (map (fn [k] (keyword (name k))) where-keys))

(def where-graph-pat-spec
  (s/+ (s/or :triples triple/normal-form-spec
             :not-triples (s/and (s/cat :k where-keys
                                        :v any?)
                                 (fn [{:keys [k v]}]
                                   (s/valid? k v))))))

(def where-spec*
  (s/* (s/alt :triple   triple/triple-vec-spec
              :nform    triple/normal-form-spec
              :union    union-spec
              :optional optional-spec
              :minus    minus-spec
              :graph    graph-spec
              :service  service-spec
              :filter   filter-spec
              :bind     bind-spec
              :values   values-spec)))

(def where-spec
  (s/or :graph-pattern where-spec*
        :sub-select where-select-spec))

(s/def ::where where-spec)


(comment
  (s/explain triple/triple-vec-spec [:?s :?p :?o])
  (s/explain-data union-spec [:union
                              [[:?book :dc10/title :?title]]
                              [[:?book :dc11/title :?title]]])
  (s/explain where-spec* [[:?s :?p :?o]
                          {:?s {:?p #{:?o1 :?o2}}}
                          [:union
                           [[:?book :dc10/title :?title]]
                           [[:?book :dc11/title :?title]]]
                          [:optional
                           [[:?s :?p :?o]]]
                          [:graph "http://example.org#graph"
                           [{:?s {:?p #{:?o}}}]]]))

(comment

  (s/conform triple/triples-spec {:?s {:?p #{:?o1 :?o2}}})
  {:union [[:?book :dc10/title :?title]
           [:?book :dc11/title :?title]]}

  {:union [{:?s {:?p #{:?o1 :?o2}}}
           {:?s {:?p #{:?o3 :?o4}}}
           {:optional {:?s {:?p #{:?o}}}}]}

;;   {?book dc10:title  ?title} UNION {?book dc11:title  ?title}
  (s/explain where-spec [{:?s2 {:?p2 #{:?o2}}}])
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

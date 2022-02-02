(ns com.yetanalytics.flint
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.query    :as qs]
            [com.yetanalytics.flint.spec.update   :as us]
            [com.yetanalytics.flint.format        :as f]
            [com.yetanalytics.flint.format.query]
            [com.yetanalytics.flint.format.update :as uf]
            [com.yetanalytics.flint.prefix        :as pre]
            [com.yetanalytics.flint.error         :as err]))

(def xsd-iri-prefix
  "<http://www.w3.org/2001/XMLSchema#>")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Conform Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-xsd-prefix
  [prefixes]
  (reduce-kv (fn [_ k v]
               (if (= xsd-iri-prefix v)
                 (reduced (name k))
                 nil))
             nil
             prefixes))

(defn- conform-sparql-err-map
  [error-kw spec-ed sparql]
  {:kind  error-kw
   :error spec-ed
   :input sparql})

(defn- conform-sparql
  ([error-kw spec sparql]
   (let [ast (s/conform spec sparql)]
     (if (= ::s/invalid ast)
       (let [spec-ed (s/explain-data spec sparql)
             err-msg (err/spec-error-msg spec-ed)
             err-map (conform-sparql-err-map error-kw spec-ed sparql)]
         (throw (ex-info err-msg err-map)))
       ast)))
  ([error-kw spec sparql index]
   (let [ast (s/conform spec sparql)]
     (if (= ::s/invalid ast)
       (let [spec-ed (s/explain-data spec sparql)
             err-msg (err/spec-error-msg spec-ed index)
             err-map (assoc (conform-sparql-err-map error-kw spec-ed sparql)
                            :index index)]
         (throw (ex-info err-msg err-map)))
       ast))))

(def conform-query
  (partial conform-sparql ::invalid-query qs/query-spec))
(def conform-update
  (partial conform-sparql ::invalid-update us/update-spec))

(defn- conform-prefixes-err-map
  [prefix-errs sparql ast]
  {:kind  ::invalid-prefixes
   :error prefix-errs
   :input sparql
   :ast   ast})

(defn- conform-prefixes
  ([sparql ast ?prefixes]
   (let [prefixes (or ?prefixes {})]
     (if-some [prefix-errs (pre/validate-prefixes prefixes ast)]
       (throw (ex-info (err/prefix-error-msg prefix-errs)
                       (conform-prefixes-err-map prefix-errs sparql ast)))
       prefixes)))
  ([sparql ast ?prefixes index]
   (let [prefixes (or ?prefixes {})]
     (if-some [prefix-errs (pre/validate-prefixes prefixes ast)]
       (throw (ex-info (err/prefix-error-msg prefix-errs index)
                       (assoc (conform-prefixes-err-map prefix-errs sparql ast)
                              :index index)))
       prefixes))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn format-query
  "Format `query` into a SPARQL Query string. Throws an exception if `query`
   does not conform to spec or if its prefixed IRIs cannot be expanded."
  [query & {:keys [pretty?] :or {pretty? false}}]
  (let [ast      (conform-query query)
        prefix-m (conform-prefixes query ast (:prefixes query))
        ?xsd-pre (get-xsd-prefix prefix-m)
        opt-m    (cond-> {:pretty? pretty?}
                   ?xsd-pre (assoc :xsd-prefix ?xsd-pre))]
    (f/format-ast ast opt-m)))

;; `format-updates` is internally quite different from a simple coll
;; map, hence the need for a separate coll fn. Not only that, but the
;; two fns emit slightly different error messages.

(defn format-update
  "Format `update` into a SPARQL Update string. Throws an exception if `update`
   does not conform to spec or if its prefixed IRIs cannot be expanded."
  [update & {:keys [pretty?] :or {pretty? false}}]
  (let [ast      (conform-update update)
        prefix-m (conform-prefixes update ast (:prefixes update))
        ?xsd-pre (get-xsd-prefix prefix-m)
        opt-m    (cond-> {:pretty? pretty?}
                   ?xsd-pre (assoc :xsd-prefix ?xsd-pre))]
    (f/format-ast ast opt-m)))

(defn format-updates
  "Format the coll `updates` into a SPARQL Update Request string. Throws
   an exception if any update does not conform to spec or has a prefixed
   IRI that cannot be expanded."
  [updates & {:keys [pretty?] :or {pretty? false}}]
  (let [indexes      (-> updates count range)
        asts         (map conform-update updates indexes)
        prefix-maps* (reduce (fn [pm-coll {pm :prefixes :as _update}]
                               (let [last-pm (last pm-coll)
                                     new-pm  (merge last-pm pm)]
                                 (conj pm-coll new-pm)))
                             []
                             updates)
        prefix-maps  (map conform-prefixes updates asts prefix-maps* indexes)
        xsd-prefixes (map get-xsd-prefix prefix-maps)
        opt-maps     (map (fn [?xsd-pre]
                            (cond-> {:pretty? pretty?}
                              ?xsd-pre (assoc :xsd-prefix ?xsd-pre)))
                          xsd-prefixes)]
    (-> (map f/format-ast asts opt-maps)
        (uf/join-updates pretty?))))

(comment
  (format-update
   '{:prefixes
     {:dc "<http://purl.org/dc/elements/1.1/>"
      :xsd "<http://www.w3.org/2001/XMLSchema#>"}
     :insert [[:graph "<http://example/bookStore2>" [[?book ?p ?v]]]]
     :where
     [[:graph
       "<http://example/bookStore>"
       [[?book :dc/date ?date]
        [:filter (> ?date #inst "1970-01-01T00:00:00.000-00:00")]
        [?book ?p ?v]]]]})
  (format-updates
   '[{:prefixes    {:dc "<http://purl.org/dc/elements/1.1/>"}
      :delete-data [[:graph "<http://example/bookStore>"
                     [["<http://example/book1>"
                       :dc/title
                       "Fundamentals of Compiler Desing"]]]]}
     {:prefixes    {:dc "<http://purl.org/dc/elements/1.1/>"}
      :insert-data [[:graph "<http://example/bookStore>"
                     [["<http://example/book1>"
                       :dc/title
                       "Fundamentals of Compiler Design"]]]]}]
   :pretty? true))

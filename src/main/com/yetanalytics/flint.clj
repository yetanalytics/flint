(ns com.yetanalytics.flint
  (:require [clojure.spec.alpha :as s]
            [clojure.walk       :as w]
            [com.yetanalytics.flint.spec.query  :as qs]
            [com.yetanalytics.flint.spec.update :as us]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.query]
            [com.yetanalytics.flint.format.update :as uf]
            [com.yetanalytics.flint.prefix :as pre]))

(def xsd-iri-prefix
  "<http://www.w3.org/2001/XMLSchema#>")

(defn- get-xsd-prefix
  [prefixes]
  (reduce-kv (fn [_ k v]
               (if (= xsd-iri-prefix v)
                 (reduced (name k))
                 nil))
             nil
             prefixes))

(defn- conform-sparql
  "Validate `sparql` according to `spec` and return the AST if valid,
   throw an exception of kind `error-kw` otherwise."
  [spec sparql error-kw]
  (let [ast (s/conform spec sparql)]
    (if (= ::s/invalid ast)
      (throw (ex-info "Cannot conform according to spec!"
                      {:kind  error-kw
                       :error (s/explain-data spec sparql)
                       :input sparql}))
      ast)))

(defn- conform-prefixes
  "Validate that all prefixed IRIs in `ast` can be expanded using the
   `?prefixes` map, return the prefix map if valid, throw an
   `::invalid-prefixes` exception otherwise."
  [?prefixes sparql ast]
  (let [prefixes (or ?prefixes {})]
    (if-some [prefix-errs (pre/validate-prefixes prefixes ast)]
      (throw (ex-info "Invalid prefixes exist!"
                      {:kind  ::invalid-prefixes
                       :error prefix-errs
                       :input sparql
                       :ast   ast}))
      prefixes)))

(defn format-query
  "Format `query` into a SPARQL Query string. Throws an exception if `query`
   does not conform to spec or if its prefixed IRIs cannot be expanded."
  [query & {:keys [pretty?] :or {pretty? false}}]
  (let [ast      (conform-sparql qs/query-spec query ::invalid-query)
        prefixes (conform-prefixes (:prefixes query) query ast)
        ?xsd-pre (get-xsd-prefix prefixes)
        opts     (cond-> {:pretty? pretty?}
                   ?xsd-pre (assoc :xsd-prefix ?xsd-pre))]
    (w/postwalk (partial f/format-ast opts) ast)))

(defn format-updates
  "Format the coll `updates` into a SPARQL Update Request string. Throws
   an exception if any update does not conform to spec or has a prefixed
   IRI that cannot be expanded."
  [updates & {:keys [pretty?] :or {pretty? false}}]
  (let [asts
        (map (fn [update]
               (conform-sparql us/update-spec update ::invalid-update))
             updates)
        prefix-maps
        (reduce (fn [pm-coll [update ast]]
                  (let [last-pm (last pm-coll)
                        new-pm  (merge last-pm (:prefixes update))]
                    (conj pm-coll (conform-prefixes new-pm update ast))))
                []
                (map vector updates asts))
        xsd-prefixes
        (map get-xsd-prefix prefix-maps)
        opt-maps
        (map (fn [?xsd-pre]
               (cond-> {:pretty? pretty?}
                 ?xsd-pre (assoc :xsd-prefix ?xsd-pre)))
             xsd-prefixes)]
    (-> (map (fn [ast opts] (w/postwalk (partial f/format-ast opts) ast))
             asts
             opt-maps)
        (uf/join-updates pretty?))))

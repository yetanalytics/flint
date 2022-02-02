(ns com.yetanalytics.flint
  (:require [clojure.spec.alpha :as s]
            [clojure.walk       :as w]
            [com.yetanalytics.flint.spec.query  :as qs]
            [com.yetanalytics.flint.spec.update :as us]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.query]
            [com.yetanalytics.flint.format.update]
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

(defn- conform-sparql [spec sparql error-kw]
  (let [ast (s/conform spec sparql)]
    (if (= ::s/invalid ast)
      (throw (ex-info "Cannot conform according to spec!"
                      {:kind  error-kw
                       :error (s/explain-data spec sparql)}))
      ast)))

(defn- conform-prefixes* [?prefixes ast]
  (let [prefixes (or ?prefixes {})]
    (if-some [prefix-errs (pre/validate-prefixes prefixes ast)]
      (throw (ex-info "Invalid prefixes exist!"
                      {:kind  ::invalid-prefixes
                       :error prefix-errs}))
      prefixes)))

(defn- conform-prefixes [sparql ast]
  (let [prefixes (or (:prefixes sparql) {})]
    (conform-prefixes* prefixes ast)))

;; TODO: Same for BASEs
(defn- conform-multi-prefixes [sparqls asts]
  (let [prefix-coll (map :prefixes sparqls)
        fst-prefix  (first prefix-coll)
        rst-prefixs (rest prefix-coll)]
    (if-not (or (and (some? fst-prefix)
                     (every? nil? rst-prefixs))
                (every? #(= fst-prefix %) rst-prefixs))
      (throw (ex-info "Must either have one global prefix set or identical prefixes!"
                      {:kind ::invalid-multi-prefixes
                       :prefixes prefix-coll}))
      (conform-prefixes* fst-prefix asts))))

(defn format-query
  "Format `query` into a SPARQL Query string. Throws an exception if `query`
   does not conform to spec."
  [query & {:keys [pretty?] :or {pretty? false}}]
  (let [ast      (conform-sparql qs/query-spec query ::invalid-query)
        prefixes (conform-prefixes query ast)
        ?xsd-pre (get-xsd-prefix prefixes)
        opts     (cond-> {:pretty? pretty?}
                   ?xsd-pre (assoc :xsd-prefix ?xsd-pre))]
    (w/postwalk (partial f/format-ast opts) ast)))

;; TODO: Refactor format-updates to reduce code duplication
(defn format-updates
  "Format a coll of `updates` into a SPARQL Update Request string.
   Throws an exception if the updates do not conform to spec."
  [updates & {:keys [pretty?] :or {pretty? false}}]
  (if (< 1 (count updates))
    (let [ast      (conform-sparql us/update-request-spec updates ::invalid-update-request)
          prefixes (conform-multi-prefixes updates ast)
          ?xsd-pre (get-xsd-prefix prefixes)
          opts     (cond-> {:pretty? pretty?}
                     ?xsd-pre (assoc :xsd-prefix ?xsd-pre))]
      (w/postwalk (partial f/format-ast opts) ast))
    (let [update   (first updates)
          ast      (conform-sparql us/update-spec update ::invalid-update)
          prefixes (conform-prefixes update ast)
          ?xsd-pre (get-xsd-prefix prefixes)
          opts     (cond-> {:pretty? pretty?}
                     ?xsd-pre (assoc :xsd-prefix ?xsd-pre))]
      (w/postwalk (partial f/format-ast opts) ast))))

(ns com.yetanalytics.flint
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.format.query]
            [com.yetanalytics.flint.spec.query         :as qs]
            [com.yetanalytics.flint.spec.update        :as us]
            [com.yetanalytics.flint.format             :as f]
            [com.yetanalytics.flint.format.update      :as uf]
            [com.yetanalytics.flint.error              :as err]
            [com.yetanalytics.flint.validate           :as v]
            [com.yetanalytics.flint.validate.aggregate :as va]
            [com.yetanalytics.flint.validate.bnode     :as vb]
            [com.yetanalytics.flint.validate.prefix    :as vp]
            [com.yetanalytics.flint.validate.scope     :as vs]))

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
  [error-kw error-loc-kws sparql]
  {:kind    error-kw
   :input   sparql
   :clauses error-loc-kws})

(defn- conform-sparql
  ([error-kw spec spec-err? sparql]
   (let [ast (s/conform spec sparql)]
     (if (= ::s/invalid ast)
       (let [spec-ed (s/explain-data spec sparql)
             err-kws (err/spec-error-keywords spec-ed)
             err-msg (err/spec-error-msg err-kws)
             err-map (if spec-err?
                       spec-ed
                       (conform-sparql-err-map error-kw err-kws sparql))]
         (throw (ex-info err-msg err-map)))
       ast)))
  ([error-kw spec spec-err? sparql index]
   (let [ast (s/conform spec sparql)]
     (if (= ::s/invalid ast)
       (let [spec-ed (s/explain-data spec sparql)
             err-kws (err/spec-error-keywords spec-ed)
             err-msg (err/spec-error-msg err-kws index)
             err-map (if spec-err?
                       (assoc spec-ed
                              ::index index)
                       (assoc (conform-sparql-err-map error-kw err-kws sparql)
                              :index index))]
         (throw (ex-info err-msg err-map)))
       ast))))

(def conform-query
  (partial conform-sparql ::invalid-query qs/query-spec))
(def conform-update
  (partial conform-sparql ::invalid-update us/update-spec))

(defn- assert-prefixes-err-map
  [prefix-errs sparql ast]
  {:kind   ::invalid-prefixes
   :errors prefix-errs
   :input  sparql
   :ast    ast})

(defn- assert-prefixes
  ([sparql ast nodes-m ?prefixes]
   (let [prefixes (or ?prefixes {})]
     (when-some [prefix-errs (vp/validate-prefixes prefixes nodes-m)]
       (throw (ex-info (err/prefix-error-msg prefix-errs)
                       (assert-prefixes-err-map prefix-errs sparql ast))))))
  ([sparql ast nodes-m ?prefixes index]
   (let [prefixes (or ?prefixes {})]
     (when-some [prefix-errs (vp/validate-prefixes prefixes nodes-m)]
       (throw (ex-info (err/prefix-error-msg prefix-errs index)
                       (assoc (assert-prefixes-err-map prefix-errs sparql ast)
                              :index index)))))))

(defn- assert-scope-err-map
  [scope-errs sparql ast]
  {:kind   ::invalid-scoped-vars
   :errors scope-errs
   :input  sparql
   :ast    ast})

(defn- assert-scoped-vars
  ([sparql ast nodes-m]
   (when-some [errs (vs/validate-scoped-vars nodes-m)]
     (throw (ex-info (err/scope-error-msg errs)
                     (assert-scope-err-map errs sparql ast)))))
  ([sparql ast nodes-m index]
   (when-some [errs (vs/validate-scoped-vars nodes-m)]
     (throw (ex-info (err/scope-error-msg errs index)
                     (assoc (assert-scope-err-map errs sparql ast)
                            :index index))))))

(defn- assert-aggregates-err-map
  [agg-errs sparql ast]
  {:kind   ::invalid-aggregates
   :errors agg-errs
   :input  sparql
   :ast    ast})

(defn- assert-aggregates
  ([sparql ast nodes-m]
   (when-some [errs (va/validate-agg-selects nodes-m)]
     (throw (ex-info (err/aggregate-error-msg errs)
                     (assert-aggregates-err-map errs sparql ast)))))
  ([sparql ast nodes-m index]
   (when-some [errs (va/validate-agg-selects nodes-m)]
     (throw (ex-info (err/aggregate-error-msg errs)
                     (assoc (assert-aggregates-err-map errs sparql ast)
                            :index index))))))

(defn- assert-bnode-err-map
  [{:keys [kind errors prev-bnodes]} sparql ast]
  (cond-> {:errors errors
           :input  sparql
           :ast    ast}
    (= ::vb/dupe-bnodes-bgp kind)
    (assoc :kind ::invalid-bnodes-bgp)
    (= ::vb/dupe-bnodes-update kind)
    (assoc :kind ::invalid-bnodes-update)
    prev-bnodes
    (assoc :prev-bnodes prev-bnodes)))

(defn- assert-bnodes
  [sparql ast nodes-m]
  (let [res (vb/validate-bnodes nodes-m)]
    (when-some [errs (second res)]
      (throw (ex-info (err/bnode-error-msg errs)
                      (assert-bnode-err-map errs sparql ast))))))

(defn- assert-bnodes-coll
  [sparql-coll ast-coll nodes-m-coll]
  (loop [inputs   sparql-coll
         asts     ast-coll
         nodes-ms nodes-m-coll
         bnodes   #{}
         idx      0]
    (when-some [nodes-m (first nodes-ms)]
      (let [res (vb/validate-bnodes bnodes nodes-m)]
        (if-some [errs (second res)]
          (throw (ex-info (err/bnode-error-msg errs idx)
                          (assoc (assert-bnode-err-map errs
                                                       (first inputs)
                                                       (first asts))
                                 :index idx)))
          (recur (rest inputs)
                 (rest asts)
                 (rest nodes-ms)
                 (first res)
                 (inc idx)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn format-query
  "Format `query` into a SPARQL Query string. Throws an exception if `query`
   does not conform to spec or if its prefixed IRIs cannot be expanded."
  [query & {:keys [pretty? validate? spec-ed?] :or {pretty?     false
                                                       validate?   true
                                                       spec-ed? false}}]
  (let [ast      (conform-query spec-ed? query)
        prefix-m (:prefixes query)
        _        (when validate?
                   (let [nodes-m (v/collect-nodes ast)]
                     (assert-prefixes query ast nodes-m prefix-m)
                     (assert-scoped-vars query ast nodes-m)
                     (assert-aggregates query ast nodes-m)
                     (assert-bnodes query ast nodes-m)))
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
  [update & {:keys [pretty? validate? spec-ed?] :or {pretty?     false
                                                     validate?   true
                                                     spec-ed? false}}]
  (let [ast      (conform-update spec-ed? update)
        prefix-m (:prefixes update)
        _        (when validate?
                   (let [nodes-m (v/collect-nodes ast)]
                     (assert-prefixes update ast nodes-m prefix-m)
                     (assert-scoped-vars update ast nodes-m)
                     (assert-aggregates update ast nodes-m)
                     (assert-bnodes update ast nodes-m)))
        ?xsd-pre (get-xsd-prefix prefix-m)
        opt-m    (cond-> {:pretty? pretty?}
                   ?xsd-pre (assoc :xsd-prefix ?xsd-pre))]
    (f/format-ast ast opt-m)))

(defn format-updates
  "Format the coll `updates` into a SPARQL Update Request string. Throws
   an exception if any update does not conform to spec or has a prefixed
   IRI that cannot be expanded."
  [updates & {:keys [pretty? validate? spec-ed?] :or {pretty?     false
                                                      validate?   true
                                                      spec-ed? false}}]
  (let [idxs     (-> updates count range)
        asts     (map (partial conform-update spec-ed?) updates idxs)
        pre-maps (reduce (fn [pm-coll {pm :prefixes :as _update}]
                           (let [last-pm (last pm-coll)
                                 new-pm  (merge last-pm pm)]
                             (conj pm-coll new-pm)))
                         []
                         updates)
        _        (when validate?
                   (let [nodes-m-coll (map v/collect-nodes asts)]
                     (dorun (map assert-prefixes updates asts nodes-m-coll pre-maps idxs))
                     (dorun (map assert-scoped-vars updates asts nodes-m-coll idxs))
                     (dorun (map assert-aggregates updates asts nodes-m-coll idxs))
                     (assert-bnodes-coll updates asts nodes-m-coll)))
        xsd-pres (map get-xsd-prefix pre-maps)
        opt-maps (map (fn [?xsd-pre]
                        (cond-> {:pretty? pretty?}
                          ?xsd-pre (assoc :xsd-prefix ?xsd-pre)))
                      xsd-pres)]
    (-> (map f/format-ast asts opt-maps)
        (uf/join-updates pretty?))))

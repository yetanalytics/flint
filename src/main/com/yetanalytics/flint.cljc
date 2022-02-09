(ns com.yetanalytics.flint
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.query    :as qs]
            [com.yetanalytics.flint.spec.update   :as us]
            [com.yetanalytics.flint.format        :as f]
            [com.yetanalytics.flint.format.query]
            [com.yetanalytics.flint.format.update :as uf]
            [com.yetanalytics.flint.error         :as err]
            [com.yetanalytics.flint.validate :as v]
            [com.yetanalytics.flint.validate.prefix :as vp]
            [com.yetanalytics.flint.validate.scope :as vs]))

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

(defn- assert-prefixes-err-map
  [prefix-errs sparql ast]
  {:kind  ::invalid-prefixes
   :error prefix-errs
   :input sparql
   :ast   ast})

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
  {:kind  ::invalid-scoped-vars
   :error scope-errs
   :input sparql
   :ast   ast})

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

;; (defn- assert-bnodes
;;   [sparql ast]
;;   (let [res (bnode/validate-bnodes ast)]
;;     (when-some [errs (:errors res)]
;;       (throw (ex-info "Invalid blank nodes!"
;;                       {:kind  (:kind res)
;;                        :error errs
;;                        :input sparql
;;                        :ast   ast})))))

;; (defn- assert-bnodes-coll
;;   [sparql-coll ast-coll]
;;   (loop [inputs sparql-coll
;;          asts   ast-coll
;;          bnodes #{}
;;          idx    0]
;;     (when-some [ast (first asts)]
;;       (let [res (bnode/validate-bnodes bnodes ast)]
;;         (if-some [errs (:errors res)]
;;           (throw (ex-info "Invalid blank nodes!"
;;                           {:kind  (:kind res)
;;                            :error errs
;;                            :input (first inputs)
;;                            :ast   ast
;;                            :index idx}))
;;           (recur (rest inputs) (rest asts) (:bnodes res) (inc idx)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn format-query
  "Format `query` into a SPARQL Query string. Throws an exception if `query`
   does not conform to spec or if its prefixed IRIs cannot be expanded."
  [query & {:keys [pretty? validate?] :or {pretty?   false
                                           validate? true}}]
  (let [ast      (conform-query query)
        prefix-m (:prefixes query)
        _        (when validate?
                   (let [nodes-m (v/collect-nodes ast)]
                     (assert-prefixes query ast nodes-m prefix-m)
                     (assert-scoped-vars query ast nodes-m)
                     #_(when-some [errs (vs/validate-scoped-vars nodes-m)]
                       (throw (ex-info (err/scope-error-msg errs)
                                       (assert-scope-err-map errs update ast)))))
                   #_(assert-prefixes query ast prefix-m)
                   #_(assert-scoped-vars query ast)
                   #_(assert-bnodes query ast))
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
  [update & {:keys [pretty? validate?] :or {pretty?   false
                                            validate? true}}]
  (let [ast      (conform-update update)
        prefix-m (:prefixes update)
        _        (when validate?
                   (let [nodes-m (v/collect-nodes ast)]
                     (assert-prefixes update ast nodes-m prefix-m)
                     (assert-scoped-vars update nodes-m ast))
                   #_(assert-bnodes update ast))
        ?xsd-pre (get-xsd-prefix prefix-m)
        opt-m    (cond-> {:pretty? pretty?}
                   ?xsd-pre (assoc :xsd-prefix ?xsd-pre))]
    (f/format-ast ast opt-m)))

(defn format-updates
  "Format the coll `updates` into a SPARQL Update Request string. Throws
   an exception if any update does not conform to spec or has a prefixed
   IRI that cannot be expanded."
  [updates & {:keys [pretty? validate?] :or {pretty?   false
                                             validate? true}}]
  (let [idxs         (-> updates count range)
        asts         (map conform-update updates idxs)
        prefix-ms    (reduce (fn [pm-coll {pm :prefixes :as _update}]
                               (let [last-pm (last pm-coll)
                                     new-pm  (merge last-pm pm)]
                                 (conj pm-coll new-pm)))
                             []
                             updates)
        _            (when validate?
                       (let [nodes-m-coll (map v/collect-nodes asts)]
                         (dorun (map assert-prefixes updates asts nodes-m-coll prefix-ms idxs))
                         (dorun (map assert-scoped-vars updates asts nodes-m-coll idxs)))
                       #_(assert-bnodes-coll updates asts))
        xsd-prefixes (map get-xsd-prefix prefix-ms)
        opt-maps     (map (fn [?xsd-pre]
                            (cond-> {:pretty? pretty?}
                              ?xsd-pre (assoc :xsd-prefix ?xsd-pre)))
                          xsd-prefixes)]
    (-> (map f/format-ast asts opt-maps)
        (uf/join-updates pretty?))))

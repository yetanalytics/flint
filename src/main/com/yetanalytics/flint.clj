(ns com.yetanalytics.flint
  (:require [clojure.spec.alpha :as s]
            [clojure.walk       :as w]
            [com.yetanalytics.flint.spec.query  :as qs]
            [com.yetanalytics.flint.spec.update :as us]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.query]
            [com.yetanalytics.flint.format.update]))

(defn format-query
  "Format `query` into a SPARQL Query string. Throws an exception if `query`
   does not conform to spec."
  [query]
  (let [ast (s/conform qs/query-spec query)]
    (if-not (= ::s/invalid ast)
      (w/postwalk (partial f/format-ast {}) ast)
      (throw (ex-info "Query does not conform to spec!"
                      {:kind  ::invalid-query
                       :error (s/explain-data qs/query-spec query)})))))

(defn format-updates
  "Format `update` (and potentially `updates`) into a SPARQL Update Request
   string. Throws an exception if the updates do not conform to spec."
  [update & updates]
  (if (not-empty updates)
    (let [ups (concat [update] updates)
          ast (s/conform us/update-request-spec ups)]
      (if-not (= ::s/invalid ast)
        (w/postwalk (partial f/format-ast {}) ast)
        (throw (ex-info "Update request does not conform to spec!"
                        {:kind  ::invalid-update-request
                         :error (s/explain-data us/update-request-spec ups)}))))
    (let [ast (s/conform us/update-spec update)]
      (if-not (= ::s/invalid ast)
        (w/postwalk (partial f/format-ast {}) ast)
        (throw (ex-info "Update does not conform to spec!"
                        {:kind ::invalid-update
                         :error (s/explain-data us/update-spec update)}))))))

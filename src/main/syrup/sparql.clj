(ns syrup.sparql
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as w]
            [syrup.sparql.format :as f]
            [syrup.sparql.spec.query :as qs]
            [syrup.sparql.spec.update :as us]
            [syrup.sparql.format.query]
            [syrup.sparql.format.update]))

(defn format-query
  [query]
  (->> query
       (s/conform qs/query-spec)
       (w/postwalk f/format-ast)))

(defn format-updates
  [update & updates]
  (if (not-empty updates)
    (->> (concat [update] updates)
         (s/conform us/update-request-spec)
         (w/postwalk f/format-ast))
    (->> update
         (s/conform us/update-spec)
         (w/postwalk f/format-ast))))

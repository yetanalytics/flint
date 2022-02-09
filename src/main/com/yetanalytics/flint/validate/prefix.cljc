(ns com.yetanalytics.flint.validate.prefix
  (:require [clojure.zip :as zip]))

(defn- invalid-prefix?
  "Does the prefix of `prefix-iri` exist in the `prefixes` map/set?
   (Or for non-namespaced `prefix-iri`, does `:$` exist?)"
  [prefixes prefix-iri]
  (if-some [pre (namespace prefix-iri)]
    (not (contains? prefixes (keyword pre)))
    (not (contains? prefixes :$))))

(defn- prefix-err-map
  [prefixes prefix-iri loc]
  {:prefixes prefixes
   :iri      prefix-iri
   :prefix   (or (some->> prefix-iri namespace keyword) :$)
   :path     (conj (->> loc zip/path (mapv first)) :ax/prefix-iri)})

(defn validate-prefixes
  "Given `node-m` a map from nodes to zipper locs, check that each prefix
   node is included in `prefixes`. If validation fails, return a coll of
   error maps; otherwise return `nil`."
  [prefixes node-m]
  (let [prefix-m (:ax/prefix-iri node-m)
        errors   (reduce (fn [acc [prefix locs]]
                           (if (invalid-prefix? prefixes prefix)
                             (->> locs
                                  (map (partial prefix-err-map prefixes prefix))
                                  (concat acc))
                             acc))
                         []
                         prefix-m)]
    (not-empty errors)))

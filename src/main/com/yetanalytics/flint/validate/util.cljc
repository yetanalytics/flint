(ns com.yetanalytics.flint.validate.util
  (:require [clojure.zip :as zip]))

(defn zip-path
  "Return the path vector (excluding array indices) that leads up to `loc`."
  [loc]
  (->> loc zip/path (mapv first)))

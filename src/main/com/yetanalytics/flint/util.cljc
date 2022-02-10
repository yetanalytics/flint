(ns com.yetanalytics.flint.util)

(defn get-keyword
  "If `x` is a `[:keyword value]` pair, return the keyword; otherwise
   return `nil`."
  [x]
  (when (vector? x)
    (let [fst (first x)]
      (when (keyword? fst)
        fst))))

(defn get-kv-pair
  "Given `coll` of `[:keyword value]` pairs, return the pair
   with keyword `k`. In other words, just like `get` for
   associative collections."
  [coll k]
  (some #(when (-> % first (= k)) %) coll))

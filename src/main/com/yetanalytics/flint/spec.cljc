(ns com.yetanalytics.flint.spec
  #?(:clj (:require [clojure.spec.alpha :as s]))
  #?(:cljs (:require-macros
            [com.yetanalytics.flint.spec :refer [sparql-keys]])))

;; Need these helpers to deal with `(or ::kspec-1 ::kspec-2 ...)`

#?(:clj
   (defn- collect-keys
     [x]
     (cond
       (coll? x)    (->> x flatten (filter keyword?))
       (keyword? x) [x]
       :else        nil)))

#?(:clj
   (defn- collect-unq-keys
     [x]
     (map (comp keyword name) (collect-keys x))))

#?(:clj
   (defmacro sparql-keys
     [& {:keys [map-specs key-comp-fn req opt req-un opt-un]
         :or {key-comp-fn compare}}]
     (let [keys-set#  (set (concat (collect-keys req)
                                   (collect-keys opt)
                                   (collect-unq-keys req-un)
                                   (collect-unq-keys opt-un)))
           keys-spec# (cond-> [`s/keys]
                        req (conj :req req)
                        opt (conj :opt opt)
                        req-un (conj :req-un req-un)
                        opt-un (conj :opt-un opt-un)
                        true seq)]
       `(s/and ~@map-specs
               ;; `restrict-keys` taken from xapi-schema.spec
               #(every? ~keys-set# (keys %))
               ~keys-spec#
               (s/conformer #(into [] %))
               (s/conformer #(sort-by first ~key-comp-fn %))))))

(ns com.yetanalytics.flint.spec.query
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom    :as ax]
            [com.yetanalytics.flint.spec.modifier :as ms]
            [com.yetanalytics.flint.spec.prologue :as ps]
            [com.yetanalytics.flint.spec.select   :as ss]
            [com.yetanalytics.flint.spec.triple   :as ts]
            [com.yetanalytics.flint.spec.where    :as ws]
            [com.yetanalytics.flint.spec.values   :as vs]))

(def key-order-map
  {:base            0
   :prefixes        1
   :select          2
   :select-distinct 2
   :select-reduced  2
   :construct       2
   :describe        2
   :ask             2
   :from            3
   :from-named      4
   :where           5
   :group-by        6
   :order-by        7
   :having          8
   :limit           9
   :offset          10
   :values          11})

(defn- qkey-comp
  [k1 k2]
  (let [n1 (get key-order-map k1 100)
        n2 (get key-order-map k2 100)]
    (- n1 n2)))

(defmacro smap->vec
  [form]
  `(s/and ~form
          (s/conformer #(into [] %))
          (s/conformer #(sort-by first qkey-comp %))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dataset Clause specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::from
  (s/and (s/or :single ax/iri-spec
               :coll   (s/and (s/coll-of ax/iri-spec :count 1 :kind vector?)
                              (s/conformer first)))
         (s/conformer second)))

(s/def ::from-named
  (s/coll-of ax/iri-spec :min-count 1 :kind vector?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Cannot use `s/merge` since conformance does not work properly with it

(def select-query-spec
  (smap->vec (s/keys :req-un [(or ::ss/select
                                  ::ss/select-distinct
                                  ::ss/select-reduced)
                              ::ws/where]
                     :opt-un [::ps/base ::ps/prefixes
                              ::from ::from-named
                              ::ms/group-by
                              ::ms/order-by
                              ::ms/having
                              ::ms/limit
                              ::ms/offset
                              ::vs/values])))

(def triples-spec
  (s/coll-of (s/or :triple/vec ts/triple-vec-nopath-spec
                   :triple/nform ts/normal-form-nopath-spec)
             :min-count 0
             :kind vector?))

(s/def ::construct triples-spec)

(def construct-query-spec
  (smap->vec (s/keys :req-un [::construct ::ws/where]
                     :opt-un [::ps/base ::ps/prefixes
                              ::from ::from-named
                              ::ms/group-by
                              ::ms/order-by
                              ::ms/having
                              ::ms/limit
                              ::ms/offset])))

(s/def ::describe
  (s/or :describe/vars-or-iris (s/coll-of ax/var-or-iri-spec
                                          :min-count 1
                                          :kind vector?)
        :wildcard ax/wildcard?))

(def describe-query-spec
  (smap->vec (s/keys :req-un [::describe]
                     :opt-un [::ps/base ::ps/prefixes
                              ::from ::from-named
                              ::ws/where
                              ::ms/group-by
                              ::ms/order-by
                              ::ms/having
                              ::ms/limit
                              ::ms/offset])))

(s/def ::ask empty?)

(def ask-query-spec
  (smap->vec (s/keys :req-un [::ask ::ws/where]
                     :opt-un [::ps/base ::ps/prefixes
                              ::from ::from-named
                              ::ms/group-by
                              ::ms/order-by
                              ::ms/having
                              ::ms/limit
                              ::ms/offset])))

(def query-spec
  (s/or :select-query    select-query-spec
        :construct-query construct-query-spec
        :describe-query  describe-query-spec
        :ask-query       ask-query-spec))

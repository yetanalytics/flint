(ns com.yetanalytics.flint.spec.update
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom    :as ax]
            [com.yetanalytics.flint.spec.prologue :as ps]
            [com.yetanalytics.flint.spec.triple   :as ts]
            [com.yetanalytics.flint.spec.where    :as ws])
  #?(:clj (:require
           [com.yetanalytics.flint.spec :refer [sparql-keys]])
     :cljs (:require-macros
            [com.yetanalytics.flint.spec :refer [sparql-keys]])))

(def key-order-map
  {:base          0
   :prefixes      1
   ;; Graph management
   :load          2
   :load-silent   2
   :clear         2
   :clear-silent  2
   :drop          2
   :drop-silent   2
   :add           2
   :add-silent    2
   :create        2
   :create-silent 2
   :move          2
   :move-silent   2
   :copy          2
   :copy-silent   2
   :to            3
   :into          3
   ;; Graph modification
   :insert-data   2
   :delete-data   2
   :delete-where  2
   :with          2
   :delete        3
   :insert        4
   :using         5
   :where         6})

(defn- key-comp
  [k1 k2]
  (let [n1 (get key-order-map k1 100)
        n2 (get key-order-map k2 100)]
    (- n1 n2)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helper specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def graph-or-default-spec
  (s/or :update/default-graph #{:default}
        :update/named-graph ax/iri-spec))

(s/def ::to graph-or-default-spec)

(s/def ::into ax/iri-spec)

(s/def ::with ax/iri-spec)

(s/def ::using
  (s/or :update/iri ax/iri-spec
        :update/named-iri (s/tuple #{:named} ax/iri-spec)))

;; Quads

(def triples-spec
  (s/coll-of (s/or :triple/vec ts/triple-vec-nopath-spec
                   :triple/nform ts/normal-form-nopath-spec)
             :kind vector?))

(def triples-novar-spec
  (s/coll-of (s/or :triple/vec ts/triple-vec-novar-spec
                   :triple/nform ts/normal-form-novar-spec)
             :kind vector?))

(def triples-noblank-spec
  (s/coll-of (s/or :triple/vec ts/triple-vec-noblank-spec
                   :triple/nform ts/normal-form-noblank-spec)
             :kind vector?))

(def triples-novar-noblank-spec
  (s/coll-of (s/or :triple/vec ts/triple-vec-novar-noblank-spec
                   :triple/nform ts/normal-form-novar-noblank-spec)
             :kind vector?))

(def quad-spec
  (s/tuple #{:graph}
           ax/var-or-iri-spec
           triples-spec))

(def quad-novar-spec
  (s/tuple #{:graph}
           ax/var-or-iri-spec
           triples-novar-spec))

(def quad-noblank-spec
  (s/tuple #{:graph}
           ax/var-or-iri-spec
           triples-noblank-spec))

(def quad-novar-noblank-spec
  (s/tuple #{:graph}
           ax/var-or-iri-spec
           triples-novar-noblank-spec))

(def triple-or-quads-spec
  (s/coll-of (s/or :triple/vec  ts/triple-vec-nopath-spec
                   :triple/nform ts/normal-form-nopath-spec
                   :triple/quads quad-spec)
             :kind vector?))

(def triple-or-quads-novar-spec
  (s/coll-of (s/or :triple/vec  ts/triple-vec-novar-spec
                   :triple/nform ts/normal-form-novar-spec
                   :triple/quads quad-novar-spec)
             :kind vector?))

(def triple-or-quads-noblank-spec
  (s/coll-of (s/or :triple/vec  ts/triple-vec-noblank-spec
                   :triple/nform ts/normal-form-noblank-spec
                   :triple/quads quad-noblank-spec)
             :kind vector?))

(def triple-or-quads-novar-noblank-spec
  (s/coll-of (s/or :triple/vec  ts/triple-vec-novar-noblank-spec
                   :triple/nform ts/normal-form-novar-noblank-spec
                   :triple/quads quad-novar-noblank-spec)
             :kind vector?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Management specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Cannot use `s/merge` since conformance does not work properly with it

(s/def ::load ax/iri-spec)
(s/def ::load-silent ::load)

(def load-update-spec
  (sparql-keys :req-un [(or ::load ::load-silent)]
               :opt-un [::ps/base ::ps/prefixes ::into]
               :key-comp-fn key-comp))

(s/def ::clear
  (s/or :ax/iri    ax/iri-spec
        :update/kw #{:default :named :all}))

(s/def ::clear-silent ::clear)

(def clear-update-spec
  (sparql-keys :req-un [(or ::clear ::clear-silent)]
               :opt-un [::ps/base ::ps/prefixes]
               :key-comp-fn key-comp))

(s/def ::drop
  (s/or :ax/iri    ax/iri-spec
        :update/kw #{:default :named :all}))

(s/def ::drop-silent ::drop)

(def drop-update-spec
  (sparql-keys :req-un [(or ::drop ::drop-silent)]
               :opt-un [::ps/base ::ps/prefixes]
               :key-comp-fn key-comp))

(s/def ::create ax/iri-spec)
(s/def ::create-silent ::create)

(def create-update-spec
  (sparql-keys :req-un [(or ::create ::create-silent)]
               :opt-un [::ps/base ::ps/prefixes]
               :key-comp-fn key-comp))

(s/def ::add graph-or-default-spec)
(s/def ::add-silent ::add)

(def add-update-spec
  (sparql-keys :req-un [(or ::add ::add-silent) ::to]
               :opt-un [::ps/base ::ps/prefixes]
               :key-comp-fn key-comp))

(s/def ::move graph-or-default-spec)
(s/def ::move-silent ::move)

(def move-update-spec
  (sparql-keys :req-un [(or ::move ::move-silent) ::to]
               :opt-un [::ps/base ::ps/prefixes]
               :key-comp-fn key-comp))

(s/def ::copy graph-or-default-spec)
(s/def ::copy-silent ::copy)

(def copy-update-spec
  (sparql-keys :req-un [(or ::copy ::copy-silent) ::to]
               :opt-un [::ps/base ::ps/prefixes]
               :key-comp-fn key-comp))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Update specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::insert-data triple-or-quads-novar-spec)

(def insert-data-update-spec
  (sparql-keys :req-un [::insert-data]
               :opt-un [::ps/base ::ps/prefixes]
               :key-comp-fn key-comp))

(s/def ::delete-data triple-or-quads-novar-noblank-spec)

(def delete-data-update-spec
  (sparql-keys :req-un [::delete-data]
               :opt-un [::ps/base ::ps/prefixes]
               :key-comp-fn key-comp))

(s/def ::delete-where triple-or-quads-noblank-spec)

(def delete-where-update-spec
  (sparql-keys :req-un [::delete-where]
               :opt-un [::ps/base ::ps/prefixes]
               :key-comp-fn key-comp))

(s/def ::insert triple-or-quads-spec)
(s/def ::delete triple-or-quads-noblank-spec)

(def modify-update-spec
  (sparql-keys :req-un [(or ::delete ::insert)
                        ::ws/where]
               :opt-un [::ps/base
                        ::ps/prefixes
                        ::delete
                        ::insert
                        ::with
                        ::using]
               :key-comp-fn key-comp))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update Request
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def update-spec
  (s/or :update/load   load-update-spec
        :update/clear  clear-update-spec
        :update/drop   drop-update-spec
        :update/create create-update-spec
        :update/add    add-update-spec
        :update/move   move-update-spec
        :update/copy   copy-update-spec
        :update/insert-data  insert-data-update-spec
        :update/delete-data  delete-data-update-spec
        :update/delete-where delete-where-update-spec
        :update/modify       modify-update-spec))

(ns syrup.sparql.spec.update
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom  :as ax]
            [syrup.sparql.spec.triple :as triple]
            [syrup.sparql.spec.where  :as ws]))

(def key-order-map
  {:bases        0
   :prefixes     1
   ;; Graph management
   :load         2
   :load-silent  2
   :clear        2
   :clear-silent 2
   :drop         2
   :drop-silent  2
   :add          2
   :add-silent   2
   :move         2
   :move-silent  2
   :copy         2
   :copy-silent  2
   :to           3
   ;; Graph modification
   :insert-data  2
   :delete-data  2
   :delete-where 2
   :with         2
   :delete       3
   :insert       4
   :using        5
   :where        6})

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
  (s/coll-of (s/or :tvec triple/triple-vec-nopath-spec
                   :nform triple/normal-form-nopath-spec)))

(def triples-novar-spec
  (s/coll-of (s/or :tvec triple/triple-vec-novar-spec
                   :nform triple/normal-form-novar-spec)))

(def quad-spec
  (s/and vector?
         (s/tuple #{:graph}
                  ax/var-or-iri-spec
                  triples-spec)))

(def quad-novar-spec
  (s/and vector?
         (s/tuple #{:graph}
                  ax/var-or-iri-spec
                  triples-novar-spec)))

(def triple-or-quads-spec
  (s/coll-of (s/or :tvec  triple/triple-vec-nopath-spec
                   :nform triple/normal-form-nopath-spec
                   :quads quad-spec)))

(def triple-or-quads-novar-spec
  (s/coll-of (s/or :tvec  triple/triple-vec-novar-spec
                   :nform triple/normal-form-novar-spec
                   :quads quad-novar-spec)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Management specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::load ax/iri-spec)
(s/def ::load-silent ::load)

(def load-update-spec
  (smap->vec (s/keys :req-un [(or ::load ::load-silent)]
                     :opt-un [::into])))

(s/def ::clear
  (s/or :iri ax/iri-spec
        :update/kw #{:default :named :all}))

(s/def ::clear-silent ::clear)

(def clear-update-spec
  (smap->vec (s/keys :req-un [(or ::clear ::clear-silent)])))

(s/def ::drop
  (s/or :iri ax/iri-spec
        :update/kw #{:default :named :all}))

(s/def ::drop-silent ::drop)

(def drop-update-spec
  (smap->vec (s/keys :req-un [(or ::drop ::drop-silent)])))

(s/def ::create ax/iri-spec)
(s/def ::create-silent ::create)

(def create-update-spec
  (smap->vec (s/keys :req-un [(or ::create ::create-silent)])))

(s/def ::add graph-or-default-spec)
(s/def ::add-silent ::add)

(def add-update-spec
  (smap->vec (s/keys :req-un [(or ::add ::add-silent) ::to])))

(s/def ::move graph-or-default-spec)
(s/def ::move-silent ::move)

(def move-update-spec
  (smap->vec (s/keys :req-un [(or ::move ::move-silent) ::to])))

(s/def ::copy graph-or-default-spec)
(s/def ::copy-silent ::copy)

(def copy-update-spec
  (smap->vec (s/keys :req-un [(or ::copy ::copy-silent) ::to])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Update specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::insert-data triple-or-quads-novar-spec)

(def insert-data-update-spec
  (smap->vec (s/keys :req-un [::insert-data])))

(s/def ::delete-data triple-or-quads-novar-spec)

(def delete-data-update-spec
  (smap->vec (s/keys :req-un [::delete-data])))

(s/def ::delete-where triple-or-quads-spec)

(def delete-where-update-spec
  (smap->vec (s/keys :req-un [::delete-where])))

(s/def ::insert triple-or-quads-spec)
(s/def ::delete triple-or-quads-spec)

(def modify-update-spec
  (smap->vec (s/keys :req-un [(or ::delete ::insert)
                              ::ws/where]
                     :opt-un [::delete
                              ::insert
                              ::with
                              ::using])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update Request
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def update-spec
  (s/or :load-update   load-update-spec
        :clear-update  clear-update-spec
        :drop-update   drop-update-spec
        :create-update create-update-spec
        :add-update    add-update-spec
        :move-update   move-update-spec
        :copy-update   copy-update-spec
        :insert-data-update  insert-data-update-spec
        :delete-data-update  delete-data-update-spec
        :delete-where-update delete-where-update-spec
        :modify-update       modify-update-spec))

(def update-request-spec
  (s/or :update-request (s/coll-of update-spec
                                   :min-count 1
                                   :kind vector?)))

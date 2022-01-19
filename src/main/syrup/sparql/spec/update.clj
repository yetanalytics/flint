(ns syrup.sparql.spec.update
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom  :as ax]
            [syrup.sparql.spec.triple :as triple]
            [syrup.sparql.spec.where  :as ws]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helper specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def graph-or-default-spec
  (s/or :default #{:default}
        :named ax/iri-spec))

(s/def ::to graph-or-default-spec)

(s/def ::into ax/iri-spec)

(s/def ::with ax/iri-spec)

(s/def ::graph ax/iri-spec)

(s/def ::using
  (s/coll-of (s/or :default ax/iri-spec
                   :named (s/keys :req-un [::graph]))))

;; Quads

(def triples-spec
  (s/coll-of (s/or :tvec triple/triple-vec-nopath-spec
                   :nform triple/normal-form-nopath-spec)))

(def quad-spec
  (s/and vector?
         (s/cat :k #{:graph}
                :v (s/cat :var-or-iri ax/var-or-iri-spec
                          :triples triples-spec))))

(def triple-or-quads-spec
  (s/coll-of (s/or :tvec  triple/triple-vec-nopath-spec
                   :nform triple/normal-form-nopath-spec
                   :quads quad-spec)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Management specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::load ax/iri-spec)
(s/def ::load-silent ::load)

(def load-update-spec
  (s/keys :req-un [(or ::load ::load-silent)]
          :opt-un [::into]))

(s/def ::clear
  (s/or :iri ax/iri-spec
        :update-kw #{:default :named :all}))
(s/def ::clear-silent ::clear)

(def clear-update-spec
  (s/keys :req-un [(or ::clear ::clear-silent)]))

(s/def ::drop
  (s/or :iri ax/iri-spec
        :update-kw #{:default :named :all}))
(s/def ::drop-silent ::drop)

(def drop-update-spec
  (s/keys :req-un [(or ::drop ::drop-silent)]))

(s/def ::create ax/iri-spec)
(s/def ::create-silent ::create)

(def create-update-spec
  (s/keys :req-un [(or ::create ::create-silent)]))

(s/def ::add graph-or-default-spec)
(s/def ::add-silent ::add)

(def add-update-spec
  (s/keys :req-un [(or ::add ::add-silent) ::to]))

(s/def ::move graph-or-default-spec)
(s/def ::move-silent ::move)

(def move-update-spec
  (s/keys :req-un [(or ::move ::move-silent) ::to]))

(s/def ::copy graph-or-default-spec)
(s/def ::copy-silent ::copy)

(def copy-update-spec
  (s/keys :req-un [(or ::copy ::copy-silent) ::to]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Update specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::insert-data triple-or-quads-spec)

(def insert-data-update-spec
  (s/keys :req-un [::insert-data]))

(s/def ::delete-data triple-or-quads-spec)

(def delete-data-update-spec
  (s/keys :req-un [::delete-data]))

(s/def ::delete-where triple-or-quads-spec)

(def delete-where-update-spec
  (s/keys :req-un [::delete-where]))

(s/def ::insert triple-or-quads-spec)
(s/def ::delete triple-or-quads-spec)

(def modify-update-spec
  (s/keys :req-un [::ws/where]
          :opt-un [::delete
                   ::insert
                   ::using
                   ::with]))

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
  (s/or :update-request (s/coll-of update-spec :min-count 1 :kind vector?)))

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
        :named ax/iri?))

(s/def ::to graph-or-default-spec)

(s/def ::into ax/iri?)

(s/def ::with ax/iri?)

(s/def ::using
  (s/coll-of (s/or :default ax/iri?
                   :named (s/keys :req-un [::graph]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Management specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::load ax/iri?)

(def load-update-spec
  (s/keys :req-un [(or ::load ::load-silent)]
          :opt-un [::into]))

(s/def ::clear
  (s/or :iri ax/iri?
        :keyword #{:default :named :all}))

(def clear-update-spec
  (s/keys :req-un [(or ::clear ::clear-silent)]))

(s/def ::drop
  (s/or :iri ax/iri?
        :keyword #{:default :named :all}))

(def drop-update-spec
  (s/keys :req-un [(or ::drop ::drop-silent)]))

(s/def ::create ax/iri?)

(def create-update-spec
  (s/keys :req-un [(or ::create ::create-silent)]))

(s/def ::add graph-or-default-spec)

(def add-update-spec
  (s/keys :req-un [(or ::add ::add-silent) ::to]))

(s/def ::move graph-or-default-spec)

(def move-update-spec
  (s/keys :req-un [(or ::move ::move-silent) ::to]))

(s/def ::copy graph-or-default-spec)

(def copy-update-spec
  (s/keys :req-un [(or ::copy ::copy-silent) ::to]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Update specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::insert-data triple/triples-spec)

(def insert-data-update-spec
  (s/keys :req-un [::insert-data]))

(s/def ::delete-data triple/triples-spec)

(def delete-data-update-spec
  (s/keys :req-un [::delete-date]))

(s/def ::delete-where triple/triples-spec)

(def delete-where-update-spec
  (s/keys :req-un [::delete-where]))

(s/def ::insert triple/triples-spec)
(s/def ::delete triple/triples-spec)

(def modify-update-spec
  (s/keys :req-un [::ws/where]
          :opt-un [::delete
                   ::insert
                   ::using
                   ::with]))

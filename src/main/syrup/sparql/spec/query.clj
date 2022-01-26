(ns syrup.sparql.spec.query
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom    :as ax]
            [syrup.sparql.spec.prologue :as pro]
            [syrup.sparql.spec.triple   :as triple]
            [syrup.sparql.spec.modifier :as mod]
            [syrup.sparql.spec.select   :as ss]
            [syrup.sparql.spec.where    :as ws]
            [syrup.sparql.spec.values   :as vs]))

(def key-order-map
  {:bases           0
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
   :offset          10})

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

(s/def ::from ax/iri-spec)
(s/def ::from-named (s/coll-of ax/iri-spec :min-count 1))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def select-query-spec
  (smap->vec (s/keys :req-un [(or ::ss/select
                                  ::ss/select-distinct
                                  ::ss/select-reduced)
                              ::ws/where]
                     :opt-un [::pro/bases ::pro/prefixes
                              ::from ::from-named
                              ::mod/group-by
                              ::mod/order-by
                              ::mod/having
                              ::mod/limit
                              ::mod/offset
                              ::vs/values])))

(def triples-spec
  (s/coll-of (s/or :triple/vec triple/triple-vec-nopath-spec
                   :triple/nform triple/normal-form-nopath-spec)
             :min-count 0))

(s/def ::construct triples-spec)

(def construct-query-spec
  (smap->vec (s/keys :req-un [::construct ::ws/where]
                     :opt-un [::pro/bases ::pro/prefixes
                              ::from ::from-named
                              ::mod/group-by
                              ::mod/order-by
                              ::mod/having
                              ::mod/limit
                              ::mod/offset])))

(s/def ::describe
  (s/or :describe/vars-or-iris (s/coll-of ax/var-or-iri-spec :min-count 1)
        :wildcard ax/wildcard?))

(def describe-query-spec
  (smap->vec (s/keys :req-un [::describe]
                     :opt-un [::pro/bases ::pro/prefixes
                              ::from ::from-named
                              ::ws/where
                              ::mod/group-by
                              ::mod/order-by
                              ::mod/having
                              ::mod/limit
                              ::mod/offset])))

(s/def ::ask empty?)

(def ask-query-spec
  (smap->vec (s/keys :req-un [::ask ::ws/where]
                     :opt-un [::pro/bases ::pro/prefixes
                              ::from ::from-named
                              ::mod/group-by
                              ::mod/order-by
                              ::mod/having
                              ::mod/limit
                              ::mod/offset])))

(def query-spec
  (s/or :select-query    select-query-spec
        :construct-query construct-query-spec
        :describe-query  describe-query-spec
        :ask-query       ask-query-spec))

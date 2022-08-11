(ns com.yetanalytics.flint.spec.where
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom    :as ax]
            [com.yetanalytics.flint.spec.expr     :as es]
            [com.yetanalytics.flint.spec.modifier :as ms]
            [com.yetanalytics.flint.spec.select   :as ss]
            [com.yetanalytics.flint.spec.triple   :as ts]
            [com.yetanalytics.flint.spec.values   :as vs])
  #?(:clj (:require
           [com.yetanalytics.flint.spec :refer [sparql-keys]])
     :cljs (:require-macros
            [com.yetanalytics.flint.spec :refer [sparql-keys]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sub-SELECT query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def key-order-map
  {:select          2
   :select-distinct 2
   :select-reduced  2
   :where           5
   :group-by        6
   :order-by        7
   :having          8
   :limit           9
   :offset          10
   :values          11})

(defn- key-comp
  [k1 k2]
  (let [n1 (get key-order-map k1 100)
        n2 (get key-order-map k2 100)]
    (- n1 n2)))

(s/def ::select
  (sparql-keys :req-un [(or ::ss/select
                            ::ss/select-distinct
                            ::ss/select-reduced)
                        ::where]
               :opt-un [::vs/values
                        ;; s/merge does not result in correct conformation
                        ::ms/group-by
                        ::ms/order-by
                        ::ms/having
                        ::ms/limit
                        ::ms/offset]
               :key-comp-fn key-comp))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WHERE clause
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti where-special-form-mm
  "Accepts a special WHERE form/graph pattern in the form `[:keyword ...]`
   and returns the appropriate regex spec. The spec applies an additional
   conformer in order to allow for identification during formatting."
  first)

(defmethod where-special-form-mm :where [_] ; recursion
  (s/& (s/cat :k #{:where}
              :v ::where)
       (s/conformer (fn [{:keys [v]}] [:where/recurse v]))))

(defmethod where-special-form-mm :union [_]
  (s/& (s/cat :k #{:union}
              :v (s/+ ::where))
       (s/conformer (fn [{:keys [v]}] [:where/union v]))))

(defmethod where-special-form-mm :optional [_]
  (s/& (s/cat :k #{:optional}
              :v ::where)
       (s/conformer (fn [{:keys [v]}] [:where/optional v]))))

(defmethod where-special-form-mm :minus [_]
  (s/& (s/cat :k #{:minus}
              :v ::where)
       (s/conformer (fn [{:keys [v]}] [:where/minus v]))))

(defmethod where-special-form-mm :graph [_]
  (s/& (s/cat :k #{:graph}
              :v1 ax/iri-or-var-spec
              :v2 ::where)
       (s/conformer (fn [{:keys [v1 v2]}] [:where/graph [v1 v2]]))))

(defmethod where-special-form-mm :service [_]
  (s/& (s/cat :k #{:service}
              :v1 ax/iri-or-var-spec
              :v2 ::where)
       (s/conformer (fn [{:keys [v1 v2]}] [:where/service [v1 v2]]))))

(defmethod where-special-form-mm :service-silent [_]
  (s/& (s/cat :k #{:service-silent}
              :v1 ax/iri-or-var-spec
              :v2 ::where)
       (s/conformer (fn [{:keys [v1 v2]}] [:where/service-silent [v1 v2]]))))

(defmethod where-special-form-mm :filter [_]
  (s/& (s/cat :k #{:filter}
              :v ::es/expr)
       (s/conformer (fn [{:keys [v]}] [:where/filter v]))))

(defmethod where-special-form-mm :bind [_]
  (s/& (s/cat :k #{:bind}
              :v ::es/expr-as-var)
       (s/conformer (fn [{:keys [v]}] [:where/bind v]))))

(defmethod where-special-form-mm :values [_]
  (s/& (s/cat :k #{:values}
              :v ::vs/values)
       (s/conformer (fn [{:keys [v]}] [:where/values v]))))

(def where-special-form-spec
  "Specs for special WHERE forms/graph patterns, which should be
   of the form `[:keyword ...]`."
  (s/and vector? (s/multi-spec where-special-form-mm first)))

(s/def ::where
  (s/or :where-sub/select
        ::select
        :where-sub/where
        (s/coll-of (s/or :where/special where-special-form-spec
                         :triple/vec    ts/triple-vec-spec
                         :triple/nform  ts/normal-form-spec)
                   :min-count 1
                   :kind vector?)
        :where-sub/empty
        (s/and vector? empty?)))

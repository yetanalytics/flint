(ns com.yetanalytics.flint.error
  "Namespace for formatting error messages and other error utils."
  (:require [clojure.spec.alpha :as s]
            [clojure.string     :as cstr]
            [com.yetanalytics.flint.spec               :as flint-spec]
            [com.yetanalytics.flint.validate.aggregate :as va]
            [com.yetanalytics.flint.validate.bnode     :as vb]
            [com.yetanalytics.flint.validate.scope     :as vs]
            #?@(:clj [[clojure.core :refer [format]]]
                :cljs [[goog.string :as gstring]
                       [goog.string.format]])))

(def ^:private fmt #?(:clj format :cljs gstring/format))

;; Might do a more sophisticated version using `s/registry`, but
;; this should do for now.
(def top-level-keywords
  #{:base :prefixes
    :select :select-distinct :select-reduced
    :construct :describe :ask
    :load :load-silent
    :clear :clear-silent
    :drop :drop-silent
    :create :create-silent
    :add :add-silent
    :move :move-silent
    :copy :copy-silent
    :insert-data :delete-data :delete-where :insert :delete
    :from :from-named
    :where
    :group-by :order-by :having :limit :offset :values
    :to :into :with :using})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; String formatting helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- join-str-coll
  "Join `str-coll` in the form `... x, y and z`."
  [str-coll]
  (if (= 1 (count str-coll))
    (first str-coll)
    (fmt "%s and %s"
         (cstr/join ", " (butlast str-coll))
         (last str-coll))))

(defn- plural-s
  "Add `s` to the end of `word` if `count` is not 1."
  [count]
  (if (= 1 count) "" "s"))

(defn- plural-has
  "Return `have` if `count` is not 1, `has` otherwise."
  [count]
  (if (= 1 count) "has" "have"))

(defn- plural-was
  "Return `were` if `count` is not 1, `was` otherwise."
  [count]
  (if (= 1 count) "was" "were"))

(defn- make-index-str
  [index]
  (fmt " at index %d" index))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec errors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn spec-error-keywords
  "Return a vector of keywords describing each failing clause."
  [spec-ed]
  (let [spec-paths (->> spec-ed
                        ::s/problems
                        (map :path))]
    (if (every? #(= 1 (count %)) spec-paths)
      [::flint-spec/top-level]
      (->> spec-paths
           (map #(some top-level-keywords %))
           (filter some?)
           distinct
           (mapv #(keyword "com.yetanalytics.flint.spec" (name %)))))))

(defn- spec-clause-strs
  [spec-err-kws]
  (->> spec-err-kws
       (map name)
       (map #(cstr/replace-first % #"-" " "))
       (map cstr/upper-case)))

(defn- spec-error-msg*
  [spec-err-kws index-str]
  (if (= [::flint-spec/top-level] spec-err-kws)
    ;; Every spec path is of the form `[:query/select]`, `[:query/ask]`,
    ;; etc. This is indicative that spec cannot traverse inside clauses
    ;; due to errors at the top level.
    (fmt "Syntax errors exist%s due to invalid map, or invalid or extra clauses!"
         index-str)
    ;; Here, "missing clause" errors will not show up in the error msg.
    ;; But they will re-emerge once the user fixes the other errors.
    (let [clause-strs (spec-clause-strs spec-err-kws)
          num-clauses (count clause-strs)]
      (cond
        (= 1 num-clauses)
        (fmt "Syntax errors exist%s in the %s clause!"
             index-str
             (first clause-strs))
        (< 1 num-clauses)
        (fmt "Syntax errors exist%s in the %s and %s clauses!"
             index-str
             (cstr/join ", " (butlast clause-strs))
             (last clause-strs))
        :else
        (fmt "Syntax errors exist%s in an unknown clause!"
             index-str)))))

(defn spec-error-msg
  "Return an error message specifying the invalid clauses and, if
   applicable, the input coll index."
  ([spec-ed]
   (spec-error-msg* spec-ed ""))
  ([spec-ed index]
   (spec-error-msg* spec-ed (make-index-str index))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Prefix errors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- prefix-error-msg*
  [prefix-errs index-str]
  (let [iri-count   (->> prefix-errs count)
        prefix-kws  (->> prefix-errs (map :prefix) distinct sort)
        prefix-strs (->> prefix-kws
                         (map name)
                         (map (partial fmt ":%s")))
        prefix-str  (join-str-coll prefix-strs)]
    (fmt "%d IRI%s%s cannot be expanded due to missing prefixes %s!"
         iri-count
         (plural-s iri-count)
         index-str
         prefix-str)))

(defn prefix-error-msg
  "Return an error message specifying the number of unexpandable IRIs,
   missing prefixes and, if applicable, the input coll index."
  ([prefix-errs]
   (prefix-error-msg* prefix-errs ""))
  ([prefix-errs index]
   (prefix-error-msg* prefix-errs (make-index-str index))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scope errors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- scope-error-msg*
  [scope-errs index-str]
  (let [[nots ins] (split-with #(= ::vs/var-not-in-scope (:kind %))
                               scope-errs)
        var-coll   (if (not-empty nots)
                     (->> nots (mapcat :variables) distinct sort)
                     (->> ins (map :variable) distinct sort))
        var-count  (->> var-coll count)
        var-strs   (->> var-coll (map str))
        var-str    (join-str-coll var-strs)]
    (fmt "%d variable%s%s in %d `expr AS var` clause%s %s %s defined in scope: %s!'"
         var-count
         (plural-s var-count)
         index-str
         (count scope-errs)
         (if (= 1 (count scope-errs)) "" "s")
         (plural-was var-count)
         (if (not-empty nots) "not" "already")
         var-str)))

(defn scope-error-msg
  "Return an error message specifying the number of variables with scope
   errors, number of locations and, if applicable, the input coll index."
  ([scope-errs]
   (scope-error-msg* scope-errs ""))
  ([scope-errs index]
   (scope-error-msg* scope-errs (fmt " at index %d" index))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Aggregate errors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- aggregate-error-msg*
  [agg-errs index-str]
  (let [[wilds errs] (split-with #(= ::va/wildcard-group-by (:kind %))
                                 agg-errs)]
    (if (not-empty wilds)
      (let [wild-count (count wilds)]
        (fmt "%d SELECT clause%s%s %s both wildcard and GROUP BY!"
             wild-count
             (plural-s wild-count)
             index-str
             (plural-has wild-count)))
      (let [var-coll (->> errs (mapcat :variables) distinct sort)
            var-count (->> var-coll count)
            var-strs  (->> var-coll (map str))
            var-str   (join-str-coll var-strs)]
        (fmt "%d variable%s%s %s illegally used in SELECTs with aggregates: %s!"
             var-count
             (plural-s var-count)
             index-str
             (plural-was var-count)
             var-str)))))

(defn aggregate-error-msg
  ([agg-errs]
   (aggregate-error-msg* agg-errs ""))
  ([agg-errs index]
   (aggregate-error-msg* agg-errs (make-index-str index))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Blank node errors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- bnode-error-msg*
  [bnode-err-m index-str]
  (let [bnode-coll  (->> bnode-err-m :errors (map :bnode) distinct)
        bnode-count (count bnode-coll)
        bnode-strs  (->> bnode-coll (map str))
        bnode-str   (if (= 1 bnode-count)
                      (first bnode-strs)
                      (fmt "%s and %s"
                           (cstr/join ", " (butlast bnode-strs))
                           (last bnode-strs)))]
    (fmt "%d blank node%s%s %s duplicated %s: %s!"
         bnode-count
         (plural-s bnode-count)
         index-str
         (plural-was bnode-count)
         (case (:kind bnode-err-m)
           ::vb/dupe-bnodes-update "from previous updates"
           ::vb/dupe-bnodes-bgp    "in multiple BGPs")
         bnode-str)))

(defn bnode-error-msg
  ([bnode-err-m]
   (bnode-error-msg* bnode-err-m ""))
  ([bnode-err-m index]
   (bnode-error-msg* bnode-err-m (make-index-str index))))

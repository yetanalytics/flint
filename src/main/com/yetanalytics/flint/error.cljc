(ns com.yetanalytics.flint.error
  (:require [clojure.spec.alpha :as s]
            [clojure.string     :as cstr]
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

(defn- spec-clause-strs
  [spec-paths]
  (->> spec-paths
       (map #(some top-level-keywords %))
       (filter some?)
       distinct
       (map name)
       (map #(cstr/replace-first % #"-" " "))
       (map cstr/upper-case)))

(defn- spec-error-msg*
  [spec-ed index-str]
  (let [spec-paths  (->> spec-ed ::s/problems (map :path))]
    (if (every? #(= 1 (count %)) spec-paths)
      ;; Every spec path is of the form `[:select-query]`, `[:ask-query]`,
      ;; etc. This is indicative that no top-level clasues exist for spec
      ;; to traverse through.
      (fmt "Syntax errors exist%s due to missing clauses!"
           index-str)
      ;; Here, "missing clause" errors will not show up in the error msg.
      ;; But they will re-emerge once the user fixes the other errors.
      (let [clause-strs (spec-clause-strs spec-paths)
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
               index-str))))))

(defn spec-error-msg
  "Return an error message specifying the invalid clauses and, if
   applicable, the input coll index."
  ([spec-ed]
   (spec-error-msg* spec-ed ""))
  ([spec-ed index]
   (spec-error-msg* spec-ed (fmt " at index %d" index))))

(defn- prefix-error-msg*
  [prefix-errs index-str]
  (let [iri-count   (->> prefix-errs count)
        prefix-kws  (->> prefix-errs (map :prefix) distinct sort)
        prefix-strs (->> prefix-kws
                         (map name)
                         (map (partial fmt ":%s")))
        prefix-str  (if (= 1 (count prefix-strs))
                      (first prefix-strs)
                      (fmt "%s and %s"
                           (cstr/join ", " (butlast prefix-strs))
                           (last prefix-strs)))]
    (fmt "%d IRI%s%s cannot be expanded due to missing prefixes %s!"
         iri-count
         (if (= 1 iri-count) "" "s")
         index-str
         prefix-str)))

(defn prefix-error-msg
  "Return an error message specifying the number of unexpandable IRIs,
   missing prefixes and, if applicable, the input coll index."
  ([prefix-errs]
   (prefix-error-msg* prefix-errs ""))
  ([prefix-errs index]
   (prefix-error-msg* prefix-errs (fmt " at index %d" index))))

(defn- scope-error-msg*
  [scope-errs index-str]
  (let [var-coll  (->> scope-errs (map :variable) distinct sort)
        var-count (->> var-coll count)
        var-strs  (->> var-coll (map name))
        var-str   (if (= 1 var-count)
                    (first var-strs)
                    (fmt "%s and %s"
                         (cstr/join ", " (butlast var-strs))
                         (last var-strs)))]
    (fmt "%d variable%s%s in %d `expr AS var` clause%s %s already defined in scope: %s!'"
         var-count
         (if (= 1 var-count) "" "s")
         index-str
         (count scope-errs)
         (if (= 1 (count scope-errs)) "" "s")
         (if (= 1 var-count) "was" "were")
         var-str)))

(defn scope-error-msg
  "Return an error message specifying the number of variables with scope
   errors, number of locations and, if applicable, the input coll index."
  ([scope-errs]
   (scope-error-msg* scope-errs ""))
  ([scope-errs index]
   (scope-error-msg* scope-errs (fmt " at index %d" index))))

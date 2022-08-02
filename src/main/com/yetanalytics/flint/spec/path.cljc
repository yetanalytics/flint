(ns com.yetanalytics.flint.spec.path
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Path Terminal
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def path-terminal-spec
  (s/and
   (comp not list?)
   (s/or :ax/iri        ax/iri?
         :ax/prefix-iri ax/prefix-iri?
         :ax/rdf-type   ax/rdf-type?)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Negated Path
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; A "negated path" (PathNegatedPropertySet in the SPARQL grammar) can
;; only contain a bunch of alternates of more negated paths.

;; Multi-spec is kind of pointless given that there's only one choice here,
;; but this makes it symmetrical to the main path spec.

(defmulti path-neg-spec-mm first)

(defmethod path-neg-spec-mm 'alt [_]
  (s/cat :path/op    #{'alt}
         :path/paths (s/* ::path-neg)))

(def path-neg-multi-spec
  (s/multi-spec path-neg-spec-mm identity))

(s/def ::path-neg
  (s/or :path/terminal
        path-terminal-spec
        :path/branch
        (s/and list?
               path-neg-multi-spec
               ;; Since we never get a singular `:path/path` there is
               ;; no need for extra conforming.
               (s/conformer #(into [] %)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Path
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Specs

(def varardic-spec
  (s/cat :path/op    #{'alt 'cat}
         :path/paths (s/* ::path)))

(def unary-spec
  (s/cat :path/op   #{'inv '? '* '+}
         :path/path ::path))

(def unary-neg-spec
  (s/cat :path/op   #{'not}
         :path/path ::path-neg))

;; Multimethods
;; Note: we could use expr/defexprspec here, but given the limited number of
;; ops that would be a bit overkill.

(defmulti path-spec-mm first)

(defmethod path-spec-mm 'alt [_] varardic-spec)
(defmethod path-spec-mm 'cat [_] varardic-spec)

(defmethod path-spec-mm 'inv [_] unary-spec)
(defmethod path-spec-mm '? [_] unary-spec)
(defmethod path-spec-mm '* [_] unary-spec)
(defmethod path-spec-mm '+ [_] unary-spec)

(defmethod path-spec-mm 'not [_] unary-neg-spec)

(def path-multi-spec
  (s/multi-spec path-spec-mm identity))

;; Putting it all together

(defn- path-conformer
  "Conform the result of a regex spec by converting any `:path/path` keys
   into `:path/paths`."
  [{op    :path/op
    path  :path/path
    paths :path/paths}]
  [[:path/op op]
   [:path/paths (if path [path] paths)]])

(s/def ::path
  (s/or :path/terminal
        path-terminal-spec
        :path/branch
        (s/and list?
               path-multi-spec
               (s/conformer path-conformer))))

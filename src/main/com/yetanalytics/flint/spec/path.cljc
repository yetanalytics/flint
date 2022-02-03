(ns com.yetanalytics.flint.spec.path
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]))

(def path-terminal-spec
  (s/and
   (comp not list?)
   (s/or :ax/iri        ax/iri?
         :ax/prefix-iri ax/prefix-iri?
         :ax/rdf-type   ax/rdf-type?)))

(s/def ::path-neg
  (s/or :path/terminal
        path-terminal-spec
        :path/branch
        (s/and
         list?
         (comp symbol? first)
         (s/or :path/varardic
               (s/cat :path/op    #{'alt}
                      :path/paths (s/* ::path-neg)))
         (s/conformer second)
         (s/conformer #(into [] %)))))

(s/def ::path
  (s/or :path/terminal
        path-terminal-spec
        :path/branch
        (s/and
         list?
         (comp symbol? first)
         (s/or :path/varardic
               (s/cat :path/op    #{'alt 'cat}
                      :path/paths (s/* ::path))
               :path/unary
               (s/cat :path/op   #{'inv '? '* '+}
                      :path/path ::path)
               :path/unary-neg
               (s/cat :path/op   #{'not}
                      :path/path ::path-neg))
         (s/conformer second)
         (s/conformer (fn [{op    :path/op
                            path  :path/path
                            paths :path/paths}]
                        [[:path/op op]
                         [:path/paths (if path [path] paths)]])))))

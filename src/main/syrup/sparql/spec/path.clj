(ns syrup.sparql.spec.path
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]))

(s/def ::path
  (s/or :path/terminal
        (s/and
         (comp not list?)
         (s/or :iri        ax/iri?
               :prefix-iri ax/prefix-iri?
               :rdf-type   ax/rdf-type?))
        :path/branch
        (s/and
         list?
         (comp symbol? first)
         (s/or :varardic
               (s/cat :op #{'alt 'cat}
                      :paths (s/* ::path))
               :unary
               (s/cat :op #{'inv 'not
                            '? '* '+}
                      :path ::path))
         (s/conformer second)
         (s/conformer (fn [{:keys [op path paths]}]
                        [[:path/op op]
                         [:path/args (if path [path] paths)]])))))

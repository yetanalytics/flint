(ns syrup.sparql.spec.path
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]))

(s/def ::path
  (s/or :path-terminal
        (s/or :iri ax/iri?
              :prefix-iri ax/prefix-iri?
              :rdf-type ax/rdf-type?)
        :path-branch
        (s/and list?
               (s/or :varardic
                     (s/cat :op #{'alt 'cat}
                            :paths (s/* ::path))
                     :unary-left
                     (s/cat :op #{'inv 'not}
                            :path ::path)
                     :unary-right
                     (s/cat :op #{'? '* '+}
                            :path ::path)
                     :unary
                     (s/cat :op #{'not}
                            :path ::path))
               (s/conformer second)
               (s/conformer (fn [{:keys [op path paths]}]
                              {:op    op
                               :paths (if path [path] paths)})))))

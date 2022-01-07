(ns syrup.sparql.spec.path
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]))

(s/def ::path
  (s/or :varardic
        (s/cat :op #{'alt 'cat 'inv '? '* '+}
               :paths (s/* ::path))
        :unary
        (s/cat :op #{'not}
               :path ::path)
        :terminal
        ax/iri-pred-spec))

(comment
  (s/conform ax/iri-pred-spec 'a)
  (s/conform ::path 'a)
  (s/conform ::path "http://example.org")
  (s/conform ::path
             '(alt :a "http://example.org" "http://example2.org"))
  )

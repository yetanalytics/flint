(ns syrup.sparql.spec.path
  (:require [clojure.spec.alpha :as s]
            [syrup.sparql.spec.axiom :as ax]))

(def path-spec
  (s/or :varardic
        (s/cat :op #{'alt 'cat 'inv '? '* '+}
               :paths (s/* path-spec))
        :unary
        (s/cat :op #{'not}
               :path path-spec)
        :terminal
        ax/iri-pred-spec))

(comment
  (s/conform ax/iri-pred-spec 'a)
  (s/conform path-spec 'a)
  (s/conform path-spec "http://example.org")
  (s/conform path-spec
             '(alt :a "http://example.org" "http://example2.org"))
  )

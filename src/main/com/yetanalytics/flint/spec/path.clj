(ns com.yetanalytics.flint.spec.path
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.axiom :as ax]))

(def path-terminal-spec
  (s/and
   (comp not list?)
   (s/or :iri        ax/iri?
         :prefix-iri ax/prefix-iri?
         :rdf-type   ax/rdf-type?)))

(s/def ::path-neg
  (s/or :path/terminal
        path-terminal-spec
        :path/branch
        (s/and
         list?
         (comp symbol? first)
         (s/or :varardic
               (s/cat :op #{'alt}
                      :paths (s/* ::path-neg)))
         (s/conformer second)
         (s/conformer (fn [{:keys [op path paths]}]
                        [[:path/op op]
                         [:path/args (if path [path] paths)]])))))

(s/def ::path
  (s/or :path/terminal
        path-terminal-spec
        :path/branch
        (s/and
         list?
         (comp symbol? first)
         (s/or :varardic
               (s/cat :op #{'alt 'cat}
                      :paths (s/* ::path))
               :unary
               (s/cat :op #{'inv '? '* '+}
                      :path ::path)
               :unary-neg
               (s/cat :op #{'not}
                      :path ::path-neg))
         (s/conformer second)
         (s/conformer (fn [{:keys [op path paths]}]
                        [[:path/op op]
                         [:path/args (if path [path] paths)]])))))

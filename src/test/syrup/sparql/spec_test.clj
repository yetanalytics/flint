(ns syrup.sparql.spec-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.query  :as qs]
            [syrup.sparql.spec.update :as us]))

(defmacro make-tests [spec dir-name]
  (let [files# (->> dir-name io/file file-seq (filter #(.isFile %)))
        tests# (map (fn [file#]
                      (let [fname# (.getName file#)
                            edn#   (edn/read-string (slurp file#))]
                        `(testing ~fname#
                           (is (s/valid? ~spec (quote ~edn#))))))
                    files#)]
    `(testing "file:" ~@tests#)))

(deftest query-tests
  (make-tests qs/query-spec "dev-resources/test-fixtures/inputs/query/"))

(deftest update-tests
  (make-tests us/update-spec "dev-resources/test-fixtures/inputs/update/"))

(deftest update-request-tests
  (make-tests us/update-request-spec "dev-resources/test-fixtures/inputs/update-request/"))

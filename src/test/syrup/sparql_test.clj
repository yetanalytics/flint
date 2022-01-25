(ns syrup.sparql-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [syrup.sparql :refer [format-query format-updates]]))

(defmacro make-format-tests [f in-dir-name out-dir-name]
  (let [in-files#  (->> in-dir-name io/file file-seq (filter #(.isFile %)))
        tests# (map (fn [in-file#]
                      (let [in-name#  (.getName in-file#)
                            out-name# (cstr/replace in-name# #".edn" ".rq")
                            out-path# (str out-dir-name out-name#)
                            out-file# (io/file out-path#)
                            edn#      (edn/read-string (slurp in-file#))
                            spql#     (slurp out-file#)]
                        `(testing ~(str in-name# " -> " out-name#)
                           (is (= ~spql# (~f (quote ~edn#)))))))
                    in-files#)]
    `(testing "file:" ~@tests#)))

(deftest query-tests
  (make-format-tests format-query
                     "dev-resources/test-fixtures/inputs/query/"
                     "dev-resources/test-fixtures/outputs/query/"))

(deftest update-tests
  (make-format-tests format-updates
                     "dev-resources/test-fixtures/inputs/update/"
                     "dev-resources/test-fixtures/outputs/update/"))

(deftest update-request-tests
  (make-format-tests (partial apply format-updates)
                     "dev-resources/test-fixtures/inputs/update-request/"
                     "dev-resources/test-fixtures/outputs/update-request/"))

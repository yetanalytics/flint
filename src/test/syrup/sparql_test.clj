(ns syrup.sparql-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [syrup.sparql :as spql :refer [format-query format-updates]]))

(defmacro make-format-tests [f in-dir-name]
  (let [in-files#  (->> in-dir-name io/file file-seq (filter #(.isFile %)))
        tests# (map (fn [in-file#]
                      (let [in-path#  (.getPath in-file#)
                            in-name#  (.getName in-file#)
                            out-path# (-> in-path#
                                          (cstr/replace #"inputs/" "outputs/")
                                          (cstr/replace #".edn" ".rq"))
                            out-file# (io/file out-path#)
                            out-name# (.getName out-file#)
                            edn#      (edn/read-string (slurp in-file#))
                            spql#     (slurp out-file#)]
                        `(testing ~(str in-name# " -> " out-name#)
                           (is (= ~spql# (~f (quote ~edn#)))))))
                    in-files#)]
    `(testing "file:" ~@tests#)))

(deftest query-tests
  (make-format-tests format-query
                     "dev-resources/test-fixtures/inputs/query/"))

(deftest update-tests
  (make-format-tests format-updates
                     "dev-resources/test-fixtures/inputs/update/"))

(deftest update-request-tests
  (make-format-tests (partial apply format-updates)
                     "dev-resources/test-fixtures/inputs/update-request/"))

(deftest exception-tests
  (testing "API functions throwing exceptions"
    (is (= ::spql/invalid-query
           (try (format-query {})
                (catch clojure.lang.ExceptionInfo e
                  (-> e ex-data :kind)))))
    (is (= ::spql/invalid-update
           (try (format-updates {})
                (catch clojure.lang.ExceptionInfo e
                  (-> e ex-data :kind)))))
    (is (= ::spql/invalid-update-request
           (try (format-updates {} {})
                (catch clojure.lang.ExceptionInfo e
                  (-> e ex-data :kind)))))))

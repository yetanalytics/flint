(ns com.yetanalytics.flint-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string  :as cstr]
            [clojure.edn     :as edn]
            [clojure.java.io :as io]
            [com.yetanalytics.flint :as flint :refer [format-query
                                                      format-updates]]))

(defn- get-in-files
  [in-dir-name]
  (->> in-dir-name io/file file-seq (filter #(.isFile %))))

(defn- in->out-file
  [in-file]
  (let [in-path  (.getPath in-file)
        out-path (-> in-path
                     (cstr/replace #"inputs/" "outputs/")
                     (cstr/replace #".edn" ".rq"))]
    (io/file out-path)))

(defmacro make-format-tests [f in-dir-name]
  (let [in-files# (get-in-files in-dir-name)
        tests#    (map (fn [in-file#]
                         (let [in-name#  (.getName in-file#)
                               out-file# (in->out-file in-file#)
                               out-name# (.getName out-file#)
                               edn#      (edn/read-string (slurp in-file#))
                               spql#     (-> (slurp out-file#)
                                             (cstr/trimr)
                                             (cstr/replace #"\s+" " "))]
                           `(testing ~(str in-name# " -> " out-name#)
                              (is (= ~spql#
                                     (~f (quote ~edn#)))))))
                       in-files#)]
    `(testing "format file:" ~@tests#)))

(defmacro make-format-pretty-tests [f in-dir-name]
  (let [in-files# (get-in-files in-dir-name)
        tests#    (map (fn [in-file#]
                         (let [in-name#  (.getName in-file#)
                               out-file# (in->out-file in-file#)
                               out-name# (.getName out-file#)
                               edn#      (edn/read-string (slurp in-file#))
                               spql#     (cstr/trimr (slurp out-file#))]
                           `(testing ~(str in-name# " -> " out-name#)
                              (is (= ~spql#
                                     (~f (quote ~edn#)))))))
                       in-files#)]
    `(testing "format and prettify file:" ~@tests#)))

(deftest query-tests
  (make-format-tests format-query
                     "dev-resources/test-fixtures/inputs/query/")
  (make-format-pretty-tests (fn [q] (format-query q :pretty? true))
                            "dev-resources/test-fixtures/inputs/query/"))

(deftest update-tests
  (make-format-tests (comp format-updates vector)
                     "dev-resources/test-fixtures/inputs/update/")
  (make-format-pretty-tests (fn [up] (format-updates [up] :pretty? true))
                            "dev-resources/test-fixtures/inputs/update/"))

(deftest update-request-tests
  (make-format-tests format-updates
                     "dev-resources/test-fixtures/inputs/update-request/")
  (make-format-pretty-tests (fn [ups] (format-updates ups :pretty? true))
                            "dev-resources/test-fixtures/inputs/update-request/"))

(deftest exception-tests
  (testing "API functions throwing exceptions"
    (is (= ::flint/invalid-query
           (try (format-query {})
                (catch clojure.lang.ExceptionInfo e
                  (-> e ex-data :kind)))))
    (is (= ::flint/invalid-update
           (try (format-updates [{}])
                (catch clojure.lang.ExceptionInfo e
                  (-> e ex-data :kind)))))
    (is (= ::flint/invalid-update-request
           (try (format-updates [{} {}])
                (catch clojure.lang.ExceptionInfo e
                  (-> e ex-data :kind)))))))

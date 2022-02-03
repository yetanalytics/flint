(ns com.yetanalytics.flint.spec-test
  #_{:clj-kondo/ignore #?(:clj [] :cljs [:unused-referred-var])}
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [com.yetanalytics.flint.spec.query  :as qs]
            [com.yetanalytics.flint.spec.update :as us]
            #?@(:clj [[clojure.edn        :as edn]
                      [clojure.java.io    :as io]]))
  #?(:cljs (:require-macros
            [com.yetanalytics.flint.spec-test :refer [make-tests]])))

#?(:clj
   (defmacro make-tests [spec dir-name]
     (let [files# (->> dir-name io/file file-seq (filter #(.isFile %)))
           tests# (map (fn [file#]
                         (let [fname# (.getName file#)
                               edn#   (edn/read-string (slurp file#))]
                           `(testing ~fname#
                              (is (s/valid? ~spec (quote ~edn#))))))
                       files#)]
       `(testing "file:" ~@tests#))))

(deftest query-tests
  (make-tests qs/query-spec
              "dev-resources/test-fixtures/inputs/query/"))

(deftest update-tests
  (make-tests us/update-spec
              "dev-resources/test-fixtures/inputs/update/"))

(deftest update-request-tests
  (make-tests (s/coll-of us/update-spec)
              "dev-resources/test-fixtures/inputs/update-request/"))

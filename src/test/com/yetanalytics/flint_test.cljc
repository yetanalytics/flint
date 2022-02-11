(ns com.yetanalytics.flint-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.yetanalytics.flint :as flint :refer [format-query
                                                      format-update
                                                      format-updates]]
            #?@(:clj [[clojure.string  :as cstr]
                      [clojure.edn     :as edn]
                      [clojure.java.io :as io]]))
  #?(:cljs (:require-macros
            [com.yetanalytics.flint-test :refer [make-format-tests
                                                 make-format-pretty-tests]])))

#?(:clj
   (defn- get-in-files
     [in-dir-name]
     (->> in-dir-name io/file file-seq (filter #(.isFile %)))))

#?(:clj
   (defn- in->out-file
     [in-file]
     (let [in-path  (.getPath in-file)
           out-path (-> in-path
                        (cstr/replace #"inputs/" "outputs/")
                        (cstr/replace #".edn" ".rq"))]
       (io/file out-path))))

#?(:clj
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
       `(testing "format file:" ~@tests#))))

#?(:clj
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
       `(testing "format and prettify file:" ~@tests#))))

(deftest query-tests
  (make-format-tests format-query
                     "dev-resources/test-fixtures/inputs/query/")
  (make-format-pretty-tests (fn [q] (format-query q :pretty? true))
                            "dev-resources/test-fixtures/inputs/query/"))

(deftest update-tests
  (make-format-tests format-update
                     "dev-resources/test-fixtures/inputs/update/")
  (make-format-pretty-tests (fn [up] (format-update up :pretty? true))
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
                (catch #?(:clj clojure.lang.ExceptionInfo
                          :cljs js/Error) e
                  (-> e ex-data :kind)))))
    (is (= ::flint/invalid-update
           (try (format-update {})
                (catch #?(:clj clojure.lang.ExceptionInfo
                          :cljs js/Error) e
                  (-> e ex-data :kind)))))
    (is (= ::flint/invalid-update
           (try (format-updates [{} {}])
                (catch #?(:clj clojure.lang.ExceptionInfo
                          :cljs js/Error) e
                  (-> e ex-data :kind)))))
    (is (= ::flint/invalid-prefixes
           (try (format-query '{:select [?x]
                                :where [[:foo/bar :a ?y]]})
                (catch #?(:clj clojure.lang.ExceptionInfo
                          :cljs js/Error) e
                  (-> e ex-data :kind)))))
    (is (= ::flint/invalid-prefixes
           (try (format-updates ['{:copy :foo :to :bar}
                                 '{:copy :baz :to :qux}])
                (catch #?(:clj clojure.lang.ExceptionInfo
                          :cljs js/Error) e
                  (-> e ex-data :kind)))))
    (is (= ::flint/invalid-scoped-vars
           (try (format-query '{:prefixes {:foo "<http://foo.org/>"}
                                :select [[2 ?x]]
                                :where [[?x :foo/bar ?y]]})
                (catch #?(:clj clojure.lang.ExceptionInfo
                          :cljs js/Error) e
                  (-> e ex-data :kind)))))
    (is (= ::flint/invalid-aggregates
           (try (format-query '{:prefixes {:foo "<http://foo.org/>"}
                                :select *
                                :where [[?x ?y ?z]]
                                :group-by [?x]})
                (catch #?(:clj clojure.lang.ExceptionInfo
                          :cljs js/Error) e
                  (-> e ex-data :kind)))))
    (is (= ::flint/invalid-bnodes-bgp
           (try (format-query '{:prefixes {:foo "<http://foo.org/>"}
                                :select [?x]
                                :where [[?x :foo/bar _1]
                                        [:optional [[?y :foo/baz _1]]]]})
                (catch #?(:clj clojure.lang.ExceptionInfo
                          :cljs js/Error) e
                  (-> e ex-data :kind)))))
    (is (= ::flint/invalid-bnodes-update
           (try (format-updates '[{:prefixes {:foo "<http://foo.org/>"}
                                   :insert-data [[:foo/bar :foo/baz _1]]}
                                  {:insert-data [[:foo/bar :foo/baz _1]]}])
                (catch #?(:clj clojure.lang.ExceptionInfo
                          :cljs js/Error) e
                  (-> e ex-data :kind)))))
    (testing "- short circuiting on error"
      (is (= 0
             (try (format-updates ['{:copy :foo :to :bar}
                                   '{:copy :baz :to :qux}])
                  (catch #?(:clj clojure.lang.ExceptionInfo
                            :cljs js/Error) e
                    (-> e ex-data :index))))))))

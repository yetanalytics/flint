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
    (testing "- short circuiting on error"
      (is (= 0
             (try (format-updates ['{:copy :foo :to :bar}
                                   '{:copy :baz :to :qux}])
                  (catch #?(:clj clojure.lang.ExceptionInfo
                            :cljs js/Error) e
                    (-> e ex-data :index))))))))

(comment
  (def sparql
    '{:prefixes   {:foaf "<http://xmlns.com/foaf/0.1/>"
                   :data "<http://example.org/foaf/>"
                   :rdfs "<http://www.w3.org/2000/01/rdf-schema#>"}
      :select     [?mbox ?nick ?ppd]
      :from-named ["<http://example.org/foaf/aliceFoaf>"
                   "<http://example.org/foaf/bobFoaf>"]
      :where      [[:graph
                    :data/aliceFoaf
                    [{?alice {:foaf/mbox  #{"mailto:alice@work.example"}
                              :foaf/knows #{?whom}}
                      ?whom  {:foaf/mbox    #{?mbox}
                              :rdfs/seeAlso #{?ppd}}
                      ?ppd   {a #{:foaf/PersonalProfileDocument}}}]]
                   [:graph
                    ?ppd
                    [{?w {:foaf/mbox #{?mbox}
                          :foaf/nick #{?nick}}}]]]})
  
  (require '[criterium.core :as crit])
  
  (crit/quick-bench (flint/format-query sparql))
  
  (time
   (dotimes [_ 10000]
     (flint/format-query sparql :validate? true)))
  
  (def sparql-2
    '{:prefixes   {:foaf "<http://xmlns.com/foaf/0.1/>"
                   :dc   "<http://purl.org/dc/elements/1.1/>"}
      :select     [?who ?g ?mbox]
      :from       ["<http://example.org/dft.ttl>"]
      :from-named ["<http://example.org/alice>"
                   "<http://example.org/bob>"]
      :where      [[?g :dc/publisher ?who]
                   [:graph ?g [[?x :foaf/mbox ?mbox]]]]})
  
  (time
   (dotimes [_ 10000]
     (flint/format-query sparql-2 :validate? true))))

;; BEFORE

;; Evaluation count : 1038 in 6 samples of 173 calls.
;;              Execution time mean : 694.017276 µs
;;     Execution time std-deviation : 53.365888 µs
;;    Execution time lower quantile : 618.786665 µs ( 2.5%)
;;    Execution time upper quantile : 756.586947 µs (97.5%)
;;                    Overhead used : 6.475562 ns

;; "Elapsed time: 6190.232667 msecs"
;; "Elapsed time: 3767.988997 msecs"

;; AFTER

;; Evaluation count : 1446 in 6 samples of 241 calls.
;;              Execution time mean : 454.109174 µs
;;     Execution time std-deviation : 27.352271 µs
;;    Execution time lower quantile : 429.148643 µs ( 2.5%)
;;    Execution time upper quantile : 492.796899 µs (97.5%)
;;                    Overhead used : 6.475562 ns

;; "Elapsed time: 5563.623451 msecs"
;; "Elapsed time: 2215.548805 msecs"

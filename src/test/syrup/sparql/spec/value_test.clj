(ns syrup.sparql.spec.value-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.value :as vs]))

(deftest conform-values-test
  (testing "Conform VALUES clause"
    (is (= '[:values-map {[?foo ?bar] [[1 :a] [2 :b] [3 :c]]}]
           (s/conform ::vs/values '{[?foo ?bar] [[1 :a] [2 :b] [3 :c]]})
           (s/conform ::vs/values '{?foo [1 2 3]
                                    ?bar [:a :b :c]})))))

(deftest invalid-values-test
  (testing "Invalid VALUES clause"
    (is (= {::s/problems [{:path [:values-map :sparql]
                           :pred `map?
                           :val  2
                           :via  [::vs/values]
                           :in   []}
                          {:path [:values-map :clojure]
                           :pred `map?
                           :val  2
                           :via  [::vs/values]
                           :in   []}]
            ::s/spec     ::vs/values
            ::s/value    2}
         (s/explain-data ::vs/values 2)))
    (is (= {:path [:values-map :clojure]
            :val  '{?foo [1 2]
                    ?bar [:a :b :c]}
            :via  [::vs/values]
            :in   []}
         (-> (s/explain-data ::vs/values '{?foo [1 2]
                                           ?bar [:a :b :c]})
             ::s/problems
             last
             (dissoc :pred))))))

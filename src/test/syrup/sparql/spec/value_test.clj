(ns syrup.sparql.spec.value-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.value :as vs]))

(deftest conform-test
  (testing "Conform VALUES clause"
    (is (= '[:values-map {[?foo ?bar] [[1 :a] [2 :b] [3 :c]]}]
           (s/conform ::vs/values '{[?foo ?bar] [[1 :a] [2 :b] [3 :c]]})
           (s/conform ::vs/values '{?foo [1 2 3]
                                    ?bar [:a :b :c]})))))

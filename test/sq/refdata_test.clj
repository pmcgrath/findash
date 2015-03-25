(ns sq.refdata-test
  (:require [clojure.test :refer :all]
            [sq.refdata :refer :all]))

(deftest get-currency-iso-alpha-codes-test
  (testing "Get currency codes"
    (let [currency-codes (get-currency-iso-alpha-codes)]
      (is (nil? (some #{"ABC"} currency-codes)) "ABC")
      (is (= "EUR" (some #{"EUR"} currency-codes) "EUR")))))

(ns findash.quotes-watcher-test
  (:require [clojure.test :refer :all]
            [findash.quotes-watcher :refer :all]))

(deftest get-acquirer-fn-test
  (testing "Get acquirer function - Currently only one acquirer"
    (let [acquirer-fn (get-acquirer-fn)]
      (is (not (nil? acquirer-fn))))))

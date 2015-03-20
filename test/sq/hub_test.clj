(ns sq.hub-test
  (:require 
    [clojure.test :refer :all]
    [sq.hub :refer :all]))

(deftest get-item-test
  (testing "Get item pre start - nothing to find in this case"
    (let [item (get-item :new-quotes-ch)]
      (is (nil? item) ":new quoutes-ch")))

  (testing "Get item - post start"
    (start)
    (let [item (get-item :unknow)]
      (is (nil? item) "unknown"))
    (let [item (get-item "new-quotes-ch")]
      (is (nil? item) "new quoutes-ch"))
    (let [item (get-item :new-quotes-ch)]
      (is (not (nil? item)) ":new quoutes-ch"))))

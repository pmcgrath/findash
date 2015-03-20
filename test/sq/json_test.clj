(ns sq.json-test
  (:require 
    [clojure.test :refer :all]
    [clj-time.core :as time]
    [sq.json :refer :all]))

(deftest generate-string-test
  (testing "Generate string"
    (let [data {:timestamp (time/date-time 2015 10 30 4 1 27 451) :reading 1}]
      (is (= "{\"timestamp\":\"2015-10-30T04:01:27.451Z\",\"reading\":1}" (generate-string data))))))

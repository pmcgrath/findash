(ns sq.json-test
  (:require 
    [clojure.test :refer :all]
    [clj-time.core :as time]
    [ring.util.io :as io]
    [sq.json :refer :all]))

(deftest generate-string-test
  (testing "Generate string"
    (let [data {:timestamp (time/date-time 2015 10 30 4 1 27 451) :reading 1}]
      (is (= "{\n  \"timestamp\" : \"2015-10-30T04:01:27.451Z\",\n  \"reading\" : 1\n}" (generate-string data))))))

(deftest parse-body-test
  (testing "Parse body - empty content"
    (let [body (io/string-input-stream "{}")
          data (parse-body body)]
      (is (= {} data))))
  
  (testing "Parse body - has content"
    (let [body (io/string-input-stream "{\"f1\": \"value 1\", \"f2\": 2}")
          data (parse-body body)]
      (is (= {:f1 "value 1" :f2 2} data)))))

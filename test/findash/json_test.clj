(ns findash.json-test
  (:require [clj-time.core :as time]
            [clojure.test :refer :all]
            [findash.json :refer :all]
            [ring.util.io :as io]))

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

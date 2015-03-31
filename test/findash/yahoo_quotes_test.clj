(ns findash.yahoo-quotes-test
  (:require [clj-time.core :as time]
            [clojure.test :refer :all]
            [findash.yahoo-quotes :refer :all]))

(deftest get-comma-separated-symbols-test
  (testing "Get comma sperated symbols - Single stock"
    (let [config {:refresh-interval-seconds 1 :stocks [{:symbol "GOOG"}]}
          comma-seperated-symbols (get-comma-separated-symbols config)]
      (is (= "GOOG" comma-seperated-symbols))))

  (testing "Get comma sperated symbols - Multiple stocks"
    (let [config {:refresh-interval-seconds 1 :stocks [{:symbol "GOOG"} {:symbol "TED.L"}]}
          comma-seperated-symbols (get-comma-separated-symbols config)]
      (is (= "GOOG,TED.L" comma-seperated-symbols)))))

(deftest get-lookup-url-test
  (testing "Get lookup url - Single stock"
    (let [config {:refresh-interval-seconds 1 :stocks [{:symbol "GOOG"}]}
          url (get-lookup-url config)]
      (is (= "http://finance.yahoo.com/webservice/v1/symbols/GOOG/quote?format=json" url))))

  (testing "Get lookup url - Multiple stocks"
    (let [config {:refresh-interval-seconds 1 :stocks [{:symbol "GOOG"} {:symbol "TED.L"}]}
          url (get-lookup-url config)]
      (is (= "http://finance.yahoo.com/webservice/v1/symbols/GOOG,TED.L/quote?format=json" url)))))

(deftest parse-raw-test
  (testing "Parse raw quote"
    (let [raw {"resource" {"classname" "Quote" "fields" { "name" "RYANAIR HOLDINGS" "price" "10.510000" "symbol" "RY4B.IR" "ts" "1425987348" "type" "equity" "utctime" "2015-03-10T11:35:48+0000" "volume" "601599"}}}
          quote (parse-raw raw)]
      (is (= {:symbol "RY4B.IR" :price 10.51 :timestamp (time/date-time 2015 3 10 11 35 48)} quote))))

  (testing "Parse raw quote"
    (let [raw {"resource" {"classname" "Quote" "fields" { "name" "RYANAIR HOLDINGS" "price" "10.510000" "symbol" "RY4B.IR" "ts" "1425987348" "type" "equity" "utctime" "2015-03-10T11:35:48+0000" "volume" "601599"}}}
          quote (parse-raw raw)]
      (is (= {:symbol "RY4B.IR" :price 10.51 :timestamp (time/date-time 2015 3 10 11 35 48)} quote)))))

(deftest acquire-test
  (testing "Acquire - This depends on a connection to yahoo"
    (let [config {:refresh-interval-seconds 1 :stocks [{:symbol "GOOG"} {:symbol "TSCO.L"}]}
          quotes (acquire config)
          find-match (fn [coll symbol] (some (fn [q] (if (= (:symbol q) symbol) q)) coll))
          goog-quote (find-match quotes "GOOG")
          tsco-quote (find-match quotes "TSCO.L")]
      (is (not (nil? goog-quote)) "goog code")
      (is (not (nil? tsco-quote)) "tsco code"))))

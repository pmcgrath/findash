(ns findash.watcher-test
  (:require [clojure.core.async :refer [<!! alts!! chan close! timeout]]
            [clojure.test :refer :all]
            [findash.watcher :refer :all]))

(def config-with-no-data {:refresh-interval-seconds 1})

(def config-with-singles {:refresh-interval-seconds 1 :stocks [{:symbol "GOOG"}] :currency-pairs [{:from "EUR" :to "GBP"}]})

(defn execute
  [config config-key publication-key data throw-exception]
  (let [get-config-fn (constantly config)
        data-ch (chan)
        acquirer-fn (fn [c] (if throw-exception (throw (Exception. "Bang")) data))
        timeout-ch (timeout 200)]
      (run-acquire-loop get-config-fn data-ch acquirer-fn config-key publication-key)
      (let [[value-from-channel ch] (alts!! [data-ch timeout-ch])]
        (close! data-ch)
        (close! timeout-ch)
      	(if (= timeout-ch ch)
          {:timed-out true}
          {:topic (:topic value-from-channel) :data (publication-key value-from-channel) :timed-out false}))))

(deftest run-acquire-loop-test
  (testing "Run acquire loop - Stocks - acquirer-fn throws exception"
    (let [result (execute config-with-no-data :stocks :quotes [] true)]
      (is (= true (:timed-out result)) "Expecting timeout as exception encountered when acquiring data, so nothing put on the data channel")))
  
  (testing "Run acquire loop - Stocks - 1 stock"
    (let [expected-quotes [{:symbol "GOOG" :price 1.0}]
          result (execute config-with-singles :stocks :quotes expected-quotes false)]
        (is (= :new-quotes (:topic result)) "Publication message topic")
        (is (= expected-quotes (:data result)) "Single quote published")))

  (testing "Run acquire loop - Stocks - 0 stocks"
    (let [result (execute config-with-no-data :curreny-pairs :rates [] false)]
      (is (= true (:timed-out result)) "Expecting timeout channel as no currency pairs to acquire")))

  (testing "Run acquire loop - CurrencyPairs - acquirer-fn throws exception"
    (let [result (execute config-with-no-data :curreny-pairs :rates [] true)]
      (is (= true (:timed-out result)) "Expecting timeout as exception encountered when acquiring data, so nothing put on the data channel")))
  
  (testing "Run acquire loop - CurrencyPairs - 1 currency pair"
    (let [expected-rates [{:from "EUR" :to "GBP" :rate 1.12345}]
          result (execute config-with-singles :currency-pairs :rates expected-rates false)]
        (is (= :new-rates (:topic result)) "Publication message topic")
        (is (= expected-rates (:data result)) "Single rate published")))

  (testing "Run acquire loop - CurrencyPairs - 0 currency pairs"
    (let [result (execute config-with-no-data :curreny-pairs :rates [] false)]
      (is (= true (:timed-out result)) "Expecting timeout channel as no currency pairs to acquire"))))

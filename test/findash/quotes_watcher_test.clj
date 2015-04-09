(ns findash.quotes-watcher-test
  (:require [clojure.core.async :refer [<!! alts!! chan close! timeout]]
            [clojure.test :refer :all]
            [findash.quotes-watcher :refer :all]))

(deftest get-acquirer-fn-test
  (testing "Get acquirer function - Currently only one acquirer"
    (let [acquirer-fn (get-acquirer-fn)]
      (is (not (nil? acquirer-fn))))))

(deftest run-acquire-loop-test
  (testing "Run acquire loop - 1 stock"
    (let [config {:refresh-interval-seconds 1 :stocks [{:symbol "GOOG"}]} 
          get-config-fn (constantly config)
          quotes [{:symbol "GOOG" :price 1.0}]
          data-ch (chan)
          acquirer-fn (constantly quotes)]
      (run-acquire-loop get-config-fn data-ch acquirer-fn)
      (let [data (<!! data-ch)
            topic (:topic data)
            published-quotes (:quotes data)]
        (is (= :new-quotes topic) "Publication message topic")
        (is (= quotes published-quotes) "Single quote published")
        (close! data-ch))))

  (testing "Run acquire loop - 0 stocks"
    (let [config {:refresh-interval-seconds 1} 
          get-config-fn (constantly config)
          quotes []
          data-ch (chan)
          acquirer-fn (constantly quotes)
          timeout-ch (timeout 200)]
      (run-acquire-loop get-config-fn data-ch acquirer-fn)
      (let [[value-from-channel ch] (alts!! [data-ch timeout-ch])]
      	(is (= timeout-ch ch) "Expecting timeout channel as no stocks to acquire")
        (close! data-ch)))))

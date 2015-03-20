(ns sq.quoteswatcher-test
  (:require 
    [clojure.test :refer :all]
    [clojure.core.async :refer [<!! alts!! chan close! timeout]]
    [clj-time.core :as time]
    [sq.quoteswatcher :refer :all]))

(deftest get-acquirer-fn-test
  (testing "Get acquirer function - Currently only one acquirer"
    (let [acquirer-fn (get-acquirer-fn)]
      (is (not (nil? acquirer-fn))))))

(deftest run-acquirer-quotes-loop-test
  (testing "Run acquire quotes loop - 1 stock"
    (let [config {:refresh-interval-seconds 1 :stocks [{:symbol "GOOG"}]} 
          get-config-fn (constantly config)
          quotes [{:symbol "GOOG" :price 1.0}]
          new-quotes-pub-ch (chan)
          acquirer-fn (constantly quotes)]
      (run-acquire-quotes-loop get-config-fn new-quotes-pub-ch acquirer-fn)
      (is (= quotes (<!! new-quotes-pub-ch)) "Single quote published")
      (close! new-quotes-pub-ch)))

  (testing "Run acquire quotes loop - 0 stocks"
    (let [config {:refresh-interval-seconds 1} 
          get-config-fn (constantly config)
          quotes []
          new-quotes-pub-ch (chan)
          acquirer-fn (constantly quotes)
          timeout-ch (timeout 200)]
      (run-acquire-quotes-loop get-config-fn new-quotes-pub-ch acquirer-fn)
      (let [[value-from-channel ch] (alts!! [new-quotes-pub-ch timeout-ch])]
      	(is (= timeout-ch ch) "Expecting timeout channel as no stocks to acquire"))
      (close! new-quotes-pub-ch))))

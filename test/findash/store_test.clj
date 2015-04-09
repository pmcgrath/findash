(ns findash.store-test
  (:require [clojure.core.async :refer [>!! <!! chan close! timeout]]
            [clojure.test :refer :all]
            [findash.store :refer :all]))

(deftest update-latest-quotes!-test
  (testing "Update latest quotes - no previous quotes exist"
    (let [quotes [{:symbol "GOOG" :price 1.0} {:symbol "TED" :price 2}]]
      (init!)
      (update-latest-quotes! quotes)
      (is (= quotes (get-latest-quotes)))))
  
  (testing "Update latest quotes - no previous quotes exist"
    (let [quotes-update1 [{:symbol "GOOG" :price 1.0} {:symbol "TED" :price 2}]
          quotes-update2 [{:symbol "AMD" :price 100.0} {:symbol "TED" :price 200}]
          expected-quotes [{:symbol "AMD" :price 100.0} {:symbol "GOOG" :price 1.0} {:symbol "TED" :price 200}]]
      (init!)
      (update-latest-quotes! quotes-update1)
      (update-latest-quotes! quotes-update2)
      (is (= expected-quotes (get-latest-quotes))))))

(deftest process-new-data-messages-test
  (testing "Process new data messages - will add new quotes to store on receiving a new-quotes message"
    (let [data-ch (chan)
          quotes-update1 [{:symbol "GOOG" :price 1.0} {:symbol "TED" :price 2}]
          quotes-update2 [{:symbol "AMD" :price 100.0} {:symbol "TED" :price 200}]
          expected-final-quotes [{:symbol "AMD" :price 100.0} {:symbol "GOOG" :price 1.0} {:symbol "TED" :price 200}]]
      (init!)
      (process-new-data-messages data-ch)
      (>!! data-ch {:topic :new-quotes :quotes quotes-update1})
      (is (= quotes-update1 (get-latest-quotes)))
      (>!! data-ch {:topic :new-quotes :quotes quotes-update2})
      (is (= expected-final-quotes (get-latest-quotes)))
      (close! data-ch)))

  (testing "Process new data messages - both new rates and new quotes"
    (let [data-ch (chan)
          quotes-update1 [{:symbol "GOOG" :price 1.0} {:symbol "TED" :price 2}]
          rates-update1 [{:from "EUR" :to "GBP" :rate 2.0} {:from "USD" :to "EUR" :rate 1.0}]
          quotes-update2 [{:symbol "AMD" :price 100.0} {:symbol "TED" :price 200}]
          rates-update2 [{:from "EUR" :to "GBP" :rate 2.222222} {:from "EUR" :to "SEK" :rate 3.333333} {:from "USD" :to "EUR" :rate 1.111111}]
          expected-final-quotes [{:symbol "AMD" :price 100.0} {:symbol "GOOG" :price 1.0} {:symbol "TED" :price 200}]
          expected-final-rates [{:from "EUR" :to "GBP" :rate 2.222222} {:from "EUR" :to "SEK" :rate 3.333333} {:from "USD" :to "EUR" :rate 1.111111}]]
      (init!)
      (process-new-data-messages data-ch)
      (>!! data-ch {:topic :new-quotes :quotes quotes-update1})
      (is (= quotes-update1 (get-latest-quotes)))
      (>!! data-ch {:topic :new-rates :rates rates-update1})
      (<!! (timeout 20)) ;; Slow down for initial creation of the storage atom
      (is (= rates-update1 (get-latest-rates)))
      (>!! data-ch {:topic :new-quotes :quotes quotes-update2})
      (is (= expected-final-quotes (get-latest-quotes)))
      (>!! data-ch {:topic :new-rates :rates rates-update2})
      (is (= expected-final-rates (get-latest-rates)))
      (close! data-ch))))

(deftest get-currency-pairs-test
  (testing "Get currency pairs"
    (let [eur-usd-pair {:from "EUR" :to "USD"}
          usd-gbp-pair {:from "USD" :to "GBP"}
          eur-aud-pair {:from "EUR" :to "AUD"}]
      (init!)
      ; 1 pair
      (add-currency-pair eur-usd-pair)
      (is (= [eur-usd-pair] (get-currency-pairs)) "First pair")
      ; 2 pairs
      (add-currency-pair usd-gbp-pair)
      (is (= [eur-usd-pair usd-gbp-pair] (get-currency-pairs)) "Second pair")
      ; 3 pairs
      (add-currency-pair eur-aud-pair)
      (is (= [eur-aud-pair eur-usd-pair usd-gbp-pair] (get-currency-pairs)) "Three pairs"))))

(ns findash.store-test
  (:require [clojure.core.async :refer [>!! chan close!]]
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

(deftest subscribe-for-quote-updates-test
  (testing "Subscribe for quote updates - will add quotes to store on receiving new quotes"
    (let [new-quotes-ch (chan)
          quotes-update1 [{:symbol "GOOG" :price 1.0} {:symbol "TED" :price 2}]
          quotes-update2 [{:symbol "AMD" :price 100.0} {:symbol "TED" :price 200}]
          expected-final-quotes [{:symbol "AMD" :price 100.0} {:symbol "GOOG" :price 1.0} {:symbol "TED" :price 200}]]
      (init!)
      (subscribe-for-quote-updates new-quotes-ch)
      (>!! new-quotes-ch quotes-update1)
      (is (= quotes-update1 (get-latest-quotes)))
      (>!! new-quotes-ch quotes-update2)
      (is (= expected-final-quotes (get-latest-quotes)))
      (close! new-quotes-ch))))


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

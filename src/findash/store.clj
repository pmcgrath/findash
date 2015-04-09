(ns findash.store
  (:require [clojure.core.async :refer [<! go-loop]]
            [findash.ref-data :as ref-data]))

(def ^:private default-config { 
  :refresh-interval-seconds 5 
  :stocks [
    {:symbol "RY4B.IR" :currency "EUR"} 
    {:symbol "LLOY.L" :currency "GBP"}
    {:symbol "TSCO.L" :currency "GBP"}
    {:symbol "GOOG" :currency "USD"}
    {:symbol "MSFT" :currency "USD"}]})

;  :currency-pairs [
;    {:from "USD" :to "EUR"}]}) 

(def ^:private config (atom {}))
(def ^:private latest-quotes (atom {}))
(def ^:private latest-rates (atom {}))

(defn init!
  ([]
    (init! default-config))
  ([new-config]
    (reset! config new-config)
    (reset! latest-quotes {})
    (reset! latest-rates {})))

(defn update-latest-quotes!
  [quotes]
  ; This allows for partial updates
  (doseq [quote quotes]
    (swap! latest-quotes assoc (:symbol quote) quote)))

(defn update-latest-rates!
  [rates]
  ; This allows for partial updates
  (let [get-key-fn (fn [rate] (str (:from rate) (:to rate)))]
    (doseq [rate rates]
      (swap! latest-rates assoc (get-key-fn rate) rate))))

(defn process-new-data-messages
  [new-data-sub-ch]
  (go-loop []
    (let [data (<! new-data-sub-ch)
          topic (:topic data)]
      (when (= topic :new-quotes)
        (update-latest-quotes! (:quotes data)))
      (when (= topic :new-rates)
        (update-latest-rates! (:rates data)))
      (recur))))

(defn get-config
  []
  @config)

(defn get-stocks
  []
  (-> @config :stocks (sort-by :symbol)))

(defn add-stock
  [new-stock]
  (let [stock-symbol (:symbol new-stock)
        matching-stock (some #(= (:symbol %) stock-symbol) (:stocks @config))]
    (if (nil? matching-stock)
      (do
        (swap! config assoc :stocks (conj (:stocks @config) new-stock))
        true)
      false)))

(defn get-currency-pairs
  []
  (->> @config :currency-pairs (sort-by #(str (:from %) (:to %)))))

(defn add-currency-pair
  [new-currency-pair]
  (let [from (:from new-currency-pair)
        to (:to new-currency-pair)
        matching-currency-pair (some #(and (= from (:from %)) ()  (= to (:to %))) (:currency-pairs @config))]
    (if (nil? matching-currency-pair)
      (do
        (swap! config assoc :currency-pairs (conj (:currency-pairs @config) new-currency-pair))
        true)
      false)))

(defn get-currency-iso-alpha-codes
  []
  (ref-data/get-currency-iso-alpha-codes))

(defn get-latest-quotes
  []
  (-> @latest-quotes sort vals))

(defn get-latest-rates
  []
  (-> @latest-rates sort vals))

(defn start 
  [new-data-sub-ch]
  (init!)
  (process-new-data-messages new-data-sub-ch))

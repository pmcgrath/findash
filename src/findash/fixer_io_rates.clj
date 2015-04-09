(ns findash.fixer-io-rates
  (:require [clj-time.format :as fmt]
            [clojure.data.json :as json]))

(defn get-distinct-base-currencies
  [config] 
  (->> config :currency-pairs (map #(:from %)) distinct))

(defn get-lookup-url
  [base-currency]
  (str "http://api.fixer.io/latest?base=" base-currency))

(defn get-data
  [url]
  (slurp url))

(defn get-for-base-currency
  [base-currency]
  (let [url (get-lookup-url base-currency)
        data (get-data url)
        rates-data (json/read-str data :key-fn keyword)
	timestamp (fmt/parse (fmt/formatters :date) (:date rates-data))
        reduction-fn (fn [accum, key, value] (conj accum {:from base-currency :to (name key) :timestamp timestamp :rate value}))]
    (reduce-kv reduction-fn [] (:rates rates-data))))

(defn get-for-base-currencies
  [base-currencies]
  (let [reduction-fn (fn [accum base-currency] conj accum (get-for-base-currency base-currency))]
    (reduce reduction-fn [] base-currencies)))

(defn filter-rates
  [config all-rates]
  (let [get-key-fn (fn [pair] (str (:from pair) (:to pair)))
        rates-index (reduce (fn [accum rate] (assoc accum (get-key-fn rate) rate)) {} all-rates)
        reduction-fn (fn [accum currency-pair] (let [key (get-key-fn currency-pair) matching-rate (get rates-index key)] (if (nil? matching-rate) accum (conj accum matching-rate))))]
    (reduce reduction-fn [] (:currency-pairs config))))

(defn acquire
  [config]
  (let [base-currencies (get-distinct-base-currencies config)
        all-rates (get-for-base-currencies base-currencies)]
    (filter-rates config all-rates)))

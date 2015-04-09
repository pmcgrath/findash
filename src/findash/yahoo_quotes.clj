(ns findash.yahoo-quotes
  (:require [clj-time.format :as fmt]
            [clojure.data.json :as json]))

(defn get-comma-separated-symbols
  [config] 
  (apply str (interpose "," (map (fn [s] (:symbol s)) (:stocks config)))))

(defn get-lookup-url
  [config]
  ; Ignoring url encoding issues, cap on number of symbols that can be looked up and the url length for now
  (str "http://finance.yahoo.com/webservice/v1/symbols/" (get-comma-separated-symbols config) "/quote?format=json"))

(defn get-data
  [url]
  (slurp url))

(defn get-raw
  [config]
  (let [url (get-lookup-url config)
        data (get-data url)]
    (json/read-str data :key-fn keyword)))

(defn parse-raw
  [quote]
  ; Destructure
  ; 	fields var as :resource internal map and then its :fields internal map
  ;	symbol, price and utctime vars from within the just destructured fields var
  ; Return a new map with just the data we need
  (let [fields (get-in quote [:resource :fields]) 
        {:keys [symbol price utctime]} fields]
    {:symbol symbol :price (read-string price) :timestamp (fmt/parse (fmt/formatters :date-time-no-ms) utctime)}))

(defn acquire
  [config]
  ; Get raw quotes
  ; Destructure quotes var as the :list internal map and then its :resources internal map
  ; Return a vector of quote data
  (let [raw (get-raw config)
        quotes (get-in raw [:list :resources])]
    (vec (map parse-raw quotes))))

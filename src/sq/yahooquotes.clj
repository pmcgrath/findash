(ns sq.yahooquotes
  (:require 
    [clojure.data.json :as json]
    [clj-time.format :as fmt]))

(defn get-comma-separated-symbols [config] 
  (apply str (interpose "," (map (fn [s] (:symbol s)) (:stocks config))))
)

(defn get-lookup-url [config]
  ; Ignoring url encoding issues, cap on number of symbols that can be looked up and the url length for now
  (str "http://finance.yahoo.com/webservice/v1/symbols/" (get-comma-separated-symbols config) "/quote?format=json")
)

(defn get-raw [config]
  ; Make web requets and then convert to json - migth be possible to do this in one step
  (json/read-str (slurp (get-lookup-url config)))
)

(defn parse-raw [quote]
  ; Destructure
  ; 	fields var as "resources" internal map and then its "fields" internal map
  ;	symbol, price and utctime vars from within the just destructured fields var
  ; Return a new map with just the data we need
  (let [fields (get-in quote ["resource" "fields"]) 
        {:strs [symbol price utctime]} fields]
    {:symbol symbol :price price :timestamp (fmt/parse (fmt/formatters :date-time-no-ms) utctime)}
  )
)

(defn acquire [config]
  ; Get raw quotes
  ; Destructure quotes var as "list" internal map and then its "resources" internal map
  ; Return a vector of quote data
  (let [raw (get-raw config)
        {{quotes "resources"} "list"} raw]
    (vec (map parse-raw quotes))
  )
)

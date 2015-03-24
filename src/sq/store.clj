(ns sq.store
  (:require
    [clojure.core.async :refer [<! go-loop]]))

(def ^:private default-config { 
  :port 3000
  :refresh-interval-seconds 5 
  :stocks [
    {:symbol "RY4B.IR" :currency "EUR"} 
    {:symbol "LLOY.L" :currency "GBP"}
    {:symbol "TSCO.L" :currency "GBP"}
    {:symbol "GOOG" :currency "USD"}
    {:symbol "MSFT" :currency "USD"}]})

(def ^:private config (atom {}))
(def ^:private latest-quotes (atom {}))

(defn init!
  ([]
    (init! default-config))
  ([new-config]
    (reset! config new-config)
    (reset! latest-quotes {})))

(defn update-latest-quotes!
  [quotes]
  ; This allows for partial updates
  (doseq [quote quotes]
    (swap! latest-quotes assoc (:symbol quote) quote)))

(defn subscribe-for-quote-updates 
  [new-quotes-sub-ch]
  (go-loop []
    (let [quotes (<! new-quotes-sub-ch)]
      (when-not (nil? quotes)
        (update-latest-quotes! quotes)
        (recur)))))

(defn get-config
  []
  @config
)

(defn get-latest-quotes
  []
  (-> @latest-quotes sort vals)
)

(defn get-stocks
  []
  (sort-by :symbol (:stocks @config))
)

(defn add-stock
  [new-stock]
  (let [stock-symbol (:symbol new-stock)
        matching-stock (some #(= (:symbol %) stock-symbol) (:stocks @config))]
    (if (nil? matching-stock)
      (do
        (swap! config assoc :stocks (conj (:stocks @config) new-stock))
        true)
    false)))

(defn start 
  [new-quotes-sub-ch]
  (init!)
  (subscribe-for-quote-updates new-quotes-sub-ch))

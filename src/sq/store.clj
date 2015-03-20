(ns sq.store
  (:require
    [clojure.core.async :refer [<! >! go go-loop]]
    [clojure.tools.logging :as log]))

(def ^:private default-config { 
  :port 3000
  :refresh-interval-seconds 5 
  :stocks [
    {:symbol "RY4B.IR" :currency "EUR" } 
    {:symbol "LLOY.L" :currency "GBP" }
    {:symbol "TSCO.L" :currency "GBP" }
    {:symbol "GOOG" :currency "USD" }
    {:symbol "MSFT" :currency "USD" }]})

(def ^:private config (atom {}))
(def ^:private latest-quotes (atom {}))

(defn handle-config-update
  [config-updates-ch]
  (fn [key atom old-state new-state] 
    (log/info "Config changed old: [" old-state "] new: [" new-state "]") 
    (go (>! config-updates-ch new-state))))

(defn init-config
  [config-updates-ch]
  (add-watch config :watcher (handle-config-update config-updates-ch))
  (reset! config default-config))

(defn update-latest-quotes!
  [quotes]
  ; This allows for partial updates
  (doseq [quote quotes]
    (swap! latest-quotes assoc (:symbol quote) quote)))

(defn subscribe-for-quote-updates 
  [quotes-sub-ch]
  (go-loop []
    (let [quotes (<! quotes-sub-ch)]
      (when-not (nil? quotes)
        (update-latest-quotes! quotes)
        (recur)))))

(defn get-config
  []
  @config
)

(defn start 
  [config-updates-ch quotes-sub-ch]
  (init-config config-updates-ch)
  (subscribe-for-quote-updates quotes-sub-ch))

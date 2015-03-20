(ns sq.quoteswatcher
  (:require 
    [clojure.core.async :refer [<! >! go-loop timeout]]
    [clojure.tools.logging :as log]
    [sq.yahooquotes :as yahooquotes]))

(def config (atom {}))

(defn get-acquirer-fn 
  []
  (fn [config]
    (yahooquotes/acquire config)))

(defn process-config-updates
  [config-updates-ch]
  (go-loop []
    (if-let [updated-config (<! config-updates-ch)]
      (do
        (swap! config updated-config)
        (log/info "Config changed ------------------>" updated-config)
        (recur)))))

(defn run-acquire-quotes-loop
  [pub-ch acquirer-fn]
  (let [config @config
        stocks-count (count (:stocks config))
        pause-interval (* (:refresh-interval-seconds config) 1000)]
    (if (> stocks-count 0)
      (go-loop []
        ; Put quotes on pub channel and pause, if put on channel returns false we exit
        (when (>! pub-ch (acquirer-fn config))
          (<! (timeout pause-interval))
          (recur))))))

(defn start 
  ([in-config config-updates-ch pub-ch]
    ; Acquirer function not passed, so call function supplying the default acquirer function
    (start in-config config-updates-ch pub-ch (get-acquirer-fn)))
  ([in-config config-updates-ch pub-ch acquirer-fn]
    ; Acquirer function passed
    (reset! config in-config)
    (process-config-updates config-updates-ch)
    (run-acquire-quotes-loop pub-ch acquirer-fn)))

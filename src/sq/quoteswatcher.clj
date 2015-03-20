(ns sq.quoteswatcher
  (:require 
    [clojure.core.async :refer [<! >! go-loop timeout]]
    [clojure.tools.logging :as log]
    [sq.yahooquotes :as yahooquotes]))

(defn get-acquirer-fn 
  [config]
  (fn []
    (yahooquotes/acquire config)))

(defn start 
  ([config pub-ch]
    ; Acquirer function not passed, so call function supplying the default acquirer function
    (start config pub-ch (get-acquirer-fn config)))
  ([config pub-ch acquirer-fn]
    ; Both config and acquirer function passed
    (let [stocks-count (count (:stocks config))
          pause-interval (* (:refresh-interval-seconds config) 1000)]
      (if (> stocks-count 0)
        (go-loop []
          ; Put quotes on pub channel and pause, if put on channel returns false we exit
          (when (>! pub-ch (acquirer-fn))
            (<! (timeout pause-interval))
            (recur)))
        (log/info "No stocks to watch")))))

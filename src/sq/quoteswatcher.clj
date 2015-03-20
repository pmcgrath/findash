(ns sq.quoteswatcher
  (:require 
    [clojure.core.async :refer [<! >! go-loop timeout]]
    [clojure.tools.logging :as log]
    [sq.yahooquotes :as yahooquotes]))

(defn get-acquirer-fn 
  []
  (fn [config]
    (yahooquotes/acquire config)))

(defn run-acquire-quotes-loop
  [get-config-fn new-quotes-pub-ch acquirer-fn]
  (go-loop []
    (let [config (get-config-fn)
          stocks-count (count (:stocks config))
          pause-interval (* (:refresh-interval-seconds config) 1000)]
      (log/info "About to get and pub quotes, count is " stocks-count) 
      ; If stocks exist, get quotes and put quotes on pub channel and pause, if stocks exit and the put on channel returns false we exit
      (when (or (= stocks-count 0) 
                (>! new-quotes-pub-ch (acquirer-fn config)))
        (<! (timeout pause-interval))
        (recur)))))

(defn start 
  ([get-config-fn new-quotes-pub-ch]
    ; Acquirer function not passed, so call function supplying the default acquirer function
    (start get-config-fn new-quotes-pub-ch (get-acquirer-fn)))
  ([get-config-fn new-quotes-pub-ch acquirer-fn]
    ; Acquirer function passed
    (run-acquire-quotes-loop get-config-fn new-quotes-pub-ch acquirer-fn)))

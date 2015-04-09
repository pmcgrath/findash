(ns findash.quotes-watcher
  (:require [clojure.core.async :refer [<! >! go-loop timeout]]
            [clojure.tools.logging :as log]
            [findash.yahoo-quotes :as yahoo-quotes]))

(defn get-acquirer-fn 
  []
  (fn [config]
    (yahoo-quotes/acquire config)))

(defn run-acquire-loop
  [get-config-fn pub-ch acquirer-fn]
  (go-loop []
    (let [config (get-config-fn)
          stocks-count (count (:stocks config))
          pause-interval (* (:refresh-interval-seconds config) 1000)]
      (log/info "About to get and pub quotes, count is " stocks-count) 
      ; If stocks exist, get quotes and put quotes on pub channel, then pause
      (when (or (= stocks-count 0) 
                (>! pub-ch {:topic :new-quotes :quotes (acquirer-fn config)}))
        (<! (timeout pause-interval))
        (recur)))))

(defn start 
  ([get-config-fn pub-ch]
    ; Acquirer function not passed, so call function supplying the default acquirer function
    (start get-config-fn pub-ch (get-acquirer-fn)))
  ([get-config-fn pub-ch acquirer-fn]
    ; Acquirer function passed
    (run-acquire-loop get-config-fn pub-ch acquirer-fn)))

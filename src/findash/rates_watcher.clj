(ns findash.rates-watcher
  (:require [clojure.core.async :refer [<! >! go-loop timeout]]
            [clojure.tools.logging :as log]
            [findash.fixer-io-rates :as fixer-io-rates]))

(defn get-acquirer-fn 
  []
  (fn [config]
    (fixer-io-rates/acquire config)))

(defn create-acquire-data-fn
  [acquirer-fn]
  (fn 
    [config]
    (let [currency-pairs-count (count (:currency-pairs config))]
      (if (= currency-pairs-count 0)
        nil
        (let [data (acquirer-fn config)]
           {:topic :new-rates :rates data})))))

(defn run-acquire-loop
  [get-config-fn pub-ch acquire-data-fn]
  (go-loop []
    (let [config (get-config-fn)
          pause-interval (* (:refresh-interval-seconds config) 1000)
          data (acquire-data-fn config)]
      (when (or (nil? data) 
                (>! pub-ch data))
        (<! (timeout pause-interval))
        (recur)))))

(defn start 
  ([get-config-fn pub-ch]
    ; Acquirer function not passed, so call function supplying the default acquirer function
    (start get-config-fn pub-ch (get-acquirer-fn)))
  ([get-config-fn pub-ch acquirer-fn]
    ; Acquirer function passed
    (run-acquire-loop get-config-fn pub-ch (create-acquire-data-fn acquirer-fn))))

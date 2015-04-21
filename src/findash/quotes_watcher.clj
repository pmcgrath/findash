(ns findash.quotes-watcher
  (:require [findash.yahoo-quotes :as yahoo-quotes]
            [findash.watcher :as watcher]))

(defn get-acquirer-fn 
  []
  (fn [config]
    (yahoo-quotes/acquire config)))

(defn start 
  ([get-config-fn pub-ch]
    ; Acquirer function not passed, so call function supplying the default acquirer function - Yahoo
    (start get-config-fn pub-ch (get-acquirer-fn)))
  ([get-config-fn pub-ch acquirer-fn]
    ; Acquirer function passed
    (watcher/run-acquire-loop get-config-fn pub-ch acquirer-fn :stocks :quotes)))

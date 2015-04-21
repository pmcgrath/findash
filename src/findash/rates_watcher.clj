(ns findash.rates-watcher
  (:require [findash.fixer-io-rates :as fixer-io-rates]
            [findash.watcher :as watcher]))

(defn get-acquirer-fn 
  []
  (fn [config]
    (fixer-io-rates/acquire config)))

(defn start 
  ([get-config-fn pub-ch]
    ; Acquirer function not passed, so call function supplying the default acquirer function - fixer.io
    (start get-config-fn pub-ch (get-acquirer-fn)))
  ([get-config-fn pub-ch acquirer-fn]
    ; Acquirer function passed
    (watcher/run-acquire-loop get-config-fn pub-ch acquirer-fn :currency-pairs :rates)))

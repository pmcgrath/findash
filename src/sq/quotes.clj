(ns sq.quotes
  (:require 
    [clojure.core.async :refer [<! >! chan go-loop timeout]]
    [clojure.tools.logging :as log]
    [sq.yahooquotes :as yahooquotes]))

(defn get-acquirer-fn [config]
  (fn []
    (yahooquotes/acquire config)
  )
)

(defn watch
  ([config]
    ; Only config passed so call function with arity 2 supplying the default acquirer function
    (watch config (get-acquirer-fn config))
  )
  ([config acquirer-fn]
    ; Both config and acquirer function passed
    (let [out (chan)
          stocks-count (count (:stocks config))
          pause-interval (* (:refresh-interval-seconds config) 1000)]
      (if (> stocks-count 0)
        (go-loop []
          ; Put quotes on out channel and pause
          (>! out (acquirer-fn))
          (<! (timeout pause-interval))
          (recur)
        )
        (log/info "No stocks to watch")
      )
      out
    )
  )
)

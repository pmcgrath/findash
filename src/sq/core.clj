(ns sq.core
  (:gen-class)
  (:require
    [clojure.core.async :refer [<!!]]
    [clojure.tools.logging :as log]
    [sq.quotes :as quotes]))

(def config { 
  :refresh-interval-seconds 20 
  :stocks [
    {:symbol "RY4B.IR" :currency "EUR" } 
    {:symbol "LLOY.L" :currency "GBP" }
    {:symbol "TSCO.L" :currency "GBP" }
    {:symbol "GOOG" :currency "USD" }
    {:symbol "MSFT" :currency "USD" }
  ] 
})

(defn -main
  [& args]
  (log/info "Starting")
  (let [updates (quotes/watch config)]
    (while true 
      (let [update (<!! updates)]
        (log/info "Got update " update)
      )
    )
  )
)  

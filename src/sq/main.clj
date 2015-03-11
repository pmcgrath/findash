(ns sq.main
  (:gen-class)
  (:require
    [clojure.core.async :refer [<!!]]
    [clojure.tools.logging :as log]
    [sq.hub :as hub]
    [sq.quotes :as quotes]
    [sq.web :as web]))

(def config { 
  :port 3000
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
  (log/info "Starting app")

  (log/info "Initialising hub")
  (hub/init)

  (log/info "Starting web app")
  (web/start (:port config))
  
  (log/info "Starting quotes watcher")
  (let [quotes-out-ch (hub/get-channel :quotes-out-ch)] 
    (quotes/start config quotes-out-ch)
  )

  (log/info "Starting local quotes consumer")
  (let [quotes-out-ch (hub/get-channel :quotes-out-ch)] 
    (while true 
      (let [quotes (<!! quotes-out-ch)]
        (log/info "Got quotes [" quotes "]")
      )
    )
  )
)

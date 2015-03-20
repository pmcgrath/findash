(ns sq.main 
  (:gen-class)
  (:require
    [clojure.core.async :refer [<!! chan tap]]
    [clojure.tools.logging :as log]
    [sq.hub :as hub]
    [sq.quoteswatcher :as quoteswatcher]
    [sq.store :as store]
    [sq.web :as web]))

(def config { 
  :port 3000
  :refresh-interval-seconds 5 
  :stocks [
    {:symbol "RY4B.IR" :currency "EUR" } 
    {:symbol "LLOY.L" :currency "GBP" }
    {:symbol "TSCO.L" :currency "GBP" }
    {:symbol "GOOG" :currency "USD" }
    {:symbol "MSFT" :currency "USD" }]})

(defn -main 
  [& args]
  (log/info "Starting app")

  (log/info "Initialising hub")
  (hub/init)

  (log/info "Starting web app")
  (let [mult-quotes-ch (hub/get-item :mult-quotes-ch)] 
    (web/start (:port config) mult-quotes-ch))
 
  (log/info "Starting store")
  (let [mult-quotes-ch (hub/get-item :mult-quotes-ch) 
        quotes-sub-ch (chan)
        _ (tap mult-quotes-ch quotes-sub-ch)] 
    (store/start config quotes-sub-ch))
 
  (log/info "Starting quotes watcher")
  (let [quotes-pub-ch (hub/get-item :quotes-ch)] 
    (quoteswatcher/start config quotes-pub-ch))

  (log/info "Starting TEMP local quotes logger")
  (let [mult-quotes-ch (hub/get-item :mult-quotes-ch) 
        quotes-ch (tap mult-quotes-ch (chan))] 
    (while true 
      (let [quotes (<!! quotes-ch)]
        (log/info "!!!!TEMP Got quotes [" quotes "]")))))

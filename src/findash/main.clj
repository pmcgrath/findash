(ns findash.main
  (:require [clojure.core.async :refer [<! go]]
            [clojure.tools.logging :as log]
            [findash.hub :as hub]
            [findash.quotes-watcher :as quotes-watcher]
            [findash.rates-watcher :as rates-watcher]
            [findash.store :as store]
            [findash.web :as web])
  (:gen-class))

(defn -main 
  [& args]
  (log/info "Starting app")

  (log/info "Starting hub")
  (hub/start)

  (log/info "Starting store")
  (let [new-data-sub-ch (hub/create-new-data-subscriber :new-quotes :new-rates)] 
    (store/start new-data-sub-ch))
 
  (log/info "Starting web app")
  (let [config (store/get-config)]
    (web/start (partial hub/create-new-data-subscriber :new-quotes :new-rates)))

  (log/info "Starting quotes watcher")
  (let [get-config-fn store/get-config
        new-data-pub-ch (hub/get-item :new-data-pub-ch)] 
    (quotes-watcher/start get-config-fn new-data-pub-ch))

  (log/info "Starting rates watcher")
  (let [get-config-fn store/get-config
        new-data-pub-ch (hub/get-item :new-data-pub-ch)] 
    (rates-watcher/start get-config-fn new-data-pub-ch))
  
  (log/info "Starting TEMP local logger")
  (let [new-data-sub-ch (hub/create-new-data-subscriber :new-quotes :new-rates)] 
    (go
      (while true 
        (let [data (<! new-data-sub-ch)]
          (log/info "!!!!TEMP Got [" data "]"))))))

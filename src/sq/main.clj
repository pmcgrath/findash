(ns sq.main 
  (:gen-class)
  (:require
    [clojure.core.async :refer [<!! chan tap]]
    [clojure.tools.logging :as log]
    [sq.hub :as hub]
    [sq.quoteswatcher :as quoteswatcher]
    [sq.store :as store]
    [sq.web :as web]))

(defn -main 
  [& args]
  (log/info "Starting app")

  (log/info "Starting hub")
  (hub/start)

  (log/info "Starting store")
  (let [mult-new-quotes-ch (hub/get-item :mult-new-quotes-ch) 
        new-quotes-sub-ch (chan)
        _ (tap mult-new-quotes-ch new-quotes-sub-ch)] 
    (store/start new-quotes-sub-ch))
 
  (log/info "Starting web app")
  (let [config (store/get-config)
        mult-quotes-ch (hub/get-item :mult-new-quotes-ch)] 
    (web/start (:port config) mult-quotes-ch))
  
  (log/info "Starting quotes watcher")
  (let [get-config-fn store/get-config
        new-quotes-pub-ch (hub/get-item :new-quotes-ch)] 
    (quoteswatcher/start get-config-fn new-quotes-pub-ch))

  (log/info "Starting TEMP local quotes logger")
  (let [mult-new-quotes-ch (hub/get-item :mult-new-quotes-ch) 
        new-quotes-ch (tap mult-new-quotes-ch (chan))] 
    (while true 
      (let [quotes (<!! new-quotes-ch)]
        (log/info "!!!!TEMP Got quotes [" quotes "]")))))

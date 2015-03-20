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

  (log/info "Initialising hub")
  (hub/init)

  (log/info "Starting store")
  (let [config-updates-ch (hub/get-item :config-updates-ch) 
        mult-new-quotes-ch (hub/get-item :mult-new-quotes-ch) 
        new-quotes-sub-ch (chan)
        _ (tap mult-new-quotes-ch new-quotes-sub-ch)] 
    (store/start config-updates-ch new-quotes-sub-ch))
 
  (log/info "Starting web app")
  (let [config (store/get-config)
        mult-quotes-ch (hub/get-item :mult-new-quotes-ch)] 
    (web/start (:port config) mult-quotes-ch))
  
  (log/info "Starting quotes watcher")
  (let [config (store/get-config)
        config-updates-ch (hub/get-item :config-updates-ch) 
        new-quotes-pub-ch (hub/get-item :new-quotes-ch)] 
    (quoteswatcher/start config config-updates-ch new-quotes-pub-ch))

  (log/info "Starting TEMP local quotes logger")
  (let [mult-new-quotes-ch (hub/get-item :mult-new-quotes-ch) 
        new-quotes-ch (tap mult-new-quotes-ch (chan))] 
    (while true 
      (let [quotes (<!! new-quotes-ch)]
        (log/info "!!!!TEMP Got quotes [" quotes "]")))))

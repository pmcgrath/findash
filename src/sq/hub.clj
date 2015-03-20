(ns sq.hub
  (:require 
    [clojure.core.async :refer [chan mult]]))

(def ^:private store (atom {}))

(defn init
  []
  ; Create channels
  (swap! store assoc :config-updates-ch (chan))
  (swap! store assoc :new-quotes-ch (chan))
  
  ; Create mult channels so we can tap 
  (swap! store assoc :mult-new-quotes-ch (mult (:new-quotes-ch @store))))

(defn get-item
  [key]
  (get @store key))

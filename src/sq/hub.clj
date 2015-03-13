(ns sq.hub
  (:require 
    [clojure.core.async :refer [chan mult]]))

(def ^:private store (atom {}))

(defn init
  []
  ; Create channels
  (swap! store assoc :quotes-ch (chan))
  
  ; Create mult channels so we can tap 
  (swap! store assoc :mult-quotes-ch (mult (:quotes-ch @store))))

(defn get-item
  [key]
  (get @store key))

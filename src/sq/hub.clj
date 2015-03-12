(ns sq.hub
  (:require 
    [clojure.core.async :refer [chan]]))

(def ^:private store (atom {}))

(defn init
  []
  ; Create channels
  (swap! store assoc :quotes-out-ch (chan))
)

(defn get-channel
  [key]
  (get @store key)
)

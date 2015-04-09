(ns findash.hub
  (:require [clojure.core.async :refer [chan pub sub]]))

(def ^:private store (atom {}))

(defn get-item
  [key]
  (get @store key))

(defn create-new-data-subscriber
  [& topics]
  (let [publication (get-item :new-data-publication)
        sub-ch (chan)]
    (doseq [topic topics]
      (sub publication topic sub-ch))
    sub-ch))

(defn start
  []
  (let [new-data-pub-ch (chan)
        new-data-publication (pub new-data-pub-ch #(:topic %))]  
    (reset! store {:new-data-pub-ch new-data-pub-ch :new-data-publication new-data-publication})))

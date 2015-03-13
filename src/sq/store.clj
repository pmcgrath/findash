(ns sq.store
  (:require
    [clojure.core.async :refer [<! go-loop]]))

(def ^:private store (atom {}))

(defn update!
  [quotes]
  ; This allows for partial updates
  (doseq [quote quotes]
    (swap! store assoc (:symbol quote) quote)))

(defn get-all
  []
  (vals @store))

(defn start 
  [config in-ch]
  (go-loop []
    (let [quotes (<! in-ch)]
      (when-not (nil? quotes)
        (update! quotes)
        (recur)
      ))))

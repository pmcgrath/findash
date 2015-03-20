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
  [config quotes-sub-ch]
  (go-loop []
    (let [quotes (<! quotes-sub-ch)]
      (when-not (nil? quotes)
        (update! quotes)
        (recur)))))

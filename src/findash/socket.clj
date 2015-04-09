(ns findash.socket
  (:require [clojure.core.async :refer [<! chan close! go-loop mult tap untap]]
            [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [org.httpkit.server :as httpkit]
            [findash.json :as json]))

(def create-new-data-sub-fn (promise))

(defn create-web-socket-message
  [uuid data]
  (let [topic (:topic data)]
    (if (= topic :new-quotes)
      {:uuid uuid :messageType "quote-updates" :quotes (:quotes data)}
      (if (= topic :new-rates)
        {:uuid uuid :messageType "rate-updates" :rates (:rates data)}
        nil))))

(defn web-socket-re-publish-updates
  [uuid channel client-sub-ch]
  (go-loop []
    (if-let [data (<! client-sub-ch)]
      (do 
        (let [message (create-web-socket-message uuid data)]
          (when message
            (log/info "-----> About to send message for" uuid " type is " (:messageType message))
            (httpkit/send! channel (json/generate-string message))))
        (recur)))))

(defn web-socket-on-close
  [uuid client-sub-ch status]
  (close! client-sub-ch)
  (log/info uuid " web socket closed: " status))

(defn web-socket-on-receive
  [uuid channel data]
  (log/info uuid " web socket received: " data))

(defn handler 
  [request]
  (let [uuid (str (java.util.UUID/randomUUID))
        client-sub-ch (@create-new-data-sub-fn)]
    (log/info uuid " web socket opened")
    (httpkit/with-channel request channel
      (web-socket-re-publish-updates uuid channel client-sub-ch)
      (httpkit/on-close channel (partial web-socket-on-close uuid client-sub-ch))
      (httpkit/on-receive channel (partial web-socket-on-receive uuid channel)))))

(defn init! 
  [in-create-new-data-sub-fn]
  (deliver create-new-data-sub-fn in-create-new-data-sub-fn))

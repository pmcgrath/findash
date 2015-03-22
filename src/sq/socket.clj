(ns sq.socket
  (:require
    [clojure.core.async :refer [<! chan close! go-loop mult tap untap]]
    [clojure.tools.logging :as log]
    [compojure.core :refer :all]
    [org.httpkit.server :as httpkit]
    [sq.json :as json]))

(def mult-quotes-ch (promise))

(defn web-socket-re-publish-quote-updates
  [uuid channel quotes-sub-ch]
  (go-loop []
    (if-let [quotes (<! quotes-sub-ch)]
      (do
        (log/info "-----> About to send " uuid " got quotes " quotes)
        (httpkit/send! channel (json/generate-string (assoc {:uuid uuid "messageType" "quote-updates"} :quotes quotes)))
        (recur)))))

(defn web-socket-on-close
  [uuid quotes-sub-ch status]
  (close! quotes-sub-ch)
  (log/info uuid " web socket closed: " status))

(defn web-socket-on-receive
  [uuid channel data]
  (log/info uuid " web socket received: " data))

(defn handler 
  [request]
  (let [uuid (str (java.util.UUID/randomUUID))
        quotes-sub-ch (chan)]
    (log/info uuid " web socket opened")
    (tap @mult-quotes-ch quotes-sub-ch)
    (httpkit/with-channel request channel
      (web-socket-re-publish-quote-updates uuid channel quotes-sub-ch)
      (httpkit/on-close channel (partial web-socket-on-close uuid quotes-sub-ch))
      (httpkit/on-receive channel (partial web-socket-on-receive uuid channel)))))

(defn init! 
  [in-mult-quotes-ch]
  (deliver mult-quotes-ch in-mult-quotes-ch))

(ns sq.web
  (:require
    [clojure.core.async :refer [<! chan close! go-loop mult tap untap]]
    [clojure.tools.logging :as log]
    [cheshire.core :as cheshire]
    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler :as handler]
    [hiccup.page :as page]
    [org.httpkit.server :as httpkit]
    [sq.store :as store]))

(defn home-page-handler
  [request]
  (page/html5
    [:head
      [:title "Home"]
      (page/include-css "/css/default.css")]
    [:body
      [:div {:id "latest"}]
      (page/include-js "/js/app.js")]))

; See https://www.snip2code.com/Snippet/106966/Simple-clojure-Ring-example
(extend-protocol cheshire.generate/JSONable
  org.joda.time.DateTime
  (to-json [t jg]
    (cheshire.generate/write-string jg (str t))))

(defn web-socket-re-publish-quote-updates
  [uuid channel quotes-sub-ch]
  (go-loop []
    (if-let [stocks (<! quotes-sub-ch)]
      (do
        (log/info "-----> About to send " uuid " got stocks " stocks)
        (httpkit/send! channel (cheshire/generate-string (assoc {:uuid uuid :message-type "stock-updates"} :stocks stocks)))
        (recur)))))

(defn web-socket-on-close
  [uuid quotes-sub-ch status]
  (close! quotes-sub-ch)
  (log/info uuid " web socket closed: " status))

(defn web-socket-on-receive
  [uuid channel data]
  (log/info uuid " web socket received: " data))

(defn web-socket-handler 
  [request]
  (let [uuid (str (java.util.UUID/randomUUID))
        mult-quotes-ch (sq.hub/get-item :mult-quotes-ch) 
        quotes-sub-ch (chan)]
    (log/info uuid " web socket opened")
    (tap mult-quotes-ch quotes-sub-ch)
    (httpkit/with-channel request channel
      (web-socket-re-publish-quote-updates uuid channel quotes-sub-ch)
      (httpkit/on-close channel (partial web-socket-on-close uuid quotes-sub-ch))
      (httpkit/on-receive channel (partial web-socket-on-receive uuid channel)))))

(defroutes app-routes
  (GET "/" request home-page-handler)
  (GET "/ws" request web-socket-handler)
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
    handler/site))

(defn start 
  [port]
  (httpkit/run-server app {:port port}))

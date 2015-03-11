(ns sq.web
  (:require
    [compojure.core :as compojure]
    [compojure.route :as route]
    [ring.adapter.jetty :as ring]
    [sq.views :as views]))

(compojure/defroutes routes
  (compojure/GET "/" [] (views/home-page))
  (route/resources "/")				; How does this work ?
  (route/not-found "Not Found")
)

(defn start
  [port]
  ; Why do I need the ":join? false" entry ? If not it blocks and makes no progress
  (ring/run-jetty #'routes {:port port :join? false})
)

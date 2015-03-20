(ns sq.web
  (:require
    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler :as handler]
    [hiccup.page :as page]
    [org.httpkit.server :as httpkit]
    [sq.socket :as socket]))

(defn home-page-handler
  [request]
  (page/html5
    [:head
      [:title "Home"]
      (page/include-css "/css/default.css")]
    [:body
      [:div {:id "latest"}]
      (page/include-js "/js/app.js")]))

(defroutes app-routes
  (GET "/" request home-page-handler)
  (GET "/ws" request socket/handler)
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
    handler/site))

(defn start 
  [port mult-latest-quotes-ch]
  (socket/init! mult-latest-quotes-ch)
  (httpkit/run-server app {:port port}))

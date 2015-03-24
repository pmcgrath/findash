(ns sq.web
  (:require
    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler :as handler]
    [hiccup.page :as page]
    [org.httpkit.server :as httpkit]
    [sq.json :as json]
    [sq.store :as store]
    [sq.socket :as socket]))

(defn get-home-page-handler
  [request]
  (page/html5
    [:head
      [:title "Home"]
      (page/include-css "/css/default.css")
      (page/include-js "/js/react-0.13.1.min.js")
      (page/include-js "/js/JSXTransformer-0.13.1.js")]
    [:body
      [:div {:id "content"}]
      [:script {:type "text/jsx;harmony=true", :src "/js/app.js"}]]))

(defn get-quotes-service-handler
  [request]
  {:status 200 :headers {"Content-Type" "application/json"} :body (json/generate-string (store/get-latest-quotes))})

(defn get-stocks-service-handler
  [request]
  {:status 200 :headers {"Content-Type" "application/json"} :body (json/generate-string (store/get-stocks))})

(defn add-stock-service-handler
  [request]
  (let [body (:body request)
        stock (json/parse-body body)]
    (println "***** Add stock!!!!!!!!!!!" stock)
    {:status 200 :headers {"Content-Type" "application/json"} :body (json/generate-string {})}))

(defroutes app-routes
  (GET "/" request get-home-page-handler)
  (context "/api" [] 
    (GET "/quotes" request get-quotes-service-handler)
    (GET "/stocks" request get-stocks-service-handler)
    (POST "/stocks" request add-stock-service-handler))
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

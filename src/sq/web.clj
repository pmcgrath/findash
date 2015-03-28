(ns sq.web
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [hiccup.page :as page]
            [org.httpkit.server :as httpkit]
            [sq.json :as json]
            [sq.socket :as socket]
            [sq.store :as store]))

(defn get-home-page-handler
  [request]
  (page/html5
    [:head
      [:title "Home"]
      (page/include-css "/css/default.css")
      (page/include-js "/js/react-0.13.1.min.js")
      (page/include-js "/js/JSXTransformer-0.13.1.js")]
    [:body
      [:div {:class "content" :id "content"}]
      [:script {:type "text/jsx", :src "/js/app.js"}]]))

(defn get-currencies-service-handler
  [request]
  {:status 200 :headers {"Content-Type" "application/json"} :body (json/generate-string (store/get-currency-iso-alpha-codes))})

(defn get-quotes-service-handler
  [request]
  {:status 200 :headers {"Content-Type" "application/json"} :body (json/generate-string (store/get-latest-quotes))})

(defn get-stocks-service-handler
  [request]
  {:status 200 :headers {"Content-Type" "application/json"} :body (json/generate-string (store/get-stocks))})

(defn add-stock-service-handler
  [request]
  (let [body (:body request)
        stock (json/parse-body body)
        symbol (:symbol stock)
        added (store/add-stock stock)]
    (if added
      {:status 200 :headers {"Content-Type" "application/json"} :body (json/generate-string {:symbol symbol})}
      {:status 409 :headers {"Content-Type" "application/json"} :body (json/generate-string {:symbol symbol :error "Conflict, stock appears to already exist"})})))

(defroutes app-routes
  (GET "/" request get-home-page-handler)
  (context "/api" [] 
    (GET "/currencies" request get-currencies-service-handler)
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

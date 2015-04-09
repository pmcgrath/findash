(ns findash.web
  (:require [clojure.walk :as walk]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [hiccup.page :as page]
            [org.httpkit.server :as httpkit]
            [findash.json :as json]
            [findash.socket :as socket]
            [findash.store :as store]))

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

(defn get-currency-pairs-service-handler
  [request]
  {:status 200 :headers {"Content-Type" "application/json"} :body (json/generate-string (store/get-currency-pairs))})

(defn add-currency-pair-service-handler
  [request]
  (let [body (:body request)
        currency-pair (json/parse-body body)
        currency-pair (walk/keywordize-keys currency-pair)
        added (store/add-currency-pair currency-pair)]
    (if added
      {:status 200 :headers {"Content-Type" "application/json"} :body (json/generate-string currency-pair)}
      {:status 409 :headers {"Content-Type" "application/json"} :body (json/generate-string (assoc currency-pair :error "Conflict, stock appears to already exist"))})))

(defn get-quotes-service-handler
  [request]
  {:status 200 :headers {"Content-Type" "application/json"} :body (json/generate-string (store/get-latest-quotes))})

(defn get-rates-service-handler
  [request]
  {:status 200 :headers {"Content-Type" "application/json"} :body (json/generate-string (store/get-latest-rates))})

(defn get-stocks-service-handler
  [request]
  {:status 200 :headers {"Content-Type" "application/json"} :body (json/generate-string (store/get-stocks))})

(defn add-stock-service-handler
  [request]
  (let [body (:body request)
        stock (json/parse-body body)
        stock (walk/keywordize-keys stock)
        added (store/add-stock stock)]
    (if added
      {:status 200 :headers {"Content-Type" "application/json"} :body (json/generate-string stock)}
      {:status 409 :headers {"Content-Type" "application/json"} :body (json/generate-string (assoc stock :error "Conflict, stock appears to already exist"))})))

(defroutes app-routes
  (GET "/" request get-home-page-handler)
  (context "/api" [] 
    (GET "/currencies" request get-currencies-service-handler)
    (GET "/currencypairs" request get-currency-pairs-service-handler)
    (POST "/currencypairs" request add-currency-pair-service-handler)
    (GET "/quotes" request get-quotes-service-handler)
    (GET "/rates" request get-rates-service-handler)
    (GET "/stocks" request get-stocks-service-handler)
    (POST "/stocks" request add-stock-service-handler))
  (GET "/ws" request socket/handler)
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
    handler/site))

(defn start 
  [port create-new-data-sub-fn]
  (socket/init! create-new-data-sub-fn)
  (httpkit/run-server app))

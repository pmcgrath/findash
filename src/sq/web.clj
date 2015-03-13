(ns sq.web
  (:require
    [compojure.core :as compojure]
    [compojure.route :as route]
    [ring.adapter.jetty :as ring]
    [sq.store :as store]
    [sq.views :as views]))

(compojure/defroutes routes
  (compojure/GET "/" req (views/generate-home-page req (store/get-all)))
  (compojure/GET "/latest" req (views/generate-latest req (store/get-all)))
  (route/resources "/")				; How does this work ?
  (route/not-found "Not Found"))

(defn start 
  [port]
  ; Why do I need the ":join? false" entry ? If not it blocks and makes no progress
  (ring/run-jetty #'routes {:port port :join? false}))




;(ns api-test.handler
;  (:require [compojure.core :refer :all]
;            [compojure.handler :as handler]
;            [ring.middleware.json :as middleware]
;            [compojure.route :as route]))
;
;(defroutes app-routes
;  (POST "/" request
;        (let [name (or (get-in request [:params :name])
;                       (get-in request [:body :name])
;                       "John Doe")]
;          {:status 200
;           :body {:name name
;                  :desc (str "The name you sent to me was " name)}}))
;  (route/resources "/")
;  (route/not-found "Not Found"))
;
;(def app
;  (-> (handler/site app-routes)
;      (middleware/wrap-json-body {:keywords? true})
;      middleware/wrap-json-response))


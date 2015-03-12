(ns sq.views
  (:require 
    [hiccup.page :as page]))

(defn generate-page-header
  [title]
  [:head
    [:title title]
    (page/include-css "/css/default.css")
  ]
)

(defn generate-home-page
  [req]
  (page/html5
    (generate-page-header "Home 1")
    [:h1 "Stock quotes"]
    [:d (str "Request is " req)]
  )
)

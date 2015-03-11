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

(defn home-page
  []
  (page/html5
    (generate-page-header "Home")
    [:p "Stock quotes"]
  )
)

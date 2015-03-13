(ns sq.views
  (:require 
    [clj-time.core :as time]
    [hiccup.page :as page]))

(defn generate-page-header
  [title]
  [:head
    [:title title]
    (page/include-css "/css/default.css")])

(defn generate-page-quote-data
  [quote]
  [:li
    [:d (:symbol quote)]
    [:d (:price quote)]])

(defn generate-home-page
  [req quotes]
  (page/html5
    (generate-page-header "Home 1")
    [:h1 (str "Stock quotes @ " (time/now))]
    [:d (str "Request is " req)]
    [:d (str "quotes is " quotes)]
    [:ul (for [quote quotes] (generate-page-quote-data quote))]))

(defn generate-latest
  [req quotes]
  {
    :status 200
    :body {
      :timestamp (time/now)
      :quotes quotes}})

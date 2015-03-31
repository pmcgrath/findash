(ns findash.json
  (:require [cheshire.core :as cheshire]))

; See https://www.snip2code.com/Snippet/106966/Simple-clojure-Ring-example
(extend-protocol cheshire.generate/JSONable
  org.joda.time.DateTime
  (to-json [t jg]
    (cheshire.generate/write-string jg (str t))))

(defn generate-string
  [data]
  (cheshire/generate-string data {:pretty true}))

(defn parse-body
  [body]
    (cheshire/parse-string (slurp body) true))

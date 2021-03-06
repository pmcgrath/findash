(defproject findash "0.1.0-SNAPSHOT"
  :description "Financial dashboard"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.3.3"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.1"]  
                 [cheshire "5.4.0"]
                 [clj-time "0.8.0"]  
                 [compojure "1.3.1"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.16"]
                 [ring/ring-defaults "0.1.2"]]
  :main ^:skip-aot findash.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

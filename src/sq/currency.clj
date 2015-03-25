(ns sq.currency
  (:require
    [clojure.data.zip.xml :as zipxml]
    [clojure.xml :as xml]
    [clojure.zip :as zip]))

(def url "http://www.currency-iso.org/dam/downloads/table_a1.xml")

(defn get-currency-iso-alpha-codes
  []
  ; See http://stackoverflow.com/questions/1194044/clojure-xml-parsing
  ; See commit f95150520d95f1e82353bf8bce0cb59bd6784593 to see parsing the xml without clojure.data.zip.xml - git show 92589d043129466768089469dd0e19753d5caabc
  (let [data (xml/parse url)
        zipped (zip/xml-zip data)
        currency-codes  (zipxml/xml-> zipped :CcyTbl :CcyNtry :Ccy zipxml/text)]
    (-> currency-codes distinct sort)))

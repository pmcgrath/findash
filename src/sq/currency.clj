(ns sq.currency
  (:require
    [clojure.data.zip.xml :as zipxml]
    [clojure.xml :as xml]
    [clojure.zip :as zip]))

(def url "http://www.currency-iso.org/dam/downloads/table_a1.xml")

(defn get-currency-iso-alpha-codes
  []
  ; See http://stackoverflow.com/questions/1194044/clojure-xml-parsing
  (let [data (xml/parse url)
        zipped (zip/xml-zip data)
        currency-codes  (zipxml/xml-> zipped :CcyTbl :CcyNtry :Ccy zipxml/text)]
    (-> currency-codes distinct sort)))

(defn extract-countries-from-root
  [root]
  ; root xml is "<ISO_4217 Pblshd="2015-01-01"><CcyTbl><CcyNtry>...."
  ; We want to a get a collection for the CcyTbl element
  ; So we need to 
  ;   get :content from root (map) which gives us a vector
  ;   get the first item from the vector which gives us a map
  ;   get :content from the map which gives us a vector (Each entry which is a map corresponds to the CcyNtry source element) 
  (-> root :content first :content))

(defn create-a-country-data-map
  [country]
  ; country xml is "<CcyNtry><CtryNm>AFGHANISTAN</CtryNm><CcyNm>Afghani</CcyNm><Ccy>AFN</Ccy><CcyNbr>971</CcyNbr><CcyMnrUnts>2</CcyMnrUnts></CcyNtry>"
  ; We want to a get a map for the CcyNtry element content
  ; So we need to
  ;   get :content from incomming map which gives us a vector
  ;   reduce the vector to a map where each vector entry results in a key based on :tag and a value based on the first element in the :content vector
  (let [country-data-items (:content country)
        reduction-fn (fn [accum country-data-item] (assoc accum (:tag country-data-item) (-> country-data-item :content first)))]
    (reduce reduction-fn {} country-data-items)))

(defn get-and-parse-countries
  []
  (let [data (xml/parse url)
        countries (extract-countries-from-root data)]
    (map create-a-country-data-map countries)))

(defn get-currency-iso-alpha-codes-long-way-round
  []
  (let [countries (get-and-parse-countries)
        currency-codes (map #(:Ccy %) countries)]
    ; Distinct and all non nil currencies (Antartica has no currency)
    (sort (remove nil? (distinct currency-codes)))))

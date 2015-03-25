(ns sq.currency
  (:require
    [clojure.xml :as xml]))

(def ^:private url "http://www.currency-iso.org/dam/downloads/table_a1.xml")

(defn- acquire-countries
  []
  ; See comment below
  (let [xml (xml/parse url)
        countries (->> xml :content first :content (map (fn [country] (:content country))))
        create-country-map-reducer-fn (fn [accum, country-attrib] (assoc accum (:tag country-attrib) (-> country-attrib :content first)))
        create-country-map-fn (fn [country] (reduce create-country-map-reducer-fn {} country))]
    (map create-country-map-fn countries)))

(def ^:private countries (future (acquire-countries)))

(defn get-currency-iso-alpha-codes
  []
  ; Using a future as this data should only be acquired once, rarely changes
  ; Sorted distinct and all non nil currencies (Antartica has no currency)
  (->> @countries (map :Ccy) (remove nil?) distinct sort))

(comment
; http://blog.korny.info/2014/03/08/xml-for-fun-and-profit.html
; See http://stackoverflow.com/questions/1194044/clojure-xml-parsing
; **** Used the following in the REPL to investigate this stuff
(require '[clojure.xml :as xml])

(defn parse [xml-string]
   (xml/parse
     (java.io.ByteArrayInputStream. (.getBytes xml-string))))

(def raw-xml "<ISO_4217 Pblshd=\"2015-01-01\">
  <CcyTbl>
    <CcyNtry>
      <CtryNm>AFGHANISTAN</CtryNm><CcyNm>Afghani</CcyNm><Ccy>AFN</Ccy><CcyNbr>971</CcyNbr><CcyMnrUnts>2</CcyMnrUnts>
    </CcyNtry>
    <CcyNtry>
      <CtryNm>ÅLAND ISLANDS</CtryNm><CcyNm>Euro</CcyNm><Ccy>EUR</Ccy><CcyNbr>978</CcyNbr><CcyMnrUnts>2</CcyMnrUnts>
    </CcyNtry>
    <CcyNtry>
      <CtryNm>BBBB</CtryNm><CcyNm>Euro</CcyNm><Ccy>EUR</Ccy><CcyNbr>978</CcyNbr><CcyMnrUnts>2</CcyMnrUnts>
    </CcyNtry>
  </CcyTbl>
</ISO_4217>")

(def xml (parse raw-xml))
(pprint xml)

(def countries (->> xml :content first :content (map (fn [country] (:content country)))))
(pprint countries)

(defn create-country-map
  [country]
  (reduce (fn [accum, country-attrib] (assoc accum (:tag country-attrib) (-> country-attrib :content first))) {} country))
(pprint (map create-country-map countries))
)

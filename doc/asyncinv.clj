; go-loop terminate using an external atom
(require '[clojure.core.async :refer [<!! <! go-loop]])
(require '[clj-time.core :as time])

(def stop (atom false))

(defn run-go
  []
  (go-loop [sequence 1]
    (println "Sequence" sequence)
    (if @stop
      (println "Stopping")
      (do
        (<! (timeout 200))
        (recur (inc sequence))))))

(run-go)
(<!! (timeout 2000))
(swap! stop (fn [curr] true))





; Synchronised producer and consumers (Messages are distributed) using an unbuffered channel where we close the channel to stop all
(require '[clojure.core.async :refer [<!! <! >! chan close! go-loop timeout]])
(require '[clj-time.core :as time])

(def message-ch (chan))

(defn run-producer
  [ch]
  (go-loop [sequence 1]
    (println "Producer is about to try to put message with sequence" sequence)
    (let [message (str "Produced " sequence " @ " (time/now))
          written (>! ch message)]
      (if-not written
        (println "Producer is exiting go loop as channel is closed")
        (do
          (<! (timeout 200))
          (recur (inc sequence)))))))

(defn run-consumer
  [name ch]
  (go-loop []
    (let [message (<! ch)]
      (if (nil? message)
        (println name "is exiting go loop as channel is closed")
        (do
          (println name "received message [" message "]")
          (recur))))))

(run-consumer "C1" message-ch)
(run-consumer "C2" message-ch)
(run-producer message-ch)

(<!! (timeout 2000))
(close! message-ch)





; Synchronised producer and subscribers using an unbuffered channel where we close the channel to stop all 
(require '[clojure.core.async :refer [<!! <! >! chan close! go-loop mult tap timeout]])
(require '[clj-time.core :as time])

(def message-ch (chan))
(def mult-message-ch (mult message-ch))

(defn run-producer
  [ch]
  (go-loop [sequence 1]
    (println "Producer is about to try to put message with sequence" sequence)
    (let [message (str "Produced " sequence " @ " (time/now))
          written (>! ch message)]
      (if-not written
        (println "Producer is exiting go loop as channel is closed")
        (do
          (<! (timeout 200))
          (recur (inc sequence)))))))

(defn run-consumer
  [name ch]
  (go-loop []
    (let [message (<! ch)]
      (if (nil? message)
        (println name "is exiting go loop as channel is closed")
        (do
          (println name "received message [" message "]")
          (recur))))))

(def c1-ch (chan))
(def c2-ch (chan))

(run-consumer "C1" (tap mult-message-ch c1-ch))
(run-consumer "C1" (tap mult-message-ch c2-ch))
(run-producer message-ch)

(<!! (timeout 2000))
(close! message-ch)



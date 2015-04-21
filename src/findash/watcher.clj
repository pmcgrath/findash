(ns findash.watcher
  (:require [clojure.core.async :refer [<! >! go-loop timeout]]
            [clojure.tools.logging :as log]))

(defn create-acquire-data-fn
  [acquirer-fn config-key publication-key]
  (fn 
    [config]
    (let [count (count (config-key config))
          topic (keyword (str "new-" (name publication-key)))]
      (log/info "About to get and pub for" publication-key "count is" count) 
      (if (= count 0)
        nil
        (try
          (let [data (acquirer-fn config)]
            {:topic topic publication-key data})
          (catch Exception exc
            (log/error exc)
            nil))))))

(defn run-acquire-loop
  [get-config-fn pub-ch acquirer-fn config-key publication-key]
  (let [acquire-data-fn (create-acquire-data-fn acquirer-fn config-key publication-key)]
    (go-loop []
      (let [config (get-config-fn)
            pause-interval (* (:refresh-interval-seconds config) 1000)
            data (acquire-data-fn config)]
        (when (or (nil? data) 
                  (>! pub-ch data))
          (<! (timeout pause-interval))
          (recur))))))

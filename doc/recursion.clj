; Sum
(loop [numbers [1 2 3 4]
       result 0] 
  (if (empty? numbers) 
    (do 
      (println "done exiting with result") 
      result) 
    (do 
      (println "working for " (first numbers))
      (recur (rest numbers) 
             (+ result (first numbers))))))





; Multiplication
(loop [numbers [1 2 3 4]
       result 1] 
  (if (empty? numbers) 
    (do 
      (println "done exiting with result") 
      result) 
    (do 
      (println "working for " (first numbers))
      (recur (rest numbers) 
             (* result (first numbers))))))





; Generalisation
(defn my-op
  [op numbers]
  (if (= 0 (count numbers))
    0
    (do
      (loop [remaining-numbers (rest numbers)
             result (first numbers)] 
        (if (empty? remaining-numbers) 
          (do 
            (println "done exiting with result") 
            result) 
          (do 
            (println "working for " (first remaining-numbers) " prev result is " result)
            (recur (rest remaining-numbers) 
                    (op result (first remaining-numbers)))))))))

; Addition
(my-op + [1])        ; 1
(my-op + [1 11 23])  ; 35
(my-op + [1 2 3 4])  ; 10

; Multiplication
(my-op * [1])        ; 1
(my-op * [1 11 23])  ; 253
(my-op * [1 2 3 4])  ; 24 


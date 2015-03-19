(require '[clojure.walk :refer :all])





; Thread first - Passes as first item to each form
(def c 5)
(-> c (+ 3) (/ 2) (- 1))                          
(macroexpand-all '(-> c (+ 3) (/ 2) (- 1)))
; Results in (- (/ (+ c 3) 2) 1)





; Thread last - Passes as last item to each form
(def c 5)
(->> c (+ 3) (/ 2) (- 1))                          
(macroexpand-all '(->> c (+ 3) (/ 2) (- 1)))
; Results in (- 1 (/ 2 (+ 3 c)))

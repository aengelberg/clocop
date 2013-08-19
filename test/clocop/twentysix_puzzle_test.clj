(ns clocop.twentysix-puzzle-test
  (:use clojure.test
        clocop.core
        clocop.constraints
        clocop.solver))

; In the 26 puzzle, there are 12 different numbers (from 1-12) arranged in a 6-point-star fashion, like so:
;    A
; B C D E
;  F   G
; H I J K
;    L

; There are seven different sets of numbers that have to add up to 26. These include:
; - The set of six corners
; - The six "sides", e.g. B C D E, or H F C A

; I represent A by "o-n", because it's on the outside ("o") and it's to the north ("n")
; Similarly, J is represented by "i-se".

(defn twentysix-puzzle
  []
  (with-store (store)
    (let [o-n (int-var "o-n" 1 12)
          o-nw (int-var "o-nw" 1 12)
          o-sw (int-var "o-sw" 1 12)
          o-s (int-var "o-s" 1 12)
          o-se (int-var "o-se" 1 12)
          o-ne (int-var "o-ne" 1 12)
          
          i-nw (int-var "i-nw" 1 12)
          i-w (int-var "i-w" 1 12)
          i-sw (int-var "i-sw" 1 12)
          i-se (int-var "i-se" 1 12)
          i-e (int-var "i-e" 1 12)
          i-ne (int-var "i-ne" 1 12)]
      (constrain! ($all-different o-n o-nw o-sw o-s o-se o-ne
                                  i-nw i-w i-sw i-se i-e i-ne))
      
      (constrain! ($= ($+ o-n o-nw o-sw o-s o-se o-ne) 26))
      
      (constrain! ($= ($+ o-nw i-nw i-ne o-ne) 26))
      (constrain! ($= ($+ o-sw i-w i-nw o-n) 26))
      (constrain! ($= ($+ o-s i-sw i-w o-nw) 26))
      (constrain! ($= ($+ o-se i-se i-sw o-sw) 26))
      (constrain! ($= ($+ o-ne i-e i-se o-s) 26))
      (constrain! ($= ($+ o-n i-ne i-e o-se) 26))
      
      (let [solved (solve!)]
        (when solved
          (println "  " (solved "o-n"))
          (println (solved "o-nw") (solved "i-nw") (solved "i-ne") (solved "o-ne"))
          (println "" (solved "i-w") " " (solved "i-e"))
          (println (solved "o-sw") (solved "i-sw") (solved "i-se") (solved "o-se"))
          (println "  " (solved "o-s")))))))
; =>
;    1
; 2 12 9 3
;  6   11
; 7 10 4 5
;    8

(deftest twentysix-puzzle-test
  (is (or (= "   1\r\n2 12 9 3\r\n 6   11\r\n7 10 4 5\r\n   8\r\n" (with-out-str (twentysix-puzzle)))
          (= "   1\n2 12 9 3\n 6   11\n7 10 4 5\n   8\n" (with-out-str (twentysix-puzzle))))))
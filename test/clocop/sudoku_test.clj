(ns clocop.sudoku-test
  "A sudoku solver using CloCoP."
  (:use clojure.test)
  (:use clocop.core)
  (:require [clocop.constraints :as c])
  (:require [clocop.solver :as s]))

(defn solve-sudoku
  "Takes and returns a sequence of sequences of characters.
Blank spaces can be marked with any non-numeric, non-space character.
Spaces will be removed from each line."
  [board]
  (with-store (store)
    (let [board (vec (map vec (map (partial filter #(not= % \space)) board)))
          vars (vec (for [i (range 9)]
                      (vec (for [j (range 9)]
                             (let [hint (try (Integer/parseInt (str (get-in board [i j])))
                                          (catch Exception e nil))]
                               (if hint
                                 (int-var (str "cell" i "_" j) hint hint)
                                 (int-var (str "cell" i "_" j) 1 9)))))))
          rows vars
          cols (apply map vector vars)
          areas (for [a (range 3)
                      b (range 3)]
                  (for [i (range 3)
                        j (range 3)]
                    (get-in vars [(+ (* a 3) i)
                                  (+ (* b 3) j)])))
          _ (doseq [group (concat rows cols areas)]
              (constrain! (c/all-different% group)))
          solved (solve!)]
      (vec (for [i (range 9)]
             (apply str (for [j (range 9)]
                          (get solved (str "cell" i "_" j)))))))))

(defn solve-sudoku-pretty
  [board]
  (doseq [line (solve-sudoku board)]
    (println line)))

(def board1
  "One of the hardest sudoku puzzles of all time"
  ["8** *** ***"
   "**3 6** ***"
   "*7* *9* 2**"
   
   "*5* **7 ***"
   "*** *45 7**"
   "*** 1** *3*"
   
   "**1 *** *68"
   "**8 5** *1*"
   "*9* *** 4**"])

(def board1answer
  ["812753649"
   "943682175"
   "675491283"
   "154237896"
   "369845721"
   "287169534"
   "521974368"
   "438526917"
   "796318452"])

(deftest board1test
  (is (= (solve-sudoku board1)
         board1answer)))
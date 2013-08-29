(ns clocop.sudoku-test
  "A sudoku solver using CloCoP."
  (:use clojure.test)
  (:use clocop.core
        clocop.constraints))

(defn solve-sudoku
  "Takes and returns a vector of vectors of numbers (with 0 as a blank space)."
  [board]
  (with-store (store)
    (let [var-grid (vec (map vec (for [i (range 9)]
                                   (for [j (range 9)]
                                     (let [n (get-in board [i j])]
                                       (int-var (str i j) (if (not= n 0)
                                                            (domain [n n])
                                                            (domain [1 9]))))))))
          rows var-grid
          cols (apply map vector var-grid)
          squares (for [a (range 3)
                        b (range 3)]
                    (for [i (range (* a 3) (* (inc a) 3))
                          j (range (* b 3) (* (inc b) 3))]
                      (get-in var-grid [i j])))]
      
      (doseq [cell-group (concat rows cols squares)]
        (constrain! (apply $all-different cell-group)))
      ; in each row, column, and 3x3 square, all of the numbers must be different.
      
      (let [solved (solve!)]
        (when solved
          (vec (map vec (for [i (range 9)]
                          (for [j (range 9)]
                            (solved (str i j)))))))))))

(defn solve-sudoku-pretty
  [board]
  (doseq [line (solve-sudoku board)]
    (println line)))

(def board1
  "One of the hardest sudoku puzzles of all time"
  [[8 0 0  0 0 0  0 0 0]
   [0 0 3  6 0 0  0 0 0]
   [0 7 0  0 9 0  2 0 0]
   
   [0 5 0  0 0 7  0 0 0]
   [0 0 0  0 4 5  7 0 0]
   [0 0 0  1 0 0  0 3 0]
   
   [0 0 1  0 0 0  0 6 8]
   [0 0 8  5 0 0  0 1 0]
   [0 9 0  0 0 0  4 0 0]])

(def board1answer
  [[8 1 2  7 5 3  6 4 9]
   [9 4 3  6 8 2  1 7 5]
   [6 7 5  4 9 1  2 8 3]
   
   [1 5 4  2 3 7  8 9 6]
   [3 6 9  8 4 5  7 2 1]
   [2 8 7  1 6 9  5 3 4]
   
   [5 2 1  9 7 4  3 6 8]
   [4 3 8  5 2 6  9 1 7]
   [7 9 6  3 1 8  4 5 2]])

(deftest board1test
  (is (= (solve-sudoku board1)
         board1answer)))
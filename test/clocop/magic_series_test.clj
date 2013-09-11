(ns clocop.magic-series-test
  (:use clojure.test
        clocop.core
        clocop.constraints))

; A "magic series" of length N is an array L, such that for all i in [0..N), L[i] = the number of occurrences of i in L.

; Here's an example of a magic series of length 5:

; [2 1 2 0 0]

(defn magic-series
  [N]
  (with-store (store)
    (let [L (vec (for [i (range N)]
                   (int-var (str i) 0 N)))] ; initialize L to be a vector of vars
      (doseq [i (range N)]
        (constrain! ($= ($occurrences L i)
                        (nth L i)))) ; L[i] = # of times i occurs in L
      
      ; This is a redundant constraint, i.e. a constraint that doesn't change the feasibility of the problem
      ; but makes the solving faster: summation(i=0..N | i * L[i]) = N. (Think about it!)
      (constrain! ($= ($weighted-sum L (range N)) N))
      
      (let [solved (solve!)]
        (map solved (map str (range 0 N)))))))

(deftest magic-series-correct?
  (doseq [N '(5 10 20 30)
          :let [series (magic-series N)]]
    (is (every? identity
                (for [i (range N)]
                  (= (nth series i) (count (filter #(= % i) series))))))))
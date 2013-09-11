(ns clocop.all-interval-series-test
  (:use clojure.test
        clocop.core
        clocop.constraints))

; An "all interval series" of length N is a permutation of array [0 1 .... N-1], such that differences between adjacent
; elements are all different

; Here's an example of an all interval series of length 5:

; [1  0  4  2  3]
;    1 -4  2 -1  (differences)

(defn all-interval-series
  [N]
  (with-store (store)
    (let [L (vec (for [i (range N)]
                   (int-var (str i) 0 N)))              ; variables for elements
          D (vec (for [i (range (dec N))]
                   (int-var (str "d" i) (- N) N)))]     ; variables for differences
      (doseq [i (range (dec N))]
        (constrain! ($= (D i) ($- (L i) (L (inc i)))))) ; difference relation
      (constrain! (apply $all-different L))             ; L is a permutation of [0 1 ... N -1]
      (constrain! (apply $all-different D))             ; all intervals are different
      ; These are symmetry breaking constraints. They reduce search space by removing feasible solutions, that can be
      ; obtained by trivial transformation of other feasible solutions:
      ; - breaks symmetry against transformation: (L_1, L_2, ... L_n-1) -> (N - L_1, N - L_2, ... N - L_n-1)
      (constrain! ($> (L 0) (L 1)))
      ; - breaks symmetry against transformation: (L_1, L_2, ... L_n-1) -> (L_n-1, ... L_2, L_1)
      (constrain! ($> (D 0) (D (- N 2))))

      (let [solved (solve!)]
        (map solved (map str (range 0 N)))))))

(deftest all-interval-series-correct?
  (doseq [N '(5 10 20 30)
          :let [series (all-interval-series N)]]
    (is (every? #(= 1 %)
          (vals (frequencies
            (for [i (range (dec N))]
              (- (nth series i) (nth series (inc i))))))))))

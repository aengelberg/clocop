(ns clocop.minimize-test
  (:use clocop.core
        clocop.constraints
        clojure.test))

(deftest minimize-test1
  (-> (with-store (store)
        (let [x (int-var "x" 1 5)]
          (get (solve! :minimize x)
               "x")))
    (= 1)
    is))

(deftest minimize-test2
  (-> (with-store (store)
        (let [x (int-var "x" 1 5)
              y (int-var "y" 1 5)]
          (constrain! ($= x ($+ y 1)))
          ((solve! :minimize x) "x")))
    (= 2)
    is))

(deftest minimize-test3
  (-> (with-store (store)
        (let [x (int-var "x" 1 5)
              y (int-var "y" 1 5)]
          (constrain! ($!= x y)) ; still should work
          ((solve! :minimize x) "x")))
    (= 1)
    is))

(deftest maximize-test
  (-> (with-store (store)
        (let [x (int-var "x" 1 5)]
          ((solve! :minimize ($- x)) "x")))
    (= 5)
    is))
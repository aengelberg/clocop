(ns clocop.binpack-test
  (:use clocop.core
        clocop.constraints
        clojure.test))

(deftest binpacktest1
  (with-store (store)
    (let [x (int-var "xloc" 0 2) ; which bin, from 0 to 2, is x in
          y (int-var "yloc" 0 2)
          z (int-var "zloc" 0 2)
          bins [1 2 3] ; the constant bin capacities
          xweight (int-var 1) ; x's weight
          yweight (int-var 3)
          zweight (int-var 2)]
      (constrain! ($binpacking :bin-sizes bins
                               :item-sizes [xweight yweight zweight]
                               :item-locations [x y z]))
      (is (= (solve!)
             {"xloc" 0
              "yloc" 2
              "zloc" 1})))))
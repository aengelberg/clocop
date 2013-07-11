CloCoP
======
####(in development)

A Clojure wrapper for JaCoP, a Java constraint programming engine. JaCoP stands for <b>JA</b>va <b>CO</b>nstraint <b>P</b>rogramming.

##Usage

There is no easily addable Lein dependency yet.

##Sample code

If you're curious, here's some sample code based on the API I have so far.

    (use 'clocop.core
         'clocop.constraints)
    
    ; Variable X is between 1 and 2. Variable Y is between 3 and 4.
    ; We know that X + Y = 6. What are X and Y?
    (with-store (store)           ; initialize the variable store
      (let [x (int-var "x" 1 2)
            y (int-var "y" 3 4)]  ; initialize the variables
        (constrain! (+% x y 6))   ; specify x + y = 6
        (solve!)))                ; searches for a solution

    => {"x" 2, "y" 4}

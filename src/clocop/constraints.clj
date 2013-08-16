(ns clocop.constraints
  "A namespace for the various constraints you can use."
  (:require [clocop.core :as core])
  (:import
    (JaCoP.core Var IntVar IntervalDomain)
    (JaCoP.constraints PrimitiveConstraint
                       
                       XeqY
                       XeqC
                       XltY
                       XltC
                       XlteqY
                       XlteqC
                       XgtY
                       XgtC
                       XgteqY
                       XgteqC
                       XneqY
                       XneqC
                       
                       XplusCeqZ
                       XplusClteqZ
                       XplusYeqC
                       XplusYeqZ
                       XplusYgtC
                       XplusYlteqZ
                       XplusYplusCeqZ
                       XplusYplusQeqZ
                       XplusYplusQgtC
                       Sum
                       SumWeight
                       
                       AbsXeqY
                       
                       XmulYeqZ
                       XmulCeqZ
                       XmodYeqZ
                       XdivYeqZ
                       XexpYeqZ
                       
                       Min
                       Max
                       
                       And
                       Or
                       Not
                       IfThen
                       IfThenElse
                       Eq
                       
                       Alldifferent
                       Element
                       Count
                       Among
                       Reified
                       )))

; Sample code: (+% x y := 3)

(defn- cartesian-product
  [lists]
  (cond
    (empty? lists) '(())
    :else (let [next-cart (cartesian-product (rest lists))]
            (apply concat (for [i (first lists)]
                            (map (partial cons i) next-cart))))))
(defn- domain-min [x]
  (if (instance? IntVar x)
    (.min (.dom x))
    x))
(defn- domain-max [x]
  (if (instance? IntVar x)
    (.max (.dom x))
    x))

(defn- pipe-helper
  [real-op args]
  (let [mins (map domain-min args)
        maxes (map domain-max args)
        key-points (for [comb (cartesian-product (map vector mins maxes))]
                     (apply real-op comb))
        [final-min final-max] [(apply min key-points) (apply max key-points)]]
    (core/int-var final-min final-max)))

(defn- typeify
  [args]
  (vec
    (for [x args]
      (cond
        (keyword? x) x
        (instance? Var x) :var
        (instance? Number x) :num))))

;;;;;; Arithmetic

(defn $+
  "Given two or more variables, returns a new variable that is constrained to equal the sum of those variables.

If you use two or three variables, one of them may be a constant number.
(i.e. ($+ x 1))"
  ([x y]
    (core/get-current-store)
    (let [accepted #{[:var :var] [:var :num]}
          piped (when (accepted (typeify [x y]))
                  (pipe-helper + [x y]))]
      (case (typeify [x y])
        [:var :var] (do (core/constrain! (XplusYeqZ. x y piped)) piped)
        [:var :num] (do (core/constrain! (XplusCeqZ. x y piped)) piped)
        [:num :var] (recur y x))))
  ([x y z]
    (core/get-current-store)
    (let [accepted #{[:var :var :var] [:var :var :num]}
          piped (when (accepted (typeify [x y z]))
                  (pipe-helper + [x y z]))]
      (case (typeify [x y z])
        [:var :var :var] (do (core/constrain! (XplusYplusQeqZ. x y z piped)) piped)
        [:var :var :num] (do (core/constrain! (XplusYplusCeqZ. x y z piped)) piped)
        [:var :num :var] (recur x z y)
        [:num :var :var] (recur y z x))))
  ([a b c d & more]
    (core/get-current-store)
    (let [args (list* a b c d more)
          total-min (apply + (map domain-min args))
          total-max (apply + (map domain-max args))
          z (core/int-var total-min total-max)]
      (core/constrain! (Sum. (into-array IntVar args)
                             z))
      z)))

(defn $-
  "Given one or more variables X, Y, Z, ... returns a new variable that is constrained to equal X - Y - Z - ... (or -X if only one argument)"
  ([x]
    (let [final-min (- (domain-max x))
          final-max (- (domain-min x))
          piped (core/int-var final-min final-max)]
      (core/constrain! (XplusYeqC. x piped 0))
      piped))
  ([x & more]
    (apply $+ x (map $- more))))

(defn $min
  "Returns a variable that is constrained to equal the minimum of the given variables."
  [& vars]
  (let [final-min (apply min (map domain-min vars))
        final-max (apply min (map domain-max vars))
        piped (core/int-var final-min final-max)]
    (core/constrain! (Min. (into-array IntVar vars) piped))
    piped))

(defn $max
  "Returns a variable that is constrained to equal the maximum of the given variables."
  [& vars]
  (let [final-min (apply max (map domain-min vars))
        final-max (apply max (map domain-max vars))
        piped (core/int-var final-min final-max)]
    (core/constrain! (Max. (into-array IntVar vars) piped))
    piped))

(defn $abs
  "Given a variable X, returns another variable Y such that |X| = Y."
  [x]
  (let [xmin (domain-min x)
        xmax (domain-max x)
        [ymin ymax] (cond
                      (< xmax 0) [(- xmax) (- xmin)]
                      (< xmin 0) [0 (max (- xmin) xmax)]
                      :else [xmin xmax])
        y (core/int-var ymin ymax)]
    (core/constrain! (AbsXeqY. x y))
    y))

(defn $weighted-sum
  "Given vars x, y, z..., and integers a, b, c..., returns a var that equals ax + by + cz + ..."
  [vars weights]
  (let [minmaxes (map sort (map vector
                                (map * (map domain-min vars) weights)
                                (map * (map domain-max vars) weights)))
        final-min (apply + (map first minmaxes))
        final-max (apply + (map second minmaxes))
        piped (core/int-var final-min final-max)]
    (core/constrain! (SumWeight. (into-array IntVar vars)
                                 (int-array weights)
                                 piped))
    piped))

(defn $*
  "Given two variables (or a variable and a number), returns a new variable that is constrained to be equal to the product of the two arguments."
  [x y]
  (core/get-current-store)
  (let [accepted #{[:var :var] [:var :num]}
        piped (when (accepted (typeify [x y]))
                (pipe-helper * [x y]))]
    (case (typeify [x y])
      [:var :var] (do (core/constrain! (XmulYeqZ. x y piped)) piped)
      [:var :num] (do (core/constrain! (XmulCeqZ. x y piped)) piped)
      [:num :var] (recur y x))))

(defn $pow
  "Given two variables X and Y, returns a new variable constrained to equal X^Y."
  [x y]
  (let [piped (pipe-helper #(int (Math/pow %1 %2)) [x y])]
    (core/constrain! (XexpYeqZ. x y piped))
    piped))

(declare $and $= $!= $< $> $<= $>=)
(defn $=
  ([x y]
    (case (typeify [x y])
      [:var :var] (XeqY. x y)
      [:var :num] (XeqC. x y)
      [:num :var] (recur y x)))
  ([x y & more]
    (let [args (list* x y more)]
      (apply $and (map (partial apply $=) (partition 2 1 args))))))
(defn $!=
  [x y]
  (case (typeify [x y])
    [:var :var] (XneqY. x y)
    [:var :num] (XneqC. x y)
    [:num :var] (recur y x)))
(defn $<
  [x y]
  (case (typeify [x y])
    [:var :var] (XltY. x y)
    [:var :num] (XltC. x y)
    [:num :var] ($> y x)))
(defn $>
  [x y]
  (case (typeify [x y])
    [:var :var] (XgtY. x y)
    [:var :num] (XgtC. x y)
    [:num :var] ($< y x)))
(defn $<=
  [x y]
  (case (typeify [x y])
    [:var :var] (XlteqY. x y)
    [:var :num] (XlteqC. x y)
    [:num :var] ($>= y x)))
(defn $>=
  [x y]
  (case (typeify [x y])
    [:var :var] (XgteqY. x y)
    [:var :num] (XgteqC. x y)
    [:num :var] ($<= y x)))

;;;;;;;; Logic

(defn $and
  "Specifies that all of the given constraints must be true.

Note: the given constraints can only be number comparisons or logic statements."
  [& constraints]
  (And. (into-array PrimitiveConstraint constraints)))

(defn $or
  "Specifies that one or more of the given constraints must be true.

Note: the given constraints can only be number comparisons or logic statements."
  [& constraints]
  (Or. (into-array PrimitiveConstraint constraints)))

(defn $not
  "Specifies that the given constraint is NOT true.

Note: the given constraints can only be number comparisons or logic statements."
  [constraint]
  (Not. constraint))

(defn $if
  "Specifies that if one constraint is true, the other constraint must be true as well. An \"else\" statement can be specified as well."
  ([if-this then-this]
    (IfThen. if-this then-this))
  ([if-this then-this else-this]
    (IfThenElse. if-this then-this else-this)))

(defn- $cond-helper
  [clauses]
  (let [c (cond
            (empty? clauses) 0
            (empty? (rest clauses)) 1
            (empty? (drop 2 clauses)) 2
            :else :more)]
    (case c
      0 ($and)
      1 (first clauses)
      2 (apply $if clauses)
      ($if (first clauses) (second clauses) ($cond-helper (drop 2 clauses))))))

(defn $cond
  "Takes inputs in a similar form as \"cond\". The final \"else\" statement can be specified with :else (like in cond) or as the odd argument (like in case)"
  [& clauses]
  ($cond-helper (remove #(= % :else) clauses)))

(defn $<=>
  "Constrains a \"bicond\" constraint P <=> Q, a.k.a. P iff Q."
  [P Q]
  (Eq. P Q))

;;;;;;;; Global constraints

(defn $all-different
  [& vars]
  (Alldifferent. (into-array IntVar vars)))

(defn $reify
  "Sometimes it's nice to be able to manipulate the true/false value of a constraint. $reify takes a constraint, and returns a variable that will be constrained to equal the 0/1 boolean value of whether that constraint is true.

i.e. ($= ($reify X) 1) would be saying that X is true.
Note: calling constrain! on the given constraint as well as reifying it isn't entirely useful, because calling constrain! will force it to be true anyway."
  [constraint]
  (let [piped (core/int-var 0 1)]
    (core/constrain! (Reified. constraint piped))
    piped))

(defn $nth
  "Given a list of vars (or numbers) L, and a variable index i, returns a var x such that L[i] = x.

Example:
(def L [1 2 3 4 5 6])
(def i (int-var \"i\" 0 5))
(constrain! ($= ($nth L i) 3))"
  [L i]
  (let [L (if (number? (first L))
            (int-array L)
            (into-array IntVar L))
        final-min (apply min (map domain-min L))
        final-max (apply max (map domain-max L))
        piped (core/int-var final-min final-max)]
    (core/constrain! (Element. i L piped 1))
    piped))

(defn $occurrences
  "Given a list of variables and a number, returns a variable X such that X = the number of occurrences of the number in the list.

Note: you can also pass in a domain instead of a number, in which case it will count how many numbers are in that domain."
  [list-of-vars item]
  (let [list-of-vars (into-array IntVar list-of-vars)
        piped (core/int-var 0 (count list-of-vars))]
    (if (instance? IntervalDomain item)
      (core/constrain! (Among. list-of-vars item piped))
      (core/constrain! (Count. list-of-vars piped item)))
    piped))
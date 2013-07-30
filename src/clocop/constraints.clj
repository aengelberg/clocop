(ns clocop.constraints
  "A namespace for the various constraints you can use."
  (:use [clojure.core.match :only (match)])
  (:require [clocop.core :as core])
  (:import
    (JaCoP.core Var IntVar)
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
  [fake-op real-op args]
  (let [mins (map domain-min args)
        maxes (map domain-max args)
        key-points (for [comb (cartesian-product (map vector mins maxes))]
                     (apply real-op comb))
        [final-min final-max] [(apply min key-points) (apply max key-points)]]
    (core/int-var (str "_(" (name fake-op) " "
                       (clojure.string/join " " (map #(if (instance? IntVar %) (.id %) %)
                                                     args))
                       ")")
                  final-min final-max)))

(defn- typeify
  [args]
  (vec
    (for [x args]
      (cond
        (keyword? x) x
        (instance? Var x) :var
        (instance? Number x) :num))))

(defn $+
  "Given two or more variables, returns a new variable that is constrained to equal the sum of those variables.

If you use two or three variables, this function supports some intuitive combinations of variables and numbers.
(i.e. ($+ x 1))"
  ([x y]
    (core/get-current-store)
    (let [accepted #{[:var :var] [:var :num]}
          piped (when (accepted (typeify [x y]))
                  (pipe-helper :+ + [x y]))]
      (case (typeify [x y])
        [:var :var] (do (core/constrain! (XplusYeqZ. x y piped)) piped)
        [:var :num] (do (core/constrain! (XplusCeqZ. x y piped)) piped)
        [:num :var] (recur y x))))
  ([x y z]
    (core/get-current-store)
    (let [accepted #{[:var :var :var] [:var :var :num]}
          piped (when (accepted (typeify [x y z]))
                  (pipe-helper :+ + [x y z]))]
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
          z (core/int-var (str "_(+ "
                               (clojure.string/join " " (map #(if (instance? IntVar %) (.id %) %)
                                                             args))
                               ")") total-min total-max)]
      (core/constrain! (Sum. (into-array IntVar args)
                             z))
      z)))

(defn $-
  "Given one or more variables X, Y, Z, ... returns a new variable that is constrained to equal X - Y - Z - ... (or -X if only one argument)"
  ([x]
    (let [final-min (- (domain-max x))
          final-max (- (domain-min x))
          piped (core/int-var (str "_(- " (.id x) ")") final-min final-max)]
      (core/constrain! ($= ($+ x piped) 0))
      piped))
  ([x & more]
    (apply $+ x (map $- more))))

(defn $weighted-sum
  "Given vars x, y, z..., and integers a, b, c..., returns a var that equals ax + by + cz + ..."
  [vars weights]
  (let [minmaxes (map sort (map vector
                                (map * (map domain-min vars) weights)
                                (map * (map domain-max vars) weights)))
        final-min (apply + (map first minmaxes))
        final-max (apply + (map second minmaxes))
        var-name (str "_(+ " (clojure.string/join " "(for [[v w] (map vector vars weights)]
                                                       (str "(* " w " " (.id v) ")"))) ")")
        piped (core/int-var var-name final-min final-max)]
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
                (pipe-helper :* * [x y]))]
    (case (typeify [x y])
      [:var :var] (do (core/constrain! (XmulYeqZ. x y piped)) piped)
      [:var :num] (do (core/constrain! (XmulCeqZ. x y piped)) piped)
      [:num :var] (recur y x))))

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

(defn $all-different
  [& vars]
  (Alldifferent. (into-array IntVar vars)))

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
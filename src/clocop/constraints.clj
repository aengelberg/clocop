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

(defn- pipe-helper
  [fake-op real-op args]
  (let [domain-min (fn [x]
                     (if (instance? IntVar x)
                       (.min (.dom x))
                       x))
        domain-max (fn [x]
                     (if (instance? IntVar x)
                       (.max (.dom x))
                       x))
        mins (map domain-min args)
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
          domain-min (fn [x]
                     (if (instance? IntVar x)
                       (.min (.dom x))
                       x))
          domain-max (fn [x]
                       (if (instance? IntVar x)
                         (.max (.dom x))
                         x))
          total-min (apply + (map domain-min args))
          total-max (apply + (map domain-max args))
          z (core/int-var (str "_(+ "
                               (clojure.string/join " " (map #(if (instance? IntVar %) (.id %) %)
                                                             args))
                               ")") total-min total-max)]
      (core/constrain! (Sum. (into-array IntVar args)
                             z)))))

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

(declare $= $!= $< $> $<= $>=)
(defn $=
  [x y]
  (case (typeify [x y])
    [:var :var] (XeqY. x y)
    [:var :num] (XeqC. x y)
    [:num :var] (recur y x)))
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
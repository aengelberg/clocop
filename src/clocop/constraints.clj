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

(defn- typeify
  [args]
  (for [x args]
    (cond
      (keyword? x) x
      (instance? Var x) :var
      (instance? Number x) :num)))

(defn arith%
  "Given an arithmetic equation (with keywords as the operators), constructs the corresponding JaCoP built-in constraint.
Operations supported:
:+ (primitive), :* (non-primitive), :div (non-primitive), :mod (non-primitive), :exp (non-primitive)
Sample usage: (arith% [:+ x y] := z)
Supports most intuitive combinations of variables and numbers (plus a few in conjunction with inequalities)."
  [[op & args :as expr] compare z]
  (let [types (typeify args)]
    (match [(vec expr) compare z]
      [[:+ x y] := z] (case (typeify [x y z])
                        [:var :var :var] (XplusYeqZ. x y z)
                        [:var :var :num] (XplusYeqC. x y z)
                        [:var :num :var] (XplusCeqZ. x y z))
      [[:+ x y q] := z] (case (typeify [x y q z])
                          [:var :var :var :var] (XplusYplusQeqZ. x y q z)
                          [:var :var :num :var] (XplusYplusCeqZ. x y q z))
      [[:+ x y] :<= z] (XplusYlteqZ. x y z)
      [[:+ x y q] :> c] (XplusYplusQgtC. x y q c)
      
      [[:* x y] := z] (case (typeify [x y z])
                        [:var :var :var] (XmulYeqZ. x y z)
                        [:var :num :var] (XmulCeqZ. x y z))
      
      [[:div x y] := z] (XdivYeqZ. x y z)
      
      [[:mod x y] := z] (XmodYeqZ. x y z)
      
      [[:exp x y] := z] (XexpYeqZ. x y z))))

(defn sum%
  "(not prim) Specifies that the sum of a bunch of variables equals another variable.
Sample usage: (sum% [a b c d e] := z)
All arguments must be variables, not numbers."
  [args eq z]
  (Sum. (into-array IntVar args)
        z))

(defn compare%
  "(prim) Specifies x = y, x < y, x > y, x <= y, x >= y, x != y.
Sample usage: (compare% :> x 3) or (compare% := a b)
You can also substitute either x or y with a number."
  [comparator x y]
  (let [types (typeify [x y])]
    (case comparator
      := (case types
           [:var :var] (XeqY. x y)
           [:var :num] (XeqC. x y)
           [:num :var] (recur := y x))
      :< (case types
           [:var :var] (XltY. x y)
           [:var :num] (XltC. x y)
           [:num :var] (recur :> y x))
      :> (case types
           [:var :var] (XgtY. x y)
           [:var :num] (XgtC. x y)
           [:num :var] (recur :< y x))
      :<= (case types
            [:var :var] (XlteqY. x y)
            [:var :num] (XlteqC. x y)
            [:num :var] (recur :>= y x))
      :>= (case types
            [:var :var] (XgteqY. x y)
            [:var :num] (XgteqC. x y)
            [:num :var] (recur :<= y x))
      :!= (case types
            [:var :var] (XneqY. x y)
            [:var :num] (XneqY. x y)
            [:num :var] (recur :!= y x))
      (throw (IllegalArgumentException. (str "Operation \"" (name comparator) "\" not supported"))))))

(defn- cartesian-product
  [lists]
  (cond
    (empty? lists) '(())
    :else (let [results (cartesian-product (rest lists))]
            (apply concat
                   (for [i (first lists)]
                     (map (partial cons i) results))))))

(defn- pipe-helper
  [single-expr]
  (let [store (core/get-current-store)
        [op & args] single-expr
        domain-min (fn [x]
                     (if (instance? IntVar x)
                       (.min (.dom x))
                       x))
        domain-max (fn [x]
                     (if (instance? IntVar x)
                       (.max (.dom x))
                       x))
        mins (map domain-min args)
        maxes (map domain-max args)
        real-op (case op :+ + :* * :div / :mod mod :exp #(Math/pow %1 %2))
        key-points (for [comb (cartesian-product (map vector mins maxes))]
                     (apply real-op comb))
        [final-min final-max] [(java.lang.Math/floor (double (apply min key-points)))
                               (java.lang.Math/ceil (double (apply max key-points)))]
        new-var (core/int-var (str "_(" (name op) " " (clojure.string/join " "
                                                                           (map #(if (instance? IntVar %)
                                                                                   (.id %)
                                                                                   %) args)) ")")
                              final-min final-max)
        _ (core/constrain! (arith% single-expr := new-var))]
    new-var))

(defn pipe
  "Given an arithmetic expression (with no equals sign), returns an auxiliary variable that is equal to that equation.

Note: this function will add some constraints and variables to your store.

Example:
(with-store (store) (pipe [:+ [:* x y] 2]))  ; this example uses vectors but any sequence will do
  => <IntVar>"
  [expr]
  (let [store (core/get-current-store)]
    (if (not (sequential? expr))
      expr
      (let [expr (for [subexpr expr]
                   (pipe subexpr))]
        (pipe-helper expr)))))

(defn constrain-arith!
  "Given an arithmetic equation, constrains that equation onto the given store.
Example: (with-store (store) (constrain-arith! [:= x y] [:= [:+ x 2] [:* y 3]])"
  [& exprs]
  (let [store (core/get-current-store)]
    (doseq [[eq & args] exprs]
      (core/constrain! (apply compare% eq (map pipe args))))))
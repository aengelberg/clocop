(ns clocop.constraints
  "A namespace for the various constraints you can use."
  (:import
    (JaCoP.core Var IntVar)
    (JaCoP.constraints XeqY
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
                       Alldifferent
                       Element)))

(defmacro def-primitive-constraint
  "Private macro to help with defining the many constraints."
  [name docstring [& args] & clauses]
  (let [clauses (map vec (partition 2 clauses))
        clauses (apply concat (for [[var? constraint-type] clauses]
                                [var? (if (sequential? constraint-type)
                                        constraint-type
                                        `(new ~constraint-type ~@args))]))]
    `(defn ~name ~docstring [~@args]
       (case (vec (map (partial instance? Var) (list ~@args)))
         ~@clauses
         (throw (IllegalArgumentException. "Wrong combination of constants and Vars"))))))

(def-primitive-constraint =%
  "Specifies an equality constraint. Can support equality between two int vars, or between an int var and a constant (in either order)."
  [X Y]
  [true true] XeqY
  [true false] XeqC
  [false true] (XeqC. Y X))

(def-primitive-constraint !=%
  "Specifies an not-equal constraint. Can support inequality between two int vars, or between an int var and a constant (in either order)."
  [X Y]
  [true true] XneqY
  [true false] XneqC
  [false true] (XneqC. Y X))

(defn all-different%
  "Specifies an all-different constraint."
  [vars]
  (Alldifferent. (into-array IntVar vars)))

(defn element-of%
  "Given an array (any seq) and two vars (i and x), specifies that the ith item in the array is equal to x.
Note that the array can either contain only vars, or contain only concrete numbers.

Another important thing is that by default, this array lookup is one-based. Add \":zero-based true\" to make it zero-based."
  [array i x & {zero-based :zero-based}]
  (let [array (if (instance? Var (first array))
                (into-array IntVar array)
                (int-array array))]
    (if zero-based
      (Element. i array x 1)
      (Element. i array x))))

(def-primitive-constraint <%
  "Specifies that X < Y. Y can be a var or a constant."
  [X Y]
  [true true] XltY
  [true false] XltC)
(def-primitive-constraint >%
  "Specifies that X > Y. Y can be a var or a constant."
  [X Y]
  [true true] XgtY
  [true false] XgtC)
(def-primitive-constraint <=%
  "Specifies that X <= Y. Y can be a var or a constant."
  [X Y]
  [true true] XlteqY
  [true false] XlteqC)
(def-primitive-constraint >=%
  "Specifies that X >= Y. Y can be a var or a constant."
  [X Y]
  [true true] XgteqY
  [true false] XgteqC)
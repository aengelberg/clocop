(ns clocop.constraints
  "A namespace for the various constraints you can use."
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
                       
                       XplusYeqZ
                       XplusCeqZ
                       XplusYeqC
                       Sum
                       SumWeight
                       
                       Alldifferent
                       Element
                       Count
                       Reified
                       )))

(defmacro ^:private def-primitive-constraint
  "Private macro to help with defining primitive constraints. Specifically, helps take cases on which arguments are variables and which are constants.

Sample form:
(def-primitive-constraint constraint-name%
  docstring
  [X Y Z]
  [true true true] class-name
  [true true false] (do something with X and Y)
  [true false true] class-name
  [true false false] class-name
  ...)
true = var. false = not var, i.e. constant.
Automatically throws an error for type groups not covered."
  [name docstring [& args] & clauses]
  (let [clauses (map vec (partition 2 clauses))
        clauses (apply concat (for [[var? constraint-type] clauses]
                                [var? (if (sequential? constraint-type)
                                        constraint-type
                                        `(new ~constraint-type ~@args))]))]
    `(defn ~name ~docstring [~@args]
       (case (vec (map (partial instance? Var) (list ~@args)))
         ~@clauses
         (throw (IllegalArgumentException. (str "Wrong combination of constants and variables")))))))

;;;;; Equality and inequality constraints ;;;;;

(def-primitive-constraint =%
  "Specifies an equality constraint. Can support equality between two variables, or between a variable and a constant (in either order)."
  [X Y]
  [true true] XeqY
  [true false] XeqC
  [false true] (XeqC. Y X))

(def-primitive-constraint !=%
  "Specifies an not-equal constraint. Can support negative equality between two variables, or between a variable and a constant (in either order)."
  [X Y]
  [true true] XneqY
  [true false] XneqC
  [false true] (XneqC. Y X))

(defn element-of%
  "Given an array (any seq) and two vars (i and x), specifies that the ith item in the array is equal to x.
Note that the array can either contain only variables, or contain only concrete numbers.

Another important thing is that by default, this array lookup is one-based. Add \":zero-based true\" to make it zero-based."
  [array i x & {zero-based :zero-based}]
  (let [array (if (instance? Var (first array))
                (into-array IntVar array)
                (int-array array))]
    (if zero-based
      (Element. i array x 1)
      (Element. i array x))))

(def-primitive-constraint <%
  "Specifies that X < Y. Y can be a variable or a constant."
  [X Y]
  [true true] XltY
  [true false] XltC)
(def-primitive-constraint >%
  "Specifies that X > Y. Y can be a variable or a constant."
  [X Y]
  [true true] XgtY
  [true false] XgtC)
(def-primitive-constraint <=%
  "Specifies that X <= Y. Y can be a variable or a constant."
  [X Y]
  [true true] XlteqY
  [true false] XlteqC)
(def-primitive-constraint >=%
  "Specifies that X >= Y. Y can be a variable or a constant."
  [X Y]
  [true true] XgteqY
  [true false] XgteqC)

;;;;; Addition ;;;;;

(def-primitive-constraint ^:private +%helper
  "dummy docstring"
  [X Y Z]
  [true true true] XplusYeqZ
  [true true false] XplusYeqC
  [true false true] XplusCeqZ)

(defn +%
  "Takes at least three arguments. In a sense, it specifies (= (apply + (butlast args)) (last args)).

NOTE: While in general you have to only use variables rather than constants, when using only three arguments, i.e. X+Y=Z, then Y or Z (but not both) can be constants."
  ([X Y Z]
    (+%helper X Y Z))
  ([A B C & moreAndZ]
    (let [args (concat [A B C] moreAndZ)
          _ (when (not (every? (partial instance? IntVar) args))
              (throw (IllegalArgumentException. "When using more than three arguments, all arguments must be variables (IntVars).")))
          left (butlast args)
          last (last args)]
      (Sum. (into-array IntVar left) last))))

(defn weighted-sum%
  "Given an array of variables x_0, x_1, ... and an array of \"weights\" (constants) w_0, w_1, ... and a variable Z,
specifies that w_0 * x_0 + w_1 * x_1 + ... = Z"
  [list-of-x list-of-w Z]
  (SumWeight. (into-array IntVar list-of-x)
              (int-array list-of-w)
              Z))

;;;;; Global constraints ;;;;;

(defn count-occurences%
  "Specifies that X (a constant) occurs N times (a variable) in an array of variables."
  [X N vars]
  (Count. (into-array IntVar vars) N X))

(defn all-different%
  "Specifies an all-different constraint."
  [vars]
  (Alldifferent. (into-array IntVar vars)))

(defn reified%
  "Takes a primitive constraint and a variable, and specifies that the variable is equal to the 0/1 value of whether the given constraint is true.

Note: the sub-constraint given does not have to (and probably shouldn't) be applied to your Store with \"constrain!\"."
  [^PrimitiveConstraint constraint ^IntVar b]
  (Reified. constraint b))
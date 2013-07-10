(ns clocop.constraints
  "A namespace for the various constraints you can use."
  (:import
    (JaCoP.core Var IntVar)
    (JaCoP.constraints XeqY
                       XeqC
                       XltY
                       XlteqY
                       XgtY
                       XgteqY
                       XneqY
                       XneqC
                       Alldifferent
                       Element)))

(defn =%
  "Specifies an equality constraint. At least one of the arguments must be a Var. The non-Var argument (if any) is a constant integer that will be assigned to the Var."
  [item1 item2]
  (case [(instance? Var item1) (instance? Var item2)]
    [true true] (XeqY. item1 item2)
    [true false] (XeqC. item1 item2)
    [false true] (XeqC. item2 item1)
    (throw (IllegalArgumentException. "=%: Expected at least one argument to be a Var."))))

(defn !=%
  "Specifies an not-equal constraint. At least one of the arguments must be a Var. The non-Var argument (if any) is a constant integer that will be assigned to not be equal to the Var."
  [item1 item2]
  (case [(instance? Var item1) (instance? Var item2)]
    [true true] (XneqY. item1 item2)
    [true false] (XneqC. item1 item2)
    [false true] (XneqC. item2 item1)
    (throw (IllegalArgumentException. "!=%: Expected at least one argument to be a Var."))))

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

(defn <%
  "Specifies that X must be less than Y."
  [X Y]
  (XltY. X Y))
(defn <=%
  "Specifies that X must be less than or equal to Y."
  [X Y]
  (XlteqY. X Y))
(defn >%
  "Specifies that X must be greater than Y."
  [X Y]
  (XgtY. X Y))
(defn >=%
  "Specifies that X must be greater than or equal to Y."
  [X Y]
  (XgteqY. X Y))
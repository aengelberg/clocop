(ns clocop.constraints
  "A namespace for the various constraints you can use."
  (:import
    (JaCoP.core Var IntVar)
    (JaCoP.constraints XeqY
                       XeqC
                       XneqY
                       XneqC
                       Alldifferent)))

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
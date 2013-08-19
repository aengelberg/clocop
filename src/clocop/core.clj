(ns clocop.core
  (:import (JaCoP.core Store
                       IntVar
                       IntDomain
                       IntervalDomain
                       BoundDomain)
           (JaCoP.search DepthFirstSearch
                         InputOrderSelect
                         IndomainMin
                         ))
  (:use clocop.solver))

(def ^:dynamic *current-store*
  "A (private) dynamic variable used in conjunction with the with-store function."
  nil)

(defn get-current-store
  "Private function for working with with-store."
  []
  (if *current-store*
    *current-store*
    (throw (Exception. "Could not find *current-store* binding. Try using with-store"))))

(defmacro with-store
  "Binds a store to a dynamic variable that the internal CloCoP functions share. with-store is more or less required to do most CloCoP operations."
  [store & body]
  `(binding [*current-store* ~store]
     ~@body))

(defn ^Store store
  "Makes a JaCoP \"Store\" object, which is the key concept in constraint programming.
No options and configurations are required for the Store itself, but you will connect all the variables, constraints, and searchers to it eventually."
  []
  (Store.))

(defn ^IntDomain domain
  "Takes an arbitrary number of [min max] pairs. This function is more capable than simply entering a min and max value in the int-var function."
  [& min-max-pairs]
  (let [domains (for [[min max] min-max-pairs]
                  (IntervalDomain. min max))
        [the-domain & other-domains] domains]
    (doseq [other-domain other-domains]
      (.addDom the-domain other-domain))
    the-domain))

(defn ^IntVar int-var
  "Creates a JaCoP \"IntVar\" object, which can have constraints on it. Must be connected to a Store object at the time of creation.

Allowed argument lists:
- (int-var min max)
- (int-var name min max)

- (int-var number)
- (int-var name number)

- (int-var domain)
- (int-var name domain)

Note that the optional \"name\" field (which is an input-order-select by default) is only used for the outputted logs, and not at all necessary to function internally."
  [& args]
  (let [store (get-current-store)]
    (case (count args)
      1 (if (instance? IntDomain (first args))
          (IntVar. store (first args))
          (IntVar. store (first args) (first args)))
      2 (case [(string? (first args)) (number? (second args))]
          [true true] (IntVar. store (first args) (second args) (second args))
          [true false] (IntVar. store (first args) (second args))
          [false true] (IntVar. store (first args) (second args)))
      3 (IntVar. store (first args) (second args) (nth args 2)))))

(defn constrain!
  "Given a constraint (created with clocop.constraints, or implements JaCoP.constraints.Constraint), imposes the constraint on your store.
The constraint doesn't take effect on the variables until you run the \"solve!\" function."
  [constraint]
  (.impose ^Store (get-current-store) ^Constraint constraint))

(defn- disclosed-variables
  [store]
  (filter #(and (identity %)
                (not= (first (.id %)) \_))
          (.vars store)))

(defn solve!
  "Finds one (or every) solution in your store. Solutions are returned in the form of a map, from the var names to their values. Variables whose names start with an underscore will not be included in the final map.
The var names included in the map are all of them by default, but if you use a custom Selector, it will only return the variables you gave to the selector.

Optional keyword arguments:
  :solutions - :one or :all (default :one)
  :minimize - an int-var that the search will minimize as much as possible.
  :log? - if true, the searcher will print a log about its search to the Console. (default false)
  :pick-var - if there are no more deductions to make, which variable should we branch the search on?
Options: keywords like :smallest-domain (see readme for all choices), or a list of variables that the solver will branch on in order (skipping over variables already solved)
Default: :smallest-domain
  :pick-val - after picking a variable, what value should we pick first?
Options: keywords like :min, see readme for all choices.
Default: :min

NOTE: Weird behavior occurs when reusing stores and constraints.
Although this function returns something, the function name is marked with an exclamation point to remind you that this function shouldn't be reused on the same store.
"
  [& args]
  (let [store (get-current-store)
        args (apply hash-map args)
        vars (disclosed-variables store)
        num-solutions (or (:solutions args) :one)
        
        pick-var-arg (or (:pick-var args)
                         :smallest-domain)
        pick-val-arg (or (:pick-val args)
                         :min)
        the-pick-val (pick-val pick-val-arg)
        the-selector (if (sequential? pick-var-arg)
                       (input-order-selector store pick-var-arg the-pick-val)
                       (selector vars
                                 :pick-var (pick-var pick-var-arg)
                                 :pick-val the-pick-val))
        
        minimize (:minimize args)
        log? (:log? args)
        search (DepthFirstSearch.)
        listener (.getSolutionListener search)
        _ (.setPrintInfo search (boolean log?))
        _ (when (= num-solutions :all)
            (.searchAll listener true))
        _ (.recordSolutions listener true)
        labeling? (if minimize
                    (.labeling search store the-selector minimize)
                    (.labeling search store the-selector))
        ]
    (cond
      (not labeling?) nil
      :else (let [solutions (for [i (range 1 (inc (.solutionsNo listener)))
                                  :let [domain-array (.getSolution listener i)]]
                              (let [vars (.getVariables listener)
                                    varnames (map #(.id %) vars)
                                    domain-vals (for [domain domain-array]
                                                  (.getElementAt domain 0))
                                    result (into {}
                                                 (for [[var val] (map vector varnames domain-vals)
                                                       :when (not= (first var) \_)]
                                                   [var val]))]
                                result))]
              
              (if (= num-solutions :one)
                (first solutions)
                solutions)))))

;(def s (store))
;(def vars [(int-var s "a" 1 2)
;           (int-var s "b" 3 4)])
;(def constraints [(clocop.constraints/=% (vars 0) 1)
;                  (clocop.constraints/=% (vars 1) 4)])
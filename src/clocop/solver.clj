(ns clocop.solver
  "A namespace with functions for customizing the way the solver works."
  (:import (JaCoP.core Var)
           (JaCoP.search SimpleSelect
                         InputOrderSelect
                         
                         ComparatorVariable
                         
                         LargestDomain
                         LargestMax
                         LargestMin
                         MaxRegret
                         MinDomainOverDegree
                         MostConstrainedDynamic
                         MostConstrainedStatic
                         SmallestDomain
                         SmallestMax
                         SmallestMin
                         WeightedDegree
                         
                         Indomain
                         
                         IndomainMin
                         IndomainMax
                         IndomainMiddle
                         IndomainList
                         IndomainRandom
                         IndomainSimpleRandom
                         )))

(defn ^SimpleSelect selector
  [variables & {:as args}]
  (let [{pick-var :pick-var
         pick-val :pick-val} args]
    (SimpleSelect. (into-array Var variables)
                   pick-var
                   pick-val)))

(defn ^InputOrderSelect input-order-selector
  [store list-of-vars pick-val]
  (InputOrderSelect. store (into-array Var list-of-vars) pick-val))

(defn ^ComparatorVariable pick-var
  [pick-var-type]
  (case pick-var-type
    :largest-domain (LargestDomain.), :largest-max (LargestMax.), :largest-min (LargestMin.),
    :max-regret (MaxRegret.),
    :min-domain-over-degree (MinDomainOverDegree.),
    :most-constrained-dynamic (MostConstrainedDynamic.)
    :most-constrained-static (MostConstrainedStatic.)
    :smallest-domain (SmallestDomain.), :smallest-max (SmallestMax.), :smallest-min (SmallestMin.),
    :weighted-degree (WeightedDegree.)))

(defn ^Indomain pick-val
  [indomain-type & args]
  (case indomain-type
    :min (IndomainMin.)
    :max (IndomainMax.)
    :middle (IndomainMiddle.)
    :list (IndomainList. (int-array (first args)) (apply pick-val (rest args)))
    :random (case (count args)
              0 (IndomainRandom.)
              1 (IndomainRandom. (first args)))
    :simple-random (IndomainSimpleRandom.)))
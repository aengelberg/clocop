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
  "A selector to pass to clocop.core/search. It is a modular system that requires the following pieces:
- a list of variables that the searcher will try setting to various values.
Keyword arguments:
:pick-var - decides what (undecided) variable to try changing next.
:pick-var-tiebreaker (optional) - will break ties created by the first one.
:in-domain - decides what value to try first when a variable is picked.

Note that these customizations are mainly to improve speed; they decide the order in which things are done.
However, everything will be tried anyway by the solver if need be."
  [variables & {:as args}]
  (let [{pick-var :pick-var
         pick-var-tiebreaker :pick-var-tiebreaker
         in-domain :in-domain} args]
    (when (or (not pick-var)
              (not in-domain))
      (throw (IllegalArgumentException. "simple-select: Must supply :pick-var and :in-domain")))
    (if pick-var-tiebreaker
      (SimpleSelect. (into-array Var variables)
                     pick-var
                     pick-var-tiebreaker
                     in-domain)
      (SimpleSelect. (into-array Var variables)
                     pick-var
                     pick-var-tiebreaker
                     in-domain))))

(defn ^InputOrderSelect input-order-selector
  "Like selector, but for the variable selecting part, it just uses a list of vars to go by.
Takes a Store, a sequence of variables (in the desired order), and an in-domain."
  [store list-of-vars in-domain]
  (InputOrderSelect. store (into-array Var list-of-vars) in-domain))

(defn ^ComparatorVariable pick-var
  "Takes one argument, a keyword, which corresponds to a JaCoP ComparatorVariable object.
i.e. :smallest-domain for JaCoP.solver.SmallestDomain"
  [pick-var-type]
  (case pick-var-type
    :largest-domain (LargestDomain.), :largest-max (LargestMax.), :largest-min (LargestMin.),
    :max-regret (MaxRegret.),
    :min-domain-over-degree (MinDomainOverDegree.),
    :most-constrained-dynamic (MostConstrainedDynamic.)
    :most-constrained-static (MostConstrainedStatic.)
    :smallest-domain (SmallestDomain.), :smallest-max (SmallestMax.), :smallest-min (SmallestMin.),
    :weighted-degree (WeightedDegree.)))

(defn ^Indomain in-domain
  "Takes a keyword argument plus maybe some extra arguments corresponding to the first argument.
Possible arg lists:
(in-domain :min) => starts from the minimum value in a domain.
(in-domain :max) => starts from the maximum value in a domain.
(in-domain :middle) => starts from the middle and works its way out.
(in-domain :list ordering other-in-domain) => uses the specified ordering,
but uses the other in-domain for the numbers not covered by the ordering.
(in-domain :random) => picks randomly. Optionally takes a seed.
(in-domain :simple-random) => faster than :random, but possibly not uniform.

(in-domain :custom fun) => creates an instance of JaCoP.search.Indomain that
will use a custom function. The function takes an IntDomain object. Look at the
JaCoP docs to find out more about what methods can be called on IntDomain."
  [indomain-type & args]
  (case indomain-type
    :min (IndomainMin.)
    :max (IndomainMax.)
    :middle (IndomainMiddle.)
    :list (IndomainList. (int-array (first args)) (second args))
    :random (case (count args)
              0 (IndomainRandom.)
              1 (IndomainRandom. (first args)))
    :simple-random (IndomainSimpleRandom.)
    :custom (reify Indomain
              (indomain [this var]
                ((first args) (.dom var))))))
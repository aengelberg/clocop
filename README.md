CloCoP
======

CloCoP is a Clojure wrapper for JaCoP, which is a Java constraint programming engine. JaCoP stands for <b>JA</b>va <b>CO</b>nstraint <b>P</b>rogramming. Can you guess what CloCoP stands for?

This library is currently in development and not finished, but I thought I'd put some info about it for others who happen to stumble across this repo.

###Usage

There is no easily addable Lein dependency yet.

###Sample code

If you're curious, here's some sample code based on the API I have so far.

    (use 'clocop.core
         'clocop.constraints)
    
    ; Variable X is between 1 and 2. Variable Y is between 3 and 4.
    ; We know that X + Y = 6. What are X and Y?
    (with-store (store)           ; initialize the variable store
      (let [x (int-var "x" 1 2)
            y (int-var "y" 3 4)]  ; initialize the variables
        (constrain! ($= ($+ x y) 6))   ; specify x + y = 6
        (solve!)))                ; searches for a solution

    => {"x" 2, "y" 4}

###But what about core.logic?

I'm aware of core.logic, but there are a few ways in which, in my opinion, JaCoP is better than MiniKanren:

+ JaCoP is more "plug-in-able," with an extensive set of customizations to the way that the search operates. There are interfaces for different components of the search, and each have several implementations.
+ I found that with core.logic, I was somewhat limited by the set of available constraints. JaCoP has many different global and FD constraints that seem to more suit my needs for solving challenging problems.
+ [As the core.logic people say,](https://github.com/clojure/core.logic/wiki/External-solvers) JaCoP is anywhere from 10X-100X faster than core.logic at solving Finite Domain problems.

Constraint Programming
======

Here is a very brief guide to the key components in CP. Understanding CP a little more will help you use CloCoP and other CP libraries.

###The Store

The store has one job and one job only: keep track of all the variables.
Constraints, searches, and variables themselves do the hard work of actually solving the problems.

One way to think about it is that it's a box for all the variables, with several constraints attached to that box.

In CloCoP, a store is created with <code>(store)</code>. To create variables and constrain constraints, you must wrap those function calls in the following macro:

    (with-store <my-store>
      (...))

so that the variables and constraints know which store you're talking about.
###Variables

Variables can only have integer values. Every variable is assigned a "domain," i.e. a finite set of integers it could possibly be.
In JaCoP, initial domains must be assigned to variables at the time of creation.

###Constraints

A constraint can be anywhere from "X = 3" to "(X + Y = 3 v Y != 4) => (Z = Y * 2)".
It has two jobs:
- On command, check if it is still feasible (e.g. in the X = 3 example, it will check if 3 is in the domain of X)
- On command, "prune" the domains of the variables in question (e.g. in the X = 3 example, it will remove any values in the domain of X that is not 3).

###The Search

The search is essentially just a depth-first search, except it always asks the constraints to prune as much as it can before branching out on different possibilities for variable values.

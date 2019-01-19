# [JOpt](http://www.eecs.harvard.edu/jopt/)

A simple wrapper for linear and quadratic mixed integer program solvers.

## What is [JOpt](http://www.eecs.harvard.edu/jopt/)?

[JOpt](http://www.eecs.harvard.edu/jopt/) is an open source Java
wrapper that provides objects like **Variable**, **Constraint**, and
**Term** and lets you express your linear or mixed integer programs in
a natural manner, while remaining agnostic to the details of the
solver backend. [JOpt](http://www.eecs.harvard.edu/jopt/) is **not a
solver**. Rather, it requires a solver such as
[Cplex](https://www-01.ibm.com/software/commerce/optimization/cplex-optimizer/)
or the free [LPSolve](http://lpsolve.sourceforge.net/5.5/) to operate.

This means that any program written with
[JOpt](http://www.eecs.harvard.edu/jopt/) **will work for any of
serveral solvers** used by the end user.</p>

## Why [JOpt](http://www.eecs.harvard.edu/jopt/)?

 * You do linear/quadratic or mixed integer programming, but want
   to think in terms of simple variables and constraints, not a
   complex solver-specific api.
 * You want to automatically distribute and load balance your
   problems to one or more solver machines (when compiled for
   this support).

## Features

 * Simple interface
 * Agnostic of underlying solver
 * Exposes various solver parameters directly
 * Sophisticated Threading support
 * Support for providing solving hints
 * Support for the IIS functionality
   of [Cplex](https://www-01.ibm.com/software/commerce/optimization/cplex-optimizer/") useful in debugging
 * Support for multiple solutions (solution pools) under CPlex.
 * Support for MIQCQP under CPlex
 * Client/server support with load balancing (when compiled for it)
 
 ## Usage
 JOpt is released on Maven Central: https://mvnrepository.com/artifact/edu.harvard.eecs/jopt
 
 To use it, just include it in your Maven / Gradle dependencies.
 
 Alternatively, if you don't use Maven nor Gradle, you can also simply download the newest version's JAR and include it in your project.
 The JAR is published along with the [release](https://github.com/blubin/JOpt/releases/).

## Limitations

 * The <a href="http://www.gurobi.com/">Gurobi</a> solver has not
   currently been integrated, though this would be straightforward to
   do (contributions welcome).

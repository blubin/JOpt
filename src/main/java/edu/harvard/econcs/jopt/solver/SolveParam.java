/*
 * Copyright (c) 2005-2017 Benjamin Lubin
 * Copyright (c) 2005-2017 The President and Fellows of Harvard College
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * - Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.harvard.econcs.jopt.solver;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * These parameters control how JOpt operates, and how the backend solver
 * operates. You can control a number of parameters. Not all of these will be
 * supported by your chosen backend solver, unfortunently. But we try to map
 * these settings into relevant solver settings. See each setting to see if they
 * are applicable to CPLEX, LPSOLVE, or ALLSOLVERS
 *
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.17 $ on $Date: 2010/12/03 20:48:23 $
 * @since Apr 19, 2005
 **/
public class SolveParam implements Serializable {


    // ENUMS go here:
    // ///////////////

    /**
     * Measure timing using CPU=1, wall clock=2 [CPLEX]
     **/
    public static final SolveParam CLOCK_TYPE = new SolveParam(0, Integer.class, "ClockType");

    /**
     * Maximum time to run solver before returning current best in seconds
     * [CPLEX]
     **/
    public static final SolveParam TIME_LIMIT = new SolveParam(1, Double.class, "TimeLimit");


    /**
     * Amount of barrier progress information to be displayed [CPLEX]
     **/
    public static final SolveParam BARRIER_DISPLAY = new SolveParam(2, Integer.class, "BarrierDisplay");

    /**
     * Min objective value for maximization problems (specified to assist
     * cutting). Set this too high --> No Feasible Solution [CPLEX]
     **/
    public static final SolveParam MIN_OBJ_VALUE = new SolveParam(3, Double.class, "MinObjValue");

    /**
     * Max objective value for minimization problems (specified to assist
     * cutting). Set this too low --> No Feasible Solution [CPLEX]
     **/
    public static final SolveParam MAX_OBJ_VALUE = new SolveParam(4, Double.class, "MaxObjValue");

    /**
     * Optimality Tolerance [CPLEX]
     **/
    public static final SolveParam OBJ_TOLERANCE = new SolveParam(5, Double.class, "ObjTolerance");

    /**
     * Optimization stops when current solution within this parameter of LP
     * relaxiation [CPLEX]
     **/
    public static final SolveParam ABSOLUTE_OBJ_GAP = new SolveParam(6, Double.class, "AbsoluteObjGap");

    /**
     * Optimization stops when current solution within this percentage of LP
     * relaxiation [CPLEX]
     **/
    public static final SolveParam RELATIVE_OBJ_GAP = new SolveParam(7, Double.class, "RelativeObjGap");

    /**
     * How close a double must be to an int to be considered an int [CPLEX]
     **/
    public static final SolveParam ABSOLUTE_INT_GAP = new SolveParam(8, Double.class, "AbsoluteIntGap");

    /**
     * Degree to which variables may violate their bounds [CPLEX]
     **/
    public static final SolveParam ABSOLUTE_VAR_BOUND_GAP = new SolveParam(9, Double.class, "AbsoluteVarBoundGap");

    /**
     * Which optimization algorithm to use. [CPLEX]
     **/
    public static final SolveParam LP_OPTIMIZATION_ALG = new SolveParam(10, Integer.class, "LPOptimizationAlg");

    /**
     * Specify what to display when solving a MIP [CPLEX]
     * <p>
     * <p>
     * From the Cplex 8.1 Documentation (see that documentation for better
     * context):
     * <p>
     * <p>
     * 0 No display<br>
     * 1 Display integer feasible solutions<br>
     * 2 Display nodes under CPX_PARAM_MIPInteger.classERVAL<br>
     * 3 Same as 2 with information on node cuts<br>
     * 4 Same as 3 with LP subproblem information at root<br>
     * 5 Same as 4 with LP subproblem information at nodes<br>
     * Default: 2<br>
     * Description: MIP node log display information.<br>
     * Determines what CPLEX reports to the screen during mixed integer
     * optimization. The amount of information displayed increases with
     * increasing values of this parameter. A setting of 0 causes no node log to
     * be displayed until the optimal solution is found. A setting of 1 displays
     * an entry for each integer feasible solution found. Each entry contains
     * the objective function value, the node count, the number of unexplored
     * nodes in the tree, and the current optimality gap. A setting of 2 also
     * generates an entry for every nth node (where n is the setting of the MIP
     * Integer.classERVAL parameter). A setting of 3 additionally generates an entry for
     * every nth node giving the number of cuts added to the problem for the
     * previous Integer.classERVAL nodes. A setting of 4 additionally generates entries
     * for the LP root relaxation according to the 'SET SIMPLEX DISPLAY'
     * setting. A setting of 5 additionally generates entries for the LP
     * subproblems, also according to the 'SET SIMPLEX DISPLAY' setting.
     **/
    public static final SolveParam MIP_DISPLAY = new SolveParam(11, Integer.class, "MIPDisplay");

    /**
     * Optimality vs. Feasibility heuristic for MIP solving. 0=balanced,
     * 1=feasibility, 2=optimality, 3=moving best bound. [CPLEX]
     **/
    public static final SolveParam MIP_EMPHASIS = new SolveParam(12, Integer.class, "MIPEmphasis");

    /**
     * Check starting values for feasibility? [CPLEX]
     **/
    public static final SolveParam CHECK_INIT_VALUE_FEASIBILITY = new SolveParam(13, Boolean.class, "CheckInitValueFeasibility");

    /**
     * For Minimization problems, stop when this value has been exceeded [CPLEX]
     **/
    public static final SolveParam MIN_OBJ_THRESHOLD = new SolveParam(14, Double.class, "MinObjThreshold");

    /**
     * For Maximization problems, stop when this value has been exceeded [CPLEX]
     **/
    public static final SolveParam MAX_OBJ_THRESHOLD = new SolveParam(15, Double.class, "MaxObjThreshold");

    /**
     * Working directory for working files for the optimzer [CPLEX]
     **/
    public static final SolveParam WORK_DIR = new SolveParam(16, String.class, "WorkDir");

    /**
     * Threads to use in solving a MIP [CPLEX]
     **/
    public static final SolveParam THREADS = new SolveParam(17, Integer.class, "Threads");

    public static final SolveParam PARALLEL_MODE = new SolveParam(18, Integer.class, "ParallelMode");

    /**
     * Set the Markov Tolerance [CPLEX]</br> Influences pivot selection during
     * basis factoring. Increasing the Markowitz threshold may improve the
     * numerical properties of the solution.</br> Value: Any number from 0.0001
     * to 0.99999; default: 0.01.
     **/
    public static final SolveParam MARKOWITZ_TOLERANCE = new SolveParam(19, Double.class, "MarkowitzTolerance");
    /**
     * Controls the trade-off between the number of solutions generated for the
     * solution pool and the amount of time or memory consumed. This parameter
     * applies both to MIP optimization and to the populate procedure. Values
     * from 1 (one) to 4 invoke increasing effort to find larger numbers of
     * solutions. Higher values are more expensive in terms of time and memory
     * but are likely to yield more solutions.
     */
    public static final SolveParam SOLUTION_POOL_INTENSITY = new SolveParam(20, Integer.class, "SolutionPoolIntensity");
    /**
     * Number of intermediate solutions to capture. 0 turns of populate.
     * Defaults to 0.
     **/
    public static final SolveParam SOLUTION_POOL_CAPACITY = new SolveParam(21, Integer.class, "SolutionPoolCapacity");
    /**
     * Sets the maximum number of mixed integer programming (MIP) solutions
     * generated for the solution pool during each call to the populate
     * procedure. Populate stops when it has generated PopulateLim solutions. A
     * solution is counted if it is valid for all filters, consistent with the
     * relative and absolute pool gap parameters, and has not been rejected by
     * the incumbent callback (if any exists), whether or not it improves the
     * objective of the model. In parallel, populate may not respect this
     * parameter exactly due to disparities between threads. That is, it may
     * happen that populate stops when it has generated a number of solutions
     * slightly more than or slightly less than this limit because of
     * differences in synchronization between threads.
     */
    public static final SolveParam POPULATE_LIMIT = new SolveParam(22, Integer.class, "PopulateLimit");
    /**
     * Designates the strategy for replacing a solution in the solution pool
     * when the solution pool has reached its capacity. The value 0
     * (CPX_SOLNPOOL_FIFO ) replaces solutions according to a first-in,
     * first-out policy. The value 1 (CPX_SOLNPOOL_OBJ ) keeps the solutions
     * with the best objective values. The value 2 (CPX_SOLNPOOL_DIV ) replaces
     * solutions in order to build a set of diverse solutions. If the solutions
     * you obtain are too similar to each other, try setting SolnPoolReplace to
     * 2. The replacement strategy applies only to the subset of solutions
     * created in the current call of MIP optimization or populate. Solutions
     * already in the pool are not affected by the replacement strategy. They
     * will not be replaced, even if they satisfy the criterion of the
     * replacement strategy.
     */
    public static final SolveParam SOLUTION_POOL_REPLACEMENT = new SolveParam(23, Integer.class, "SolutionPoolReplacementStrategy");
    // Internal variables
    // ///////////////////
    /**
     * Name of mipInstance file - set to empty string if you don't want to write
     * one out [CPLEX]
     **/
    public static final SolveParam PROBLEM_FILE = new SolveParam(101, String.class, "ProblemFile", true);

    /**
     * If given a set of proposed starting values, should we set missing values
     * to be zero [CPLEX]
     **/
    public static final SolveParam ZERO_MISSING_PROPOSED = new SolveParam(102, Boolean.class, "ZeroMissingProposed", true);

    /**
     * Maximum amount to backoff and retry solving mip with looser constraint
     * tolerance. CPlex max is .1, .1 is default. typically either .1 or 0
     **/
    public static final SolveParam CONSTRAINT_BACKOFF_LIMIT = new SolveParam(103, Double.class, "ConstraintBackoffLimit", true);

    /**
     * Should we attempt to determine conflcit set?
     **/
    public static final SolveParam CALCULATE_CONFLICT_SET = new SolveParam(104, Boolean.class, "CalculateConflictSet", true);

    /**
     * Display output?
     **/
    public static final SolveParam DISPLAY_OUTPUT = new SolveParam(105, Boolean.class, "DisplayOutput", true);

    /**
     * Calculate duals?
     **/
    public static final SolveParam CALC_DUALS = new SolveParam(106, Boolean.class, "CalcDuals", true);
    /**
     * 0 (default) is none 1 is callback 2 is solution pool
     */
    public static final SolveParam SOLUTION_POOL_MODE = new SolveParam(107, Integer.class, "SolPoolMode", true);
    /**
     * How much of the time it took to solve the original problem should be
     * spend on populating the solution pool. If missing or negative than
     * populate is not limited
     */
    public static final SolveParam RELATIVE_POOL_SOLVE_TIME = new SolveParam(108, Double.class, "SolPoolRelSolveTime", true);

    /**
     * Flag whether the user wants to accept a suboptimal solution after timeout or have an Exception thrown.
     */
    public static final SolveParam ACCEPT_SUBOPTIMAL = new SolveParam(109, Boolean.class, "AcceptSuboptimal", true);

    // Other stuff below:
    // //////////////////

    private static final long serialVersionUID = 200505191821l;

    private int enumUID;
    private String name;
    private Class type;

    /**
     * if set, isInternal signifies that this is a JOpt parameter; doesn't
     * control solver.
     */
    private boolean isInternal;

    private SolveParam(int enumUID, Class type, String name, boolean isInternal) {
        this.enumUID = enumUID;
        this.name = name;
        this.type = type;
        this.isInternal = isInternal;
    }

    private SolveParam(int enumUID, Class type, String name) {
        this(enumUID, type, name, false);
    }

    public Class getType() {
        return type;
    }

    /**
     * Make serialization work.
     **/
    private Object readResolve() throws ObjectStreamException {
        switch (enumUID) {
            case 0:
                return CLOCK_TYPE;
            case 1:
                return TIME_LIMIT;
            case 2:
                return BARRIER_DISPLAY;
            case 3:
                return MIN_OBJ_VALUE;
            case 4:
                return MAX_OBJ_VALUE;
            case 5:
                return OBJ_TOLERANCE;
            case 6:
                return ABSOLUTE_OBJ_GAP;
            case 7:
                return RELATIVE_OBJ_GAP;
            case 8:
                return ABSOLUTE_INT_GAP;
            case 9:
                return ABSOLUTE_VAR_BOUND_GAP;
            case 10:
                return LP_OPTIMIZATION_ALG;
            case 11:
                return MIP_DISPLAY;
            case 12:
                return MIP_EMPHASIS;
            case 13:
                return CHECK_INIT_VALUE_FEASIBILITY;
            case 14:
                return MIN_OBJ_THRESHOLD;
            case 15:
                return MAX_OBJ_THRESHOLD;
            case 16:
                return WORK_DIR;
            case 17:
                return THREADS;
            case 18:
                return PARALLEL_MODE;
            case 19:
                return MARKOWITZ_TOLERANCE;
            case 20:
                return SOLUTION_POOL_INTENSITY;
            case 21:
                return SOLUTION_POOL_CAPACITY;
            case 22:
                return POPULATE_LIMIT;
            case 23:
                return SOLUTION_POOL_REPLACEMENT;
            case 101:
                return PROBLEM_FILE;
            case 102:
                return ZERO_MISSING_PROPOSED;
            case 103:
                return CONSTRAINT_BACKOFF_LIMIT;
            case 104:
                return CALCULATE_CONFLICT_SET;
            case 105:
                return DISPLAY_OUTPUT;
            case 106:
                return CALC_DUALS;
            case 107:
                return SOLUTION_POOL_MODE;
            case 108:
                return RELATIVE_POOL_SOLVE_TIME;
            case 109:
                return ACCEPT_SUBOPTIMAL;

        }
        throw new InvalidObjectException("Unknown enum: " + enumUID);
    }

    public String toString() {
        return name;
    }

    public boolean isInteger() {
        return type.equals(Integer.class);
    }

    public boolean isDouble() {
        return type.equals(Double.class);
    }

    public boolean isBoolean() {
        return type.equals(Boolean.class);
    }

    public boolean isString() {
        return type.equals(String.class);
    }

    public boolean isInternal() {
        return isInternal == true;
    }

    public String getTypeDescription() {
        return type.getSimpleName();
    }

    public boolean equals(Object obj) {
        return obj != null && obj.getClass().equals(SolveParam.class) && ((SolveParam) obj).enumUID == enumUID;
    }

    public int hashCode() {
        return enumUID;
    }

}

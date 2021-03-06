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
package edu.harvard.econcs.jopt.solver.server.cplex;

import edu.harvard.econcs.jopt.solver.*;
import edu.harvard.econcs.jopt.solver.MIPInfeasibleException.Cause;
import edu.harvard.econcs.jopt.solver.mip.*;
import edu.harvard.econcs.jopt.solver.server.SolverServer;
import ilog.concert.*;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Solves a MIP structure. Should be the only class using CPlex code directly.
 *
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.39 $ on $Date: 2013/12/04 02:18:20 $
 * @since Apr 2004
 **/
public class CPlexMIPSolver implements IMIPSolver {

    private static final Logger logger = LogManager.getLogger(CPlexMIPSolver.class);

    // private static final boolean debug = true;
    // private static final String fileName = "mipInstance";

    public IMIPResult solve(IMIP mip) throws MIPException {
        IloCplex cplex = CPLEXInstanceManager.INSTANCE.checkOutCplex();
        try {
            // This blocks until one can be obtained:
            while (cplex.getObjective() != null) {
                CPLEXInstanceManager.INSTANCE.checkInCplex(cplex);
                logger.warn("Model not cleared");
                cplex = CPLEXInstanceManager.INSTANCE.checkOutCplex();
            }

            logger.debug("About to set parameters... ");

            setControlParams(cplex, mip.getSpecifiedSolveParams(), mip::getSolveParam);

            // Log only if DISPLAY_OUTPUT was set to true or debug logging mode is enabled
            if (mip.getBooleanSolveParam(SolveParam.DISPLAY_OUTPUT, false)) {
                cplex.setOut(System.out);
            } else {
                cplex.setOut(null);
            }

            // cplex.setParam(IloCplex.DoubleParam.EpInt,
            // 1.0/(MIP.MAX_VALUE*1.0-1));
            // cplex.setParam(IloCplex.DoubleParam.EpRHS,
            // 1.0/(MIP.MAX_VALUE*1.0-1));

            long convertStartTime = System.currentTimeMillis();
            logger.debug("Starting to convert mip to Cplex object.");

            Map<String, IloNumVar> vars = setupVariables(mip, cplex);
            Map<Constraint, IloRange> constraintsToIloConstraints = setupConstraints(mip, cplex, vars);

            setUpObjective(mip, cplex, vars);

            logger.debug("Converting mip done. Took: " + (System.currentTimeMillis() - convertStartTime) + " ms");

            // write model to file for debugging:
            // ///////////////////////////////////

            String fileName = mip.getStringSolveParam(SolveParam.PROBLEM_FILE, new String("mipInstance"));
            if (!fileName.equals("")) {
                cplex.exportModel(/* "" + */fileName + ".lp");// + ".txt");
            }

            return solveMip(mip, cplex, vars, constraintsToIloConstraints);
        } catch (IloException e) {
            if (mip.getBooleanSolveParam(SolveParam.DISPLAY_OUTPUT, true)) {
                e.printStackTrace();
            }
            throw new MIPException("Cplex Exception: " + e.toString());
        } catch (RuntimeException ex) {
            if (mip.getBooleanSolveParam(SolveParam.DISPLAY_OUTPUT, true)) {
                ex.printStackTrace();
            }
            throw ex;
        } finally {
            CPLEXInstanceManager.INSTANCE.checkInCplex(cplex);
        }
    }

    private IMIPResult solveMip(IMIP mip, IloCplex cplex, Map<String, IloNumVar> vars, Map<Constraint, IloRange> constraintsToIloConstraints) throws IloException {

        // Solve MIP and extract results:
        // ///////////////////////////////
        Map<Constraint, Double> constraintidsToDuals = null;

        logger.info("Starting to solve mip.");
        long startTime = System.currentTimeMillis();
        double startTicks = cplex.getDetTime();
        long solveTime = 0;
        double objValue = 0;
        double bestObjValue = 0;
        double relGap = 0;
        double absGap = 0;
        Map<String, Double> values = new HashMap<>();
        boolean done = false;
        IntermediateSolutionGatherer solutionListener = null;
        Queue<PoolSolution> poolSolutions = null;
        if (mip.getIntSolveParam(SolveParam.SOLUTION_POOL_MODE, 0) == 1) {
            // If we will be using solution pool mode 1, prepare the solution listener
            solutionListener = new IntermediateSolutionGatherer(vars, mip.getIntSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 0));
            cplex.use(solutionListener);
        }
        while (!done) {

            if (cplex.solve()) {
                // Extract variable values from result
                for (String varName : mip.getVars().keySet()) {
                    if (mip.getVar(varName).ignore()) {
                        logger.debug("Skipping Variable " + varName);
                        continue;
                    }
                    IloNumVar numVar = vars.get(varName);
                    logger.debug(numVar + " : " + varName);
                    try {
                        if (numVar.getType().equals(IloNumVarType.Float)) {
                            values.put(varName, cplex.getValue(numVar));
                        } else {
                            values.put(varName, (double) (int) Math.round(cplex.getValue(numVar)));
                        }
                    } catch (Exception e) {
                        // This 'cause of a strange exception we're
                        // seeing...
                        e.printStackTrace();
                        throw new MIPException("Exception talking to CPLEX: " + e.getMessage() + "\n\n Occured while processing variable: " + numVar
                                + "\n\nThis exception usually occurs because you have inserted a variable in your LP/MIP"
                                + "\nthat never shows up in a constraint. (CPLEX wonders why you declared the variable"
                                + "\nin the first place.) To see if this is the case, before running your solve, consider"
                                + "\ninserting the line: System.out.println(yourMip.toString()); As a warning, a mip.toString"
                                + "\ncan be very slow with large problems, and in general, you should avoid making this call"
                                + "\nif you are concerned about performance. If, for some reason, you WANT to insert variables"
                                + "\nthat aren't bounded, you at least need to explicitly throw in a constraint bounding the"
                                + "\nvariable by -infinity and infinity -- but this is more likely a bug in your user code.");
                    }
                    logger.debug("var " + varName + ": " + values.get(varName));
                }

                objValue = cplex.getObjValue();
                logger.debug("obj: " + objValue);
                bestObjValue = cplex.getBestObjValue();
                relGap = cplex.getMIPRelativeGap();
                absGap = Math.abs(bestObjValue - objValue);
                // Handle the pool if requested. Mode 3 is handled a bit differently and should run even if there is only
                // one solution requested.
                if (cplex.isMIP()
                        && (mip.getIntSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 0) > 1)
                            || mip.getIntSolveParam(SolveParam.SOLUTION_POOL_MODE, 0) == 3) {
                    // Solution pool mode 2: Just use the regular populate() feature from CPLEX
                    if (mip.getIntSolveParam(SolveParam.SOLUTION_POOL_MODE, 0) == 2) {
                        long cplexSolveEnd = System.currentTimeMillis();
                        double cplexSolveEndTicks = cplex.getDetTime();
                        double originalSolveLimit = cplex.getParam(DoubleParam.TimeLimit);
                        double originalDetSolveLimit = cplex.getParam(DoubleParam.DetTimeLimit);
                        double populateFactor = mip.getDoubleSolveParam(SolveParam.RELATIVE_POOL_SOLVE_TIME, -1);
                        if (populateFactor >= 0) {
                            double populateSolveLimit = populateFactor * (cplexSolveEnd - startTime) / 1000d;
                            cplex.setParam(DoubleParam.TimeLimit, populateSolveLimit);
                            double detPopulateSolveLimit = populateFactor * (cplexSolveEndTicks - startTicks);
                            cplex.setParam(DoubleParam.DetTimeLimit, detPopulateSolveLimit);
                        }
                        cplex.populate();
                        cplex.setParam(DoubleParam.TimeLimit, originalSolveLimit);
                        cplex.setParam(DoubleParam.DetTimeLimit, originalDetSolveLimit);
                    // Solution pool mode 3: Re-solve the MIP while forbidding previous solutions with constraints
                    } else if (mip.getIntSolveParam(SolveParam.SOLUTION_POOL_MODE, 0) == 3) {
                        poolSolutions = new LinkedList<>();
                        PoolSolution optimal = new PoolSolution(objValue, bestObjValue, values);
                        optimal.setPoolGaps(objValue);
                        poolSolutions.add(optimal); // Add optimal solution first
                        List<Collection<Variable>> listOfCollections = new ArrayList<>(mip.getAdvancedVariablesOfInterest());
                        if (listOfCollections.isEmpty()) {
                            throw new MIPException("Please specify a collection of boolean variables "
                                    + "that can be used to distinguish different solutions.");
                        }
                        IMIP copyOfMip = mip.typedClone();
                        // Reset the solution pool such that we can solve sequentially
                        copyOfMip.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 0);
                        Set<Variable> zVars = new HashSet<>();
                        Set<Variable> variables1 = new HashSet<>();
                        Set<Variable> variables0 = new HashSet<>();
                        for (int i = 0; i < listOfCollections.size(); i++) {
                            Constraint zDef = new Constraint(CompareType.EQ, 0);
                            copyOfMip.add(zDef);
                            double sum = 0;
                            for (Variable var : listOfCollections.get(i)) {
                                if (var.getType() != VarType.BOOLEAN) {
                                    throw new MIPException("Currently, only boolean variables can be used to distinguish "
                                            + "different solutions.");
                                }
                                if (!mip.containsVar(var)) {
                                    throw new MIPException("MIP does not contain Variable " + var + ".");
                                }
                                sum += optimal.getValue(var);
                                zDef.addTerm(1, var);
                            }
                            Variable z = new Variable("z_for_solution_pool_" + i, VarType.BOOLEAN, 0, 1);
                            copyOfMip.add(z);
                            zVars.add(z);
                            zDef.addTerm(-1, z);
                            if (sum > 1 + 1e8) {
                                throw new MIPException("Currently, only variable sets that have a maximum of 1 variable set to 1 are supported.");
                            } else if (sum < 1 + 1e-8 && sum > 1 - 1e-8) {
                                variables1.add(z);
                            } else {
                                variables0.add(z);
                            }
                        }

                        if (variables1.size() + variables0.size() != listOfCollections.size()) {
                            throw new MIPException("Some variables got lost on the way...");
                        }

                        for (int solutionCount = 1; solutionCount < mip.getIntSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 0); solutionCount++) {
                            Variable y = new Variable("y_for_solution_pool_" + solutionCount, VarType.BOOLEAN, 0, 1);
                            copyOfMip.add(y);
                            Constraint ones = new Constraint(CompareType.LEQ, variables1.size() - 1 + 1e-8);
                            variables1.forEach(v -> ones.addTerm(1, v));
                            ones.addTerm(-MIP.MAX_VALUE, y);
                            copyOfMip.add(ones);

                            Constraint zeroes = new Constraint(CompareType.GEQ, 1 - 1e-8 - MIP.MAX_VALUE);
                            variables0.forEach(v -> zeroes.addTerm(1, v));
                            zeroes.addTerm(-MIP.MAX_VALUE, y);
                            copyOfMip.add(zeroes);

                            IMIPResult poolSolution = solve(copyOfMip);
                            Map<String, Double> poolValues = new HashMap<>();
                            for (String name : vars.keySet()) {
                                poolValues.put(name, poolSolution.getValue(name));
                            }
                            PoolSolution sol = new PoolSolution(poolSolution.getObjectiveValue(), bestObjValue, poolValues);
                            sol.setPoolGaps(optimal.getObjectiveValue());
                            poolSolutions.add(sol);

                            variables1 = zVars.stream().filter(v -> poolSolution.getValue(v) <= 1.1 && poolSolution.getValue(v) >= 0.9).collect(Collectors.toSet());
                            variables0 = zVars.stream().filter(v -> poolSolution.getValue(v) <= 0.1 && poolSolution.getValue(v) >= -0.1).collect(Collectors.toSet());

                            if (variables1.size() + variables0.size() != listOfCollections.size()) {
                                throw new MIPException("Some variables got lost on the way...");
                            }
                        }
                    // Solution pool mode 4: Use CPLEX features and parameters to fill the pool while reusing the previous search tree
                    } else if (mip.getIntSolveParam(SolveParam.SOLUTION_POOL_MODE, 0) == 4) {
                        if (mip.getIntSolveParam(SolveParam.SOLUTION_POOL_INTENSITY, -1) > -1
                            || mip.getIntSolveParam(SolveParam.SOLUTION_POOL_REPLACEMENT, -1) > -1
                            || mip.getIntSolveParam(SolveParam.POPULATE_LIMIT, -1) > -1) {
                            logger.info("Solution pool mode 4: This overrides any user-defined settings " +
                                    "of the parameters SOLUTION_POOL_INTENSITY, SOLUTION_POOL_REPLACEMENT, " +
                                    "and POPULATE_LIMIT.");
                        }

                        double poolTimeLimit = mip.getDoubleSolveParam(SolveParam.SOLUTION_POOL_MODE_4_TIME_LIMIT, -1);
                        if (mip.getDoubleSolveParam(SolveParam.TIME_LIMIT, -1) > -1 && poolTimeLimit < 0) {
                            logger.info("You defined a time limit of {}s for this run. " +
                                    "To populate the solution pool, the same time limit is used. " +
                                    "If you'd like to have a different time limit for populating the solution pool, " +
                                    "set the SOLUTION_POOL_MODE_4_TIME_LIMIT parameter accordingly.",
                                    mip.getDoubleSolveParam(SolveParam.TIME_LIMIT));
                            poolTimeLimit = mip.getDoubleSolveParam(SolveParam.TIME_LIMIT);
                        } else if (poolTimeLimit < 0) {
                            poolTimeLimit = 1e75;
                        }

                        double detPoolTimeLimit = mip.getDoubleSolveParam(SolveParam.SOLUTION_POOL_MODE_4_DETERMINISTIC_TIME_LIMIT, -1);
                        if (mip.getDoubleSolveParam(SolveParam.DETERMINISTIC_TIME_LIMIT, -1) > -1 && detPoolTimeLimit < 0) {
                            logger.info("You defined a deterministic time limit of {}s for this run. " +
                                            "To populate the solution pool, the same deterministic time limit is used. " +
                                            "If you'd like to have a different deterministic time limit for populating the solution pool, " +
                                            "set the SOLUTION_POOL_MODE_4_DETERMINISTIC_TIME_LIMIT parameter accordingly.",
                                    mip.getDoubleSolveParam(SolveParam.DETERMINISTIC_TIME_LIMIT));
                            detPoolTimeLimit = mip.getDoubleSolveParam(SolveParam.DETERMINISTIC_TIME_LIMIT);
                        } else if (detPoolTimeLimit < 0) {
                            detPoolTimeLimit = 1e75;
                        }

                        double solutionPoolMultiplier = mip.getDoubleSolveParam(SolveParam.SOLUTION_POOL_MODE_4_MULTIPLIER, 2d);
                        int finalSolutionPoolCapacity = mip.getIntSolveParam(SolveParam.SOLUTION_POOL_CAPACITY);
                        cplex.setParam(IntParam.SolnPoolCapacity, 2100000000);
                        cplex.setParam(IloCplex.Param.MIP.Pool.Capacity, 2100000000);
                        cplex.setParam(IntParam.SolnPoolIntensity, 4);
                        cplex.setParam(IloCplex.Param.MIP.Pool.Intensity, 4);
                        cplex.setParam(IntParam.SolnPoolReplace, 1);
                        cplex.setParam(IloCplex.Param.MIP.Pool.Replace, 1);
                        cplex.setParam(IntParam.PopulateLim, finalSolutionPoolCapacity);
                        cplex.setParam(IloCplex.Param.MIP.Limits.Populate, finalSolutionPoolCapacity);
                        logger.debug("Calling populate()");
                        cplex.populate();
                        IloCplex.CplexStatus status = cplex.getCplexStatus();
                        logger.debug("Initial Status: {}", status);

                        double absoluteSolutionPoolGapTolerance = mip.getDoubleSolveParam(SolveParam.SOLUTION_POOL_MODE_4_ABSOLUTE_GAP_TOLERANCE, 0);
                        double relativeSolutionPoolGapTolerance = mip.getDoubleSolveParam(SolveParam.SOLUTION_POOL_MODE_4_RELATIVE_GAP_TOLERANCE, 0);
                        if (objValue == 0 && relativeSolutionPoolGapTolerance > 0) {
                            logger.warn("You have set a relative solution pool gap tolerance, but the best " +
                                    "solution has an objective value of zero. A relative tolerance will not work in " +
                                    "that case, you must rely on the absolute gap tolerance.");
                        }
                        int count = 0;
                        double absSolPoolGap = 1e75;
                        double relSolPoolGap = 1e75;
                        long startTimeOfSolutionPool = System.currentTimeMillis();
                        double startTicksOfSolutionPool = cplex.getDetTime();
                        while (IloCplex.CplexStatus.PopulateSolLim.equals(status)
                                || IloCplex.CplexStatus.AbortTimeLim.equals(status)
                                || CplexStatus.AbortDetTimeLim.equals(status)) {

                            if (System.currentTimeMillis() - startTimeOfSolutionPool > poolTimeLimit * 1000) {
                                logger.info("Early termination after {} iterations of filling the solution pool: " +
                                        "Time limit reached.", count);
                                break;
                            }
                            if (cplex.getDetTime() - startTicksOfSolutionPool > detPoolTimeLimit) {
                                logger.info("Early termination after {} iterations of filling the solution pool: " +
                                        "Deterministic time limit reached.", count);
                                break;
                            }
                            if (absoluteSolutionPoolGapTolerance > absSolPoolGap) {
                                logger.info("Early termination after {} iterations of filling the solution pool: " +
                                        "Absolute solution pool gap is within tolerance.", count);
                                break;
                            }
                            if (relativeSolutionPoolGapTolerance > relSolPoolGap) {
                                logger.info("Early termination after {} iterations of filling the solution pool: " +
                                        "Relative solution pool gap is within tolerance.", count);
                                break;
                            }

                            logger.debug("Start of round {}.", count + 1);
                            printPool(cplex);
                            clearDuplicates(mip, vars, cplex);
                            logger.debug("After clearing duplicates in round {}.", count + 1);
                            printPool(cplex);
                            truncatePool(mip, cplex);
                            logger.debug("After truncating pool in round {}.", count + 1);
                            printPool(cplex);

                            // If solutionPoolSize < solutionPoolCapacity, skip the absGap setting
                            if (cplex.getSolnPoolNsolns() >= finalSolutionPoolCapacity) {
                                absSolPoolGap = getAbsoluteSolutionPoolGap(cplex);
                                relSolPoolGap = absSolPoolGap / (1e-10 + Math.abs(objValue));
                                logger.debug("Setting the absolute solution pool gap to {} in round {}.", absSolPoolGap, count + 1);
                                cplex.setParam(DoubleParam.SolnPoolAGap, absSolPoolGap);
                                cplex.setParam(IloCplex.Param.MIP.Pool.AbsGap, absSolPoolGap);
                            }
                            int popLim = (int) (solutionPoolMultiplier * finalSolutionPoolCapacity);
                            cplex.setParam(IntParam.PopulateLim, popLim);
                            cplex.setParam(IloCplex.Param.MIP.Limits.Populate, popLim);
                            logger.debug("Calling populate()");
                            cplex.populate();
                            status = cplex.getCplexStatus();
                            logger.debug("Status: {}", status);

                            count++;
                        }
                        clearDuplicates(mip, vars, cplex);
                        truncatePool(mip, cplex);
                        logger.debug("Pool filled. Made {} refinement(s).", count);

                        if (!CplexStatus.OptimalPopulated.equals(status)
                                && !CplexStatus.OptimalPopulatedTol.equals(status)) {
                            if (IloCplex.CplexStatus.PopulateSolLim.equals(status)
                                    || CplexStatus.AbortTimeLim.equals(status)
                                    || CplexStatus.AbortDetTimeLim.equals(status)) {
                                logger.info("Filling the solution pool terminated early due to the user settings " +
                                        "(time limit or absolute/relative solution pool gap). " +
                                        "Note that this does not guarantee that you have the k best solutions.");
                            } else {
                                logger.warn("Final status ({}) is not what was expected. " +
                                        "Maybe something went wrong.", status);
                            }
                        }
                    }
                }

                done = true;

                long endTime = System.currentTimeMillis();
                solveTime = endTime - startTime;
                logger.info("Solve time: " + solveTime + " ms");
                if (!cplex.isMIP() && mip.getBooleanSolveParam(SolveParam.CALC_DUALS, false)) {
                    constraintidsToDuals = new HashMap<>();
                    for (Constraint constraint : constraintsToIloConstraints.keySet()) {
                        IloConstraint iloConstraint = constraintsToIloConstraints.get(constraint);
                        if (iloConstraint instanceof IloRange) {
                            IloRange iloRange = (IloRange) iloConstraint;
                            constraintidsToDuals.put(constraint, new Double(cplex.getDual(iloRange)));
                        }
                    }
                }

                IloCplex.Status optStatus = cplex.getStatus();
                logger.debug("CPlex solution status: " + optStatus);
            } else {
                // Solve returned an error.
                IloCplex.Status optStatus = cplex.getStatus();
                logger.info("CPlex solve failed status: " + optStatus);
                if (optStatus == IloCplex.Status.Infeasible || optStatus == IloCplex.Status.InfeasibleOrUnbounded) {
                    Object cplexParam = getCplexParam(SolveParam.ABSOLUTE_VAR_BOUND_GAP);
                    double dval = cplex.getParam((IloCplex.DoubleParam) cplexParam) * 10;
                    if (dval > mip.getDoubleSolveParam(SolveParam.CONSTRAINT_BACKOFF_LIMIT)) {
                        MIPException e = createInfesibilityException(cplex, vars, constraintsToIloConstraints, mip);
                        throw e;
                    } else {
                        logger.warn("No feasible Solution. Resolving with looser tolerance: " + dval);
                        cplex.setParam((IloCplex.DoubleParam) cplexParam, dval);
                        cplexParam = getCplexParam(SolveParam.ABSOLUTE_INT_GAP);
                        cplex.setParam((IloCplex.DoubleParam) cplexParam, dval);
                    }
                } else {
                    IloCplex.CplexStatus cplexStatus = cplex.getCplexStatus();
                    throw new MIPException("Solve failed with status: " + optStatus + " (CplexStatus: " + cplexStatus + "), no conflict calculation possible");
                }
            }
        }

        if (cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim || cplex.getCplexStatus() == IloCplex.CplexStatus.AbortDetTimeLim) {
            if (mip.getBooleanSolveParam(SolveParam.ACCEPT_SUBOPTIMAL, true)) {
                logger.warn("Suboptimal solution! Continuing... To reject suboptimal solutions," +
                        "set SolveParam.ACCEPT_SUBOPTIMAL to false.");
            } else {
                throw new MIPException("Solving the MIP timed out, delivering only a suboptimal solution.\n" +
                        "Due to user preferences, an exception is thrown. To accept suboptimal solutions after a timeout,\n" +
                        "set SolveParam.ACCEPT_SUBOPTIMAL to true.");
            }
        }

        // In solution pool mode 3, we filled the pool manually and don't extract any pool solutions from the CPLEX result
        if (mip.getIntSolveParam(SolveParam.SOLUTION_POOL_MODE, 0) != 3) {
            poolSolutions = solutionListener != null ? solutionListener.solutions : new LinkedList<>();
            poolSolutions.addAll(findPoolSolutions(cplex, vars, mip.getIntSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 0)));
            for (PoolSolution poolSolution : poolSolutions) {
                poolSolution.setPoolGaps(objValue);
            }
        }

        MIPResult res = new MIPResult(objValue, values, constraintidsToDuals);
        res.setPoolSolutions(poolSolutions);
        res.setSolveTime(solveTime);
        res.setRelativeGap(relGap);
        res.setAbsoluteGap(absGap);
        return res;
    }

    private void printPool(IloCplex cplex) {
        if (logger.isDebugEnabled()) {
            List<Double> objectives = new ArrayList<>();
            try {
                for (int i = 0; i < cplex.getSolnPoolNsolns(); i++) {
                    objectives.add(cplex.getObjValue(i));
                }
                objectives.sort(Double::compareTo);
                logger.debug("Pool:\n{}", objectives);
            } catch (IloException e) {
                logger.error("Couldn't print the solution pool.", e);
            }
        }
    }

    private void truncatePool(IMIP mip, IloCplex cplex) {
        try {
            int poolSize = cplex.getSolnPoolNsolns();
            int requestedSolutions = mip.getIntSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 0);
            logger.debug("Starting to truncate pool from {} to {} solutions.", poolSize, requestedSolutions);
            if (poolSize <= requestedSolutions) {
                return;
            }
            Map<Integer, Double> indexObjectiveMap = new HashMap<>();
            for (int i = 0; i < poolSize; i++) {
                indexObjectiveMap.put(i, cplex.getObjValue(i));
            }
            List<Integer> truncatedSolutions = indexObjectiveMap.entrySet().stream()
                    .sorted((o1, o2) -> {
                        if (mip.isObjectiveMin()) return Double.compare(o2.getValue(), o1.getValue());
                        else return Double.compare(o1.getValue(), o2.getValue());
                    }) // Reverse order
                    .map(Map.Entry::getKey)
                    .limit(poolSize - requestedSolutions)
                    .sorted((o1, o2) -> Integer.compare(o2, o1))
                    .collect(Collectors.toList());

            for (int i : truncatedSolutions) {
                cplex.delSolnPoolSolns(i, 1);
            }
            logger.debug("Done truncating.");
        } catch (IloException e) {
            throw new MIPException("Couldn't truncate the solution pool.", e);
        }

    }

    private void clearDuplicates(IMIP mip, Map<String, IloNumVar> vars, IloCplex cplex) {
        if (mip.getAdvancedVariablesOfInterest() == null) return;
        List<Integer> duplicateSolutions = new ArrayList<>();
        try {
            int solutionPoolSize = cplex.getSolnPoolNsolns();
            logger.debug("Starting to clear duplicates in a pool of size {}.", solutionPoolSize);
            for (int i = 0; i < solutionPoolSize - 1; ++i) {
                if (!duplicateSolutions.contains(i)) {
                    PoolSolution iSol = extractSolution(cplex, vars, i);
                    for (int j = i + 1; j < solutionPoolSize; ++j) {
                        if (!duplicateSolutions.contains(j)) {
                            PoolSolution jSol = extractSolution(cplex, vars, j);
                            if (iSol.isDuplicateAdvanced(jSol, mip.getAdvancedVariablesOfInterest())) {
                                duplicateSolutions.add(j);
                            }
                        }
                    }
                }
            }
            duplicateSolutions.sort((o1, o2) -> Integer.compare(o2, o1)); // Reverse order
            for (int i : duplicateSolutions) {
                cplex.delSolnPoolSolns(i, 1);
            }
            logger.debug("Done clearing duplicates.");
        } catch (IloException e) {
            throw new MIPException("Couldn't delete duplicates from solution pool.");
        }
    }


    private double getAbsoluteSolutionPoolGap(IloCplex cplex) throws IloException {
        /*
         * Find the minimal and maximal value in the solution pool.
         * The minimal value is the optimal solution, the maximum value
         * is the worst of the currently k best solutions. We can set the
         * absolute solution pool gap to the difference of the maximum
         * and minimum solution, that will make CPLEX stop prematurely in
         * case it can prove that no better solutions can be found
         */
        double min = MIP.MAX_VALUE;
        double max = -MIP.MAX_VALUE;
        for (int solutionNumber = 0; solutionNumber < cplex.getSolnPoolNsolns(); ++solutionNumber) {
            double obj = cplex.getObjValue(solutionNumber);
            if (obj < min) min = obj;
            if (obj > max) max = obj;
        }
        return max - min + 1e-6;
    }

    private Queue<PoolSolution> findPoolSolutions(IloCplex cplex, Map<String, IloNumVar> vars, int capacity) throws IloException {
        LinkedList<PoolSolution> poolSolutions = new LinkedList<>();
        int solnPoolNsolns = cplex.getSolnPoolNsolns();
        logger.debug("Found {} pool solutions", solnPoolNsolns);
        for (int solutionNumber = 0; solutionNumber < cplex.getSolnPoolNsolns(); ++solutionNumber) {
            poolSolutions.add(extractSolution(cplex, vars, solutionNumber));
        }
        return poolSolutions.stream()
                .sorted(Comparator.comparingDouble(PoolSolution::getObjectiveValue))
                .limit(capacity)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public void exportToDisk(IMIP mip, Path path) {
        IloCplex cplex = CPLEXInstanceManager.INSTANCE.checkOutCplex();
        try {
            setControlParams(cplex, mip.getSpecifiedSolveParams(), mip::getSolveParam);
            Map<String, IloNumVar> vars = setupVariables(mip, cplex);
            setupConstraints(mip, cplex, vars);

            setUpObjective(mip, cplex, vars);
            cplex.exportModel(path.toString());
        } catch (Exception ex) {
            logger.error("Failed to write cplex model to disk", ex);
        } finally {
            CPLEXInstanceManager.INSTANCE.checkInCplex(cplex);
        }

    }

    private Map<String, IloNumVar> setupVariables(IMIP mip, IloCplex cplex) throws IloException {
        // Setup Variables:
        // ////////////////

        Map<String, IloNumVar> vars = new HashMap<>(); // varName to IloNumVar
        int numberOfBooleanAndIntVariables = 0;

        IloNumVarType varType = IloNumVarType.Float;
        for (Variable var : mip.getVars().values()) {
            if (var.ignore()) {
                logger.debug("Skipping variable: " + var);
                continue;
            }
            logger.debug("Adding variable: " + var);
            VarType type = var.getType();
            if (VarType.DOUBLE.equals(type)) {
                varType = IloNumVarType.Float;
            } else if (VarType.INT.equals(type)) {
                varType = IloNumVarType.Int;
                numberOfBooleanAndIntVariables++;
            } else if (VarType.BOOLEAN.equals(type)) {
                varType = IloNumVarType.Bool;
                numberOfBooleanAndIntVariables++;
            }
            IloNumVar numVar = cplex.numVar(var.getLowerBound(), var.getUpperBound(), varType, var.getName());
            vars.put(var.getName(), numVar);
        }
        cplex.add(vars.values().toArray(new IloNumVar[vars.size()]));
        // Propose Values, if any:
        // ///////////////////////
        proposeValues(mip, cplex, vars, numberOfBooleanAndIntVariables);
        return vars;
    }

    private void proposeValues(IMIP mip, IloCplex cplex, Map<String, IloNumVar> vars, int numberOfBooleanAndIntVariables) throws IloException {
        if (!mip.getVarsWithProposedValues().isEmpty()) {
            IloNumVar[] varArray;
            double[] valArray;
            boolean bZeroMissingVariables = mip.getBooleanSolveParam(SolveParam.ZERO_MISSING_PROPOSED, false);
            Iterator iter;

            if (bZeroMissingVariables) {
                // In this case, prepare to zero out all missing Int/Bools
                varArray = new IloNumVar[mip.getVars().size()];
                valArray = new double[mip.getVars().size()];
                iter = mip.getVars().values().iterator();
            } else {
                // In this case, at most mip.getVarsWithProposedValues()
                // will be set.
                varArray = new IloNumVar[mip.getVarsWithProposedValues().size()];
                valArray = new double[mip.getVarsWithProposedValues().size()];
                iter = mip.getVarsWithProposedValues().iterator();
            }

            int i = 0;
            int numberOfProposedBooleanAndIntVariables = 0;
            Variable v;
            VarType type;

            while (iter.hasNext()) {
                v = (Variable) iter.next();
                type = v.getType();
                varArray[i] = vars.get(v.getName());
                if ((!bZeroMissingVariables) || (mip.getVarsWithProposedValues().contains(v))) {
                    if (type == VarType.BOOLEAN) {
                        valArray[i] = mip.getProposedBooleanValue(v) ? 1 : 0;
                        numberOfProposedBooleanAndIntVariables++;
                    } else if (type == VarType.INT) {
                        valArray[i] = mip.getProposedIntValue(v);
                        numberOfProposedBooleanAndIntVariables++;
                    } else if (type == VarType.DOUBLE) {
                        valArray[i] = mip.getProposedDoubleValue(v);
                    }

                    logger.debug("proposing value: " + v.getName() + "\t" + valArray[i]);
                } else {
                    valArray[i] = 0;
                    if ((type == VarType.BOOLEAN) || (type == VarType.INT)) {
                        numberOfProposedBooleanAndIntVariables++;
                    }
                }
                i++;
            }

            if (numberOfProposedBooleanAndIntVariables != numberOfBooleanAndIntVariables) {
                throw new MIPException("Proposing Values: Total and Proposed Boolean and Int Variables not equal: proposition won't be feasible.\n"
                        + "numberOfBooleanAndIntVariables, numberOfProposedBooleanAndIntVariables: " + numberOfBooleanAndIntVariables + ", "
                        + numberOfProposedBooleanAndIntVariables);
                /*
                 * logger.warn(
                 * "Proposing Values: Total and Proposed Boolean and Int Variables not equal: proposition won't be feasible."
                 * ); logger.warn(
                 * "Proposing Values: numberOfBooleanAndIntVariables, numberOfProposedBooleanAndIntVariables: "
                 * + numberOfBooleanAndIntVariables + ", " +
                 * numberOfProposedBooleanAndIntVariables);
                 */
                // See "SetVectors" at
                // http://www.eecs.harvard.edu/~jeffsh/_cplexdoc/javadocman/html/
            }

            logger.debug("Using primed start values for solving.");
            if (cplex.isMIP()) {
                cplex.addMIPStart(varArray, valArray);
            } else {
                cplex.setStart(valArray, null, varArray, null, null, null);
            }
            // AdvInd was BooleanParam.MIPStart prior to CPLEX 10.
            // 0=no MIPStart, 1=MIPStart, including partial starts.
            cplex.setParam(IloCplex.IntParam.AdvInd, 1);
        }
    }

    private void setUpObjective(IMIP mip, IloCplex cplex, Map<String, IloNumVar> vars) throws IloException {
        // Setup Objective:
        // ////////////////

        // Add linear objective terms:
        IloLinearNumExpr linearObjFunc = cplex.linearNumExpr();
        int linearObjTermsUsed = 0;
        for (LinearTerm term : mip.getLinearObjectiveTerms()) {
            if (mip.getVar(term.getVarName()).ignore()) {
                logger.debug("Skipping term: " + term);
                continue;
            }
            linearObjTermsUsed++;
            IloNumVar v = vars.get(term.getVarName());
            linearObjFunc.addTerm(term.getCoefficient(), v);
        }

        // Add quadratic objective terms:
        IloQuadNumExpr quadObjFunc = cplex.quadNumExpr();
        int quadraticObjTermsUsed = 0;
        for (QuadraticTerm term : mip.getQuadraticObjectiveTerms()) {
            if (mip.getVar(term.getVarNameA()).ignore() || mip.getVar(term.getVarNameB()).ignore()) {
                logger.debug("Skipping term: " + term);
                continue;
            }
            quadraticObjTermsUsed++;
            IloNumVar varA = vars.get(term.getVarNameA());
            IloNumVar varB = vars.get(term.getVarNameB());
            quadObjFunc.addTerm(term.getCoefficient(), varA, varB);
        }

        // Now make a single expression from the above two if needed:
        IloNumExpr numObjFunc = null;
        if (linearObjTermsUsed == 0 && quadraticObjTermsUsed == 0) {
            throw new MIPException("Objective must have at least one term.");
        } else if (quadraticObjTermsUsed == 0) {
            numObjFunc = linearObjFunc;
        } else if (linearObjTermsUsed == 0) {
            numObjFunc = quadObjFunc;
        } else {
            IloLQNumExpr lqexpr = cplex.lqNumExpr();
            lqexpr.add(linearObjFunc);
            lqexpr.add(quadObjFunc);
            numObjFunc = lqexpr;
        }

        logger.debug("Objective: " + numObjFunc);

        if (mip.isObjectiveMin()) {
            cplex.addMinimize(numObjFunc);
        } else {
            cplex.addMaximize(numObjFunc);
        }
    }

    private Map<Constraint, IloRange> setupConstraints(IMIP mip, IloCplex cplex, Map<String, IloNumVar> vars) throws IloException {
        // Setup Constraints:
        // ///////////////////
        Map<Constraint, IloRange> constraintidsToConstraints = new HashMap<>();
        for (Constraint constraint : mip.getConstraints()) {

            logger.debug("Adding constraint: " + constraint);

            // Add Linear Terms:
            int linearTermsUsed = 0;
            IloLinearNumExpr linearExpr = cplex.linearNumExpr();
            for (LinearTerm term : constraint.getLinearTerms()) {
                Variable var = mip.getVar(term.getVarName());
                if (var == null) {
                    throw new MIPException("Invalid variable name in term: " + term);
                }
                if (var.ignore()) {
                    logger.debug("Skipping term: " + term);
                    continue;
                }
                linearTermsUsed++;
                linearExpr.addTerm(term.getCoefficient(), vars.get(term.getVarName()));
            }

            // Add Quadratic Terms:
            int quadraticTermsUsed = 0;
            IloQuadNumExpr quadExpr = cplex.quadNumExpr();
            for (QuadraticTerm term : constraint.getQuadraticTerms()) {
                Variable varA = mip.getVar(term.getVarNameA());
                if (varA == null) {
                    throw new MIPException("Invalid variable name in term: " + term);
                }
                Variable varB = mip.getVar(term.getVarNameB());
                if (varB == null) {
                    throw new MIPException("Invalid variable name in term: " + term);
                }
                if (varA.ignore() || varB.ignore()) {
                    logger.debug("Skipping term: " + term);
                    continue;
                }
                quadraticTermsUsed++;
                quadExpr.addTerm(term.getCoefficient(), vars.get(term.getVarNameA()), vars.get(term.getVarNameB()));
            }

            // Now make a single constraint from the above two if needed:
            IloNumExpr numExpr = null;
            if (linearTermsUsed == 0 && quadraticTermsUsed == 0) {
                logger.debug("Skipping constraint" + constraint);
                continue;
            } else if (quadraticTermsUsed == 0) {
                numExpr = linearExpr;
            } else if (linearTermsUsed == 0) {
                numExpr = quadExpr;
            } else {
                IloLQNumExpr lqexpr = cplex.lqNumExpr();
                lqexpr.add(linearExpr);
                lqexpr.add(quadExpr);
                numExpr = lqexpr;
            }

            // Use the name description for the name, if available.
            String name = constraint.getDescription();

            // Add to the MIP, including the comparison and constant:
            CompareType type = constraint.getType();
            IloRange iloRange = null;
            if (CompareType.EQ.equals(type)) {

                iloRange = cplex.eq(numExpr, constraint.getConstant(), name);
            } else if (CompareType.LEQ.equals(type)) {
                iloRange = cplex.le(numExpr, constraint.getConstant(), name);
            } else if (CompareType.GEQ.equals(type)) {
                iloRange = cplex.ge(numExpr, constraint.getConstant(), name);
            } else {
                throw new MIPException("Invalid constraint type: " + type);
            }
            constraintidsToConstraints.put(constraint, iloRange);
        }
        IloRange[] constraints = constraintidsToConstraints.values().toArray(new IloRange[constraintidsToConstraints.size()]);
        cplex.add(constraints);
        return constraintidsToConstraints;
    }

    private MIPException createInfesibilityException(IloCplex cplex, Map<String, IloNumVar> vars, Map<Constraint, IloRange> constraintsToIloConstraints, IMIP mip) {
        if (!mip.getBooleanSolveParam(SolveParam.CALCULATE_CONFLICT_SET, true)) {
            throw new MIPInfeasibleException("MIP Infeasible: set CALCULATE_CONFLICT_SET to obtain a refined conflict set");
        }

        Map<IloNumVarBound, Variable> vConstToVars = new HashMap<>(vars.size());
        for (String varName : vars.keySet()) {
            IloNumVar numVar = vars.get(varName);
            Variable mipVar = mip.getVar(varName);
            if (mipVar.getType() != VarType.BOOLEAN) {
                // only include non-boolean variables
                vConstToVars.put(cplex.lowerBound(numVar), mipVar);
                vConstToVars.put(cplex.upperBound(numVar), mipVar);
            }
        }
        Map<IloConstraint, Constraint> iloConstraintsToConstraints = new HashMap<IloConstraint, Constraint>(constraintsToIloConstraints.size());
        for (Constraint constraint : constraintsToIloConstraints.keySet()) {
            IloConstraint iloConst = constraintsToIloConstraints.get(constraint);
            iloConstraintsToConstraints.put(iloConst, constraint);
        }

        // Now build the full array:
        ArrayList<IloConstraint> full = new ArrayList<>(vConstToVars.size() + constraintsToIloConstraints.size());
        full.addAll(vConstToVars.keySet());
        full.addAll(constraintsToIloConstraints.values());

        IloConstraint[] arr = full.toArray(new IloConstraint[full.size()]);
        double prefs[] = new double[arr.length];
        Arrays.fill(prefs, 1);

        try {
            if (!cplex.refineConflict(arr, prefs)) {
                throw new MIPInfeasibleException("Could not refine conflict");
            }
            Map<Variable, Cause> mipVarsToCauses = new HashMap<>();
            for (IloConstraint iloConst : full) {
                ConflictStatus cStat = cplex.getConflict(iloConst);
                if (cStat == ConflictStatus.Member || cStat == ConflictStatus.PossibleMember) {
                    Variable v = vConstToVars.get(iloConst);
                    if (v != null) {
                        IloNumVarBound b = (IloNumVarBound) iloConst;
                        mipVarsToCauses.put(v, getCause(b.getType()));
                    }

                } else {
                    iloConstraintsToConstraints.remove(iloConst);
                }
            }
            return new MIPInfeasibleException(mipVarsToCauses, iloConstraintsToConstraints.values());
        } catch (IloException e) {
            throw new MIPException("Solve failed but could not determine Conflict Set: " + e.toString(), e);
        }
    }

    private MIPInfeasibleException.Cause getCause(IloNumVarBoundType type) {
        if (IloNumVarBoundType.Lower.equals(type)) {
            return MIPInfeasibleException.Cause.LOWER;
        } else if (IloNumVarBoundType.Upper.equals(type)) {
            return MIPInfeasibleException.Cause.UPPER;
        }
        throw new MIPException("Invalid type: " + type);
    }

    public void setControlParams(IloCplex cplex, Iterable<SolveParam> params, Function<SolveParam, Object> getValue) {
        for (SolveParam solveParam : params) {
            Object value = getValue.apply(solveParam);
            if (!solveParam.isInternal()) {
                Object cplexParam = getCplexParam(solveParam);
                logger.debug("Setting " + solveParam.toString() + " to: " + value.toString());
                try {
                    if (solveParam.isBoolean()) {
                        cplex.setParam((BooleanParam) cplexParam, (Boolean) value);
                    } else if (solveParam.isInteger()) {
                        cplex.setParam((IntParam) cplexParam, (Integer) value);
                    } else if (solveParam.isDouble()) {
                        cplex.setParam((DoubleParam) cplexParam, (Double) value);
                    } else if (solveParam.isString()) {
                        cplex.setParam((StringParam) cplexParam, (String) value);
                    } else {
                        throw new MIPException("Invalid solver param time: " + value);
                    }
                } catch (IloException e) {
                    throw new MIPException(solveParam + ": " + value + ": " + e.toString());
                }
            }
        }
    }

    private PoolSolution extractSolution(IloCplex cplex, Map<String, IloNumVar> vars, int index) {
        try {
            Map<String, Double> poolValues = new HashMap<>();
            for (String name : vars.keySet()) {
                IloNumVar var = vars.get(name);
                poolValues.put(name, cplex.getValue(var, index));
            }
            return new PoolSolution(cplex.getObjValue(index), cplex.getBestObjValue(), poolValues);
        } catch (IloException e) {
            throw new MIPException("Couldn't extract solution.", e);
        }
    }

    public Object getCplexParam(SolveParam solveParam) {
        if (SolveParam.CLOCK_TYPE.equals(solveParam)) {
            return IloCplex.IntParam.ClockType;
        } else if (SolveParam.TIME_LIMIT.equals(solveParam)) {
            return IloCplex.DoubleParam.TimeLimit;
        } else if (SolveParam.BARRIER_DISPLAY.equals(solveParam)) {
            return IloCplex.IntParam.BarDisplay;
        } else if (SolveParam.MIN_OBJ_VALUE.equals(solveParam)) {
            return IloCplex.DoubleParam.CutLo;
        } else if (SolveParam.MAX_OBJ_VALUE.equals(solveParam)) {
            return IloCplex.DoubleParam.CutUp;
        } else if (SolveParam.OBJ_TOLERANCE.equals(solveParam)) {
            return IloCplex.DoubleParam.EpOpt;
        } else if (SolveParam.ABSOLUTE_OBJ_GAP.equals(solveParam)) {
            return IloCplex.DoubleParam.EpAGap;
        } else if (SolveParam.RELATIVE_OBJ_GAP.equals(solveParam)) {
            return IloCplex.DoubleParam.EpGap;
        } else if (SolveParam.ABSOLUTE_INT_GAP.equals(solveParam)) {
            return IloCplex.DoubleParam.EpInt;
        } else if (SolveParam.ABSOLUTE_VAR_BOUND_GAP.equals(solveParam)) {
            return IloCplex.DoubleParam.EpRHS;
        } else if (SolveParam.LP_OPTIMIZATION_ALG.equals(solveParam)) {
            return IloCplex.IntParam.RootAlg;
        } else if (SolveParam.MIP_DISPLAY.equals(solveParam)) {
            return IloCplex.IntParam.MIPDisplay;
        } else if (SolveParam.MIP_EMPHASIS.equals(solveParam)) {
            return IloCplex.IntParam.MIPEmphasis;
        } else if (SolveParam.CHECK_INIT_VALUE_FEASIBILITY.equals(solveParam)) {
            // prior to CPLEX 10: IloCplex.BooleanParam.MIPStart;
            return IloCplex.IntParam.AdvInd;
        } else if (SolveParam.MIN_OBJ_THRESHOLD.equals(solveParam)) {
            return IloCplex.DoubleParam.ObjLLim;
        } else if (SolveParam.MAX_OBJ_THRESHOLD.equals(solveParam)) {
            return IloCplex.DoubleParam.ObjULim;
        } else if (SolveParam.WORK_DIR.equals(solveParam)) {
            return IloCplex.StringParam.WorkDir;
        } else if (SolveParam.THREADS.equals(solveParam)) {
            return IloCplex.IntParam.Threads;
        } else if (SolveParam.PARALLEL_MODE.equals(solveParam)) {
            return IloCplex.IntParam.ParallelMode;
        } else if (SolveParam.MARKOWITZ_TOLERANCE.equals(solveParam)) {
            return IloCplex.DoubleParam.EpMrk;
        } else if (SolveParam.SOLUTION_POOL_INTENSITY.equals(solveParam)) {
            return IloCplex.IntParam.SolnPoolIntensity;
        } else if (SolveParam.SOLUTION_POOL_CAPACITY.equals(solveParam)) {
            return IloCplex.IntParam.SolnPoolCapacity;
        } else if (SolveParam.SOLUTION_POOL_REPLACEMENT.equals(solveParam)) {
            return IloCplex.IntParam.SolnPoolReplace;
        } else if (SolveParam.POPULATE_LIMIT.equals(solveParam)) {
            return IloCplex.IntParam.PopulateLim;
        } else if (SolveParam.DATACHECK.equals(solveParam)) {
            return IloCplex.Param.Read.DataCheck;
        } else if (SolveParam.OPTIMALITY_TARGET.equals(solveParam)) {
            return IloCplex.Param.OptimalityTarget;
        } else if (SolveParam.QTOLIN.equals(solveParam)) {
            return IloCplex.Param.Preprocessing.QToLin;
        } else if (SolveParam.DETERMINISTIC_TIME_LIMIT.equals(solveParam)) {
            return IloCplex.Param.DetTimeLimit;
        }
        throw new MIPException("Invalid solve param: " + solveParam);
    }

    /**
     * Gather up the intermediate solutions as they become available and store them as PoolSolutions.
     * Right now we just keep the latest n elements. Could do priority queue based on
     * objective, instead, depending on requirements.
     *
     * @author blubin
     */
    protected static class IntermediateSolutionGatherer extends IloCplex.MIPInfoCallback {

        /**
         * For now, we store in a linked list. This would be more efficient:
         * http://commons.apache.org/proper/commons-collections/javadocs/api-
         * release/index.html
         */
        private Queue<PoolSolution> solutions = new LinkedList<PoolSolution>();
        private int numIntermediateSolutions;
        private Map<String, IloNumVar> vars;

        public IntermediateSolutionGatherer(Map<String, IloNumVar> vars, int numIntermediateSolutions) {
            this.vars = vars;
            this.numIntermediateSolutions = numIntermediateSolutions;
        }

        @Override
        protected void main() throws IloException {
            if (hasIncumbent()) {
                solutions.add(createSolution());
                if (solutions.size() > numIntermediateSolutions) {
                    solutions.poll();
                }
            }
        }

        /**
         * Return the solution list, but first remove any entry that matches the
         * overall solution.
         */
        public Queue<PoolSolution> getSolutionList() {

            return solutions;
        }

        private PoolSolution createSolution() {
            Map<String, Double> values = new HashMap<String, Double>();
            for (String name : vars.keySet()) {
                IloNumVar var = vars.get(name);
                try {
                    values.put(name, getIncumbentValue(var));
                } catch (IloException e) {
                    throw new MIPException("Couldn't get incumbent value.", e);
                }
            }
            try {
                return new PoolSolution(getIncumbentObjValue(), getBestObjValue(), values);
            } catch (IloException e) {
                throw new MIPException("Couldn't get incumbent objective value.", e);
            }
        }
    }

    public static void main(String argv[]) {
        if (argv.length < 1 || argv.length > 2) {
            logger.error("Usage: edu.harvard.econcs.jopt.solver.server.cplex.CPlexMIPSolver <port> <num simultaneous>");
            System.exit(1);
        }
        int port = Integer.parseInt(argv[0]);
        int numSimultaneous = 10;
        if (argv.length >= 2) {
            numSimultaneous = Integer.parseInt(argv[1]);
        }
        InstanceManager.setNumSimultaneous(numSimultaneous);
        SolverServer.createServer(port, CPlexMIPSolver.class);
    }

}

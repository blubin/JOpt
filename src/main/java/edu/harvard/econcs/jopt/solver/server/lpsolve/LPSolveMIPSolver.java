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
package edu.harvard.econcs.jopt.solver.server.lpsolve;

import edu.harvard.econcs.jopt.solver.*;
import edu.harvard.econcs.jopt.solver.mip.*;
import edu.harvard.econcs.jopt.solver.server.SolverServer;
import edu.harvard.econcs.util.NativeUtils;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Class for solving MIPs based on the LPSolve solver.
 *
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.10 $ on $Date: 2013/12/04 02:18:20 $
 * @since Jan 4, 2005
 **/
public class LPSolveMIPSolver implements IMIPSolver {

    // private static Log log = new Log(LPSolveMIPSolver.class);
    // private static final String fileName = "mipInstance";
    private static final long TIME_LIMIT = 60000;
    private static boolean debug = false;

    static {
        try {
            System.loadLibrary("lpsolve55j");
        } catch (UnsatisfiedLinkError e) {
            System.out.print("No linked binary files of LPSolve found. Trying to provide them via tempDir... ");
            try {
                initLocalLpSolve();
                LpSolve.lpSolveVersion(); // A check if all links are in place
                System.out.println("Succeeded!");
            } catch (Exception ex) {
                System.err.println("Failed.");
                System.err.println("---------------------------------------------------\n" +
                        "Error encountered while trying to solve MIP with LPSolve:\n" +
                        "The native libraries were not found in the java library path," +
                        "and providing them via the tempDir failed as well.\n" +
                        "If you're sure you want to use LPSolve, follow these instructions to get it running:\n" +
                        "\t1.\tDownload the lp_solve_5.5.2.0_dev_* package that fits your platform from\n" +
                        "\t\t\thttps://sourceforge.net/projects/lpsolve/files/lpsolve/5.5.2.0/\n" +
                        "\t2.\tDownload the java interface for LPSolve from\n" +
                        "\t\t\thttps://sourceforge.net/projects/lpsolve/files/lpsolve/5.5.2.0/lp_solve_5.5.2.0_java.zip/download\n" +
                        "\t3.\tExtract both packages and place the dev-package (from 1.) anywhere where the\n" +
                        "\t\t\tPATH (-> Windows) or LD_LIBRARY_PATH (-> Unix) environment variable is pointing at\n" +
                        "\t\t\t(or make it point there).\n" +
                        "\t4.\tPlace the corresponding interface file (ending in j, e.g. 64_lpsolve55j.dll for Windows)\n" +
                        "\t\t\tfrom the /lib directory of the java interface (from 2.) into the same directory as the other package.\n" +
                        "\t5.\tRestart your IDE to freshly load the environment variables.\n" +
                        "---------------------------------------------------");
                throw e;
            }
        }
    }

    public static void main(String argv[]) {
        if (argv.length != 1) {
            System.err.println("Usage: edu.harvard.econcs.jopt.solver.server.cplex.LPSolveMIPSolver <port>");
            System.exit(1);
        }
        int port = Integer.parseInt(argv[0]);
        SolverServer.createServer(port, LPSolveMIPSolver.class);
    }

    private static void initLocalLpSolve() throws Exception {
        // Find or create the jopt-lib-lpsolve directory in temp
        File lpSolveTempDir = NativeUtils.createTempDir("jopt-lib-lpsolve");
        lpSolveTempDir.deleteOnExit();

        // Add this directory to the java library path
        NativeUtils.addLibraryPath(lpSolveTempDir.getAbsolutePath());

        // Add the right files to this directory
        if (SystemUtils.IS_OS_WINDOWS) {
            if (SystemUtils.OS_ARCH.contains("64")) {
                NativeUtils.loadLibraryFromJar("/lib/64_lpsolve55.dll", lpSolveTempDir);
                NativeUtils.loadLibraryFromJar("/lib/64_lpsolve55j.dll", lpSolveTempDir);
            } else {
                NativeUtils.loadLibraryFromJar("/lib/32_lpsolve55.dll", lpSolveTempDir);
                NativeUtils.loadLibraryFromJar("/lib/32_lpsolve55j.dll", lpSolveTempDir);
            }
        } else if (SystemUtils.IS_OS_UNIX) {
            if (SystemUtils.OS_ARCH.contains("64")) {
                NativeUtils.loadLibraryFromJar("/lib/64_liblpsolve55.so", lpSolveTempDir);
                NativeUtils.loadLibraryFromJar("/lib/64_liblpsolve55j.so", lpSolveTempDir);
            } else {
                NativeUtils.loadLibraryFromJar("/lib/32_liblpsolve55.so", lpSolveTempDir);
                NativeUtils.loadLibraryFromJar("/lib/32_liblpsolve55j.so", lpSolveTempDir);
            }
        }
    }

    public IMIPResult solve(IMIP mip) throws MIPException {
        try {
            Map<String, LinearTerm> objTerms = getObjTerms(mip);
            List<Variable> activeVars = getActiveVars(mip);
            // Create a problem with (constraints, rows):
            LpSolve solver = LpSolve.makeLp(0, activeVars.size());
            solver.setAddRowmode(true);
            solver.setTimeout(TIME_LIMIT);
            if (mip.isSolveParamSpecified(SolveParam.TIME_LIMIT)) {
                long timeout = (long) mip.getDoubleSolveParam(SolveParam.TIME_LIMIT);
                solver.setTimeout(timeout);
            }

            if (mip.isSolveParamSpecified(SolveParam.ABSOLUTE_OBJ_GAP)) {
                solver.setMipGap(true, mip.getDoubleSolveParam(SolveParam.ABSOLUTE_OBJ_GAP));
            }
            if (mip.isSolveParamSpecified(SolveParam.RELATIVE_OBJ_GAP)) {
                solver.setMipGap(false, mip.getDoubleSolveParam(SolveParam.RELATIVE_OBJ_GAP));
            }

            if (mip.isSolveParamSpecified(SolveParam.ABSOLUTE_VAR_BOUND_GAP)) {
                solver.setEpsb(mip.getDoubleSolveParam(SolveParam.ABSOLUTE_VAR_BOUND_GAP));
                solver.setEpsd(mip.getDoubleSolveParam(SolveParam.ABSOLUTE_VAR_BOUND_GAP));
                solver.setEpsel(mip.getDoubleSolveParam(SolveParam.ABSOLUTE_VAR_BOUND_GAP));
            }

            if (mip.isSolveParamSpecified(SolveParam.ABSOLUTE_INT_GAP)) {
                solver.setEpsint(mip.getDoubleSolveParam(SolveParam.ABSOLUTE_INT_GAP));
            }

            // define objective:

            if (mip.isObjectiveMax()) {
                solver.setMaxim();
            }
            if (mip.isObjectiveMin()) {
                solver.setMinim();
            }

            double[] obj = new double[activeVars.size() + 1];
            for (int i = 0; i < activeVars.size(); i++) {
                Variable v = activeVars.get(i);
                LinearTerm t = objTerms.get(v.getName());
                obj[i + 1] = t == null ? 0 : t.getCoefficient();
                solver.setColName(i + 1, v.getName());
            }
            solver.setObjFn(obj);

            // setup variables:
            for (int i = 0; i < activeVars.size(); i++) {
                Variable v = activeVars.get(i);
                VarType t = v.getType();
                if (t == VarType.BOOLEAN) {
                    solver.setBinary(i + 1, true);
                }
                if (t == VarType.DOUBLE) {
                    solver.setBounds(i + 1, v.getLowerBound(), v.getUpperBound());
                }
                if (t == VarType.INT) {
                    solver.setBounds(i + 1, v.getLowerBound(), v.getUpperBound());
                    solver.setInt(i + 1, true);
                }
            }

            // add constraints
            List<Constraint> constraints = mip.getConstraints();
            for (Constraint c : constraints) {
                List<LinearTerm> terms = getTerms(mip, activeVars, c);
                double[] row = new double[activeVars.size() + 1];
                for (int j = 0; j < activeVars.size(); j++) {
                    LinearTerm t = terms.get(j);
                    row[j + 1] = t == null ? 0 : t.getCoefficient();
                }
                solver.addConstraint(row, getType(c.getType()), c.getConstant());
                // solver.setRowName(i+1, c.toString());
            }
            solver.setAddRowmode(false);

            if (!debug) {
                solver.setVerbose(0);
            }

            // solver.setDebug(debug);

            // solve the problem
            int result = solver.solve();
            if (result == LpSolve.SUBOPTIMAL) {
                if (mip.getBooleanSolveParam(SolveParam.ACCEPT_SUBOPTIMAL, true)) {
                    System.out.println("Suboptimal solution! Continuing... To reject suboptimal solutions," +
                            "set SolveParam.ACCEPT_SUBOPTIMAL to false.");
                } else {
                    throw new MIPException("Solving the MIP timed out, delivering only a suboptimal solution.\n" +
                            "Due to user preferences, an exception is thrown. To accept suboptimal solutions after a timeout,\n" +
                            "set SolveParam.ACCEPT_SUBOPTIMAL to true.");
                }
            } else if (result != LpSolve.OPTIMAL) {
                String problem = solver.getStatustext(result);
                solver.deleteLp();
                throw new MIPInfeasibleException(problem);
            }

            if (debug) {
                // write out the formulation:
                // solver.writeLp(fileName);
                solver.printLp();
            }

            // Fill the results:
            Map<String, Double> values = new HashMap<>();
            double[] vars = solver.getPtrVariables();
            for (int i = 0; i < activeVars.size(); i++) {
                Variable v = activeVars.get(i);
                values.put(v.getName(), vars[i]);
            }
            Map<Constraint, Double> duals = null;
            if (mip.getBooleanSolveParam(SolveParam.CALC_DUALS, false)) {
                duals = new HashMap<>();
                double[] dualVars = solver.getPtrDualSolution();
                for (int i = 0; i < constraints.size(); i++) {
                    duals.put(mip.getConstraints().get(i), dualVars[i + 1]);
                }
            }

            MIPResult ret = new MIPResult(solver.getObjective(), values, duals);

            // print solution
            if (debug) {
                System.out.println("Value of objective function: " + solver.getObjective());
                double[] var = solver.getPtrVariables();
                for (int i = 0; i < var.length; i++) {
                    System.out.println("Value of var[" + i + "] = " + var[i]);
                }
            }

            // delete the problem and free memory
            solver.deleteLp();

            return ret;
        } catch (LpSolveException e) {
            throw new MIPException("Exception solving MIP: " + e);
        }
    }

    private List<Variable> getActiveVars(IMIP mip) {
        List<Variable> ret = new ArrayList<>(mip.getNumVars());
        for (Variable v : mip.getVars().values()) {
            if (!v.ignore()) {
                ret.add(v);
            }
        }
        return ret;
    }

    private Map<String, LinearTerm> getObjTerms(IMIP mip) {
        if (!mip.getQuadraticObjectiveTerms().isEmpty()) {
            throw new MIPException("MIP has quadratic terms, not supported by LPSolve");
        }
        Map<String, LinearTerm> ret = new HashMap<>();
        for (LinearTerm t : mip.getLinearObjectiveTerms()) {
            Variable v = mip.getVar(t.getVarName());
            if (!v.ignore()) {
                ret.put(t.getVarName(), t);
            }
        }
        return ret;
    }

    private List<LinearTerm> getTerms(IMIP mip, List<Variable> activeVars, Constraint c) {
        if (!c.getQuadraticTerms().isEmpty()) {
            throw new MIPException("Constraint has quadratic terms, not supported by LPSolve. " + c);
        }
        List<LinearTerm> ret = new ArrayList<>();
        Map<Variable, LinearTerm> varToTerm = new HashMap<>();
        for (LinearTerm t : c.getLinearTerms()) {
            if (varToTerm.containsKey(mip.getVar(t.getVarName()))) {
                varToTerm.put(mip.getVar(t.getVarName()),
                        new LinearTerm(varToTerm.get(mip.getVar(t.getVarName())).getCoefficient()
                                + t.getCoefficient(), mip.getVar(t.getVarName())));
            } else {
                varToTerm.put(mip.getVar(t.getVarName()), t);
            }
        }
        for (int i = 0; i < activeVars.size(); i++) {
            ret.add(varToTerm.get(activeVars.get(i)));
        }
        return ret;
    }

    private int getType(CompareType type) {
        if (type == CompareType.EQ) {
            return LpSolve.EQ;
        }
        if (type == CompareType.GEQ) {
            return LpSolve.GE;
        }
        if (type == CompareType.LEQ) {
            return LpSolve.LE;
        }
        throw new RuntimeException("Unknown type: " + type);
    }
}

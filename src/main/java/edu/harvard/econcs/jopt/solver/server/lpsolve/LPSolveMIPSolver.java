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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.IMIPSolver;
import edu.harvard.econcs.jopt.solver.MIPException;
import edu.harvard.econcs.jopt.solver.MIPInfeasibleException;
import edu.harvard.econcs.jopt.solver.SolveParam;
import edu.harvard.econcs.jopt.solver.mip.CompareType;
import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.MIPResult;
import edu.harvard.econcs.jopt.solver.mip.LinearTerm;
import edu.harvard.econcs.jopt.solver.mip.VarType;
import edu.harvard.econcs.jopt.solver.mip.Variable;
import edu.harvard.econcs.jopt.solver.server.SolverServer;

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

    {
        // Load the necessary System libraries:
        // java.library.path must point to lib directory...
        System.loadLibrary("lpsolve55");
        System.loadLibrary("lpsolve55j");
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
                solver.setTimeout((long) mip.getDoubleSolveParam(SolveParam.TIME_LIMIT));
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
            if (result != LpSolve.OPTIMAL) {
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
        List<Variable> ret = new ArrayList<Variable>(mip.getNumVars());
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
            varToTerm.put(mip.getVar(t.getVarName()), t);
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

    public static void main(String argv[]) {
        if (argv.length != 1) {
            System.err.println("Usage: edu.harvard.econcs.jopt.solver.server.cplex.LPSolveMIPSolver <port>");
            System.exit(1);
        }
        int port = Integer.parseInt(argv[0]);
        SolverServer.createServer(port, LPSolveMIPSolver.class);
    }
}

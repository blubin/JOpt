/*
 * Copyright (c) 2005
 *	The President and Fellows of Harvard College.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package edu.harvard.econcs.jopt.solver.server.cplex;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLQNumExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarBound;
import ilog.concert.IloNumVarBoundType;
import ilog.concert.IloNumVarType;
import ilog.concert.IloQuadNumExpr;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BooleanParam;
import ilog.cplex.IloCplex.ConflictStatus;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.StringParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.IMIPSolver;
import edu.harvard.econcs.jopt.solver.MIPException;
import edu.harvard.econcs.jopt.solver.MIPInfeasibleException;
import edu.harvard.econcs.jopt.solver.SolveParam;
import edu.harvard.econcs.jopt.solver.mip.CompareType;
import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.LinearTerm;
import edu.harvard.econcs.jopt.solver.mip.MIPResult;
import edu.harvard.econcs.jopt.solver.mip.QuadraticTerm;
import edu.harvard.econcs.jopt.solver.mip.VarType;
import edu.harvard.econcs.jopt.solver.mip.Variable;
import edu.harvard.econcs.jopt.solver.server.SolverServer;
import edu.harvard.econcs.util.Log;

/**
 * Solves a MIP structure. Should be the only class using CPlex code directly.
 * 
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.39 $ on $Date: 2013/12/04 02:18:20 $
 * @since Apr 2004
 **/
public class CPlexMIPSolver implements IMIPSolver {
	
	private static Log log = new Log(CPlexMIPSolver.class);
//	private static final boolean debug = true;
//	private static final String fileName = "mipInstance";
	
	public IMIPResult solve(IMIP mip) throws MIPException {
		IloCplex cplex = null; 
		try {
			//This blocks until one can be obtained:
			cplex= InstanceManager.getInstance().checkOutCplex();
			
			if (!mip.getBooleanSolveParam(SolveParam.DISPLAY_OUTPUT, true)) {
				cplex.setOut(null);
			}
			
			log.info("About to set parameters... " );
			
			setControlParams(cplex, mip);
			int numberOfBooleanAndIntVariables = 0;
			
			//cplex.setParam(IloCplex.DoubleParam.EpInt, 1.0/(MIP.MAX_VALUE*1.0-1));
			//cplex.setParam(IloCplex.DoubleParam.EpRHS, 1.0/(MIP.MAX_VALUE*1.0-1));
		
			long convertStartTime = System.currentTimeMillis();
			if (log.isInfoEnabled()) {
				log.info("Starting to convert mip to Cplex object.");
			}
			
			//Setup Variables:
			//////////////////
			
			Map vars = new HashMap();	// varName to IloNumVar
			IloNumVarType varType = IloNumVarType.Float; 
			Collection mipVars = mip.getVars().values();
			for (Iterator iter = mipVars.iterator(); iter.hasNext(); ) {
				Variable var = (Variable) iter.next();
				if (var.ignore()) {
					if (log.isDebugEnabled()) 
						log.debug("Skipping variable: " + var);
					continue;
				}
				if (log.isDebugEnabled()) 
					log.debug("Adding variable: " + var);
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
				vars.put(var.getName(), cplex.numVar(var.getLowerBound(), var.getUpperBound(), varType, var.getName()));
			}
			
			// Setup Constraints:
			/////////////////////
			
			Map constraintidsToConstraints = new HashMap();
			Map constraintidsToDuals = null;
			
			
			for (Constraint constraint : mip.getConstraints()) {
				if (log.isDebugEnabled()) 
					log.debug("Adding constraint: " + constraint);

				// Add Linear Terms:
				int linearTermsUsed = 0;
				IloLinearNumExpr linearExpr = cplex.linearNumExpr();
				for (LinearTerm term : constraint.getLinearTerms() ) {
					Variable var = mip.getVar(term.getVarName());
					if (var == null) {
						throw new MIPException("Invalid variable name in term: " + term);
					}
					if (var.ignore()) {
						if (log.isDebugEnabled()) 
							log.debug("Skipping term: " + term);
						continue;
					}
					linearTermsUsed++;
					linearExpr.addTerm(term.getCoefficient(), (IloNumVar) vars.get(term.getVarName()));
				}
				
				// Add Quadratic Terms:
				int quadraticTermsUsed = 0;
				IloQuadNumExpr quadExpr = cplex.quadNumExpr();
				for (QuadraticTerm term : constraint.getQuadraticTerms() ) {
					Variable varA = mip.getVar(term.getVarNameA());
					if (varA == null) {
						throw new MIPException("Invalid variable name in term: " + term);
					}
					Variable varB = mip.getVar(term.getVarNameB());
					if (varB == null) {
						throw new MIPException("Invalid variable name in term: " + term);
					}
					if (varA.ignore() || varB.ignore()) {
						if (log.isDebugEnabled()) 
							log.debug("Skipping term: " + term);
						continue;
					}
					quadraticTermsUsed++;
					quadExpr.addTerm(term.getCoefficient(), (IloNumVar) vars.get(term.getVarNameA()), (IloNumVar) vars.get(term.getVarNameB()));
				}

				// Now make a single constraint from the above two if needed:
				IloNumExpr numExpr = null;
				if (linearTermsUsed == 0 && quadraticTermsUsed == 0) {
					if (log.isDebugEnabled()) 
						log.debug("Skipping constraint" + constraint);
					continue;
				} else if(quadraticTermsUsed == 0) {
					numExpr = linearExpr;
				} else if(linearTermsUsed == 0) {
					numExpr = quadExpr;
				} else {
					IloLQNumExpr lqexpr = cplex.lqNumExpr();
					lqexpr.add(linearExpr);
					lqexpr.add(quadExpr);
					numExpr = lqexpr;
				}
				
				//Use the name description for the name, if available.
				String name = constraint.getDescription();
				
				//Add to the MIP, including the comparison and constant:
				CompareType type = constraint.getType();
				IloRange iloRange = null;
				if (CompareType.EQ.equals(type)) {
					iloRange = cplex.addEq(numExpr, constraint.getConstant(), name);
				} else if (CompareType.LEQ.equals(type)) {
					iloRange = cplex.addLe(numExpr, constraint.getConstant(), name);
				} else if (CompareType.GEQ.equals(type)) {
					iloRange = cplex.addGe(numExpr, constraint.getConstant(), name);
				} else {
					throw new MIPException("Invalid constraint type: " + type);
				}
				constraintidsToConstraints.put(new Integer(constraint.getId()), iloRange);
			}
			
			//Setup Objective:
			//////////////////

			//Add linear objective terms:
			IloLinearNumExpr linearObjFunc = cplex.linearNumExpr();
			int linearObjTermsUsed = 0;
			for (LinearTerm term : mip.getLinearObjectiveTerms()) {
				if (mip.getVar(term.getVarName()).ignore()) {
					if (log.isDebugEnabled()) 
						log.debug("Skipping term: " + term);
					continue;
				}
				linearObjTermsUsed++;
				IloNumVar v = (IloNumVar) vars.get(term.getVarName());
				linearObjFunc.addTerm(term.getCoefficient(), v);
			}
			
			//Add quadratic objective terms:
			IloQuadNumExpr quadObjFunc = cplex.quadNumExpr();
			int quadraticObjTermsUsed = 0;
			for (QuadraticTerm term : mip.getQuadraticObjectiveTerms()) {
				if (mip.getVar(term.getVarNameA()).ignore() || mip.getVar(term.getVarNameB()).ignore()) {
					if (log.isDebugEnabled()) 
						log.debug("Skipping term: " + term);
					continue;
				}
				quadraticObjTermsUsed++;
				IloNumVar varA = (IloNumVar) vars.get(term.getVarNameA());
				IloNumVar varB = (IloNumVar) vars.get(term.getVarNameB());
				quadObjFunc.addTerm(term.getCoefficient(), varA, varB);
			}

			// Now make a single expression from the above two if needed:
			IloNumExpr numObjFunc = null;
			if (linearObjTermsUsed == 0 && quadraticObjTermsUsed == 0) {
				throw new MIPException("Objective must have at least one term.");
			} else if(quadraticObjTermsUsed == 0) {
				numObjFunc = linearObjFunc;
			} else if(linearObjTermsUsed == 0) {
				numObjFunc = quadObjFunc;
			} else {
				IloLQNumExpr lqexpr = cplex.lqNumExpr();
				lqexpr.add(linearObjFunc);
				lqexpr.add(quadObjFunc);
				numObjFunc = lqexpr;
			}

			if (log.isDebugEnabled()) 
				log.debug("Objective: " + numObjFunc);
			
			if (mip.isObjectiveMin()) {
				cplex.addMinimize(numObjFunc);
			} else {
				cplex.addMaximize(numObjFunc);
			}
			
			if (log.isInfoEnabled()) {
				long convertEndTime = System.currentTimeMillis();
				log.info("Converting mip done. Took: " + (convertEndTime-convertStartTime) + " ms");
			}
			
			//Propose Values, if any:
			/////////////////////////
			
			if (!mip.getVarsWithProposedValues().isEmpty()) {
				IloNumVar varArray[];
				double valArray[];
				boolean bZeroMissingVariables = mip.getBooleanSolveParam(SolveParam.ZERO_MISSING_PROPOSED, new Boolean(false));
				Iterator iter;
				
				if (bZeroMissingVariables == true) {
					// In this case, prepare to zero out all missing Int/Bools
					varArray = new IloNumVar[mip.getVars().size()];
					valArray = new double[mip.getVars().size()];
					iter = mip.getVars().values().iterator();
				} else {
					// In this case, at most mip.getVarsWithProposedValues() will be set.
					varArray = new IloNumVar[mip.getVarsWithProposedValues().size()];
					valArray = new double[mip.getVarsWithProposedValues().size()];
					iter = mip.getVarsWithProposedValues().iterator();
				}
				
				int i=0;
				int numberOfProposedBooleanAndIntVariables = 0;
				Variable v;
				VarType type;
				
				for (; iter.hasNext(); ) {
					v = (Variable)iter.next();
					type = v.getType();
					varArray[i] = (IloNumVar)vars.get(v.getName());
					if ((!bZeroMissingVariables) || (mip.getVarsWithProposedValues().contains(v))) {						
						if (type == VarType.BOOLEAN) {
							valArray[i] = mip.getProposedBooleanValue(v)?1:0;
							numberOfProposedBooleanAndIntVariables++;
						} else if (type == VarType.INT) {
							valArray[i] = mip.getProposedIntValue(v);
							numberOfProposedBooleanAndIntVariables++;
						} else if (type == VarType.DOUBLE) {
							valArray[i] = mip.getProposedDoubleValue(v);
						}						
						
						if (log.isDebugEnabled()) {
							log.debug("proposing value: " + v.getName() + "\t" + valArray[i]);
						}
					} else {
						if (bZeroMissingVariables == true) {
							valArray[i] = 0;
							if ((type == VarType.BOOLEAN) || (type == VarType.INT)) {
								numberOfProposedBooleanAndIntVariables++;
							}
						}
					}					
					i++;					
				}
				
				if (numberOfProposedBooleanAndIntVariables != numberOfBooleanAndIntVariables) {
					throw new MIPException ("Proposing Values: Total and Proposed Boolean and Int Variables not equal: proposition won't be feasible.\n" +
							"numberOfBooleanAndIntVariables, numberOfProposedBooleanAndIntVariables: " + numberOfBooleanAndIntVariables + ", " + numberOfProposedBooleanAndIntVariables);					
/*					log.warn("Proposing Values: Total and Proposed Boolean and Int Variables not equal: proposition won't be feasible.");
					log.warn("Proposing Values: numberOfBooleanAndIntVariables, numberOfProposedBooleanAndIntVariables: " + numberOfBooleanAndIntVariables + ", " + numberOfProposedBooleanAndIntVariables);
*/					
					// See "SetVectors" at http://www.eecs.harvard.edu/~jeffsh/_cplexdoc/javadocman/html/
				}

				if (log.isInfoEnabled()) {
					log.info("Using primed start values for solving.");
				}
				cplex.setVectors(valArray, null, varArray, null, null, null);
				//AdvInd was BooleanParam.MIPStart prior to CPLEX 10.
				//0=no MIPStart, 1=MIPStart, including partial starts.
				cplex.setParam(IloCplex.IntParam.AdvInd, 1);
			}
			
			// write model to file for debugging:
			/////////////////////////////////////
			
			String fileName = mip.getStringSolveParam(SolveParam.PROBLEM_FILE, new String("mipInstance"));
            if (!fileName.equals("")) {
                cplex.exportModel(/*"" +*/ fileName + ".lp");// + ".txt");
            }

            // Solve MIP and extract results:
            /////////////////////////////////
            
            log.info("Starting to solve mip.");
            long startTime = System.currentTimeMillis();
			long solveTime = 0;
			double objValue = 0;
			Map values = new HashMap();
			boolean done = false;			
			while (!done) {
				
				if (cplex.solve()) {
				//if (solutionGoal==null ? cplex.solve() : cplex.solve(solutionGoal)) {
					done = true;
					
					long endTime = System.currentTimeMillis();
					solveTime = endTime-startTime;
					log.main("Solve time: " + solveTime + " ms");
					for (Iterator iter = mip.getVars().keySet().iterator(); iter.hasNext(); ) {
						String varName = (String ) iter.next();
						if (mip.getVar(varName).ignore()) {
							if (log.isDebugEnabled()) 
								log.debug("Skipping Variable " + varName);
							continue;
						}
						IloNumVar numVar = (IloNumVar) vars.get(varName);
						if (log.isDebugEnabled()) 
							log.debug(numVar+" : "+varName);
						try {
							if (numVar.getType().equals(IloNumVarType.Float)) {
								values.put(varName, new Double(cplex.getValue(numVar)));
							} else {
								values.put(varName, new Double((int)Math.round(cplex.getValue(numVar))));
							}
						} catch (Exception e) {
							//This 'cause of a strange exception we're seeing...
							e.printStackTrace();
							throw new MIPException("Exception talking to CPLEX: " + e.getMessage() +
									"\n\n Occured while processing variable: " + numVar +
									"\n\nThis exception usually occurs because you have inserted a variable in your LP/MIP" +
									"\nthat never shows up in a constraint. (CPLEX wonders why you declared the variable" + 
									"\nin the first place.) To see if this is the case, before running your solve, consider" +
									"\ninserting the line: System.out.println(yourMip.toString()); As a warning, a mip.toString" +
									"\ncan be very slow with large problems, and in general, you should avoid making this call" +
									"\nif you are concerned about performance. If, for some reason, you WANT to insert variables" + 
									"\nthat aren't bounded, you at least need to explicitly throw in a constraint bounding the" +
									"\nvariable by -infinity and infinity -- but this is more likely a bug in your user code.");
						}
						if (log.isDebugEnabled()) 
							log.debug("var " + varName + ": " + values.get(varName));
					}
					if (!cplex.isMIP() && mip.getBooleanSolveParam(SolveParam.CALC_DUALS, false)) {
						constraintidsToDuals = new HashMap();
						for (Iterator cIter = constraintidsToConstraints.keySet().iterator(); cIter.hasNext(); ) { 
							Integer cid = (Integer)cIter.next();
							IloConstraint c = (IloConstraint) constraintidsToConstraints.get(cid);
							if (c instanceof IloRange) {
								IloRange iloRange = (IloRange) c;
								constraintidsToDuals.put(cid, new Double(cplex.getDual(iloRange)));
							}
						}
					}
					objValue = cplex.getObjValue();
					if (log.isDebugEnabled()) 
						log.debug("obj: " + objValue);
					IloCplex.Status optStatus = cplex.getStatus();
					if (log.isDebugEnabled()) 
						log.debug("CPlex solution status: " + optStatus);
				} else {
					// Solve returned an error.
					IloCplex.Status optStatus = cplex.getStatus();
					if (log.isDebugEnabled()) {
						log.debug("CPlex solve failed status: " + optStatus);
					}
					if (optStatus == IloCplex.Status.Infeasible ||
						optStatus == IloCplex.Status.InfeasibleOrUnbounded) {
						Object cplexParam = getCplexParam(SolveParam.ABSOLUTE_VAR_BOUND_GAP);
						double dval = cplex.getParam((IloCplex.DoubleParam) cplexParam) * 10;
						if (dval > mip.getDoubleSolveParam(SolveParam.CONSTRAINT_BACKOFF_LIMIT)) {
							MIPException e = createInfesibilityException(cplex, vars, constraintidsToConstraints, mip);
							InstanceManager.getInstance().checkInCplex(cplex);
							throw e;
						} else {
							log.main("No feasible Solution. Resolving with looser tolerance: " + dval);
							cplex.setParam((IloCplex.DoubleParam)cplexParam, dval);
							cplexParam = getCplexParam(SolveParam.ABSOLUTE_INT_GAP);
							cplex.setParam((IloCplex.DoubleParam)cplexParam, dval);
						}
					} else {
						InstanceManager.getInstance().checkInCplex(cplex);
						throw new MIPException("Solve failed with status: " + optStatus + ", no conflict calculation possible");
					}					
				}
			}
			InstanceManager.getInstance().checkInCplex(cplex);
			
			MIPResult res = new MIPResult(objValue, values, constraintidsToDuals);
			res.setSolveTime(solveTime);
			return res;
		} catch (IloException e) {
			InstanceManager.getInstance().checkInCplex(cplex);
			if (mip.getBooleanSolveParam(SolveParam.DISPLAY_OUTPUT, true)) {
				e.printStackTrace();
			}
			throw new MIPException("Cplex Exception: "+ e.toString());
		} catch (RuntimeException ex) {
			InstanceManager.getInstance().checkInCplex(cplex);
			if (mip.getBooleanSolveParam(SolveParam.DISPLAY_OUTPUT, true)) {
				ex.printStackTrace();
			}
			throw ex;
		}
	}		

	private MIPException createInfesibilityException(IloCplex cplex, Map vars, Map constraintidsToIloConstraints, IMIP mip) {
		if(!mip.getBooleanSolveParam(SolveParam.CALCULATE_CONFLICT_SET, true)) {
			throw new MIPInfeasibleException("MIP Infeasible: set CALCULATE_CONFLICT_SET to obtain a refined conflict set");
		}
		
		Map vConstToVars = new HashMap();
		for (Iterator i = vars.keySet().iterator(); i.hasNext(); ) {
			String varName = (String) i.next();
			IloNumVar numVar = (IloNumVar) vars.get(varName);
			Variable mipVar = mip.getVar(varName);
			if (mipVar.getType() != VarType.BOOLEAN) {
				//only include non-boolean variables
				vConstToVars.put(cplex.lowerBound(numVar), mipVar);
				vConstToVars.put(cplex.upperBound(numVar), mipVar);
			}
		}
		Map iloConstraintsToConstraints = new HashMap();
		for (Iterator cIter = constraintidsToIloConstraints.keySet().iterator(); cIter.hasNext(); ) { 
			Integer cid = (Integer)cIter.next();
			IloConstraint iloConst = (IloConstraint) constraintidsToIloConstraints.get(cid);
			Constraint c = mip.getConstraint(cid.intValue());
			iloConstraintsToConstraints.put(iloConst, c);
		}
		
		//Now build the full array:
		ArrayList full = new ArrayList();
		full.addAll(vConstToVars.keySet());
		full.addAll(iloConstraintsToConstraints.keySet());
		
		IloConstraint[] arr = (IloConstraint[])full.toArray(new IloConstraint[full.size()]);
		double prefs[] = new double[arr.length];
		Arrays.fill(prefs, 1);
		
		try {
			if (!cplex.refineConflict(arr, prefs)) {
				throw new MIPInfeasibleException("Could not refine conflict");
			}
			Map mipVarsToCauses = new HashMap();
			Set constraints = new HashSet();
			for(Iterator iter=full.iterator(); iter.hasNext(); ) {
				IloConstraint iloConst = (IloConstraint)iter.next();
				ConflictStatus cStat = cplex.getConflict(iloConst);
				if (cStat == ConflictStatus.Member ||
				    cStat == ConflictStatus.PossibleMember) {
					Variable v = (Variable)vConstToVars.get(iloConst);
					if (v!=null) {
						IloNumVarBound b = (IloNumVarBound)iloConst;
						mipVarsToCauses.put(v, getCause(b.getType()));
					}
					Constraint c = (Constraint)iloConstraintsToConstraints.get(iloConst);
					if (c != null) {
						constraints.add(c);
					}
				}
			}
			return new MIPInfeasibleException(mipVarsToCauses, constraints);
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
	
	private void setControlParams(IloCplex cplex, IMIP mip) {
		for (Iterator i = mip.getSpecifiedSolveParams().iterator(); i.hasNext(); ) {
			SolveParam solveParam = (SolveParam) i.next();
			if (!solveParam.isInternal()) {
				Object cplexParam = getCplexParam(solveParam);
				Object value = mip.getSolveParam(solveParam);
				if (log.isInfoEnabled()) {
					log.info("Setting " + solveParam.toString() + " to: " + value.toString() );
				}
				try {
					if (solveParam.isBoolean()) {
						cplex.setParam((BooleanParam) cplexParam, ((Boolean) value).booleanValue());
					} else if (solveParam.isInteger()) {
						cplex.setParam((IloCplex.IntParam)cplexParam, ((Integer) value).intValue());
					} else if (solveParam.isDouble()) {
						cplex.setParam((DoubleParam) cplexParam, ((Double) value).doubleValue());
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
	
	private Object getCplexParam(SolveParam solveParam) {
		if (SolveParam.CLOCK_TYPE.equals(solveParam)) {
			return IloCplex.IntParam.ClockType;
		} else if (SolveParam.TIME_LIMIT.equals(solveParam)) {
			return IloCplex.DoubleParam.TiLim;
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
		}
		throw new MIPException("Invalid solve param: " + solveParam);
	}
	
	public static void main(String argv[]) {
		if (argv.length < 1 || argv.length > 2) {
			System.err.println("Usage: edu.harvard.econcs.jopt.solver.server.cplex.CPlexMIPSolver <port> <num simultaneous>");
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


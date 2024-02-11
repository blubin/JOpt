package edu.harvard.econcs.jopt.solver.server.cplex;

import java.util.Map;

import edu.harvard.econcs.jopt.solver.ILazyConstraintCallbackContext;
import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.MIPException;
import edu.harvard.econcs.jopt.solver.SolveParam;
import edu.harvard.econcs.jopt.solver.mip.CompareType;
import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.LinearTerm;
import edu.harvard.econcs.jopt.solver.mip.Variable;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.LazyConstraintCallback;;

public class CPLEXLazyConstraintCallbackContext extends LazyConstraintCallback
		implements ILazyConstraintCallbackContext {

	private IloCplex cplex;
	private IMIP mip;
	private Map<String, IloNumVar> vars;

	public CPLEXLazyConstraintCallbackContext(IloCplex cplex, IMIP mip, Map<String, IloNumVar> vars) {
		this.cplex = cplex;
		this.mip = mip;
		this.vars = vars;
	}

	@Override
	public void addLazyConstraint(Constraint constraint) {
		try {
			if (constraint.hasQuadraticTerms()) {
				throw new MIPException("Cplex does not support quadratic terms in lazy constraints");
			}

			// Add Linear Terms:
			int linearTermsUsed = 0;
			IloLinearNumExpr numExpr = cplex.linearNumExpr();
			for (LinearTerm term : constraint.getLinearTerms()) {
				Variable var = mip.getVar(term.getVarName());
				if (var == null) {
					throw new MIPException("Invalid variable name in term: " + term);
				}
				if (var.ignore()) {
					// System.out.println("Skipping term: " + term);
					continue;
				}
				linearTermsUsed++;
				numExpr.addTerm(term.getCoefficient(), vars.get(term.getVarName()));
			}

			if (linearTermsUsed == 0) {
				// nothing to add
				return;
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

			// add the new lazy constraint
			this.add(iloRange);
		} catch (IloException e) {
			if (mip.getBooleanSolveParam(SolveParam.DISPLAY_OUTPUT, true)) {
				e.printStackTrace();
			}
			throw new MIPException("Cplex Exception: " + e.toString());
		}
	}

	@Override
	protected void main() throws IloException {
		mip.getLazyConstraintCallback().callback(this);
	}

	@Override
	public double getValue(Variable variable) {
		return this.getValue(variable.getName());
	}

	@Override
	public double getValue(String varName) {
		try {
			return super.getValue(vars.get(varName));
		} catch (IloException e) {
			if (mip.getBooleanSolveParam(SolveParam.DISPLAY_OUTPUT, true)) {
				e.printStackTrace();
			}
			throw new MIPException("Cplex Exception: " + e.toString());
		}
	}

	@Override
	public double getIncumbentObjectiveValue() {
		try {
			return super.getIncumbentObjValue();
		} catch (IloException e) {
			if (mip.getBooleanSolveParam(SolveParam.DISPLAY_OUTPUT, true)) {
				e.printStackTrace();
			}
			throw new MIPException("Cplex Exception: " + e.toString());
		}
	}

	@Override
	public double getRelativeGap() {
		try {
			return this.getMIPRelativeGap();
		} catch (IloException e) {
			if (mip.getBooleanSolveParam(SolveParam.DISPLAY_OUTPUT, true)) {
				e.printStackTrace();
			}
			throw new MIPException("Cplex Exception: " + e.toString());
		}
	}

}

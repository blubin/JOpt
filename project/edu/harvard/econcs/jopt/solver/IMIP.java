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
package edu.harvard.econcs.jopt.solver;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.LinearTerm;
import edu.harvard.econcs.jopt.solver.mip.QuadraticTerm;
import edu.harvard.econcs.jopt.solver.mip.Term;
import edu.harvard.econcs.jopt.solver.mip.Variable;

/**
 * Interface specifying a Mixed Integer Program
 * 
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.20 $ on $Date: 2013/12/04 02:54:09 $
 * @since Apr 12, 2004
 **/
public interface IMIP extends Serializable {

	// Variables
	////////////
	
	/**
	 * @return a Map from Strings to Variables.
	 */
	public Map<String,Variable> getVars();
	
	/** @return the Variable corresponding to the String name. */
	public Variable getVar(String name);
	
	/** 
	 * Adds a variable to the MIP formulation. Depending on the implementation
	 * you may find that this variable doesn't show up in the solver. (For instance
	 * if there is no reference to the variable in any constraint
	 * @return the string representation of the variable
	 */
	public String add(Variable var);
	
	/** 
	 * Removes a variable from the MIP formulation. Depending on the implementation
	 * this may not actually communicate with the solver, if this variable is unused
	 * by the solver.
	 */
	public void remove(Variable var);

	/**
	 * @return the number of variables in the MIP formulation.
	 */
	public int getNumVars();

	// Proposed Variable Values:
	////////////////////////////	
	
	/** Sets proposed value for this variable */
	public void proposeValue(Variable var, double value);
	
	/** Sets proposed value for this variable */
	public void proposeValue(Variable var, int value);	

	/** Sets proposed value for this variable */
	public void proposeValue(Variable var, boolean value);	

	/** Removes proposed value for this variable */
	public void removeProposedValue(Variable var);	
	
	/** Clears proposed value for this variable */
	public void clearProposedValues();	

	/** useful for copying proposed var/vals to a new MIP */
	public Set getVarsWithProposedValues();

	/** returns the currently proposed value for Integer variable var */
	public int getProposedIntValue(Variable var);
	
	/** returns the currently proposed value for Double variable var */
	public double getProposedDoubleValue(Variable var);

	/** returns the currently proposed value for Boolean variable var */	
	public boolean getProposedBooleanValue(Variable var);
	
	
	// Objective Function:
	//////////////////////
	
	//Linear:
	
	/**
	 * @return an iterator on the Linear objective Term objects
	 */
	public Collection<LinearTerm> getLinearObjectiveTerms();
	
	/** Get the terms sorted lexicographically **/
	public Collection<LinearTerm> getSortedLinearObjectiveTerms();
	
	/**
	 * Adds a Term to the objective function.
	 * @param term
	 */
	public void addObjectiveTerm(LinearTerm term);
	
	/**
	 * Adds a Term to the objective function.
	 * @param term
	 */
	public void addObjectiveTerm(double coefficient, Variable var);
	
	/**
	 * removes a Term from the objective function.
	 * @param term
	 */
	public void removeObjectiveTerm(LinearTerm term);
	
	//Quadratic:
	
	/**
	 * @return an iterator on the objective Quadratic Term objects
	 */
	public Collection<QuadraticTerm> getQuadraticObjectiveTerms();

	/** Get the terms sorted lexicographically **/
	public Collection<QuadraticTerm> getSortedQuadraticObjectiveTerms();

	/**
	 * Adds a Term to the objective function.
	 * @param term
	 */
	public void addObjectiveTerm(QuadraticTerm term);

	/**
	 * Adds a Term to the objective function.
	 * @param term
	 */
	public void addObjectiveTerm(double coefficient, Variable varA, Variable varB);

	/**
	 * removes a Term from the objective function.
	 * @param term
	 */
	public void removeObjectiveTerm(QuadraticTerm term);
	
	//Both
	
	/**
	 * @return All the Term objects
	 */
	public Collection<Term> getObjectiveTerms();

	// Overall Objective:
	/////////////////////
	
	/**
	 * 
	 * @return true if objective is a max
	 */
	public boolean isObjectiveMax();

	/**
	 * 
	 * @return true if objective is a min
	 */
	public boolean isObjectiveMin();

	/**
	 * Set the objective to be MAX (true) or MIN (false)
	 * @param isMax
	 */
	public void setObjectiveMax(boolean isMax);

	// Constraints
	//////////////
	
	/**
	 * @return An iterator over the constraints
	 */
	public Collection<Constraint> getConstraints();
	
	/**
	 * 
	 * @return the number of constraints in the system
	 */
	public int getNumConstraints();
	
	/** Get all of the constraint ids that have been used.  **/
	public Set<Integer> getConstraintIds();

	/**
	 * 
	 * @param constraintId the id passed in during constraint creation
	 * @return the Constraint object
	 */
	public Constraint getConstraint(int constraintId);
	
	/**
	 * Adds a new constraint
	 * @param constraint the constraint to add
	 * @return constraintId should be a UNIQUE number; useful for getting the dual of this constraint
	 */
	public int add(Constraint constraint);

	/**
	 * Adds a new constraint
	 * @param constraint the constraint to add
	 * @param constraintId should be a UNIQUE number; useful for getting the dual of this constraint
	 * @return
	 */
	public int add(Constraint constraint, int constraintId);

	/**
	 * removes a constraint from the formulation.
	 * @param constraintId
	 */
	public void remove(int constraintId);
		
	// Solve Parameters:
	////////////////////
	
	/**
	 * These functions control how JOpt operates, and how the backend solver operates.
	 * You can control a number of parameters. Not all of these will be supported by
	 * 	your chosen backend solver, unfortunently. But we try to map these settings
	 * into relevant solver settings. See each setting to see if they are applicable
	 * to CPLEX, LPSOLVE, or ALLSOLVERS
	 */
	
	public Object getSolveParam(SolveParam param);

	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public int getIntSolveParam(SolveParam param, int defaultValue);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public double getDoubleSolveParam(SolveParam param, double defaultValue);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public boolean getBooleanSolveParam(SolveParam param, boolean defaultValue);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public int getIntSolveParam(SolveParam param, Integer defaultValue);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public double getDoubleSolveParam(SolveParam param, Double defaultValue);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public boolean getBooleanSolveParam(SolveParam param, Boolean defaultValue);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public String getStringSolveParam(SolveParam param, String defaultValue);	
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public int getIntSolveParam(SolveParam param);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public double getDoubleSolveParam(SolveParam param);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public boolean getBooleanSolveParam(SolveParam param);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public String getStringSolveParam(SolveParam param);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public void setSolveParam(SolveParam param, int value);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public void setSolveParam(SolveParam param, double value);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public void setSolveParam(SolveParam param, boolean value);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public void setSolveParam(SolveParam param, String value);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public Set getSpecifiedSolveParams();
	
	public boolean isSolveParamSpecified(SolveParam param);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	public void clearSolveParams();
	
	/** reset behavior is solver specific; see implementing class */
	public void resetDefaultSolveParams();
	
	// General Functions:
	/////////////////////
	
	public IMIP typedClone();
}
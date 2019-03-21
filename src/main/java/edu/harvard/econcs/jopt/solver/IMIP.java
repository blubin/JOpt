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

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	Map<String,Variable> getVars();

    /**
     * @return whether the mip contains the variable.
     */
    boolean containsVar(Variable var);

    /**
     * @return whether the mip contains the variable.
     */
    boolean containsVar(String name);

	/** @return the Variable corresponding to the String name. */
	Variable getVar(String name);

	/**
	 * Adds a variable to the MIP formulation. Depending on the implementation
	 * you may find that this variable doesn't show up in the solver. (For instance
	 * if there is no reference to the variable in any constraint
	 * @return the string representation of the variable
	 */
	String add(Variable var);

	/**
	 * Removes a variable from the MIP formulation. Depending on the implementation
	 * this may not actually communicate with the solver, if this variable is unused
	 * by the solver.
	 */
	void remove(Variable var);

	/**
	 * @return the number of variables in the MIP formulation.
	 */
	int getNumVars();

	/**
	 * Variables of interest can be used for finding the k best solutions to an optimization
	 * in combination with SOLUTION_POOL_MODE = 3 or SOLUTION_POOL_MODE = 4. They form the set
	 * of variables that distinguish different solutions in the context of the MIP. For example, in an auction,
	 * these variables could be the allocation variables.
	 *
	 * If you're interested in a more advanced structure of variables of interest,
	 * see {@link #setAdvancedVariablesOfInterest(Collection)}.
	 *
	 * @param variables The variables of interest. For SOLUTION_POOL_MODE = 3, only boolean variables are supported.
	 */
	default void setVariablesOfInterest(Collection<Variable> variables) {
		setAdvancedVariablesOfInterest(variables
				.stream()
				.map(v -> Stream.of(v).collect(Collectors.toSet()))
				.collect(Collectors.toSet())
		);
	}

	/**
	 * Internally, the variables of interest are stored as a collection of collections, even when the user
     * sets them as a single collection. But a user may want to set the variables of interest directly in this advanced
     * structure. This gives, without breaking the API of a simple collection of variables, the user the possibility
	 * to define <b>sets of variables that all have to be equal in their sum to be considered a duplicate.</b>
	 *
	 * @param variableSets The sets of variables of interest. For SOLUTION_POOL_MODE = 3, only boolean variables are
	 *                     supported.
	 */
	void setAdvancedVariablesOfInterest(Collection<Collection<Variable>> variableSets);

	/**
	 * @return the variables of interest if defined, else null
	 */
	default Collection<Variable> getVariablesOfInterest() {
		if (getAdvancedVariablesOfInterest() == null) return null;
		return getAdvancedVariablesOfInterest().stream().flatMap(Collection::stream).collect(Collectors.toSet());
	}

	Collection<Collection<Variable>> getAdvancedVariablesOfInterest();

	// Proposed Variable Values:
	////////////////////////////

	/** Sets proposed value for this variable */
	void proposeValue(Variable var, double value);
	
	/** Sets proposed value for this variable */
	void proposeValue(Variable var, int value);

	/** Sets proposed value for this variable */
	void proposeValue(Variable var, boolean value);

	/** Removes proposed value for this variable */
	void removeProposedValue(Variable var);
	
	/** Clears proposed value for this variable */
	void clearProposedValues();

	/** useful for copying proposed var/vals to a new MIP */
	Set<Variable> getVarsWithProposedValues();
	/** useful for copying proposed var/vals to a new MIP */
	Map<Variable,Object> getProposedValuesMap();
    /** useful for copying proposed var/vals to a new MIP */
	void setProposedValues(Map<Variable, Object> proposedValues);
   
	/** returns the currently proposed value for Integer variable var */
	int getProposedIntValue(Variable var);
	
	/** returns the currently proposed value for Double variable var */
	double getProposedDoubleValue(Variable var);

	/** returns the currently proposed value for Boolean variable var */
	boolean getProposedBooleanValue(Variable var);
	
	
	// Objective Function:
	//////////////////////
	
	//Linear:
	
	/**
	 * @return an iterator on the Linear objective Term objects
	 */
	Collection<LinearTerm> getLinearObjectiveTerms();
	
	/** Get the terms sorted lexicographically **/
	Collection<LinearTerm> getSortedLinearObjectiveTerms();
	
	/**
	 * Adds a Term to the objective function.
	 * @param term
	 */
	void addObjectiveTerm(LinearTerm term);
	
	/**
	 * Adds a Term to the objective function.
	 */
	void addObjectiveTerm(double coefficient, Variable var);
	
	/**
	 * removes a Term from the objective function.
	 * @param term
	 */
	void removeObjectiveTerm(LinearTerm term);
	
	//Quadratic:
	
	/**
	 * @return an iterator on the objective Quadratic Term objects
	 */
	Collection<QuadraticTerm> getQuadraticObjectiveTerms();

	/** Get the terms sorted lexicographically **/
	Collection<QuadraticTerm> getSortedQuadraticObjectiveTerms();

	/**
	 * Adds a Term to the objective function.
	 * @param term
	 */
	void addObjectiveTerm(QuadraticTerm term);

	/**
	 * Adds a Term to the objective function.
	 */
	void addObjectiveTerm(double coefficient, Variable varA, Variable varB);

	/**
	 * removes a Term from the objective function.
	 * @param term
	 */
	void removeObjectiveTerm(QuadraticTerm term);

	/**
	 * Removes the objective
	 * @return whether a previous objective was set
	 */
	boolean clearObjective();
	//Both
	
	/**
	 * @return All the Term objects
	 */
	Collection<Term> getObjectiveTerms();

	// Overall Objective:
	/////////////////////
	
	/**
	 * 
	 * @return true if objective is a max
	 */
	boolean isObjectiveMax();

	/**
	 * 
	 * @return true if objective is a min
	 */
	boolean isObjectiveMin();

	/**
	 * Set the objective to be MAX (true) or MIN (false)
	 * @param isMax
	 */
	void setObjectiveMax(boolean isMax);

	// Constraints
	//////////////
	
	/**
	 * @return An iterator over the constraints
	 */
	List<Constraint> getConstraints();
	
	/**
	 * 
	 * @return the number of constraints in the system
	 */
	int getNumConstraints();
	

	/**
	 * Adds a new constraint
	 * @param constraint the constraint to add
	 * @return constraintId should be a UNIQUE number; useful for getting the dual of this constraint
	 */
	void add(Constraint constraint);


	/**
     * removes a constraint from the formulation.
     * @param constraint
	 * @return 
     */
	boolean remove(Constraint constraint);
	
	// Solve Parameters:
	////////////////////
	
	/**
	 * These functions control how JOpt operates, and how the backend solver operates.
	 * You can control a number of parameters. Not all of these will be supported by
	 * 	your chosen backend solver, unfortunently. But we try to map these settings
	 * into relevant solver settings. See each setting to see if they are applicable
	 * to CPLEX, LPSOLVE, or ALLSOLVERS
	 */

	Object getSolveParam(SolveParam param);

	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	int getIntSolveParam(SolveParam param, int defaultValue);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	double getDoubleSolveParam(SolveParam param, double defaultValue);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	boolean getBooleanSolveParam(SolveParam param, boolean defaultValue);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	int getIntSolveParam(SolveParam param, Integer defaultValue);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	double getDoubleSolveParam(SolveParam param, Double defaultValue);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	boolean getBooleanSolveParam(SolveParam param, Boolean defaultValue);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	String getStringSolveParam(SolveParam param, String defaultValue);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	int getIntSolveParam(SolveParam param);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	double getDoubleSolveParam(SolveParam param);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	boolean getBooleanSolveParam(SolveParam param);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	String getStringSolveParam(SolveParam param);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	void setSolveParam(SolveParam param, Object value);
    
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	Set<SolveParam> getSpecifiedSolveParams();
	
	boolean isSolveParamSpecified(SolveParam param);
	
	/**@see edu.harvard.econcs.jopt.solver.IMIP#getSolveParam(SolveParam)*/
	void clearSolveParams();
	
	/** reset behavior is solver specific; see implementing class */
	void resetDefaultSolveParams();
	
	// General Functions:
	/////////////////////
	
	IMIP typedClone();

}
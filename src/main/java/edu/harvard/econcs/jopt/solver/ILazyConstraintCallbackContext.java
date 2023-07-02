package edu.harvard.econcs.jopt.solver;

import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.Variable;

/**
 * The context allows access to the current incument solution during a lazy
 * constraint callback call. Additional constraints may be added using the
 * {@link #addLazyConstraint(Constraint)} method.
 * 
 * @author Manuel Beyeler
 * @see ILazyConstraintCallback
 */
public interface ILazyConstraintCallbackContext {
	/**
	 * @param constraint the lazy constraint to add to the problem
	 */
	void addLazyConstraint(Constraint constraint);

	/**
	 * @param variable 
	 * @return the value of the given variable for this incumbent solution
	 */
	double getValue(Variable variable);

	/**
	 * @param varName 
	 * @return the value of the given variable for this incumbent solution
	 */
	double getValue(String varName);

	/**
	 * @return the objective value of this incumbent solution
	 */
	double getIncumbentObjectiveValue();

	/**
	 * @return the current relative mip gap
	 */
	double getRelativeGap();
}

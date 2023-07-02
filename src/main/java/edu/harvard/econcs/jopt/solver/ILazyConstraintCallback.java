package edu.harvard.econcs.jopt.solver;

/**
 * This callback method will be called during the solve process of a MIP when a
 * new integer feasible solution was found. The callback can be used to validate
 * this solution and add additional constraint to invalidate the solution with
 * additional lazy constraint if necessary. This is helpful if an enumeration of
 * all constraint is not feasible (i.e. for an exponential number of
 * constraints).
 * 
 * The callback can only be used if the problem is a MIP.
 * 
 * Note that you may need to add the same lazy constraints again in multiple
 * callback calls in some cases. The safest way is to always add the necessary
 * constraints to invalidate the solution.
 * 
 * @author Manuel Beyeler
 */
public interface ILazyConstraintCallback {
	/**
	 * This method will be called when an incumbent solution was found.
	 * @param context the context
	 */
	void callback(ILazyConstraintCallbackContext context);
}

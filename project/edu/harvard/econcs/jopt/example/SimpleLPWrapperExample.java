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
package edu.harvard.econcs.jopt.example;

import java.util.Map;

import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.MIPWrapper;
import edu.harvard.econcs.jopt.solver.mip.Variable;

/*
 * A simple example of how to use JOpt: alternative syntax.<br>
 * <br>
 * This is an even simpler syntax usage of JOpt than ExampleSimple 
 * I guess all things being equal, I prefer the ExampleSimple syntax
 * but this exists as well.<br>
 * <pre>
 * MAX 2X + 2Y
 * subject to
 * X - 2Y <= 5
 * Y = 2
 * </pre>
 * 
 * @author Jeff Schneidman; Last modified by $Author: blubin $
 * @version $Revision: 1.1 $ on $Date: 2013/12/04 02:54:09 $
 * @since Dec 13, 2004
 */
public class SimpleLPWrapperExample {

	/** If you wanted to use a remote solver, you would do something like: */
    //SolverClient solverClient = new SolverClient("econcs.eecs.harvard.edu", 2001);

	/** If you want to use the locally installed CPLEX server, do something like: */
    SolverClient solverClient = new SolverClient();

    /**
     * After the call to solve, result will hold the results of the solver, such as the
     * objective value and values of individual variables, and the dual, etc.
     */
    private IMIPResult result;
    
    /**
     * All constraints should have a unique id so that their dual can be examined.
     */
    static int constraintID = 0;
    
    /**
     * MAX 2X + 2Y
     * subject to
     * X - 2Y <= 5
     * Y = 2
     */
    
    public SimpleLPWrapperExample() {

        /**
         * First we construct the mip formulation that we wish to solve. In this example,
         * it is actually an LP. Later in this file, we put different constraints into the
         * MIP. Constraints are filled with Terms. Terms are filled with a Variable and a
         * coefficient on the variable.
         * <br><br>
    	 * Create a new maximization MIP
    	 */
    	MIPWrapper mip = MIPWrapper.makeNewMaxMIP();
    	
    	/** Create two variables, x and y, which are doubles that can range from -INF to INF
    	 * and add them to the MIP.
    	 */
    	
    	Variable x = mip.makeNewDoubleVar("x");
    	Variable y = mip.makeNewDoubleVar("y");
    	        
        /**
         * Put stuff in the objective function, to get a MAX 2X + 2Y.
         */
        mip.addObjectiveTerm(2, x);
        mip.addObjectiveTerm(2, y);

        /**
         * Create a new constraint, showing off two ways to make a term.
         * X - 2Y <= 5
         */        
        
        Constraint c1 = mip.beginNewLEQConstraint(5);
        c1.addTerm(1, x);
        c1.addTerm(-2, y);
        mip.endConstraint(c1);

        /**
         * Create a new constraint.
         * Y == 2
         */        
         
        Constraint c2 = mip.beginNewEQConstraint(2);
        c2.addTerm(1, y);
        mip.endConstraint(c2);
        
        /** 
         * Demonstrate how we can write out a mip or a constraint (or a term/variable)
         * for debugging output.
         */
        System.out.println(mip.toString());
        System.out.println(c2.toString());
        
        /**
         * Solve the MIP
         */
        
        result = solverClient.solve(mip);
                
        /**
         * Get the objective value (should be 22) and the values of the variables.
         */
        
        double objective = result.getObjectiveValue();
        Map m = result.getValues();
        
        System.out.println("objective: " + objective);
        System.out.println("x is " + m.get("x"));
        System.out.println("y is " + m.get("y"));
    }
    
    public static void main(String[] args) {
        new SimpleLPWrapperExample();
    }

}

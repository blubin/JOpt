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
package edu.harvard.econcs.jopt.solver.mip;

import edu.harvard.econcs.jopt.solver.IMIP;

/**
 * This wrapper goes even further in simplicity than the other MIP classes and
 * might help you get started with your MIP programming. See
 * ExampleSimpleWrapper.java for an example.
 * 
 * @author Jeff Shneidman; Last modified by $Author: blubin $
 * @version $Revision: 1.5 $ on $Date: 2010/10/28 00:11:26 $
 * @since Nov 26, 2004
 */
public class MIPWrapper extends MIP {

    // MIP CREATION
    // /////////////

    /**
	 * 
	 */
    private static final long serialVersionUID = 3258129150505202743L;

    /**
     * only use static initializers
     */
    private MIPWrapper() {
      

    }

    /**
     * Create a new MIP that is has a MAXimization as it's objective function.
     * You would then add some objective terms. The following code makes a new
     * MIP with "MAX 3X" as the objective.<br>
     * <br>
     * 
     * MIPWrapper mip = makeNewMaxMIP();<br>
     * Variable x = makeNewDoubleVar("x");<br>
     * mip.addObjectiveTerm(x, 3);
     * 
     * @return a mip object.
     */
    public static MIPWrapper makeNewMaxMIP() {
        MIPWrapper mip = new MIPWrapper();
        mip.setObjectiveMax(true);
        return mip;
    }

    /**
     * Create a new MIP that is has a MINimization as it's objective function.
     * You would then add some objective terms. The following code makes a new
     * MIP with "MIN 3X" as the objective. <br>
     * <br>
     * MIPWrapper mip = makeNewMinMIP();<br>
     * Variable x = makeNewDoubleVar("x");<br>
     * mip.addObjectiveTerm(x, 3);
     */
    public static MIPWrapper makeNewMinMIP() {
        MIPWrapper mip = new MIPWrapper();
        mip.setObjectiveMax(false);
        return mip;
    }

    /**
     * 
     * @param mip
     * @return a new MIP that has the same constraints and vars as the old mip
     *         but no objective The Vars and Constraints are the same Objects so
     *         altering them will have an affect on both objects. Adding and
     *         removing constraints is independent though.
     */
    public static MIPWrapper makeMIPWithoutObjective(IMIP mip) {
        MIPWrapper newMIP;
        if (mip.isObjectiveMax()) {
            newMIP = MIPWrapper.makeNewMaxMIP();
        } else {
            newMIP = MIPWrapper.makeNewMinMIP();
        }
        for (Variable var : mip.getVars().values()) {
            newMIP.add(var);
            
        }
        for (Constraint constraint : mip.getConstraints()) {
            newMIP.add(constraint);
        }
        newMIP.setProposedValues(mip.getProposedValuesMap());
        return newMIP;

    }

    // Variable CREATION
    // //////////////////

    /** Makes a new double variable that can range from -inf to inf */
    public Variable makeNewDoubleVar(String name) {
        Variable newVar = new Variable(name, VarType.DOUBLE, -1 * MIP.MAX_VALUE, MIP.MAX_VALUE);
        this.add(newVar);
        return newVar;
    }

    /** Makes a new Integer variable that can range from -inf to inf */
    public Variable makeNewIntegerVar(String name) {
        Variable newVar = new Variable(name, VarType.INT, -1 * MIP.MAX_VALUE, MIP.MAX_VALUE);
        this.add(newVar);
        return newVar;
    }

    /** Makes a new indicator (boolean) variable that can range from 0 to 1 */
    public Variable makeNewBooleanVar(String name) {
        Variable newVar = new Variable(name, VarType.BOOLEAN, 0, 1);
        this.add(newVar);
        return newVar;
    }


    /** Call this when you are done building up a constraint. */
    public void endConstraint(Constraint constraint) {
        this.add(constraint);
    }

    /**
     * Creates a new Less Than or Equals constraint
     * 
     * This example makes a constraint "-2y <= 3"<br>
     * (assumes earlier call like:MIPWrapper mip = makeNewMaxMIP(), and a
     * Variable y) <br>
     * <br>
     * Constraint c = beginNewLEQConstraint(3);<br>
     * c.addTerm(y, -2);<br>
     * mip.endConstraint(c);
     * 
     * * @param constant is the value that is on the right hand side of the
     * equation.
     * 
     * @return the new constraint. You will want to add terms to the constraint.
     * 
     */
    public Constraint beginNewLEQConstraint(double constant) {
        Constraint newC = new Constraint(CompareType.LEQ, constant);
        return newC;
    }

    /**
     * Creates a new Less Than or Equals constraint
     * 
     * @see beginNewLEQConstraint
     */
    public Constraint beginNewGEQConstraint(double constant) {
        Constraint newC = new Constraint(CompareType.GEQ, constant);
        return newC;
    }

    /**
     * Creates a new Less Equals constraint
     * 
     * @see beginNewLEQConstraint
     */
    public Constraint beginNewEQConstraint(double constant) {
        Constraint newC = new Constraint(CompareType.EQ, constant);
        return newC;
    }

}

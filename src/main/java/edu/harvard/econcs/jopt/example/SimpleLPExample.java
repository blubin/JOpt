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
package edu.harvard.econcs.jopt.example;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.SolveParam;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.mip.CompareType;
import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.MIP;
import edu.harvard.econcs.jopt.solver.mip.VarType;
import edu.harvard.econcs.jopt.solver.mip.Variable;

/**
 * Simple JOpt usage example:<br>
 * <pre>
 * MAX 2X+2Y
 * subject to
 * X - 2Y <= 5
 * Y = 2
 * </pre>
 * 
 * @author Jeff Schneidman; Last modified by $Author: blubin $
 * @version $Revision: 1.1 $ on $Date: 2013/12/04 02:54:09 $
 * @since Dec 13, 2005
 **/
public class SimpleLPExample {

	private IMIP mip;
	
    public SimpleLPExample() {
    }
    
    public IMIP getMIP() {
    	return mip;
    }
    
    public void buildMIP() {
    	mip = new MIP();
    	
    	Variable x = new Variable("x", VarType.DOUBLE, -MIP.MAX_VALUE, MIP.MAX_VALUE);
        Variable y = new Variable("y", VarType.DOUBLE, -MIP.MAX_VALUE, MIP.MAX_VALUE);
        
        mip.add(x);
        mip.add(y);
        
        mip.setObjectiveMax(false);
        mip.addObjectiveTerm(2, x);
        mip.addObjectiveTerm(2, y);

        Constraint c1 = new Constraint(CompareType.LEQ, 5);
        c1.addTerm(1, x);
        c1.addTerm(2, y);
        mip.add(c1);

        Constraint c2 = new Constraint(CompareType.EQ, 2);
        c2.addTerm(1, y);
        mip.add(c2);        
    }
    
    public IMIPResult solve() {
        mip.setSolveParam(SolveParam.CALC_DUALS, true);
		SolverClient solverClient = new SolverClient();
    	IMIPResult result= solverClient.solve(mip);
    	System.out.println(result.getDual(mip.getConstraints().get(0)));
        System.out.println(result.getDual(mip.getConstraints().get(1)));
        return result;
    }

    public static void main(String[] argv) {
    	SimpleLPExample example = new SimpleLPExample();
    	example.buildMIP();
    	System.out.println(example.getMIP());
    	System.out.println(example.solve());
    }
}
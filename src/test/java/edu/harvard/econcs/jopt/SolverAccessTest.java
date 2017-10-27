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
package edu.harvard.econcs.jopt;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.mip.*;
import edu.harvard.econcs.jopt.solver.server.cplex.CPlexMIPSolver;
import edu.harvard.econcs.jopt.solver.server.lpsolve.LPSolveMIPSolver;
import org.junit.Assert;
import org.junit.Test;

public class SolverAccessTest {

	@Test
	public void solverAccessTest() {
		IMIP mip = new MIP();
		Variable v = new Variable("a", VarType.INT, 0, 3);
		mip.add(v);
		Constraint c1 = new Constraint(CompareType.GEQ, 1);
		c1.addTerm(new LinearTerm(1, v));
		Constraint c2 = new Constraint(CompareType.GEQ, 2);
		c2.addTerm(new LinearTerm(1, v));
		Constraint c3 = new Constraint(CompareType.LEQ, 5);
		c3.addTerm(new LinearTerm(1, v));
		mip.add(c1);
		mip.add(c2);
		mip.add(c3);
		mip.setObjectiveMax(true);
		mip.addObjectiveTerm(1, v);
		
	    SolverClient client = new SolverClient();
	    IMIPResult result = client.solve(mip);
	}
}

package edu.harvard.econcs.jopt;

import edu.harvard.econcs.jopt.example.ComplexExample;
import edu.harvard.econcs.jopt.example.PieceWiseLinearExample;
import edu.harvard.econcs.jopt.example.SimpleLPExample;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.server.cplex.CPlexMIPSolver;
import edu.harvard.econcs.jopt.solver.server.lpsolve.LPSolveMIPSolver;
import org.junit.Test;

/**
 * @author Fabio Isler
 */
public class Examples {

	@Test
	public void simpleLPExample() {
		IMIPResult lpSolveResult = SimpleLPExample.test(new SolverClient(new LPSolveMIPSolver()));
		IMIPResult cplexResult = SimpleLPExample.test(new SolverClient(new CPlexMIPSolver()));
		TestSuite.compareMultipleResults(lpSolveResult, cplexResult);
	}

	@Test
	public void complexExample() {
		IMIPResult lpSolveResult = ComplexExample.test(new SolverClient(new LPSolveMIPSolver()));
		IMIPResult cplexResult = ComplexExample.test(new SolverClient(new CPlexMIPSolver()));
		TestSuite.compareMultipleResults(lpSolveResult, cplexResult);
	}

	@Test
	public void pieceWiseLinearExample() {
		IMIPResult lpSolveResult = PieceWiseLinearExample.test(new SolverClient(new LPSolveMIPSolver()));
		IMIPResult cplexResult = PieceWiseLinearExample.test(new SolverClient(new CPlexMIPSolver()));
		TestSuite.compareMultipleResults(lpSolveResult, cplexResult);
	}
}

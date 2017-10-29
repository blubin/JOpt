package edu.harvard.econcs.jopt;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.server.cplex.CPlexMIPSolver;
import edu.harvard.econcs.jopt.solver.server.lpsolve.LPSolveMIPSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Fabio Isler
 */
public class SolverAccessTest {

	private static final Logger logger = LogManager.getLogger(SolverAccessTest.class);

	@Test
	public void defaultSolverAccessTest() {
		IMIP mip = TestSuite.provideTrivialExample();

		try {
			SolverClient client = new SolverClient();
			client.solve(mip);
		} catch (NoClassDefFoundError e) {
			logger.error("Probably, cplex.jar was not found. This should not happen, it should in this case fall back to lpsolve.");
			fail(e.getMessage());
		} catch (UnsatisfiedLinkError e) {
			logger.error("The native libraries of the default solver were not found. Check the logger for instructions.");
			fail(e.getMessage());
		}
	}

	@Test
	public void cplexSolverAccessTest() {
		IMIP mip = TestSuite.provideTrivialExample();

		try {
			SolverClient client = new SolverClient(new CPlexMIPSolver());
			client.solve(mip);
		} catch (NoClassDefFoundError e) {
			logger.error("Cplex.jar was not found.");
			fail(e.getMessage());
		} catch (UnsatisfiedLinkError e) {
			logger.error("The native libraries of cplex were not found. Check the logger for instructions.");
			fail(e.getMessage());
		}
	}

	@Test
	public void lpsolveSolverAccessTest() {
		IMIP mip = TestSuite.provideTrivialExample();

		try {
			SolverClient client = new SolverClient(new LPSolveMIPSolver());
			client.solve(mip);
		} catch (UnsatisfiedLinkError e) {
			logger.error("The native libraries of lpsolve were not found. Check the logger for instructions.");
			fail(e.getMessage());
		}
	}
}

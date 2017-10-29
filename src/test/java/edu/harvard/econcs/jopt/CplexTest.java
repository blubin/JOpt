package edu.harvard.econcs.jopt;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.MIPException;
import edu.harvard.econcs.jopt.solver.SolveParam;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.server.cplex.CPlexMIPSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Fabio Isler
 */
public class CplexTest {

    private static final Logger logger = LogManager.getLogger(CplexTest.class);

	@Test
    @Ignore // TODO: Find a more complex example that takes long enough for cplex
	public void testTimeoutBehavior() {
		IMIP mip = TestSuite.provideComplexExample();
		mip.setSolveParam(SolveParam.TIME_LIMIT, 0.008);
        SolverClient lpSolveSolverClient = new SolverClient(new CPlexMIPSolver());

        try {
            lpSolveSolverClient.solve(mip);
        } catch (MIPException e) {
            fail(e.getMessage());
        }

        mip.setSolveParam(SolveParam.ACCEPT_SUBOPTIMAL, true);
        try {
            lpSolveSolverClient.solve(mip);
        } catch (MIPException e) {
            fail(e.getMessage());
        }

        mip.setSolveParam(SolveParam.ACCEPT_SUBOPTIMAL, false);
        try {
            lpSolveSolverClient.solve(mip);
            fail("Should have failed. Was it too fast?");
        } catch (MIPException e) {
            // Success - should throw this error
            logger.info("Successfully caught exception for the timeout.");
        }
	}
}

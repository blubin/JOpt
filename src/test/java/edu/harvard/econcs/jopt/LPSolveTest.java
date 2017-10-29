package edu.harvard.econcs.jopt;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.MIPException;
import edu.harvard.econcs.jopt.solver.SolveParam;
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
public class LPSolveTest {

    private static final Logger logger = LogManager.getLogger(LPSolveTest.class);

    @Test
    public void compareResultsToCplex() {
        IMIP mip = TestSuite.provideTrivialExample();

        SolverClient lpSolveSolverClient = new SolverClient(new LPSolveMIPSolver());
        IMIPResult lpSolveResult = lpSolveSolverClient.solve(mip);
        SolverClient cplexSolverClient = new SolverClient(new CPlexMIPSolver());
        IMIPResult cplexResult = cplexSolverClient.solve(mip);
        TestSuite.compareMultipleResults(lpSolveResult, cplexResult);
    }

    @Test
    public void testTimeoutBehavior() {
        IMIP mip = TestSuite.provideComplexExample();
        mip.setSolveParam(SolveParam.TIME_LIMIT, 2.0);
        SolverClient lpSolveSolverClient = new SolverClient(new LPSolveMIPSolver());

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

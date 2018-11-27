package edu.harvard.econcs.jopt;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.MIPException;
import edu.harvard.econcs.jopt.solver.SolveParam;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.ISolution;
import edu.harvard.econcs.jopt.solver.mip.Variable;
import edu.harvard.econcs.jopt.solver.server.cplex.CPlexMIPSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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

	@Test
    public void testSolutionPoolMode3() {
	    testSolutionPoolMode3(1);
	    testSolutionPoolMode3(5);
	    testSolutionPoolMode3(10);
	    testSolutionPoolMode3(100);
	    testSolutionPoolMode3(1000);
    }

    private void testSolutionPoolMode3(int solutionPoolCapacity) {
        IMIP mip = TestSuite.provideComplexExample();
        mip.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 3);
        mip.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, solutionPoolCapacity);
        Set<Variable> variablesOfInterest = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 7; j++) {
                variablesOfInterest.add(mip.getVar("x_" + i + j));
            }
        }
        mip.setVariablesOfInterest(variablesOfInterest);

        SolverClient lpSolveSolverClient = new SolverClient(new CPlexMIPSolver());

        IMIPResult result = lpSolveSolverClient.solve(mip);
        ArrayList<ISolution> solutions = new ArrayList<>(result.getPoolSolutions());
        assertNonEqualSolutions(solutions);
    }



    @Test
    public void testSolutionPoolMode4() {
        testSolutionPoolMode4(1);
        testSolutionPoolMode4(5);
        testSolutionPoolMode4(10);
        testSolutionPoolMode4(100);
        testSolutionPoolMode4(1000);
    }

    private void testSolutionPoolMode4(int solutionPoolCapacity) {
        IMIP mip = TestSuite.provideComplexExample();
        //mip.setSolveParam(SolveParam.DISPLAY_OUTPUT, true);
        mip.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 4);
        mip.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, solutionPoolCapacity);

        SolverClient lpSolveSolverClient = new SolverClient(new CPlexMIPSolver());

        IMIPResult result = lpSolveSolverClient.solve(mip);
        ArrayList<ISolution> solutions = new ArrayList<>(result.getPoolSolutions());
        assertNonEqualSolutions(solutions);
    }

    @Test
    public void testSolutionPoolMode3vsMode4() {
	    /*
	     * FIXME:
	     * This fails most probably because mode 4 can find two solutions that have different
	     * values in non-decisive variables -> Same allocation, different solution
	     */
        IMIP mipMode3 = TestSuite.provideComplexExample();
        mipMode3.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 3);
        mipMode3.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 100);

        Set<Variable> variablesOfInterest = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 7; j++) {
                variablesOfInterest.add(mipMode3.getVar("x_" + i + j));
            }
        }
        mipMode3.setVariablesOfInterest(variablesOfInterest);

        SolverClient clientMode3 = new SolverClient(new CPlexMIPSolver());

        IMIPResult resultMode3 = clientMode3.solve(mipMode3);
        ArrayList<ISolution> solutionsMode3 = new ArrayList<>(resultMode3.getPoolSolutions());
        assertNonEqualSolutions(solutionsMode3);

        IMIP mipMode4 = TestSuite.provideComplexExample();
        mipMode4.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 4);
        mipMode4.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 100);

        SolverClient clientMode4 = new SolverClient(new CPlexMIPSolver());

        IMIPResult resultMode4 = clientMode4.solve(mipMode4);
        ArrayList<ISolution> solutionsMode4 = new ArrayList<>(resultMode4.getPoolSolutions());
        assertNonEqualSolutions(solutionsMode4);

        assertEquals(solutionsMode3.stream().mapToDouble(ISolution::getObjectiveValue).sum(),
                solutionsMode4.stream().mapToDouble(ISolution::getObjectiveValue).sum(), 1e-6);
    }

    private void assertNonEqualSolutions(ArrayList<ISolution> solutions) {
        for (int i = 0; i < solutions.size(); i++) {
            ISolution sol1 = solutions.get(i);
            for (int j = 0; j < solutions.size(); j++) {
                if (i != j) {
                    ISolution sol2 = solutions.get(j);
                    assertNotEquals(sol1.getValues(), sol2.getValues());
                }
            }
        }
    }
}

package edu.harvard.econcs.jopt;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.MIPException;
import edu.harvard.econcs.jopt.solver.SolveParam;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.ISolution;
import edu.harvard.econcs.jopt.solver.mip.*;
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
	    testSolutionPoolMode3(250);
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

    @Test
    public void testSimpleExampleMode3() {
        IMIP mip = TestSuite.provideSimpleExample();
        mip.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 3);
        mip.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 100);

        Set<Variable> variablesOfInterest = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            variablesOfInterest.add(mip.getVar("x" + i));
        }
        mip.setVariablesOfInterest(variablesOfInterest);
        mip.setSolveParam(SolveParam.DISPLAY_OUTPUT, true);

        SolverClient client = new SolverClient(new CPlexMIPSolver());
        IMIPResult result = client.solve(mip);
        ArrayList<ISolution> solutions = new ArrayList<>(result.getPoolSolutions());
        assertNonEqualSolutions(solutions);
    }

    @Test
    public void testSimpleExampleMode4() {
        IMIP mip = TestSuite.provideSimpleExample();
        mip.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 4);
        mip.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 100);

        mip.setSolveParam(SolveParam.DISPLAY_OUTPUT, true);

        SolverClient client = new SolverClient(new CPlexMIPSolver());
        IMIPResult result = client.solve(mip);
        ArrayList<ISolution> solutions = new ArrayList<>(result.getPoolSolutions());
        assertNonEqualSolutions(solutions);
    }

    @Test
    public void testSimpleExampleMode3vsMode4() {
        IMIP mipMode3 = TestSuite.provideSimpleExample();
        mipMode3.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 3);
        mipMode3.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 100);

        Set<Variable> variablesOfInterest = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            variablesOfInterest.add(mipMode3.getVar("x" + i));
        }
        mipMode3.setVariablesOfInterest(variablesOfInterest);

        SolverClient clientMode3 = new SolverClient(new CPlexMIPSolver());
        IMIPResult resultMode3 = clientMode3.solve(mipMode3);
        ArrayList<ISolution> solutionsMode3 = new ArrayList<>(resultMode3.getPoolSolutions());

        IMIP mipMode4 = TestSuite.provideSimpleExample();
        mipMode4.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 4);
        mipMode4.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 100);

        SolverClient clientMode4 = new SolverClient(new CPlexMIPSolver());
        IMIPResult resultMode4 = clientMode4.solve(mipMode4);
        ArrayList<ISolution> solutionsMode4 = new ArrayList<>(resultMode4.getPoolSolutions());

        assertEquals(solutionsMode3.stream().mapToDouble(ISolution::getObjectiveValue).sum(),
                solutionsMode4.stream().mapToDouble(ISolution::getObjectiveValue).sum(), 1e-6);
	}

    @Test
    public void testSimpleExampleMode3vsMode4WithIrrelevantVariables() {
        IMIP mip = TestSuite.provideSimpleExample();
        Variable irrelevantVariable = new Variable("irrelevantVariable", VarType.BOOLEAN, 0, 1);
        mip.add(irrelevantVariable);
        Constraint irrelevantConstraint = new Constraint(CompareType.LEQ, 1);
        irrelevantConstraint.addTerm(1, irrelevantVariable);
        mip.add(irrelevantConstraint);

        IMIP mipMode3 = mip.typedClone();
        logger.info("MIP Mode 3:\n{}", mipMode3);
        mipMode3.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 3);
        mipMode3.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 10);

        Set<Variable> variablesOfInterest = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            variablesOfInterest.add(mipMode3.getVar("x" + i));
        }
        mipMode3.setVariablesOfInterest(variablesOfInterest);

        SolverClient clientMode3 = new SolverClient(new CPlexMIPSolver());
        IMIPResult resultMode3 = clientMode3.solve(mipMode3);
        ArrayList<ISolution> solutionsMode3 = new ArrayList<>(resultMode3.getPoolSolutions());

        IMIP mipMode4 = mip.typedClone();
        logger.info("MIP Mode 4:\n{}", mipMode4);
        mipMode4.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 4);
        mipMode4.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 10);

        SolverClient clientMode4 = new SolverClient(new CPlexMIPSolver());
        IMIPResult resultMode4 = clientMode4.solve(mipMode4);
        ArrayList<ISolution> solutionsMode4 = new ArrayList<>(resultMode4.getPoolSolutions());

        // Make sure that the solutions have the structure we expect (two solutions in mode 4 for one solution in mode 3)
        int countForMode4 = 0;
        for (int i = 0; i < 5; i++) {
            assertEquals(solutionsMode3.get(i).getObjectiveValue(), solutionsMode4.get(countForMode4++).getObjectiveValue(), 1e-6);
            assertEquals(solutionsMode3.get(i).getObjectiveValue(), solutionsMode4.get(countForMode4++).getObjectiveValue(), 1e-6);
        }
    }

    @Test
    public void testSimpleExampleMode3vsMode4WithIrrelevantVariablesFullSet() {
        IMIP mip = TestSuite.provideSimpleExample();
        Variable irrelevantVariable = new Variable("irrelevantVariable", VarType.BOOLEAN, 0, 1);
        mip.add(irrelevantVariable);
        Constraint irrelevantConstraint = new Constraint(CompareType.LEQ, 1);
        irrelevantConstraint.addTerm(1, irrelevantVariable);
        mip.add(irrelevantConstraint);

        IMIP mipMode3 = mip.typedClone();
        logger.info("MIP Mode 3:\n{}", mipMode3);
        mipMode3.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 3);
        mipMode3.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 10);

        Set<Variable> variablesOfInterest = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            variablesOfInterest.add(mipMode3.getVar("x" + i));
        }
        // Add irrelevant variable as well
        variablesOfInterest.add(irrelevantVariable);
        mipMode3.setVariablesOfInterest(variablesOfInterest);

        SolverClient clientMode3 = new SolverClient(new CPlexMIPSolver());
        IMIPResult resultMode3 = clientMode3.solve(mipMode3);
        ArrayList<ISolution> solutionsMode3 = new ArrayList<>(resultMode3.getPoolSolutions());

        IMIP mipMode4 = mip.typedClone();
        logger.info("MIP Mode 4:\n{}", mipMode4);
        mipMode4.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 4);
        mipMode4.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 10);

        SolverClient clientMode4 = new SolverClient(new CPlexMIPSolver());
        IMIPResult resultMode4 = clientMode4.solve(mipMode4);
        ArrayList<ISolution> solutionsMode4 = new ArrayList<>(resultMode4.getPoolSolutions());

        // Make sure that the solutions are now equal
        int countForMode4 = 0;
        for (int i = 0; i < 10; i++) {
            assertEquals(solutionsMode3.get(i).getObjectiveValue(), solutionsMode4.get(countForMode4++).getObjectiveValue(), 1e-6);
        }
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

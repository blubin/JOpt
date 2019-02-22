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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Fabio Isler
 */
public class CplexTest {

    private static final Logger logger = LogManager.getLogger(CplexTest.class);

    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

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
    public void testDuplicateCheckTrue() {
        Map<String, Double> poolValuesFirst = new HashMap<>();
        poolValuesFirst.put("A", 1.0);
        poolValuesFirst.put("B", 2.0);
        poolValuesFirst.put("C", 3.0);
        poolValuesFirst.put("IRRELEVANT", 0.0);

        ISolution first = new PoolSolution(10.0, poolValuesFirst);

        Map<String, Double> poolValuesSecond = new HashMap<>();
        poolValuesSecond.put("A", 1.0);
        poolValuesSecond.put("B", 2.0);
        poolValuesSecond.put("C", 3.0);
        poolValuesSecond.put("IRRELEVANT", 1.0);

        ISolution second = new PoolSolution(10.0, poolValuesSecond);

        Set<Variable> variablesOfInterest = new HashSet<>();
        variablesOfInterest.add(new Variable("A", VarType.INT, 0, MIP.MAX_VALUE));
        variablesOfInterest.add(new Variable("B", VarType.INT, 0, MIP.MAX_VALUE));
        variablesOfInterest.add(new Variable("C", VarType.INT, 0, MIP.MAX_VALUE));
        assertTrue(first.isDuplicate(second, variablesOfInterest));
    }

    @Test
    public void testDuplicateCheckFalse() {
        Map<String, Double> poolValuesFirst = new HashMap<>();
        poolValuesFirst.put("A", 1.0);
        poolValuesFirst.put("B", 2.0);
        poolValuesFirst.put("C", 3.0);

        ISolution first = new PoolSolution(10.0, poolValuesFirst);

        Map<String, Double> poolValuesSecond = new HashMap<>();
        poolValuesSecond.put("A", 1.0);
        poolValuesSecond.put("B", 2.0);
        poolValuesSecond.put("C", 0.0);

        ISolution second = new PoolSolution(10.0, poolValuesSecond);

        Set<Variable> variablesOfInterest = new HashSet<>();
        variablesOfInterest.add(new Variable("A", VarType.INT, 0, MIP.MAX_VALUE));
        variablesOfInterest.add(new Variable("B", VarType.INT, 0, MIP.MAX_VALUE));
        variablesOfInterest.add(new Variable("C", VarType.INT, 0, MIP.MAX_VALUE));
        assertFalse(first.isDuplicate(second, variablesOfInterest));
    }

	@Test
    public void testSolutionPoolMode3() {
	    testSolutionPoolMode3(2);
	    testSolutionPoolMode3(5);
	    testSolutionPoolMode3(10);
	    testSolutionPoolMode3(50);
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
        assertNonEqualSolutions(solutions, variablesOfInterest);
    }



    @Test
    public void testSolutionPoolMode4() {
        testSolutionPoolMode4(2);
        testSolutionPoolMode4(5);
        testSolutionPoolMode4(10);
        testSolutionPoolMode4(100);
        testSolutionPoolMode4(1000);
    }

    private void testSolutionPoolMode4(int solutionPoolCapacity) {
        IMIP mip = TestSuite.provideComplexExample();
        mip.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 4);
        mip.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, solutionPoolCapacity);

        SolverClient lpSolveSolverClient = new SolverClient(new CPlexMIPSolver());

        IMIPResult result = lpSolveSolverClient.solve(mip);
        ArrayList<ISolution> solutions = new ArrayList<>(result.getPoolSolutions());
        assertNonEqualSolutions(solutions);
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

        SolverClient client = new SolverClient(new CPlexMIPSolver());
        IMIPResult result = client.solve(mip);
        ArrayList<ISolution> solutions = new ArrayList<>(result.getPoolSolutions());
        assertNonEqualSolutions(solutions);
    }

    @Test
    public void testSimpleExampleMode4() {
        testSimpleExampleMode4(2);
        testSimpleExampleMode4(10);
        testSimpleExampleMode4(100);
        testSimpleExampleMode4(200);
        testSimpleExampleMode4(500);
        testSimpleExampleMode4(800);
    }

    private void testSimpleExampleMode4(int capacity) {
        IMIP mip = TestSuite.provideSimpleExample();
        mip.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 4);
        mip.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, capacity);

        SolverClient client = new SolverClient(new CPlexMIPSolver());
        IMIPResult result = client.solve(mip);
        ArrayList<ISolution> solutions = new ArrayList<>(result.getPoolSolutions());
        assertNonEqualSolutions(solutions);
        ISolution lastSolution = solutions.get(solutions.size() - 1);
        assertEquals(capacity - 1, lastSolution.getObjectiveValue(), 1e-10);
    }

    @Test
    public void testSimpleExampleMode4NotEnoughSolutions() {
        IMIP mip = TestSuite.provideSimpleExample();
        mip.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 4);
        mip.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 1100);

        SolverClient client = new SolverClient(new CPlexMIPSolver());
        IMIPResult result = client.solve(mip);
        ArrayList<ISolution> solutions = new ArrayList<>(result.getPoolSolutions());
        assertNonEqualSolutions(solutions);
        assertTrue(solutions.size() < 1100);
        ISolution lastSolution = solutions.get(solutions.size() - 1);
        assertEquals(999, lastSolution.getObjectiveValue(), 1e-10);
    }

    @Test
    public void testMode4WithIrrelevantVariableAndVariablesOfInterest() {
        testMode4WithIrrelevantVariableAndVariablesOfInterest(2);
        testMode4WithIrrelevantVariableAndVariablesOfInterest(10);
        testMode4WithIrrelevantVariableAndVariablesOfInterest(100);
        testMode4WithIrrelevantVariableAndVariablesOfInterest(200);
        testMode4WithIrrelevantVariableAndVariablesOfInterest(500);
        testMode4WithIrrelevantVariableAndVariablesOfInterest(1000);
    }

    private void testMode4WithIrrelevantVariableAndVariablesOfInterest(int capacity) {
        IMIP mip = TestSuite.provideSimpleExample();
        Variable irrelevantVariable = new Variable("irrelevantVariable", VarType.BOOLEAN, 0, 1);
        mip.add(irrelevantVariable);
        Constraint irrelevantConstraint = new Constraint(CompareType.LEQ, 1);
        irrelevantConstraint.addTerm(1, irrelevantVariable);
        mip.add(irrelevantConstraint);

        mip.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 4);
        mip.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, capacity);

        Set<Variable> variablesOfInterest = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            variablesOfInterest.add(mip.getVar("x" + i));
        }
        mip.setVariablesOfInterest(variablesOfInterest);

        SolverClient client = new SolverClient(new CPlexMIPSolver());
        IMIPResult result = client.solve(mip);
        ArrayList<ISolution> solutions = new ArrayList<>(result.getPoolSolutions());
        assertNonEqualSolutions(solutions, variablesOfInterest);
        ISolution lastSolution = solutions.get(solutions.size() - 1);
        assertEquals(capacity - 1, lastSolution.getObjectiveValue(), 1e-10);
    }

    @Test
    public void testMode3vsMode4() {
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
    public void testMode3vsMode4WithIrrelevantVariables() {
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
    public void testMode3vsMode4WithIrrelevantVariablesAndVariablesOfInterest() {
        IMIP mip = TestSuite.provideSimpleExample();
        Variable irrelevantVariable = new Variable("irrelevantVariable", VarType.BOOLEAN, 0, 1);
        mip.add(irrelevantVariable);
        Constraint irrelevantConstraint = new Constraint(CompareType.LEQ, 1);
        irrelevantConstraint.addTerm(1, irrelevantVariable);
        mip.add(irrelevantConstraint);

        IMIP mipMode3 = mip.typedClone();
        logger.info("MIP Mode 3:\n{}", mipMode3);
        mipMode3.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 3);
        mipMode3.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 100);

        Set<Variable> variablesOfInterestMode3 = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            variablesOfInterestMode3.add(mipMode3.getVar("x" + i));
        }
        mipMode3.setVariablesOfInterest(variablesOfInterestMode3);

        SolverClient clientMode3 = new SolverClient(new CPlexMIPSolver());
        IMIPResult resultMode3 = clientMode3.solve(mipMode3);
        ArrayList<ISolution> solutionsMode3 = new ArrayList<>(resultMode3.getPoolSolutions());

        IMIP mipMode4 = mip.typedClone();
        logger.info("MIP Mode 4:\n{}", mipMode4);
        mipMode4.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 4);
        mipMode4.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 100);

        Set<Variable> variablesOfInterestMode4 = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            variablesOfInterestMode4.add(mipMode4.getVar("x" + i));
        }
        mipMode4.setVariablesOfInterest(variablesOfInterestMode4);

        SolverClient clientMode4 = new SolverClient(new CPlexMIPSolver());
        IMIPResult resultMode4 = clientMode4.solve(mipMode4);
        ArrayList<ISolution> solutionsMode4 = new ArrayList<>(resultMode4.getPoolSolutions());

        // Make sure that by setting variables of interest in mode 4, we achieve the same as in mode 3
        for (int i = 0; i < 100; i++) {
            assertEquals(solutionsMode3.get(i).getObjectiveValue(), solutionsMode4.get(i).getObjectiveValue(), 1e-6);
        }
    }

    @Test
    public void testMode3vsMode4WithIrrelevantVariablesFullSet() {
        IMIP mip = TestSuite.provideSimpleExample();
        Variable irrelevantVariable = new Variable("irrelevantVariable", VarType.BOOLEAN, 0, 1);
        mip.add(irrelevantVariable);
        Constraint irrelevantConstraint = new Constraint(CompareType.LEQ, 1);
        irrelevantConstraint.addTerm(1, irrelevantVariable);
        mip.add(irrelevantConstraint);

        IMIP mipMode3 = mip.typedClone();
        logger.info("MIP Mode 3:\n{}", mipMode3);
        mipMode3.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 3);
        mipMode3.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 100);

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
        mipMode4.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 100);

        SolverClient clientMode4 = new SolverClient(new CPlexMIPSolver());
        IMIPResult resultMode4 = clientMode4.solve(mipMode4);
        ArrayList<ISolution> solutionsMode4 = new ArrayList<>(resultMode4.getPoolSolutions());

        // Make sure that the solutions are now equal
        for (int i = 0; i < 100; i++) {
            assertEquals(solutionsMode3.get(i).getObjectiveValue(), solutionsMode4.get(i).getObjectiveValue(), 1e-6);
        }
    }

    @Test
    public void testSettingMode4GapTolerance() {
        IMIP mip = TestSuite.provideSimpleExample();
        Variable irrelevantVariable = new Variable("irrelevantVariable", VarType.BOOLEAN, 0, 1);
        mip.add(irrelevantVariable);
        Constraint irrelevantConstraint = new Constraint(CompareType.LEQ, 1);
        irrelevantConstraint.addTerm(1, irrelevantVariable);
        mip.add(irrelevantConstraint);
        Set<Variable> variablesOfInterestMode4 = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            variablesOfInterestMode4.add(mip.getVar("x" + i));
        }
        mip.setVariablesOfInterest(variablesOfInterestMode4);

        logger.info("MIP:\n{}", mip);
        mip.setSolveParam(SolveParam.SOLUTION_POOL_MODE, 4);
        mip.setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 10);
        mip.setSolveParam(SolveParam.SOLUTION_POOL_MODE_4_ABSOLUTE_GAP_TOLERANCE, 600.0);

        SolverClient client = new SolverClient(new CPlexMIPSolver());
        // Following the log, it's visible that the solution pool population was terminated early because of
        // the absolute gap tolerance
        IMIPResult result = client.solve(mip);
        System.out.println(result);
    }

    private void assertNonEqualSolutions(ArrayList<ISolution> solutions, Collection<Variable> variablesOfInterest) {
        for (int i = 0; i < solutions.size(); i++) {
            ISolution sol1 = solutions.get(i);
            for (int j = 0; j < solutions.size(); j++) {
                if (i != j) {
                    ISolution sol2 = solutions.get(j);
                    assertNotEquals(sol1.getValues(), sol2.getValues());
                    if (variablesOfInterest != null) {
                        assertFalse(sol1.isDuplicate(sol2, variablesOfInterest));
                    }
                }
            }
        }
    }

    private void assertNonEqualSolutions(ArrayList<ISolution> solutions) {
        assertNonEqualSolutions(solutions, null);
    }
}

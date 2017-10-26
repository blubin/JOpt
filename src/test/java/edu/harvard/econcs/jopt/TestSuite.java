package edu.harvard.econcs.jopt;

import edu.harvard.econcs.jopt.example.ComplexExample;
import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.mip.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.util.Map;

/**
 * @author Fabio Isler
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        SolverAccessTest.class,
        Examples.class,
        LPSolveTest.class,
        CplexTest.class
})
public class TestSuite {

    @BeforeClass
    public static void init() {

    }

    @AfterClass
    public static void cleanUp() {

    }

    public static void compareMultipleResults(IMIPResult... results) {
        compare(true, results);
    }

    public static void compareObjectiveMultipleResults(IMIPResult... results) {
        compare(false, results);
    }

    private static void compare(boolean checkForEqualVariables, IMIPResult... results) {
        for (IMIPResult result : results) {
            for (IMIPResult comparison : results) {
                Assert.assertEquals(result.getObjectiveValue(), comparison.getObjectiveValue(), 0.00000001);
                Assert.assertEquals(result.getValues().size(), comparison.getValues().size());
                if (checkForEqualVariables) {
                    for (Map.Entry<String, Double> entry : result.getValues().entrySet()) {
                        Assert.assertEquals("Expected " + entry.getKey() + " to be equal in both results.", entry.getValue(), comparison.getValues().get(entry.getKey()), 0.00000001);
                    }
                }
            }
        }
    }

    public static IMIP provideBanalExample() {
        MIP mip = new MIP();
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

        return mip;
    }

    public static IMIP provideComplexExample() {
        double[] factories = new double[]{3, 2, 3, 2, 3, 2, 3, 2};
        double[] customers = new double[]{2, 1, 1, 2, 1, 1, 2, 1, 1, 2, 1, 1};
        double[][] costs = new double[][]{
                {2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5},    // Costs from Factory 1
                {0.5, 1, 2, 0.5, 1, 2, 0.5, 1, 2, 0.5, 1, 2},  // Costs from Factory 2
                {0.5, 1, 2, 0.5, 1, 2, 0.5, 1, 2, 0.5, 1, 2},  // Costs from Factory 3
                {0.5, 1, 2, 0.5, 1, 2, 0.5, 1, 2, 0.5, 1, 2},  // Costs from Factory 4
                {2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5},    // Costs from Factory 5
                {0.5, 1, 2, 0.5, 1, 2, 0.5, 1, 2, 0.5, 1, 2},  // Costs from Factory 6
                {2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5},    // Costs from Factory 7
                {0.5, 1, 2, 0.5, 1, 2, 0.5, 1, 2, 0.5, 1, 2},  // Costs from Factory 8
        };
        double[][] fixedCosts = new double[][]{
                {1, 2, 10, 1, 2, 2, 3, 3, 1, 4, 4, 7},    // Costs from Factory 1
                {1, 1, 4, 2, 2, 2, 5, 3, 3, 4, 4, 4},    // Costs from Factory 2
                {1, 1, 73, 2, 2, 2, 3, 3, 1, 4, 4, 4},    // Costs from Factory 3
                {1, 1, 21, 2, 2, 2, 3, 3, 3, 4, 4, 4},    // Costs from Factory 4
                {4, 4, 0, 2, 2, 3, 7, 63, 3, 1, 1, 1},    // Costs from Factory 5
                {1, 1, 0, 2, 2, 2, 3, 3, 1, 4, 4, 4},    // Costs from Factory 6
                {1, 1, 1, 2, 5, 2, 3, 8, 3, 2, 2, 2},    // Costs from Factory 7
                {1, 1, 1, 2, 2, 2, 43, 6, 3, 4, 4, 4},    // Costs from Factory 8
        };
        double[] extraCosts = new double[]{3.5, 0.5, 1, 4, 2, 3.5, 3, 2.5};

        return new ComplexExample().buildMIP(factories, customers, costs, fixedCosts, extraCosts, true);

    }
}



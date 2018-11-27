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

import java.util.ArrayList;
import java.util.Iterator;
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

    public static IMIP provideTrivialExample() {
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
        int noOfFactories = 10;
        int noOfCustomers = 7;

        ArrayList<Double> alFactories = new ArrayList<>();
        for (int i = 0; i < noOfFactories; i++) {
            alFactories.add((double) (i % 3) + 1);
        }

        ArrayList<Double> alCustomers = new ArrayList<>();
        for (int i = 0; i < noOfCustomers; i++) {
            alCustomers.add((double) ((i + 5) % 4) + 1);
        }

        ArrayList<ArrayList<Double>> alCosts = new ArrayList<>();
        ArrayList<ArrayList<Double>> alFixedCosts = new ArrayList<>();
        for (double f : alFactories) {
            ArrayList<Double> alC = new ArrayList<>();
            ArrayList<Double> alF = new ArrayList<>();
            for (double c : alCustomers) {
                alC.add((f + c + 2) % 3 + 1);
                alF.add((f + c + 5) % 4 + 1);
            }
            alCosts.add(alC);
            alFixedCosts.add(alF);
        }

        ArrayList<Double> alExtraCosts = new ArrayList<>();
        for (int i = 0; i < noOfFactories; i++) {
            alExtraCosts.add((double) ((i + 2) % 4) + 1);
        }

        double[][] costs = new double[alFactories.size()][];
        double[][] fixedCosts = new double[alFactories.size()][];
        for (int i = 0; i < alFactories.size(); i++) {
            costs[i] = convertDoubles(alCosts.get(i));
            fixedCosts[i] = convertDoubles(alFixedCosts.get(i));
        }



        return new ComplexExample().buildMIP(
                convertDoubles(alFactories),
                convertDoubles(alCustomers),
                costs,
                fixedCosts,
                convertDoubles(alExtraCosts),
                true);
    }

    public static IMIP provideSimpleExample() {
        IMIP mip = new MIP();
        Variable x1 = new Variable("x1", VarType.BOOLEAN, 0, 1); mip.add(x1);
        Variable x2 = new Variable("x2", VarType.BOOLEAN, 0, 1); mip.add(x2);
        Variable x3 = new Variable("x3", VarType.BOOLEAN, 0, 1); mip.add(x3);
        Variable x4 = new Variable("x4", VarType.BOOLEAN, 0, 1); mip.add(x4);
        Variable x5 = new Variable("x5", VarType.BOOLEAN, 0, 1); mip.add(x5);
        Variable x6 = new Variable("x6", VarType.BOOLEAN, 0, 1); mip.add(x6);
        Variable x7 = new Variable("x7", VarType.BOOLEAN, 0, 1); mip.add(x7);
        Variable x8 = new Variable("x8", VarType.BOOLEAN, 0, 1); mip.add(x8);
        Variable x9 = new Variable("x9", VarType.BOOLEAN, 0, 1); mip.add(x9);
        Variable x10 = new Variable("x10", VarType.BOOLEAN, 0, 1); mip.add(x10);

        mip.addObjectiveTerm(128, x1);
        mip.addObjectiveTerm(256, x2);
        mip.addObjectiveTerm(2, x3);
        mip.addObjectiveTerm(32, x4);
        mip.addObjectiveTerm(8, x5);
        mip.addObjectiveTerm(16, x6);
        mip.addObjectiveTerm(4, x7);
        mip.addObjectiveTerm(1, x8);
        mip.addObjectiveTerm(512, x9);
        mip.addObjectiveTerm(64, x10);

        Constraint c1 = new Constraint(CompareType.LEQ, 999); mip.add(c1);
        c1.addTerm(128, x1);
        c1.addTerm(256, x2);
        c1.addTerm(2, x3);
        c1.addTerm(32, x4);
        c1.addTerm(8, x5);
        c1.addTerm(16, x6);
        c1.addTerm(4, x7);
        c1.addTerm(1, x8);
        c1.addTerm(512, x9);
        c1.addTerm(64, x10);

        return mip;
    }

    private static double[] convertDoubles(ArrayList<Double> doubles) {
        double[] ret = new double[doubles.size()];
        Iterator<Double> iterator = doubles.iterator();
        for (int i = 0; i < ret.length; i++) {
            ret[i] = iterator.next();
        }
        return ret;
    }
}



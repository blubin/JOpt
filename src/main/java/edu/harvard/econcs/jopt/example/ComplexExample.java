package edu.harvard.econcs.jopt.example;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.mip.MIP;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.mip.CompareType;
import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.VarType;
import edu.harvard.econcs.jopt.solver.mip.Variable;

/**
 * An example for a multi-commodity transportation model
 *
 * @author Fabio Isler
 */
public class ComplexExample {

    private IMIP mip;

    public IMIP getMIP() {
        return mip;
    }

    /**
     * @param factories Array containing each factory's availability
     * @param customers Array containing each customer's demand
     * @param costs 2D-Array containing costs for the transportation from factory i to customer j
     * @param fixedCosts 2D-Array containing the fixed costs if at least one unit is shipped from factory i to customer j
     * @param extraCosts Array containing each factories's cost per customer they serve beyond the first
     * @param transportUnitsAreFractional: Flag whether to accept fractional units to be transported
     */
    public IMIP buildMIP(double[] factories, double[] customers, double[][] costs, double[][] fixedCosts, double[] extraCosts, boolean transportUnitsAreFractional) {

        mip = new MIP();

        int n = factories.length;
        int m = customers.length;

        mip.setObjectiveMax(false);

        Variable[][] flow = new Variable[n][m];
        Variable[][] used = new Variable[n][m];
        Variable[] extra = new Variable[n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                // Non-negativity constraints are implemented in the variable definition already
                // Change for d) is implemented by the inline if-else statement "transportUnitsAreFractional ? VarType.DOUBLE : VarType.INT"
                flow[i][j] = new Variable("flow_" + i + j, transportUnitsAreFractional ? VarType.DOUBLE : VarType.INT, 0, MIP.MAX_VALUE);
                mip.add(flow[i][j]);
                used[i][j] = new Variable("x_" + i + j, VarType.BOOLEAN, 0, 1);
                mip.add(used[i][j]);
                mip.addObjectiveTerm(costs[i][j], flow[i][j]);
                mip.addObjectiveTerm(fixedCosts[i][j], used[i][j]);

                // We have defined enough to create the fixed charge constraints already
                // -> To avoid going through the two loops again later, I add it here already
                Constraint trigger = new Constraint(CompareType.LEQ, 0);
                trigger.addTerm(1, flow[i][j]);
                trigger.addTerm(-MIP.MAX_VALUE, used[i][j]);
                mip.add(trigger);
            }
            extra[i] = new Variable("y_" + i, VarType.INT, 0, MIP.MAX_VALUE);
            mip.add(extra[i]);
            mip.addObjectiveTerm(extraCosts[i], extra[i]);
        }

        // Capacity & Extra cost constraints
        Constraint capacity;
        Constraint extraCostConstraint;
        for (int i = 0; i < n; i++) {
            capacity = new Constraint(CompareType.LEQ, factories[i]);
            extraCostConstraint = new Constraint(CompareType.LEQ, 1);
            for (int j = 0; j < m; j++) {
                capacity.addTerm(1, flow[i][j]);
                extraCostConstraint.addTerm(1, used[i][j]);
            }
            mip.add(capacity);

            extraCostConstraint.addTerm(-1, extra[i]);
            mip.add(extraCostConstraint);
        }

        // Demand constraints
        Constraint demand;
        for (int j = 0; j < m; j++) {
            demand = new Constraint(CompareType.GEQ, customers[j]);
            for (int i = 0; i < n; i++) {
                demand.addTerm(1, flow[i][j]);
            }
            mip.add(demand);
        }
        return mip;
    }

    public IMIPResult solve(SolverClient client) {
        return client.solve(mip);
    }

    public static void main(String[] args) {

        ComplexExample example = new ComplexExample();

        // Test case 1: Cost should be 8.0
        double[] factories = new double[]{3, 2};
        double[] customers = new double[]{2, 1, 1};
        double[][] costs = new double[][]{
                {2, 2, 2},    // Costs from Factory 1
                {0.5, 1, 2},  // Costs from Factory 2
        };
        double[][] fixedCosts = new double[][]{
                {1, 1, 1},    // Costs from Factory 1
                {1, 1, 1},    // Costs from Factory 2
        };
        double[] extraCosts = new double[]{3.5, 0.5};

        example.buildMIP(factories, customers, costs, fixedCosts, extraCosts, true);

        System.out.println(example.getMIP());
        System.out.println(example.solve(new SolverClient()));
    }

    public static IMIPResult test(SolverClient client) {
        ComplexExample example = new ComplexExample();
        // Test case 1: Cost should be 8.0
        double[] factories = new double[]{3, 2};
        double[] customers = new double[]{2, 1, 1};
        double[][] costs = new double[][]{
                {2, 2, 2},    // Costs from Factory 1
                {0.5, 1, 2},  // Costs from Factory 2
        };
        double[][] fixedCosts = new double[][]{
                {1, 1, 1},    // Costs from Factory 1
                {1, 1, 1},    // Costs from Factory 2
        };
        double[] extraCosts = new double[]{3.5, 0.5};

        example.buildMIP(factories, customers, costs, fixedCosts, extraCosts, true);

        return example.solve(client);
    }
}

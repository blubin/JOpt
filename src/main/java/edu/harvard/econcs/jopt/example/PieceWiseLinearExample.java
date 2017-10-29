package edu.harvard.econcs.jopt.example;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.mip.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An example for a MIP that includes a piecewise linear function
 *
 * @author Fabio Isler
 */
public class PieceWiseLinearExample {

    private static final Logger logger = LogManager.getLogger(PieceWiseLinearExample.class);

    private IMIP mip;

    public IMIP getMIP() {
        return mip;
    }


    public void buildMIP() {

        mip = new MIP();

        mip.setObjectiveMax(true);

        Variable x = new Variable("x", VarType.DOUBLE, 0, 3);
        Variable y = new Variable("y", VarType.DOUBLE, -MIP.MAX_VALUE, MIP.MAX_VALUE);
        mip.add(x);
        mip.add(y);

        mip.addObjectiveTerm(1, y);

        Variable Z1 = new Variable("Z1", VarType.BOOLEAN, 0,1);
        Variable Z2 = new Variable("Z2", VarType.BOOLEAN, 0,1);
        Variable Z3 = new Variable("Z3", VarType.BOOLEAN, 0,1);
        Variable condX1 = new Variable("condX1", VarType.DOUBLE, 0,3);
        Variable condX2 = new Variable("condX2", VarType.DOUBLE, 0,3);
        Variable condX3 = new Variable("condX3", VarType.DOUBLE, 0,3);
        mip.add(Z1);
        mip.add(Z2);
        mip.add(Z3);
        mip.add(condX1);
        mip.add(condX2);
        mip.add(condX3);


        Constraint c1 = new Constraint(CompareType.LEQ, 0);
        c1.addTerm(0, Z1);
        c1.addTerm(-1, condX1);
        mip.add(c1);
        Constraint c2 = new Constraint(CompareType.LEQ, 0);
        c2.addTerm(-1, Z1);
        c2.addTerm(1, condX1);
        mip.add(c2);
        Constraint c3 = new Constraint(CompareType.LEQ, 0);
        c3.addTerm(1, Z2);
        c3.addTerm(-1, condX2);
        mip.add(c3);
        Constraint c4 = new Constraint(CompareType.LEQ, 0);
        c4.addTerm(-2, Z2);
        c4.addTerm(1, condX2);
        mip.add(c4);
        Constraint c5 = new Constraint(CompareType.LEQ, 0);
        c5.addTerm(-3, Z3);
        c5.addTerm(1, condX3);
        mip.add(c5);
        Constraint c6 = new Constraint(CompareType.LEQ, 0);
        c6.addTerm(2, Z3);
        c6.addTerm(-1, condX3);
        mip.add(c6);

        Constraint c7 = new Constraint(CompareType.EQ, 1);
        c7.addTerm(1, Z1);
        c7.addTerm(1, Z2);
        c7.addTerm(1, Z3);
        mip.add(c7);
        Constraint c8 = new Constraint(CompareType.EQ, 0);
        c8.addTerm(1, condX1);
        c8.addTerm(1, condX2);
        c8.addTerm(1, condX3);
        c8.addTerm(-1, x);
        mip.add(c8);

        // 0.0 Z1 + 0.0 Z1 + 1.0 Z2 - 3.0 Z2 + 4.0 Z3 + 2.0 Z3 + 1.0 condX1 + 3.0 condX2 - 1.0 condX3 - 1.0 y == 0.0
        Constraint c9 = new Constraint(CompareType.EQ, 0);
        c9.addTerm(0, Z1);
        c9.addTerm(0, Z1);
        c9.addTerm(1, Z2);
        c9.addTerm(-3, Z2);
        c9.addTerm(4, Z3);
        c9.addTerm(2, Z3);
        c9.addTerm(1, condX1);
        c9.addTerm(3, condX2);
        c9.addTerm(-1, condX3);
        c9.addTerm(-1, y);
        mip.add(c9);

    }

    public IMIPResult solve(SolverClient client) {
        return client.solve(mip);
    }

    public static void main(String[] args) {

        PieceWiseLinearExample example = new PieceWiseLinearExample();

        example.buildMIP();

        IMIP mip = example.getMIP();
        logger.info(mip);

        IMIPResult result = example.solve(new SolverClient());
        logger.info(result);
    }

    public static IMIPResult test(SolverClient client) {
        PieceWiseLinearExample example = new PieceWiseLinearExample();
        example.buildMIP();
        return example.solve(client);
    }
}

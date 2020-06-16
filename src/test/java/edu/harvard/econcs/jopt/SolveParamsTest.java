package edu.harvard.econcs.jopt;

import edu.harvard.econcs.jopt.solver.*;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.server.cplex.CPlexMIPSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * @author Fabio Isler
 */
public class SolveParamsTest {

    private static final Logger logger = LogManager.getLogger(SolveParamsTest.class);

    @Test
    public void testDefaultSolveParams() {
        IMIP mip = TestSuite.provideSimpleExample();
        assertThat(mip.getIntSolveParam(SolveParam.CLOCK_TYPE, 1), is(2));
        assertThat(mip.getBooleanSolveParam(SolveParam.ZERO_MISSING_PROPOSED, false), is(true));
        assertThat(mip.getStringSolveParam(SolveParam.PROBLEM_FILE, "default"), is(""));
        assertThat(mip.getDoubleSolveParam(SolveParam.CONSTRAINT_BACKOFF_LIMIT, 0d), is(.1d));
        assertThat(mip.getBooleanSolveParam(SolveParam.DISPLAY_OUTPUT, true), is(false));
        assertThat(mip.getBooleanSolveParam(SolveParam.CALCULATE_CONFLICT_SET, false), is(true));
        assertThat(mip.getDoubleSolveParam(SolveParam.MAX_OBJ_THRESHOLD, 0d), is(1e75));
        assertThat(mip.getDoubleSolveParam(SolveParam.MIN_OBJ_VALUE, 0d), is(-1e75));
        assertThat(mip.getIntSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 1), is(0));
        assertThat(mip.getIntSolveParam(SolveParam.SOLUTION_POOL_MODE, 1), is(0));
        assertThat(mip.getIntSolveParam(SolveParam.PARALLEL_MODE, 0), is(1));
    }

    @Test
    public void testOverridingSolveParams() {
        IMIP mip = TestSuite.provideSimpleExample();
        mip.setSolveParam(SolveParam.PARALLEL_MODE, 0);
        assertThat(mip.getIntSolveParam(SolveParam.PARALLEL_MODE, 1), is(0));
    }

    @Test
    public void testAmbiguousTypeSolveParams() {
        IMIP mip = TestSuite.provideSimpleExample();
        mip.setSolveParam(SolveParam.DISPLAY_OUTPUT, "true");
        assertThat(mip.getBooleanSolveParam(SolveParam.DISPLAY_OUTPUT, false), is(true));
        // The following is actually something that happened in experiments before: If you provided an integer instead
        // of a double, the parameter was simply ignored by the previous method. Now, it is successfully transformed
        // into a proper double.
        mip.setSolveParam(SolveParam.CONSTRAINT_BACKOFF_LIMIT, 2);
        assertThat(mip.getDoubleSolveParam(SolveParam.CONSTRAINT_BACKOFF_LIMIT, 1d), is(2d));
    }

    @Test
    public void testDisplayOutput() {
        IMIP mip = TestSuite.provideSimpleExample();
        mip.setSolveParam(SolveParam.DISPLAY_OUTPUT, true);
        logger.info("Here should be a log:");
        new SolverClient(new CPlexMIPSolver()).solve(mip);
        mip.setSolveParam(SolveParam.DISPLAY_OUTPUT, false);
        logger.info("Here should be NO log:");
        new SolverClient(new CPlexMIPSolver()).solve(mip);
        mip.setSolveParam(SolveParam.DISPLAY_OUTPUT, true);
        logger.info("Here should be a log again:");
        new SolverClient(new CPlexMIPSolver()).solve(mip);
        mip.setSolveParam(SolveParam.DISPLAY_OUTPUT, false);
        logger.info("Here should be no log again:");
        new SolverClient(new CPlexMIPSolver()).solve(mip);
    }
}

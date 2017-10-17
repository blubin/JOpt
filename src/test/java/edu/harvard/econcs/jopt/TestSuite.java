/**
 * Copyright by Michael Weiss, weiss.michael@gmx.ch
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package edu.harvard.econcs.jopt;

import edu.harvard.econcs.jopt.example.ComplexExample;
import edu.harvard.econcs.jopt.example.SimpleLPExample;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.util.Map;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        LPSolveTest.class,
        Examples.class,
        SolverAccessTest.class
})

public class TestSuite {

    @BeforeClass
    public static void init() {

    }

    @AfterClass
    public static void cleanUp() {

    }

    public static void compareMultipleResults(IMIPResult... results) {
        for (IMIPResult result : results) {
            for (IMIPResult comparison : results) {
                Assert.assertEquals(result.getObjectiveValue(), comparison.getObjectiveValue(), 0.00000001);
                Assert.assertEquals(result.getValues().size(), comparison.getValues().size());
                for (Map.Entry<String, Double> entry : result.getValues().entrySet()) {
                    Assert.assertEquals(entry.getValue(), comparison.getValues().get(entry.getKey()), 0.00000001);
                }
            }
        }
    }
}

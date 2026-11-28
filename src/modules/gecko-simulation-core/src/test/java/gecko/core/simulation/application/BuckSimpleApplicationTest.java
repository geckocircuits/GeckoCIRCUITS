/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.simulation.application;

import gecko.core.testutil.TraceComparisonHelper;
import gecko.core.testutil.TraceComparisonHelper.StepData;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Compares physical buck-converter state against the independently generated desktop trace. */
@Tag("application")
public class BuckSimpleApplicationTest {
    private static final double RELATIVE_TOLERANCE = 0.01;

    @Test
    void namedSignalComparison() throws Exception {
        List<StepData> golden = ConverterTraceFixture.golden("buck_simple");
        List<StepData> actual = ConverterTraceFixture.actual("buck_simple");
        assertEquals(golden.size(), actual.size(), "Every reference step must be checked");
        for (int index = 0; index < golden.size(); index++) {
            StepData expected = golden.get(index);
            StepData observed = actual.get(index);
            assertEquals(expected.time(), observed.time(), 1e-12);
            assertEquals(expected.switchR(), observed.switchR(), "Switch state at step " + index);
            assertEquals(expected.diodeR(), observed.diodeR(), "Diode state at step " + index);
            assertEquals(expected.gateSignals(), observed.gateSignals(), "Applied gate at step " + index);
        }
    }

    @Test
    void groundReferencedTraceComparison() throws Exception {
        var result = TraceComparisonHelper.compare(ConverterTraceFixture.golden("buck_simple"),
                ConverterTraceFixture.actual("buck_simple"), RELATIVE_TOLERANCE);
        assertTrue(result.match(), () -> "First divergent buck state: " + result);
    }

    @Test
    void goldenTraceStepCountMatches() throws Exception {
        assertEquals(ConverterTraceFixture.golden("buck_simple").size(),
                ConverterTraceFixture.actual("buck_simple").size());
    }
}

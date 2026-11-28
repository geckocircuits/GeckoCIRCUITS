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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Compares all physical boost-converter state against the desktop trace. */
@Tag("application")
public class BoostSimpleApplicationTest {
    private static final double RELATIVE_TOLERANCE = 0.01;

    @Test
    void goldenTraceComparison() throws Exception {
        var result = TraceComparisonHelper.compare(ConverterTraceFixture.golden("boost_simple"),
                ConverterTraceFixture.actual("boost_simple"), RELATIVE_TOLERANCE);
        assertTrue(result.match(), () -> "First divergent boost state: " + result);
    }
}

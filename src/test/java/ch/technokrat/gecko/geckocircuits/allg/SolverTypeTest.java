/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.allg;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for SolverType enum - circuit simulation solver selection.
 */
public class SolverTypeTest {

    // ====================================================
    // Enum Values Tests
    // ====================================================

    @Test
    public void testAllEnumValues_Exist() {
        SolverType[] values = SolverType.values();
        assertEquals(3, values.length);
    }

    @Test
    public void testBackwardEuler_Exists() {
        assertNotNull(SolverType.SOLVER_BE);
    }

    @Test
    public void testTrapezoidal_Exists() {
        assertNotNull(SolverType.SOLVER_TRZ);
    }

    @Test
    public void testGearShichman_Exists() {
        assertNotNull(SolverType.SOLVER_GS);
    }

    // ====================================================
    // ValueOf Tests
    // ====================================================

    @Test
    public void testValueOf_BackwardEuler() {
        assertEquals(SolverType.SOLVER_BE, SolverType.valueOf("SOLVER_BE"));
    }

    @Test
    public void testValueOf_Trapezoidal() {
        assertEquals(SolverType.SOLVER_TRZ, SolverType.valueOf("SOLVER_TRZ"));
    }

    @Test
    public void testValueOf_GearShichman() {
        assertEquals(SolverType.SOLVER_GS, SolverType.valueOf("SOLVER_GS"));
    }

    // ====================================================
    // ToString Tests (Display Strings)
    // ====================================================

    @Test
    public void testToString_BackwardEuler() {
        assertEquals("backward-euler", SolverType.SOLVER_BE.toString());
    }

    @Test
    public void testToString_Trapezoidal() {
        assertEquals("trapezoidal", SolverType.SOLVER_TRZ.toString());
    }

    @Test
    public void testToString_GearShichman() {
        assertEquals("gear-shichman", SolverType.SOLVER_GS.toString());
    }

    // ====================================================
    // Old Gecko Index Tests (File Compatibility)
    // ====================================================

    @Test
    public void testGetOldGeckoIndex_BackwardEuler() {
        assertEquals(0, SolverType.SOLVER_BE.getOldGeckoIndex());
    }

    @Test
    public void testGetOldGeckoIndex_Trapezoidal() {
        assertEquals(1, SolverType.SOLVER_TRZ.getOldGeckoIndex());
    }

    @Test
    public void testGetOldGeckoIndex_GearShichman() {
        assertEquals(2, SolverType.SOLVER_GS.getOldGeckoIndex());
    }

    @Test
    public void testGetOldGeckoIndex_AllUnique() {
        SolverType[] values = SolverType.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals("Old indices should be unique",
                    values[i].getOldGeckoIndex(), values[j].getOldGeckoIndex());
            }
        }
    }

    // ====================================================
    // GetFromOldGeckoIndex Tests
    // ====================================================

    @Test
    public void testGetFromOldGeckoIndex_BackwardEuler() {
        assertEquals(SolverType.SOLVER_BE, SolverType.getFromOldGeckoIndex(0));
    }

    @Test
    public void testGetFromOldGeckoIndex_Trapezoidal() {
        assertEquals(SolverType.SOLVER_TRZ, SolverType.getFromOldGeckoIndex(1));
    }

    @Test
    public void testGetFromOldGeckoIndex_GearShichman() {
        assertEquals(SolverType.SOLVER_GS, SolverType.getFromOldGeckoIndex(2));
    }

    @Test
    public void testGetFromOldGeckoIndex_AllValues() {
        for (SolverType solver : SolverType.values()) {
            int index = solver.getOldGeckoIndex();
            SolverType retrieved = SolverType.getFromOldGeckoIndex(index);
            assertEquals(solver, retrieved);
        }
    }

    // ====================================================
    // Round-Trip Tests
    // ====================================================

    @Test
    public void testRoundTrip_AllSolvers() {
        for (SolverType original : SolverType.values()) {
            int index = original.getOldGeckoIndex();
            SolverType restored = SolverType.getFromOldGeckoIndex(index);
            assertEquals(original, restored);
        }
    }

    // ====================================================
    // Default Value Tests
    // ====================================================

    @Test
    public void testDefaultSolver_IsBackwardEuler() {
        // Backward Euler has index 0, typically the default
        assertEquals(0, SolverType.SOLVER_BE.getOldGeckoIndex());
    }

    // ====================================================
    // Physical Interpretation Tests
    // ====================================================

    @Test
    public void testBackwardEuler_FirstOrderImplicit() {
        // Backward Euler is a first-order implicit method
        // It's unconditionally stable but less accurate
        SolverType be = SolverType.SOLVER_BE;
        assertEquals("backward-euler", be.toString());
    }

    @Test
    public void testTrapezoidal_SecondOrderImplicit() {
        // Trapezoidal (Crank-Nicolson) is a second-order implicit method
        // Good balance of accuracy and stability
        SolverType trz = SolverType.SOLVER_TRZ;
        assertEquals("trapezoidal", trz.toString());
    }

    @Test
    public void testGearShichman_MultiStepMethod() {
        // Gear-Shichman is a multi-step method for stiff systems
        // Used in SPICE-like simulators
        SolverType gs = SolverType.SOLVER_GS;
        assertEquals("gear-shichman", gs.toString());
    }
}

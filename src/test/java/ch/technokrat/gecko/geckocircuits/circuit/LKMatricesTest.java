/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.allg.SolverType;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for LKMatrices - the MNA (Modified Nodal Analysis) matrix builder.
 *
 * These tests focus on matrix structure, solver configuration, and basic operations.
 * Full circuit matrix assembly tests require complete netlist setup.
 */
public class LKMatricesTest {

    private static final double TOLERANCE = 1e-15;

    @Test
    public void testConstructor_WithBackwardEulerSolver() {
        LKMatrices matrices = new LKMatrices(SolverType.SOLVER_BE);
        assertNotNull("LKMatrices should be created", matrices);
    }

    @Test
    public void testConstructor_WithTrapezoidalSolver() {
        LKMatrices matrices = new LKMatrices(SolverType.SOLVER_TRZ);
        assertNotNull("LKMatrices should be created", matrices);
    }

    @Test
    public void testConstructor_WithGearShichmanSolver() {
        LKMatrices matrices = new LKMatrices(SolverType.SOLVER_GS);
        assertNotNull("LKMatrices should be created", matrices);
    }

    @Test
    public void testInitialMatrixSize_IsZero() {
        LKMatrices matrices = new LKMatrices(SolverType.SOLVER_BE);
        assertEquals("Initial matrix size should be 0", 0, matrices.matrixSize);
    }

    @Test
    public void testInitialArrays_AreNull() {
        LKMatrices matrices = new LKMatrices(SolverType.SOLVER_BE);
        // Before initialization, arrays should be null
        assertNull("Matrix a should be null before init", matrices.a);
        assertNull("Vector b should be null before init", matrices.bVector);
        assertNull("Vector p should be null before init", matrices.p);
    }

    @Test
    public void testSolverType_Values() {
        // Verify all solver types exist
        assertNotNull(SolverType.SOLVER_BE);
        assertNotNull(SolverType.SOLVER_TRZ);
        assertNotNull(SolverType.SOLVER_GS);
    }

    @Test
    public void testMultipleInstances_AreIndependent() {
        LKMatrices matrices1 = new LKMatrices(SolverType.SOLVER_BE);
        LKMatrices matrices2 = new LKMatrices(SolverType.SOLVER_TRZ);

        // Modify one instance
        matrices1.matrixSize = 10;

        // Other instance should be unaffected
        assertEquals(0, matrices2.matrixSize);
    }

    @Test
    public void testMutualInductanceArrays_InitiallyNull() {
        LKMatrices matrices = new LKMatrices(SolverType.SOLVER_BE);

        // Mutual inductance arrays should be null before initialization
        assertNull("zuLKOP2gehoerigeM_spgQnr should be null", matrices.zuLKOP2gehoerigeM_spgQnr);
        assertNull("zuLKOP2gehoerigeM_kWerte should be null", matrices.zuLKOP2gehoerigeM_kWerte);
    }

    @Test
    public void testSolverType_BackwardEuler_HasCorrectIndex() {
        assertEquals("Backward Euler should have index 0", 0, SolverType.SOLVER_BE.getOldGeckoIndex());
    }

    @Test
    public void testSolverType_Trapezoidal_HasCorrectIndex() {
        assertEquals("Trapezoidal should have index 1", 1, SolverType.SOLVER_TRZ.getOldGeckoIndex());
    }

    @Test
    public void testSolverType_GearShichman_HasCorrectIndex() {
        assertEquals("Gear-Shichman should have index 2", 2, SolverType.SOLVER_GS.getOldGeckoIndex());
    }

    @Test
    public void testSolverType_FromOldGeckoIndex() {
        assertEquals(SolverType.SOLVER_BE, SolverType.getFromOldGeckoIndex(0));
        assertEquals(SolverType.SOLVER_TRZ, SolverType.getFromOldGeckoIndex(1));
        assertEquals(SolverType.SOLVER_GS, SolverType.getFromOldGeckoIndex(2));
    }

    @Test
    public void testSolverType_DisplayStrings() {
        assertEquals("backward-euler", SolverType.SOLVER_BE.toString());
        assertEquals("trapezoidal", SolverType.SOLVER_TRZ.toString());
        assertEquals("gear-shichman", SolverType.SOLVER_GS.toString());
    }

    @Test
    public void testPotentialArrays_AreNullBeforeInit() {
        LKMatrices matrices = new LKMatrices(SolverType.SOLVER_BE);
        assertNull("pALT should be null before init", matrices.pALT);
        assertNull("pALTALT should be null before init", matrices.pALTALT);
        assertNull("pALTALTALT should be null before init", matrices.pALTALTALT);
    }

    @Test
    public void testCurrentArrays_AreNullBeforeInit() {
        LKMatrices matrices = new LKMatrices(SolverType.SOLVER_BE);
        assertNull("iALT should be null before init", matrices.iALT);
        assertNull("iALTALT should be null before init", matrices.iALTALT);
        assertNull("iALTALTALT should be null before init", matrices.iALTALTALT);
    }
}

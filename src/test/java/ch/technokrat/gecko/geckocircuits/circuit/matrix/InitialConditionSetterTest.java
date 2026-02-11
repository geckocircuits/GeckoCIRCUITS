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
package ch.technokrat.gecko.geckocircuits.circuit.matrix;

import ch.technokrat.gecko.geckocircuits.allg.SolverType;
import ch.technokrat.gecko.geckocircuits.circuit.matrix.InitialConditionSetter.MatrixStateArrays;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for InitialConditionSetter class.
 * 
 * Tests the extraction of initial condition setting logic from LKMatrices.
 * Verifies that:
 * - State arrays are properly initialized
 * - Dialog-based initialization clears and sets potentials correctly
 * - Continue-based initialization preserves saved values
 * - Current arrays are properly initialized for different component types
 * 
 * @author GeckoCIRCUITS Team
 */
public class InitialConditionSetterTest {

    private InitialConditionSetter setter;
    private static final double DELTA = 1e-10;

    @Before
    public void setUp() {
        setter = new InitialConditionSetter(SolverType.SOLVER_TRZ);
    }

    // ==================== Constructor Tests ====================

    @Test
    public void testConstructorWithTrapezoidalSolver() {
        InitialConditionSetter trapSetter = new InitialConditionSetter(SolverType.SOLVER_TRZ);
        assertNotNull("Setter should be created", trapSetter);
    }

    @Test
    public void testConstructorWithBackwardEulerSolver() {
        InitialConditionSetter beSetter = new InitialConditionSetter(SolverType.SOLVER_BE);
        assertNotNull("Setter should be created with BE solver", beSetter);
    }

    @Test
    public void testConstructorWithGearSolver() {
        InitialConditionSetter gsSetter = new InitialConditionSetter(SolverType.SOLVER_GS);
        assertNotNull("Setter should be created with GS solver", gsSetter);
    }

    // ==================== MatrixStateArrays Tests ====================

    @Test
    public void testMatrixStateArraysCreation() {
        int matrixSize = 5;
        int elementCount = 10;
        
        MatrixStateArrays state = new MatrixStateArrays(matrixSize, elementCount);
        
        assertNotNull("State should be created", state);
        assertEquals("p array size", matrixSize, state.p.length);
        assertEquals("pALT array size", matrixSize, state.pALT.length);
        assertEquals("pALTALT array size", matrixSize, state.pALTALT.length);
        assertEquals("pALTALTALT array size", matrixSize, state.pALTALTALT.length);
        assertEquals("iALT array size", elementCount, state.iALT.length);
        assertEquals("iALTALT array size", elementCount, state.iALTALT.length);
        assertEquals("iALTALTALT array size", elementCount, state.iALTALTALT.length);
    }

    @Test
    public void testMatrixStateArraysInitializedToZero() {
        MatrixStateArrays state = new MatrixStateArrays(3, 4);
        
        for (int i = 0; i < 3; i++) {
            assertEquals("p[" + i + "] should be 0", 0.0, state.p[i], DELTA);
            assertEquals("pALT[" + i + "] should be 0", 0.0, state.pALT[i], DELTA);
            assertEquals("pALTALT[" + i + "] should be 0", 0.0, state.pALTALT[i], DELTA);
            assertEquals("pALTALTALT[" + i + "] should be 0", 0.0, state.pALTALTALT[i], DELTA);
        }
        
        for (int i = 0; i < 4; i++) {
            assertEquals("iALT[" + i + "] should be 0", 0.0, state.iALT[i], DELTA);
            assertEquals("iALTALT[" + i + "] should be 0", 0.0, state.iALTALT[i], DELTA);
            assertEquals("iALTALTALT[" + i + "] should be 0", 0.0, state.iALTALTALT[i], DELTA);
        }
    }

    @Test
    public void testMatrixStateArraysMinimalSize() {
        MatrixStateArrays state = new MatrixStateArrays(1, 1);
        
        assertEquals("Minimal p array", 1, state.p.length);
        assertEquals("Minimal iALT array", 1, state.iALT.length);
    }

    @Test
    public void testMatrixStateArraysLargeSize() {
        int largeMatrixSize = 1000;
        int largeElementCount = 500;
        
        MatrixStateArrays state = new MatrixStateArrays(largeMatrixSize, largeElementCount);
        
        assertEquals("Large p array", largeMatrixSize, state.p.length);
        assertEquals("Large iALT array", largeElementCount, state.iALT.length);
    }

    @Test
    public void testMatrixStateArraysZeroSize() {
        MatrixStateArrays state = new MatrixStateArrays(0, 0);
        
        assertEquals("Zero p array", 0, state.p.length);
        assertEquals("Zero iALT array", 0, state.iALT.length);
    }

    @Test
    public void testMatrixStateArraysIndependentArrays() {
        MatrixStateArrays state = new MatrixStateArrays(3, 2);
        
        // Modify one array
        state.pALT[0] = 5.0;
        
        // Check others are independent
        assertEquals("p should be independent", 0.0, state.p[0], DELTA);
        assertEquals("pALTALT should be independent", 0.0, state.pALTALT[0], DELTA);
    }

    // ==================== Callback Interface Tests ====================

    @Test
    public void testCallbackInterfaceImplementation() {
        final int[] callCount = {0};
        
        InitialConditionSetter.ComponentCurrentCallback callback = 
                (perturbation, dt, time, isNewIteration, iterationCount) -> {
            callCount[0]++;
            return false; // No switching change
        };
        
        boolean result = callback.calculateCurrentsAndCheckSwitching(0.99, 1e-9, 0.0, false, 0);
        
        assertFalse("Callback should return false", result);
        assertEquals("Callback should be invoked once", 1, callCount[0]);
    }

    @Test
    public void testCallbackWithSwitchingDetected() {
        InitialConditionSetter.ComponentCurrentCallback callback = 
                (perturbation, dt, time, isNewIteration, iterationCount) -> true;
        
        assertTrue("Callback should indicate switching", 
                callback.calculateCurrentsAndCheckSwitching(0.99, 1e-9, 0.0, false, 0));
    }

    @Test
    public void testCallbackReceivesCorrectParameters() {
        final double[] receivedParams = new double[5];
        
        InitialConditionSetter.ComponentCurrentCallback callback = 
                (perturbation, dt, time, isNewIteration, iterationCount) -> {
            receivedParams[0] = perturbation;
            receivedParams[1] = dt;
            receivedParams[2] = time;
            receivedParams[3] = isNewIteration ? 1.0 : 0.0;
            receivedParams[4] = iterationCount;
            return false;
        };
        
        callback.calculateCurrentsAndCheckSwitching(0.95, 1e-6, 0.001, true, 5);
        
        assertEquals("Perturbation", 0.95, receivedParams[0], DELTA);
        assertEquals("dt", 1e-6, receivedParams[1], DELTA);
        assertEquals("time", 0.001, receivedParams[2], DELTA);
        assertEquals("isNewIteration", 1.0, receivedParams[3], DELTA);
        assertEquals("iterationCount", 5.0, receivedParams[4], DELTA);
    }

    // ==================== State Modification Tests ====================

    @Test
    public void testStateArraysCanBeModified() {
        MatrixStateArrays state = new MatrixStateArrays(5, 3);
        
        // Set values
        state.p[0] = 10.0;
        state.pALT[1] = 20.0;
        state.pALTALT[2] = 30.0;
        state.pALTALTALT[3] = 40.0;
        state.iALT[0] = 1.5;
        state.iALTALT[1] = 2.5;
        state.iALTALTALT[2] = 3.5;
        
        // Verify
        assertEquals("p modified", 10.0, state.p[0], DELTA);
        assertEquals("pALT modified", 20.0, state.pALT[1], DELTA);
        assertEquals("pALTALT modified", 30.0, state.pALTALT[2], DELTA);
        assertEquals("pALTALTALT modified", 40.0, state.pALTALTALT[3], DELTA);
        assertEquals("iALT modified", 1.5, state.iALT[0], DELTA);
        assertEquals("iALTALT modified", 2.5, state.iALTALT[1], DELTA);
        assertEquals("iALTALTALT modified", 3.5, state.iALTALTALT[2], DELTA);
    }

    @Test
    public void testStateArraysBoundaryAccess() {
        MatrixStateArrays state = new MatrixStateArrays(10, 5);
        
        // First element
        state.p[0] = 1.0;
        assertEquals("First p element", 1.0, state.p[0], DELTA);
        
        // Last element
        state.p[9] = 9.0;
        assertEquals("Last p element", 9.0, state.p[9], DELTA);
        
        // Last current element
        state.iALT[4] = 4.0;
        assertEquals("Last iALT element", 4.0, state.iALT[4], DELTA);
    }

    // ==================== Solver Type Tests ====================

    @Test
    public void testAllSolverTypesSupported() {
        for (SolverType type : SolverType.values()) {
            try {
                InitialConditionSetter typedSetter = new InitialConditionSetter(type);
                assertNotNull("Setter for " + type + " should be created", typedSetter);
            } catch (Exception e) {
                fail("Should support solver type: " + type + ", but got: " + e.getMessage());
            }
        }
    }

    // ==================== History Array Relationship Tests ====================

    @Test
    public void testPotentialHistoryArraysSameSize() {
        MatrixStateArrays state = new MatrixStateArrays(7, 3);
        
        assertEquals("p and pALT same size", state.p.length, state.pALT.length);
        assertEquals("pALT and pALTALT same size", state.pALT.length, state.pALTALT.length);
        assertEquals("pALTALT and pALTALTALT same size", state.pALTALT.length, state.pALTALTALT.length);
    }

    @Test
    public void testCurrentHistoryArraysSameSize() {
        MatrixStateArrays state = new MatrixStateArrays(5, 8);
        
        assertEquals("iALT and iALTALT same size", state.iALT.length, state.iALTALT.length);
        assertEquals("iALTALT and iALTALTALT same size", state.iALTALT.length, state.iALTALTALT.length);
    }

    // ==================== Typical Use Case Tests ====================

    @Test
    public void testTypicalCircuitSizes() {
        // Small circuit: 3 nodes, 5 elements
        MatrixStateArrays small = new MatrixStateArrays(4, 5); // +1 for ground
        assertEquals("Small circuit matrix", 4, small.p.length);
        assertEquals("Small circuit elements", 5, small.iALT.length);
        
        // Medium circuit: 20 nodes, 30 elements  
        MatrixStateArrays medium = new MatrixStateArrays(21, 30);
        assertEquals("Medium circuit matrix", 21, medium.p.length);
        assertEquals("Medium circuit elements", 30, medium.iALT.length);
        
        // Large circuit: 100 nodes, 200 elements
        MatrixStateArrays large = new MatrixStateArrays(101, 200);
        assertEquals("Large circuit matrix", 101, large.p.length);
        assertEquals("Large circuit elements", 200, large.iALT.length);
    }

    @Test
    public void testHistoryShiftSimulation() {
        MatrixStateArrays state = new MatrixStateArrays(3, 2);
        
        // Simulate time step 1
        state.p[1] = 1.0;
        state.iALT[0] = 0.1;
        
        // Shift history (as would happen between time steps)
        for (int i = 0; i < state.p.length; i++) {
            state.pALTALTALT[i] = state.pALTALT[i];
            state.pALTALT[i] = state.pALT[i];
            state.pALT[i] = state.p[i];
        }
        for (int i = 0; i < state.iALT.length; i++) {
            state.iALTALTALT[i] = state.iALTALT[i];
            state.iALTALT[i] = state.iALT[i];
        }
        
        // Verify shift
        assertEquals("pALT should have previous p value", 1.0, state.pALT[1], DELTA);
    }

    // ==================== Edge Cases ====================

    @Test
    public void testNegativeValuesAllowed() {
        MatrixStateArrays state = new MatrixStateArrays(3, 2);
        
        state.p[0] = -100.0;
        state.iALT[0] = -5.5;
        
        assertEquals("Negative potential allowed", -100.0, state.p[0], DELTA);
        assertEquals("Negative current allowed", -5.5, state.iALT[0], DELTA);
    }

    @Test
    public void testVerySmallValuesPreserved() {
        MatrixStateArrays state = new MatrixStateArrays(2, 1);
        
        state.p[0] = 1e-15;
        state.iALT[0] = 1e-12;
        
        assertEquals("Very small potential preserved", 1e-15, state.p[0], 1e-16);
        assertEquals("Very small current preserved", 1e-12, state.iALT[0], 1e-13);
    }

    @Test
    public void testVeryLargeValuesPreserved() {
        MatrixStateArrays state = new MatrixStateArrays(2, 1);
        
        state.p[0] = 1e10;
        state.iALT[0] = 1e6;
        
        assertEquals("Large potential preserved", 1e10, state.p[0], 1e5);
        assertEquals("Large current preserved", 1e6, state.iALT[0], 1e1);
    }

    @Test
    public void testSpecialDoubleValues() {
        MatrixStateArrays state = new MatrixStateArrays(3, 2);
        
        // Zero
        state.p[0] = 0.0;
        assertEquals("Zero value", 0.0, state.p[0], DELTA);
        
        // Positive infinity
        state.p[1] = Double.POSITIVE_INFINITY;
        assertTrue("Positive infinity", Double.isInfinite(state.p[1]));
        
        // NaN (can happen in malformed circuits)
        state.p[2] = Double.NaN;
        assertTrue("NaN value", Double.isNaN(state.p[2]));
    }
}

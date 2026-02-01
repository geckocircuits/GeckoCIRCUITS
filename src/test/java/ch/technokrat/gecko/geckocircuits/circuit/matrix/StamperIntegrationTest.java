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

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Integration tests for matrix stampers working together.
 * Verifies correct behavior of multiple stampers in composite circuits.
 */
public class StamperIntegrationTest {

    private static final double TOLERANCE = 1e-12;
    private double dt;
    private SolverContext solverContextBE;
    private SolverContext solverContextTRZ;
    private StamperRegistry registry;

    @Before
    public void setUp() {
        dt = 1e-6;
        solverContextBE = new SolverContext(dt, SolverContext.SOLVER_BE);
        solverContextTRZ = new SolverContext(dt, SolverContext.SOLVER_TRZ);
        registry = StamperRegistry.createDefault();
    }

    // Registry retrieval
    @Test
    public void testRegistryRetrievalAllTypes() {
        assertTrue("Resistor stamper", registry.getStamper(CircuitTyp.LK_R) instanceof ResistorStamper);
        assertTrue("Capacitor stamper", registry.getStamper(CircuitTyp.LK_C) instanceof CapacitorStamper);
        assertTrue("Inductor stamper", registry.getStamper(CircuitTyp.LK_L) instanceof InductorStamper);
        assertTrue("Voltage source stamper", registry.getStamper(CircuitTyp.LK_U) instanceof VoltageSourceStamper);
        assertTrue("Current source stamper", registry.getStamper(CircuitTyp.LK_I) instanceof CurrentSourceStamper);
        assertTrue("Diode stamper", registry.getStamper(CircuitTyp.LK_D) instanceof DiodeStamper);
    }

    @Test
    public void testRegistryRequiredStamper() {
        IMatrixStamper stamper = registry.getStamperRequired(CircuitTyp.LK_R);
        assertNotNull("Should return resistor stamper", stamper);
        assertTrue("Should be correct type", stamper instanceof ResistorStamper);
    }

    // Multi-component circuit with various types
    @Test
    public void testRLCCircuit_WithSolver() {
        // Circuit: Voltage source - R - L - C - GND
        double[][] a = new double[5][5];
        double[] b = new double[5];

        // Get stampers from registry
        IMatrixStamper rStamper = registry.getStamper(CircuitTyp.LK_R);
        IMatrixStamper cStamper = registry.getStamper(CircuitTyp.LK_C);
        IMatrixStamper lStamper = registry.getStamper(CircuitTyp.LK_L);
        IMatrixStamper uStamper = registry.getStamper(CircuitTyp.LK_U);

        // Voltage source: 10V (nodes 0-4, current var at 3)
        uStamper.stampMatrixA(a, 0, 4, 3, new double[]{0, 10.0}, dt);
        uStamper.stampVectorB(b, 0, 4, 3, new double[]{0, 10.0}, dt, 0.0, null);

        // Resistor 100Ω (nodes 0-1)
        rStamper.stampMatrixA(a, 0, 1, 0, new double[]{100.0}, dt);

        // Inductor 1mH (nodes 1-2)
        lStamper.stampMatrixA(a, 1, 2, 0, new double[]{1e-3}, dt);
        double[] lHistory = {0.1}; // 100mA initial
        lStamper.stampVectorB(b, 1, 2, 0, new double[]{1e-3}, dt, 0.0, lHistory);

        // Capacitor 1uF (nodes 2-4)
        cStamper.stampMatrixA(a, 2, 4, 0, new double[]{1e-6}, dt);
        double[] cHistory = {5.0}; // 5V initial
        cStamper.stampVectorB(b, 2, 4, 0, new double[]{1e-6}, dt, 0.0, cHistory);

        // Verify non-zero matrix entries
        assertTrue("Matrix should be populated", a[0][0] != 0);
        assertTrue("b vector should be populated", b[3] != 0);

        // Verify voltage source contribution
        assertEquals("Voltage source RHS", 10.0, b[3], TOLERANCE);
    }

    // Solver context integration
    @Test
    public void testCapacitorStamping_WithBESolver() {
        double[][] a = new double[3][3];
        CapacitorStamper cStamper = new CapacitorStamper();

        // Use BE solver context to determine conductance
        double gBE = solverContextBE.getCapacitorConductance(1e-6);

        // Manual stamp
        cStamper.stampMatrixA(a, 0, 1, 0, new double[]{1e-6}, dt);

        // Should match
        assertEquals("Capacitor conductance matches BE", gBE, a[0][0], TOLERANCE);
    }

    @Test
    public void testCapacitorStamping_WithTRZSolver() {
        double[][] a = new double[3][3];
        CapacitorStamper cStamper = new CapacitorStamper();

        // Use TRZ solver context
        double gTRZ = solverContextTRZ.getCapacitorConductance(1e-6);
        double gTrapezoidalFromStamper = cStamper.getAdmittanceWeightTrapezoidal(1e-6, dt);

        // Should match
        assertEquals("Trapezoidal conductance matches", gTRZ, gTrapezoidalFromStamper, TOLERANCE);
    }

    @Test
    public void testInductorStamping_WithBESolver() {
        double[][] a = new double[3][3];
        InductorStamper lStamper = new InductorStamper();

        // Use BE solver context
        double gBE = solverContextBE.getInductorConductance(1e-3);

        // Manual stamp
        lStamper.stampMatrixA(a, 0, 1, 0, new double[]{1e-3}, dt);

        // Should match
        assertEquals("Inductor conductance matches BE", gBE, a[0][0], TOLERANCE);
    }

    @Test
    public void testInductorStamping_WithTRZSolver() {
        double[][] a = new double[3][3];
        InductorStamper lStamper = new InductorStamper();

        // Use TRZ solver context
        double gTRZ = solverContextTRZ.getInductorConductance(1e-3);
        double gTrapezoidalFromStamper = lStamper.getAdmittanceWeightTrapezoidal(1e-3, dt);

        // Should match
        assertEquals("Trapezoidal inductor conductance matches", gTRZ, gTrapezoidalFromStamper, TOLERANCE);
    }

    // Multiple stampers in sequence
    @Test
    public void testSequentialStamping_AllComponentTypes() {
        double[][] a = new double[6][6];
        double[] b = new double[6];

        ResistorStamper r = (ResistorStamper) registry.getStamper(CircuitTyp.LK_R);
        CapacitorStamper c = (CapacitorStamper) registry.getStamper(CircuitTyp.LK_C);
        InductorStamper l = (InductorStamper) registry.getStamper(CircuitTyp.LK_L);
        VoltageSourceStamper u = (VoltageSourceStamper) registry.getStamper(CircuitTyp.LK_U);
        CurrentSourceStamper i = (CurrentSourceStamper) registry.getStamper(CircuitTyp.LK_I);

        // Stamp each component type
        r.stampMatrixA(a, 0, 1, 0, new double[]{100.0}, dt);
        c.stampMatrixA(a, 1, 2, 0, new double[]{1e-6}, dt);
        l.stampMatrixA(a, 2, 3, 0, new double[]{1e-3}, dt);
        u.stampMatrixA(a, 3, 4, 5, new double[]{0, 12.0}, dt);
        i.stampMatrixA(a, 4, 5, 0, new double[]{0, 1.0}, dt);

        r.stampVectorB(b, 0, 1, 0, new double[]{100.0}, dt, 0.0, null);
        c.stampVectorB(b, 1, 2, 0, new double[]{1e-6}, dt, 0.0, new double[]{5.0});
        l.stampVectorB(b, 2, 3, 0, new double[]{1e-3}, dt, 0.0, new double[]{0.1});
        u.stampVectorB(b, 3, 4, 5, new double[]{0, 12.0}, dt, 0.0, null);
        i.stampVectorB(b, 4, 5, 0, new double[]{0, 1.0}, dt, 0.0, null);

        // Verify matrix is populated
        for (int row = 0; row < 5; row++) {
            boolean hasEntry = false;
            for (int col = 0; col < 6; col++) {
                if (a[row][col] != 0.0) {
                    hasEntry = true;
                    break;
                }
            }
            assertTrue("Row " + row + " should have entries", hasEntry);
        }

        // Verify b vector has entries
        assertTrue("b vector should have entries", b[5] != 0 || b[1] != 0);
    }

    // Current calculation through components
    @Test
    public void testCurrentCalculation_AcrossResistor() {
        ResistorStamper stamper = (ResistorStamper) registry.getStamper(CircuitTyp.LK_R);

        double vx = 12.0;
        double vy = 0.0;
        double[] param = {100.0};

        double current = stamper.calculateCurrent(vx, vy, param, dt, 0.0);

        // I = V/R = 12/100 = 0.12 A
        assertEquals("Current across 100Ω resistor", 0.12, current, TOLERANCE);
    }

    @Test
    public void testCurrentCalculation_AcrossCapacitor() {
        CapacitorStamper stamper = (CapacitorStamper) registry.getStamper(CircuitTyp.LK_C);

        double vx = 10.0;
        double vy = 0.0;
        double[] param = {1e-6};

        double current = stamper.calculateCurrent(vx, vy, param, dt, 0.0);

        // I = G * V = (C/dt) * V = (1e-6/1e-6) * 10 = 10 A
        assertEquals("Current across 1uF capacitor", 10.0, current, TOLERANCE);
    }

    @Test
    public void testCurrentCalculation_AcrossInductor() {
        InductorStamper stamper = (InductorStamper) registry.getStamper(CircuitTyp.LK_L);

        double vx = 5.0;
        double vy = 0.0;
        double iPrev = 0.5;
        double[] param = {1e-3};

        double current = stamper.calculateCurrent(vx, vy, param, dt, iPrev);

        // i = i_prev + (dt/L) * v = 0.5 + (1e-6/1e-3) * 5 = 0.5 + 0.005 = 0.505
        assertEquals("Current through 1mH inductor", 0.505, current, TOLERANCE);
    }

    // AC source integration
    @Test
    public void testACVoltageSource() {
        double[] param = {VoltageSourceStamper.SOURCE_AC, 100.0, 50.0, 0.0}; // 100V, 50Hz
        VoltageSourceStamper stamper = (VoltageSourceStamper) registry.getStamper(CircuitTyp.LK_U);

        // At t=0, sin(0) = 0
        double v1 = stamper.calculateSourceVoltage(param, 0.0);
        assertEquals("AC source at t=0", 0.0, v1, TOLERANCE);

        // At quarter period, sin(pi/2) = 1
        double quarterPeriod = 1.0 / (4 * 50.0);
        double v2 = stamper.calculateSourceVoltage(param, quarterPeriod);
        assertEquals("AC source at T/4", 100.0, v2, 1e-9);
    }

    @Test
    public void testACCurrentSource() {
        double[] param = CurrentSourceStamper.createACParameters(10.0, 1000.0, 0.0); // 10A, 1kHz
        CurrentSourceStamper stamper = (CurrentSourceStamper) registry.getStamper(CircuitTyp.LK_I);

        // Peak should be at quarter period
        double quarterPeriod = 1.0 / (4 * 1000.0);
        double current = stamper.calculateSourceCurrent(param, quarterPeriod);
        assertEquals("AC current at peak", 10.0, current, 1e-9);
    }

    // Registry coverage
    @Test
    public void testRegistryDefaultSize() {
        assertEquals("Default registry should have 6 stampers", 6, registry.size());
    }

    @Test
    public void testRegistryAllTypesPresent() {
        assertTrue("Has resistor", registry.hasStamper(CircuitTyp.LK_R));
        assertTrue("Has capacitor", registry.hasStamper(CircuitTyp.LK_C));
        assertTrue("Has inductor", registry.hasStamper(CircuitTyp.LK_L));
        assertTrue("Has voltage source", registry.hasStamper(CircuitTyp.LK_U));
        assertTrue("Has current source", registry.hasStamper(CircuitTyp.LK_I));
        assertTrue("Has diode", registry.hasStamper(CircuitTyp.LK_D));
    }

    // IMatrixStamper interface consistency
    @Test
    public void testAllStampersImplementInterface() {
        for (CircuitTyp type : registry.getRegisteredTypes()) {
            IMatrixStamper stamper = registry.getStamper(type);
            assertNotNull("Stamper for " + type + " should not be null", stamper);
            assertTrue("Stamper should implement IMatrixStamper", stamper instanceof IMatrixStamper);
        }
    }

    // Stamper methods accessibility
    @Test
    public void testStamperMethodsCallable() {
        double[][] a = new double[3][3];
        double[] b = new double[3];

        ResistorStamper r = (ResistorStamper) registry.getStamper(CircuitTyp.LK_R);
        double[] param = {100.0};

        // All methods should be callable without exception
        r.stampMatrixA(a, 0, 1, 0, param, dt);
        r.stampVectorB(b, 0, 1, 0, param, dt, 0.0, null);
        double current = r.calculateCurrent(10.0, 0.0, param, dt, 0.0);
        double admittance = r.getAdmittanceWeight(100.0, dt);

        assertNotEquals("Methods should be functional", 0.0, current, TOLERANCE);
    }

    // Time-varying source integration
    @Test
    public void testTimeVaryingACSource_AcrossTimeSteps() {
        double[] param = CurrentSourceStamper.createACParameters(1.0, 1.0, 0.0);
        CurrentSourceStamper stamper = (CurrentSourceStamper) registry.getStamper(CircuitTyp.LK_I);

        double[] times = {0.0, 0.25, 0.5, 0.75, 1.0};
        double[] expectedSines = {0.0, 1.0, 0.0, -1.0, 0.0};

        for (int i = 0; i < times.length; i++) {
            double current = stamper.calculateSourceCurrent(param, times[i]);
            assertEquals("AC source at t=" + times[i],
                    expectedSines[i], current, 1e-9);
        }
    }

    // Complex network stamping
    @Test
    public void testComplexNetwork_AllTypesIntegrated() {
        StamperRegistry testRegistry = StamperRegistry.createDefault();
        double[][] a = new double[10][10];
        double[] b = new double[10];

        // Create a network with all component types
        // V_source - R1 - Node A
        //          - C1 - Node B
        //          - L1 - Node C
        //          - I_source - Ground

        ResistorStamper r = (ResistorStamper) testRegistry.getStamper(CircuitTyp.LK_R);
        CapacitorStamper c = (CapacitorStamper) testRegistry.getStamper(CircuitTyp.LK_C);
        InductorStamper l = (InductorStamper) testRegistry.getStamper(CircuitTyp.LK_L);
        VoltageSourceStamper v = (VoltageSourceStamper) testRegistry.getStamper(CircuitTyp.LK_U);
        CurrentSourceStamper i = (CurrentSourceStamper) testRegistry.getStamper(CircuitTyp.LK_I);

        // Voltage source 12V
        v.stampMatrixA(a, 0, 9, 8, new double[]{0, 12.0}, dt);
        v.stampVectorB(b, 0, 9, 8, new double[]{0, 12.0}, dt, 0.0, null);

        // Resistor branch
        r.stampMatrixA(a, 0, 1, 0, new double[]{100.0}, dt);

        // Capacitor branch
        c.stampMatrixA(a, 0, 2, 0, new double[]{1e-6}, dt);
        c.stampVectorB(b, 0, 2, 0, new double[]{1e-6}, dt, 0.0, new double[]{5.0});

        // Inductor branch
        l.stampMatrixA(a, 0, 3, 0, new double[]{1e-3}, dt);
        l.stampVectorB(b, 0, 3, 0, new double[]{1e-3}, dt, 0.0, new double[]{0.1});

        // Current source to ground
        i.stampMatrixA(a, 9, 9, 0, new double[]{0, 1.0}, dt);
        i.stampVectorB(b, 9, 9, 0, new double[]{0, 1.0}, dt, 0.0, null);

        // Verify non-trivial matrix
        int nonZeroCount = 0;
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                if (a[row][col] != 0.0) nonZeroCount++;
            }
        }
        assertTrue("Matrix should have many entries", nonZeroCount > 10);
    }
}

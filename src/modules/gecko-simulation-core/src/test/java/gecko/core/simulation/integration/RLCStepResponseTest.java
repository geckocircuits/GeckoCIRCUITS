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
package gecko.core.simulation.integration;

import gecko.core.allg.SolverType;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.simulation.solver.ComponentCurrentCalculator;
import gecko.core.simulation.solver.MatrixSolver;
import gecko.core.testutil.AnalyticalSolutions;
import gecko.core.testutil.SyntheticCircuitBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test C7: Series RLC step response.
 *
 * <p>Verifies capacitor voltage dynamics for the series RLC circuit
 * Vin → R → L → node3 → C → GND built by {@code SyntheticCircuitBuilder.rlcSeriesCircuit}.
 *
 * <p>Topology:
 * <pre>
 *   Vin (LK_U, DC) ──[node1]── R ──[node2]── L ──[node3]── C ──[GND]
 * </pre>
 *
 * <ul>
 *   <li>Elements: [0]=Vin, [1]=R, [2]=L, [3]=C</li>
 *   <li>Nodes: 0=GND, 1=Vin+, 2=R-L junction, 3=L-C junction (capacitor voltage)</li>
 *   <li>Capacitor voltage is at node 3: {@code solver.getP()[3]}</li>
 * </ul>
 *
 * <p>Tests cover:
 * <ol>
 *   <li>Underdamped (ζ ≈ 0.158): oscillation, overshoot, and final settling to Vin</li>
 *   <li>Overdamped (ζ ≈ 1.58): monotonic rise to Vin without overshoot</li>
 *   <li>Analytical comparison: numerical solution matches closed-form at several time points</li>
 * </ol>
 */
@Tag("integration")
class RLCStepResponseTest {

    private static final double VIN = 10.0;
    private static final double DT = 1e-6;  // 1 μs — fine enough for ω₀ ≈ 3162 rad/s

    // Underdamped parameters: R=1Ω, L=1mH, C=100μF
    // ω₀ = 1/√(LC) = 1/√(1e-3 * 1e-4) ≈ 3162 rad/s
    // ζ  = R / (2√(L/C)) = 1 / (2*√10) ≈ 0.158  → underdamped
    private static final double R_UNDER = 1.0;
    private static final double L_UNDER = 1e-3;
    private static final double C_UNDER = 100e-6;

    // Overdamped parameters: R=100Ω, L=10mH, C=10μF
    // ω₀ = 1/√(10e-3 * 10e-6) = 1/√(1e-7) ≈ 3162 rad/s
    // ζ  = 100 / (2*√(10e-3/10e-6)) = 100 / (2*√1000) ≈ 1.58  → overdamped
    private static final double R_OVER = 100.0;
    private static final double L_OVER = 10e-3;
    private static final double C_OVER = 10e-6;

    /**
     * Runs the simulation loop and returns the MatrixSolver after {@code totalSteps} steps.
     *
     * <p>Follows the exact pattern from RCStepResponseTest with no extra calls between steps.
     *
     * @param netlist    configured circuit netlist
     * @param totalSteps number of time steps to simulate
     * @return MatrixSolver containing the final node-voltage vector ({@code getP()})
     */
    private MatrixSolver runSimulation(CircuitNetlist netlist, int totalSteps) {
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];

        for (int step = 0; step < totalSteps; step++) {
            double time = step * DT;
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
        }

        return solver;
    }

    /**
     * Runs the simulation step-by-step, capturing the capacitor voltage at each step.
     *
     * @param netlist    configured circuit netlist
     * @param totalSteps number of time steps to simulate
     * @return array of capacitor voltages, one per step (at node 3)
     */
    private double[] runSimulationCapVoltages(CircuitNetlist netlist, int totalSteps) {
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];
        double[] vcapHistory = new double[totalSteps];

        for (int step = 0; step < totalSteps; step++) {
            double time = step * DT;
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
            vcapHistory[step] = solver.getP()[3];
        }

        return vcapHistory;
    }

    // ========================================================================
    // Test 1: Underdamped — oscillates and settles
    // ========================================================================

    /**
     * Underdamped series RLC (ζ ≈ 0.158): capacitor voltage must overshoot Vin
     * (confirming oscillation) and then settle within 1% of Vin after ~10ms.
     *
     * <p>Settling time estimate: 5/(ζ*ω₀) = 5/(0.158*3162) ≈ 10ms → 10000 steps at DT=1μs.
     */
    @Test
    void underdampedOscillatesAndSettles() {
        // Verify we are actually underdamped
        double zeta = AnalyticalSolutions.rlcSeriesDampingRatio(R_UNDER, L_UNDER, C_UNDER);
        assertTrue(zeta < 1.0,
                String.format("Expected underdamped (ζ < 1), got ζ = %.4f", zeta));

        // Simulate for ~10ms = 10000 steps
        int totalSteps = 10000;
        double[] vcap = runSimulationCapVoltages(
                SyntheticCircuitBuilder.rlcSeriesCircuit(VIN, R_UNDER, L_UNDER, C_UNDER),
                totalSteps);

        // Verify overshoot: capacitor voltage must exceed Vin at some point
        double maxVcap = 0.0;
        for (double v : vcap) {
            if (v > maxVcap) {
                maxVcap = v;
            }
        }
        assertTrue(maxVcap > VIN,
                String.format("Underdamped circuit must overshoot Vin=%.1f V; max observed=%.4f V",
                        VIN, maxVcap));

        // Verify final settlement within 1% of Vin
        double finalVcap = vcap[totalSteps - 1];
        assertEquals(VIN, finalVcap, VIN * 0.01,
                String.format("After 10ms, V_C should settle within 1%% of %.1f V; got %.4f V",
                        VIN, finalVcap));
    }

    // ========================================================================
    // Test 2: Overdamped — monotonically rises, no overshoot
    // ========================================================================

    /**
     * Overdamped series RLC (ζ ≈ 1.58): capacitor voltage must rise monotonically
     * (no overshoot) and settle within 1% of Vin after ~5ms.
     *
     * <p>For overdamped response the slower time constant is approximately
     * 1/(σ - ωn) where σ = ζ*ω₀ and ωn = ω₀*√(ζ²-1).
     * At 5ms with these parameters the voltage has essentially reached steady state.
     */
    @Test
    void overdampedMonotonicallyRises() {
        // Verify we are actually overdamped
        double zeta = AnalyticalSolutions.rlcSeriesDampingRatio(R_OVER, L_OVER, C_OVER);
        assertTrue(zeta > 1.0,
                String.format("Expected overdamped (ζ > 1), got ζ = %.4f", zeta));

        // Simulate for ~5ms = 5000 steps
        int totalSteps = 5000;
        double[] vcap = runSimulationCapVoltages(
                SyntheticCircuitBuilder.rlcSeriesCircuit(VIN, R_OVER, L_OVER, C_OVER),
                totalSteps);

        // Verify monotonic rise: no step should decrease by more than numerical noise
        double previousVoltage = 0.0;
        for (int step = 0; step < totalSteps; step++) {
            assertTrue(vcap[step] >= previousVoltage - 1e-10,
                    String.format("Step %d: overdamped voltage decreased from %.6e to %.6e V",
                            step, previousVoltage, vcap[step]));
            previousVoltage = vcap[step];
        }

        // Verify no overshoot: final voltage must not exceed Vin
        double finalVcap = vcap[totalSteps - 1];
        assertTrue(finalVcap <= VIN * 1.001,
                String.format("Overdamped circuit must not overshoot Vin=%.1f V; got %.4f V",
                        VIN, finalVcap));

        // Verify final settlement within 1% of Vin
        assertEquals(VIN, finalVcap, VIN * 0.01,
                String.format("After 5ms, V_C should settle within 1%% of %.1f V; got %.4f V",
                        VIN, finalVcap));
    }

    // ========================================================================
    // Test 3: Numerical solution matches analytical solution at sampled times
    // ========================================================================

    /**
     * Underdamped series RLC: numerical capacitor voltage at t = 1ms, 2ms, 5ms, 10ms
     * must match the analytical closed-form solution within 5% relative tolerance.
     *
     * <p>5% tolerance is used because Backward Euler introduces first-order integration
     * error (phase lag and amplitude damping), which is more pronounced at the oscillation
     * frequency of the underdamped RLC.
     */
    @Test
    void settledVoltageMatchesAnalytical() {
        // Sample times [s] and corresponding step indices at DT=1μs
        double[] sampleTimes = {1e-3, 2e-3, 5e-3, 10e-3};
        int[] sampleSteps  = {1000,  2000,  5000,  10000};

        int totalSteps = sampleSteps[sampleSteps.length - 1];
        double[] vcap = runSimulationCapVoltages(
                SyntheticCircuitBuilder.rlcSeriesCircuit(VIN, R_UNDER, L_UNDER, C_UNDER),
                totalSteps);

        for (int i = 0; i < sampleTimes.length; i++) {
            double t = sampleTimes[i];
            double analytical = AnalyticalSolutions.rlcSeriesStepVoltage(
                    VIN, R_UNDER, L_UNDER, C_UNDER, t);
            double numerical = vcap[sampleSteps[i] - 1];

            // Allow 5% relative tolerance to account for Backward Euler phase/amplitude error
            double tolerance = Math.abs(analytical) * 0.05 + 1e-6;  // absolute floor for near-zero
            assertEquals(analytical, numerical, tolerance,
                    String.format("t=%.1fms: analytical=%.4f V, numerical=%.4f V (tol=%.4f V)",
                            t * 1e3, analytical, numerical, tolerance));
        }
    }
}

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
 * Integration test for the thermal domain using a first-order thermal RC network.
 *
 * <p>Topology (thermal analogy: voltage=temperature, current=heat flow):
 * <pre>
 *   TH_FLOW (heat source) ──[node1]── TH_RTH ──[node2]── TH_TEMP (ambient=25°C)
 *                              │── TH_CTH ──[GND]
 * </pre>
 *
 * <p>The thermal network uses the same MNA stampers as their electrical analogues:
 * TH_RTH uses ResistorStamper, TH_CTH uses CapacitorStamper,
 * TH_FLOW uses CurrentSourceStamper (via buildVectorB TH_FLOW case),
 * TH_TEMP uses VoltageSourceStamper.
 *
 * <p>Parameters:
 * <ul>
 *   <li>P = 10 W (constant heat source)</li>
 *   <li>Rth = 5 K/W (thermal resistance)</li>
 *   <li>Cth = 10 J/K (thermal capacitance)</li>
 *   <li>T_ambient = 25°C (fixed boundary condition)</li>
 *   <li>τ = Rth * Cth = 50 s</li>
 *   <li>T_steady_state = T_ambient + P * Rth = 25 + 50 = 75°C</li>
 * </ul>
 *
 * <p>Analytical solution:
 * T_hot(t) = T_ambient + P * Rth * (1 - exp(-t/τ))
 *
 * <p>Node mapping in solver.getP():
 * <ul>
 *   <li>solver.getP()[0] = GND (thermal reference, ≈ 0)</li>
 *   <li>solver.getP()[1] = hot node temperature</li>
 *   <li>solver.getP()[2] = ambient node temperature (fixed at T_ambient)</li>
 * </ul>
 */
@Tag("integration")
class ThermalDomainTest {

    private static final double POWER = 10.0;       // heat source [W]
    private static final double RTH = 5.0;           // thermal resistance [K/W]
    private static final double CTH = 10.0;          // thermal capacitance [J/K]
    private static final double T_AMBIENT = 25.0;    // ambient temperature [°C]

    private static final double TAU = RTH * CTH;     // = 50 s
    private static final double T_STEADY = T_AMBIENT + POWER * RTH;  // = 75°C

    // dt = τ/100 for good numerical accuracy
    private static final double DT = TAU / 100.0;   // = 0.5 s

    /**
     * Runs the thermal simulation for a given number of time steps.
     *
     * @param totalSteps number of simulation steps to execute
     * @return the MatrixSolver after the run (call getP() to read node temperatures)
     */
    private MatrixSolver runSimulation(int totalSteps) {
        CircuitNetlist netlist = SyntheticCircuitBuilder.thermalRCNetwork(POWER, RTH, CTH, T_AMBIENT);
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
            netlist.storeResults(solver.getPALT(), solver.getIALT());
        }

        return solver;
    }

    /**
     * Test 1: Steady-state temperature matches analytical solution after 10τ.
     *
     * <p>After 10 time constants the RC network reaches >99.99% of steady state.
     * Expected: T_hot ≈ T_ambient + P * Rth = 25 + 10*5 = 75°C.
     * Tolerance: 2% relative error.
     */
    @Test
    void steadyStateTemperatureMatchesAnalytical() {
        int totalSteps = (int) (10 * TAU / DT);  // 10τ = 500 s → 1000 steps
        MatrixSolver solver = runSimulation(totalSteps);

        double tHot = solver.getP()[1];
        double expected = AnalyticalSolutions.thermalSteadyStateTemperature(POWER, RTH, T_AMBIENT);

        assertEquals(expected, tHot, expected * 0.02,
                String.format("Hot node temperature after 10τ: expected %.1f°C, got %.3f°C",
                        expected, tHot));
    }

    /**
     * Test 2: Temperature at 1τ matches the analytical step response.
     *
     * <p>The solver initializes all node potentials to 0. The ambient voltage source
     * immediately enforces T_node2 = T_ambient, but the hot node starts at 0°C.
     * Therefore the response is a step from T=0 to T_steady = T_ambient + P*Rth.
     *
     * <p>Analytical solution with T_initial=0:
     * T_hot(t) = T_steady * (1 - exp(-t/τ)) = 75 * (1 - 1/e) ≈ 47.4°C at t=τ.
     *
     * <p>Tolerance: 5% (Backward Euler introduces first-order truncation error).
     */
    @Test
    void temperatureAtOneTimeConstant() {
        int stepsForOneTau = (int) (TAU / DT);  // 1τ = 50 s → 100 steps
        MatrixSolver solver = runSimulation(stepsForOneTau);

        double tHot = solver.getP()[1];
        // Initial condition is T=0, so T_hot rises from 0 to T_steady.
        // Analytical: T_hot(τ) = T_steady * (1 - 1/e)
        double tSteady = AnalyticalSolutions.thermalSteadyStateTemperature(POWER, RTH, T_AMBIENT);
        double expected = tSteady * (1.0 - Math.exp(-1.0));  // 75 * 0.6321 ≈ 47.4°C

        assertEquals(expected, tHot, expected * 0.05,
                String.format("Hot node temperature at 1τ: expected %.3f°C (63.2%% of T_steady), got %.3f°C",
                        expected, tHot));
    }

    /**
     * Test 3: Hot node temperature rises monotonically over 5τ.
     *
     * <p>A first-order RC thermal network has no overshoot — the step response is
     * strictly monotonically increasing. Any non-monotonicity indicates a numerical
     * stability problem.
     */
    @Test
    void monotonicTemperatureRise() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.thermalRCNetwork(POWER, RTH, CTH, T_AMBIENT);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];
        double previousTemp = Double.NEGATIVE_INFINITY;

        int totalSteps = (int) (5 * TAU / DT);  // 5τ = 250 s → 500 steps
        for (int step = 0; step < totalSteps; step++) {
            double time = step * DT;
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
            netlist.storeResults(solver.getPALT(), solver.getIALT());

            double tHot = solver.getP()[1];
            assertTrue(tHot >= previousTemp - 1e-9,
                    String.format("Step %d: temperature decreased from %.6f to %.6f°C",
                            step, previousTemp, tHot));
            previousTemp = tHot;
        }

        // After 5τ, temperature should be well above ambient (>98% of steady-state).
        // Note: initial condition is T=0, so steady state approaches T_steady=75°C.
        // 98% of T_steady = 73.5°C. Using 98% to account for BE numerical damping.
        double minExpected = T_STEADY * 0.98;
        assertTrue(previousTemp > minExpected,
                String.format("After 5τ, temperature should be >98%% of T_steady (%.1f°C), got %.3f°C",
                        minExpected, previousTemp));
    }

    /**
     * Test 4: Ambient node (node2) stays fixed at T_ambient throughout the simulation.
     *
     * <p>TH_TEMP is a voltage source that enforces the boundary condition T_node2 = T_ambient.
     * This node must remain constant at 25°C regardless of the heat injected at node1.
     */
    @Test
    void ambientNodeIsFixed() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.thermalRCNetwork(POWER, RTH, CTH, T_AMBIENT);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];

        int totalSteps = (int) (10 * TAU / DT);  // 10τ = 500 s → 1000 steps
        for (int step = 0; step < totalSteps; step++) {
            double time = step * DT;
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
            netlist.storeResults(solver.getPALT(), solver.getIALT());

            double tAmbientActual = solver.getP()[2];
            assertEquals(T_AMBIENT, tAmbientActual, 1e-6,
                    String.format("Step %d: ambient node should be fixed at %.1f°C, got %.6f°C",
                            step, T_AMBIENT, tAmbientActual));
        }
    }
}

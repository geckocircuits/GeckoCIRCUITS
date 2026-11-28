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
 * Integration test C3: RC step response.
 *
 * <p>Verifies capacitor history terms and voltage storage for
 * Vin → R → node2 → C → GND circuit.
 *
 * <p>Expected: V_C(t) = Vin * (1 - e^(-t/RC)), τ = RC.
 */
@Tag("integration")
class RCStepResponseTest {

    private static final double VIN = 10.0;
    private static final double R = 1000.0;      // 1 kΩ
    private static final double C = 1e-6;         // 1 μF
    private static final double TAU = R * C;      // 1 ms
    private static final double VSS = VIN;        // steady-state capacitor voltage = Vin

    // Use dt = τ/100 for good accuracy
    private static final double DT = TAU / 100.0;

    @Test
    void steadyStateVoltageMatchesAnalytical() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.rcCircuit(VIN, R, C);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];

        // Run for 10τ (should reach >99.99% of steady state)
        int totalSteps = (int) (10 * TAU / DT);
        for (int step = 0; step < totalSteps; step++) {
            double time = step * DT;
            netlist.setSimulationTime(time);
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
            netlist.storeResults(solver.getPALT(), solver.getIALT());
        }

        // Capacitor voltage = V(node2) - V(GND) = p[2]
        double vcap = solver.getP()[2];
        assertEquals(VSS, vcap, VSS * 0.02,
                String.format("Capacitor voltage after 10τ: expected %.1f V, got %.3f V",
                        VSS, vcap));
    }

    @Test
    void voltageAtOneTimeConstant() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.rcCircuit(VIN, R, C);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];

        // Run for exactly 1τ
        int stepsForOneTau = (int) (TAU / DT);
        for (int step = 0; step < stepsForOneTau; step++) {
            double time = step * DT;
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
        }

        double expectedAt1Tau = AnalyticalSolutions.rcStepVoltage(VIN, R, C, TAU);
        double vcap = solver.getP()[2];
        // Backward Euler introduces some deviation; allow 5% tolerance
        assertEquals(expectedAt1Tau, vcap, expectedAt1Tau * 0.05,
                String.format("V_C at 1τ: expected %.3f V (63.2%% of Vin), got %.3f V",
                        expectedAt1Tau, vcap));
    }

    @Test
    void monotonicVoltageRise() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.rcCircuit(VIN, R, C);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];
        double previousVoltage = 0.0;

        int totalSteps = (int) (5 * TAU / DT);
        for (int step = 0; step < totalSteps; step++) {
            double time = step * DT;
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);

            double vcap = solver.getP()[2];
            // Capacitor voltage should be monotonically increasing
            assertTrue(vcap >= previousVoltage - 1e-12,
                    String.format("Step %d: voltage decreased from %.6e to %.6e",
                            step, previousVoltage, vcap));
            previousVoltage = vcap;
        }

        // Final voltage should be close to Vin
        assertTrue(previousVoltage > 0.99 * VSS,
                "After 5τ, capacitor voltage should be >99% of Vin");
    }

    @Test
    void currentDecaysToZero() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.rcCircuit(VIN, R, C);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];

        int totalSteps = (int) (10 * TAU / DT);
        for (int step = 0; step < totalSteps; step++) {
            double time = step * DT;
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
        }

        // Resistor current (element 1) should be ~0 at steady state
        double iR = Math.abs(currents[1]);
        double maxExpectedCurrent = VIN / R * 0.01;  // <1% of initial
        assertTrue(iR < maxExpectedCurrent,
                String.format("After 10τ, current should be ~0, got %.6e A", iR));
    }
}

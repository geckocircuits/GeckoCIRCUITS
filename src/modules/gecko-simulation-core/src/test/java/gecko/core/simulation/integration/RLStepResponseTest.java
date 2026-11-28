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
 * Integration test C2: RL step response.
 *
 * <p>Verifies inductor history terms and time-stepping accuracy for
 * Vin → R → L → GND circuit.
 *
 * <p>Expected: I(t) = (V/R)*(1 - e^(-Rt/L)), steady-state I = V/R, τ = L/R.
 */
@Tag("integration")
class RLStepResponseTest {

    private static final double VIN = 10.0;
    private static final double R = 10.0;       // 10 Ω
    private static final double L = 1e-3;        // 1 mH
    private static final double TAU = L / R;     // 0.1 ms
    private static final double ISS = VIN / R;   // 1 A

    // Use dt = τ/100 for good accuracy with Backward Euler
    private static final double DT = TAU / 100.0;

    @Test
    void steadyStateCurrentMatchesAnalytical() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.rlCircuit(VIN, R, L);
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
        }

        // Inductor current (element 2) should be at steady-state
        assertEquals(ISS, currents[2], ISS * 0.02,
                String.format("Inductor current after 10τ: expected %.3f A, got %.3f A",
                        ISS, currents[2]));
    }

    @Test
    void currentAtOneTimeConstant() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.rlCircuit(VIN, R, L);
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

        double expectedAt1Tau = AnalyticalSolutions.rlStepCurrent(VIN, R, L, TAU);
        // Backward Euler introduces some damping; allow 5% tolerance
        assertEquals(expectedAt1Tau, currents[2], expectedAt1Tau * 0.05,
                String.format("Current at 1τ: expected %.4f A (63.2%% of Iss), got %.4f A",
                        expectedAt1Tau, currents[2]));
    }

    @Test
    void monotonicCurrentRise() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.rlCircuit(VIN, R, L);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];
        double previousCurrent = 0.0;

        int totalSteps = (int) (5 * TAU / DT);
        for (int step = 0; step < totalSteps; step++) {
            double time = step * DT;
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);

            // Current should be monotonically increasing
            assertTrue(currents[2] >= previousCurrent - 1e-12,
                    String.format("Step %d: current decreased from %.6e to %.6e",
                            step, previousCurrent, currents[2]));
            previousCurrent = currents[2];
        }

        // Final current should be close to steady state
        assertTrue(previousCurrent > 0.99 * ISS,
                "After 5τ, current should be >99% of steady-state");
    }

    @Test
    void trapezoidalSolverConverges() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.rlCircuit(VIN, R, L);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_TRZ);
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

        // Trapezoidal should also reach steady state with similar accuracy
        assertEquals(ISS, currents[2], ISS * 0.02,
                "Trapezoidal solver: inductor current at steady state");
    }
}

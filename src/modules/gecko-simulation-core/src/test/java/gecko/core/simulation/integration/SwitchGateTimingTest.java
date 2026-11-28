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
import gecko.core.testutil.SyntheticCircuitBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test C5: Resistor + MOSFET Switch.
 *
 * <p>Verifies gate signal timing, switch R_ON/R_OFF from params, and A matrix
 * rebuild when switch state changes.
 *
 * <p>Vin → S → R → GND. Gate toggles manually between ON/OFF.
 * Expected: V_out = ~Vin when ON, ~0 when OFF.
 */
@Tag("integration")
class SwitchGateTimingTest {

    private static final double VIN = 10.0;
    private static final double R = 10.0;
    private static final double R_ON = 1e-3;
    private static final double R_OFF = 1e9;
    private static final double DT = 1e-6;

    @Test
    void switchOffProducesZeroOutput() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.switchCircuit(VIN, R, R_ON, R_OFF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        // Switch starts OFF (params[0] = R_OFF, params[8] = 0)
        solver.buildMatrixA(netlist, DT, 0, false);
        solver.buildVectorB(netlist, DT, 0, false);
        solver.solve();

        double[] p = solver.getP();
        // node2 (S-R junction) should be ~0 since switch is OFF
        assertTrue(Math.abs(p[2]) < 0.01,
                "With switch OFF, output node should be ~0V, got " + p[2]);
    }

    @Test
    void switchOnProducesFullVoltage() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.switchCircuit(VIN, R, R_ON, R_OFF);

        // Turn switch ON: set params[0] = R_ON
        double[] switchParams = netlist.getParameter(1);
        switchParams[0] = R_ON;
        switchParams[8] = 1.0;

        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        solver.buildMatrixA(netlist, DT, 0, false);
        solver.buildVectorB(netlist, DT, 0, false);
        solver.solve();

        double[] p = solver.getP();
        double[] currents = new double[netlist.getElementCount()];
        calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, 0, true);

        // node2 should be ~Vin (R_ON << R, so almost no voltage drop across switch)
        double vout = p[2];
        assertEquals(VIN, vout, VIN * 0.01,
                "With switch ON, output should be ~Vin, got " + vout);

        // Current should be ~Vin/R
        double expectedI = VIN / R;
        assertEquals(expectedI, currents[2], expectedI * 0.01,
                "Current through R should be ~Vin/R");
    }

    @Test
    void switchToggleDuringSimulation() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.switchCircuit(VIN, R, R_ON, R_OFF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];
        double[] switchParams = netlist.getParameter(1);

        // Phase 1: Run 10 steps with switch OFF
        for (int step = 0; step < 10; step++) {
            double time = step * DT;
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
        }

        double vOutOff = solver.getP()[2];
        assertTrue(Math.abs(vOutOff) < 0.01, "OFF phase: output ~0V");

        // Phase 2: Turn switch ON and run 10 more steps
        switchParams[0] = R_ON;
        switchParams[8] = 1.0;

        for (int step = 10; step < 20; step++) {
            double time = step * DT;
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
        }

        double vOutOn = solver.getP()[2];
        assertEquals(VIN, vOutOn, VIN * 0.01,
                "ON phase: output should jump to ~Vin");

        // Phase 3: Turn switch OFF again
        switchParams[0] = R_OFF;
        switchParams[8] = 0.0;

        for (int step = 20; step < 30; step++) {
            double time = step * DT;
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
        }

        double vOutOff2 = solver.getP()[2];
        assertTrue(Math.abs(vOutOff2) < 0.01,
                "After turning OFF again, output should return to ~0V");
    }

    @Test
    void resistanceMismatchDoesNotCorruptSolution() {
        // Ensure that setting params[0] directly (like DomainCoupler does)
        // correctly changes the A matrix behavior
        CircuitNetlist netlist = SyntheticCircuitBuilder.switchCircuit(VIN, R, R_ON, R_OFF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        // Set to intermediate resistance (e.g., 100Ω)
        double[] switchParams = netlist.getParameter(1);
        switchParams[0] = 100.0;

        solver.buildMatrixA(netlist, DT, 0, false);
        solver.buildVectorB(netlist, DT, 0, false);
        solver.solve();

        // Expected: voltage divider between Rsw=100 and R=10
        // V_out = Vin * R / (Rsw + R) = 10 * 10 / 110 ≈ 0.909
        double expected = VIN * R / (100.0 + R);
        assertEquals(expected, solver.getP()[2], expected * 0.01,
                "Intermediate resistance: voltage divider");
    }
}

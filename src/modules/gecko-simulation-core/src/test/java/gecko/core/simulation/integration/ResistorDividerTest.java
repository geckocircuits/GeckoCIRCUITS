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
 * Integration test C1: Pure resistor divider.
 *
 * <p>Verifies that the MNA matrix build + solve produces the correct DC
 * steady-state for Vin → R1 → node2 → R2 → GND.
 *
 * <p>Expected: V(node2) = Vin * R2 / (R1 + R2), I = Vin / (R1 + R2).
 */
@Tag("integration")
class ResistorDividerTest {

    private static final double VIN = 10.0;
    private static final double R1 = 1000.0;
    private static final double R2 = 1000.0;
    private static final double DT = 1e-6;
    private static final double TOL = 1e-3;  // 0.1% relative tolerance

    @Test
    void midpointVoltageMatchesAnalytical() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.resistorDivider(VIN, R1, R2);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        solver.buildMatrixA(netlist, DT, 0, false);
        solver.buildVectorB(netlist, DT, 0, false);
        solver.solve();

        double[] p = solver.getP();
        double expected = AnalyticalSolutions.resistorDividerVoltage(VIN, R1, R2);

        // node 0 = GND ≈ 0, node 1 = Vin, node 2 = midpoint
        assertEquals(0.0, p[0], 1e-6, "Ground node should be ~0");
        assertEquals(VIN, p[1], VIN * TOL, "Vin node");
        assertEquals(expected, p[2], expected * TOL,
                String.format("Midpoint: expected %.3f, got %.3f", expected, p[2]));
    }

    @Test
    void currentThroughDividerMatchesAnalytical() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.resistorDivider(VIN, R1, R2);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        solver.buildMatrixA(netlist, DT, 0, false);
        solver.buildVectorB(netlist, DT, 0, false);
        solver.solve();

        double[] currents = new double[netlist.getElementCount()];
        calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, 0, true);

        double expectedI = AnalyticalSolutions.resistorDividerCurrent(VIN, R1, R2);

        // Current through R1 (element 1) and R2 (element 2) should be equal
        assertEquals(expectedI, currents[1], expectedI * TOL,
                "R1 current should match analytical");
        assertEquals(expectedI, currents[2], expectedI * TOL,
                "R2 current should match analytical");
    }

    @Test
    void asymmetricDivider() {
        double r1 = 3000.0;
        double r2 = 1000.0;
        CircuitNetlist netlist = SyntheticCircuitBuilder.resistorDivider(VIN, r1, r2);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        solver.buildMatrixA(netlist, DT, 0, false);
        solver.buildVectorB(netlist, DT, 0, false);
        solver.solve();

        double expected = AnalyticalSolutions.resistorDividerVoltage(VIN, r1, r2);
        assertEquals(expected, solver.getP()[2], expected * TOL,
                "Asymmetric divider: expected " + expected);
    }

    @Test
    void multiStepStabilityForDCCircuit() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.resistorDivider(VIN, R1, R2);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double expected = AnalyticalSolutions.resistorDividerVoltage(VIN, R1, R2);
        double[] currents = new double[netlist.getElementCount()];

        // Run 100 steps — DC circuit should remain at same value
        for (int step = 0; step < 100; step++) {
            double time = step * DT;
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
        }

        assertEquals(expected, solver.getP()[2], expected * TOL,
                "After 100 steps, DC divider should be stable");
    }
}

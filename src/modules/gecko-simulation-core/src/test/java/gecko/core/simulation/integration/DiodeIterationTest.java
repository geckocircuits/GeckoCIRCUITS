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
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.simulation.solver.ComponentCurrentCalculator;
import gecko.core.simulation.solver.MatrixSolver;
import gecko.core.testutil.AnalyticalSolutions;
import gecko.core.testutil.SyntheticCircuitBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test C4: Resistor + Diode circuit.
 *
 * <p>Verifies diode iteration (ON→OFF toggle), forward voltage compensation
 * in the b-vector, and the convergence of the Newton-like iteration.
 *
 * <p>Forward bias (Vin=+10V): I ≈ (Vin - Uf) / R.
 * <p>Reverse bias (Vin=-10V): I ≈ 0.
 */
@Tag("integration")
class DiodeIterationTest {

    private static final double VIN_FWD = 10.0;
    private static final double VIN_REV = -10.0;
    private static final double R = 1000.0;      // 1 kΩ
    private static final double UF = 0.7;
    private static final double R_ON = 1e-3;
    private static final double R_OFF = 1e9;
    private static final double DT = 1e-6;

    /**
     * Performs one step of the simulation including diode iteration,
     * matching the HeadlessSimulationEngine loop.
     */
    private double[] simulateOneStep(CircuitNetlist netlist, MatrixSolver solver,
                                      ComponentCurrentCalculator calc, double time) {
        solver.buildMatrixA(netlist, DT, time, false);
        solver.buildVectorB(netlist, DT, time, false);
        solver.solve();

        // Diode iteration (simplified version of HeadlessSimulationEngine.doDiodeIteration)
        doDiodeIteration(netlist, solver);

        double[] currents = new double[netlist.getElementCount()];
        calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
        solver.shiftHistory();
        System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);

        return currents;
    }

    private void doDiodeIteration(CircuitNetlist netlist, MatrixSolver solver) {
        double stoergroesse = 1.0;
        int iterCount = 0;
        boolean changed = true;
        double[] p = solver.getP();
        double[][] a = solver.getSystemMatrix();
        double[] bVector = solver.getb();

        while (changed) {
            changed = false;
            for (int i = 0; i < netlist.getElementCount(); i++) {
                CircuitTypCore type = netlist.getType(i);
                if (type != CircuitTypCore.LK_D) continue;

                double[] params = netlist.getParameter(i);
                int x = netlist.getNodeX(i);
                int y = netlist.getNodeY(i);
                double vd = p[x] - p[y];
                double rD = params[0];
                double uf = params[1];

                if ((vd < stoergroesse * uf) && (rD < 10000)) {
                    double aOLD = 1.0 / rD;
                    double bOLD = uf / rD;
                    params[0] = params[3]; // rOff
                    double aNEW = 1.0 / params[0];
                    double bNEW = uf / params[0];
                    a[x][x] += (-aOLD + aNEW);
                    a[y][y] += (-aOLD + aNEW);
                    a[x][y] += (aOLD - aNEW);
                    a[y][x] += (aOLD - aNEW);
                    bVector[x] += (-bOLD + bNEW);
                    bVector[y] += (bOLD - bNEW);
                    changed = true;
                } else if ((vd > stoergroesse * uf) && (rD > 10000)) {
                    double aOLD = 1.0 / rD;
                    double bOLD = uf / rD;
                    params[0] = params[2]; // rOn
                    double aNEW = 1.0 / params[0];
                    double bNEW = uf / params[0];
                    a[x][x] += (-aOLD + aNEW);
                    a[y][y] += (-aOLD + aNEW);
                    a[x][y] += (aOLD - aNEW);
                    a[y][x] += (aOLD - aNEW);
                    bVector[x] += (-bOLD + bNEW);
                    bVector[y] += (bOLD - bNEW);
                    changed = true;
                }
            }

            if (changed) {
                solver.setMatrixChanged();
                solver.solve();
                iterCount++;
                if (iterCount > 2) stoergroesse *= 0.99;
                if (iterCount > 10000) fail("Diode iteration did not converge");
            }
        }
    }

    @Test
    void forwardBiasedDiodeConducts() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.diodeCircuit(VIN_FWD, R, UF, R_ON, R_OFF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = simulateOneStep(netlist, solver, calc, 0);

        // After diode iteration, diode should be ON
        double rD = netlist.getParameter(2)[0];
        assertTrue(rD < 10000, "Diode should be ON (low resistance), got rD=" + rD);

        // Current should match analytical: I ≈ (Vin - Uf) / R
        // (R_ON is very small compared to R, so R dominates)
        double expectedI = AnalyticalSolutions.diodeForwardCurrent(VIN_FWD, R, UF);
        assertEquals(expectedI, currents[1], expectedI * 0.05,
                String.format("Forward current: expected %.4f mA, got %.4f mA",
                        expectedI * 1e3, currents[1] * 1e3));
    }

    @Test
    void reverseBiasedDiodeBlocks() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.diodeCircuit(VIN_REV, R, UF, R_ON, R_OFF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = simulateOneStep(netlist, solver, calc, 0);

        // Diode should remain OFF
        double rD = netlist.getParameter(2)[0];
        assertTrue(rD > 10000, "Diode should be OFF (high resistance), got rD=" + rD);

        // Current should be ~0
        assertTrue(Math.abs(currents[1]) < 1e-6,
                "Reverse current should be ~0, got " + currents[1]);
    }

    @Test
    void diodeTogglesFromOffToOn() {
        // Start with OFF diode and forward voltage
        CircuitNetlist netlist = SyntheticCircuitBuilder.diodeCircuit(VIN_FWD, R, UF, R_ON, R_OFF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        // Verify diode starts OFF
        assertEquals(R_OFF, netlist.getParameter(2)[0], 1e-6,
                "Diode should start in OFF state");

        // After one step with diode iteration, it should toggle ON
        simulateOneStep(netlist, solver, calc, 0);

        // Diode should now be ON
        double rD = netlist.getParameter(2)[0];
        assertEquals(R_ON, rD, 1e-6,
                "Diode should have toggled to ON state");
    }

    @Test
    void multiStepStabilityWithDiode() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.diodeCircuit(VIN_FWD, R, UF, R_ON, R_OFF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double expectedI = AnalyticalSolutions.diodeForwardCurrent(VIN_FWD, R, UF);
        double[] currents = null;

        // Run 100 steps — DC circuit with forward-biased diode should be stable
        for (int step = 0; step < 100; step++) {
            double time = step * DT;
            currents = simulateOneStep(netlist, solver, calc, time);
        }

        assertNotNull(currents);
        assertEquals(expectedI, currents[1], expectedI * 0.05,
                "After 100 steps, current should remain stable at analytical value");
    }
}

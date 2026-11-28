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
 * Integration test C6: Synthetic buck converter.
 *
 * <p>Full integration test combining: diode iteration + gate timing +
 * inductor/capacitor history + current calculation.
 *
 * <p>Topology: Vin → S → sw_node → L → out → R_load → GND, D from GND to sw_node,
 * C from out to GND.
 *
 * <p>Expected (CCM): Vo = D * Vin.
 */
@Tag("integration")
class SyntheticBuckTest {

    private static final double VIN = 24.0;
    private static final double L = 100e-6;       // 100 μH
    private static final double C = 10e-6;         // 10 μF
    private static final double R_LOAD = 10.0;     // 10 Ω
    private static final double DUTY = 0.5;        // 50% duty cycle
    private static final double FREQ = 100e3;      // 100 kHz
    private static final double PERIOD = 1.0 / FREQ;

    private static final double R_SW_ON = 1e-3;
    private static final double R_SW_OFF = 1e9;
    private static final double UF = 0.7;
    private static final double R_D_ON = 1e-3;
    private static final double R_D_OFF = 1e9;

    // dt = period/100 = 100ns (10 points per duty cycle transition)
    private static final double DT = PERIOD / 100.0;

    /**
     * Performs diode iteration on the netlist (matching HeadlessSimulationEngine logic).
     */
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
                if (type != CircuitTypCore.LK_D && type != CircuitTypCore.LK_THYR
                        && type != CircuitTypCore.LK_IGBT) {
                    continue;
                }

                double[] params = netlist.getParameter(i);
                int x = netlist.getNodeX(i);
                int y = netlist.getNodeY(i);
                double vd = p[x] - p[y];
                double rD = params[0];
                double uf = params[1];

                if ((vd < stoergroesse * uf) && (rD < 10000)) {
                    double aOLD = 1.0 / rD;
                    double bOLD = uf / rD;
                    params[0] = params[3];
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
                    params[0] = params[2];
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

    /**
     * Applies gate signal to the switch (element index 1) in the buck netlist.
     * Matches DomainCoupler.applyGateSignals() for LK_S:
     * params[0] = gate > 0.5 ? params[1] : params[2]
     */
    private void applyGateSignal(CircuitNetlist netlist, boolean on) {
        double[] switchParams = netlist.getParameter(1); // element 1 = switch
        if (on) {
            switchParams[0] = switchParams[1]; // R_ON
            switchParams[8] = 1.0;
        } else {
            switchParams[0] = switchParams[2]; // R_OFF
            switchParams[8] = 0.0;
        }
    }

    @Test
    void outputVoltageConvergesToDutyTimesVin() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.buckConverter(
                VIN, L, C, R_LOAD, R_SW_ON, R_SW_OFF, UF, R_D_ON, R_D_OFF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];
        int stepsPerPeriod = (int) (PERIOD / DT);
        int onSteps = (int) (DUTY * stepsPerPeriod);

        // Run for 50 switching periods to reach steady state
        int totalPeriods = 50;
        double vout = 0;

        for (int period = 0; period < totalPeriods; period++) {
            for (int stepInPeriod = 0; stepInPeriod < stepsPerPeriod; stepInPeriod++) {
                int globalStep = period * stepsPerPeriod + stepInPeriod;
                double time = globalStep * DT;

                // Apply gate signal based on duty cycle
                boolean gateOn = (stepInPeriod < onSteps);
                applyGateSignal(netlist, gateOn);

                // Build and solve
                solver.buildMatrixA(netlist, DT, time, false);
                solver.buildVectorB(netlist, DT, time, false);
                solver.solve();

                // Diode iteration
                doDiodeIteration(netlist, solver);

                // Compute currents and shift history
                calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
                solver.shiftHistory();
                System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
            }

            vout = solver.getP()[3]; // node 3 = output
        }

        // Expected: Vo = D * Vin (minus diode drop ~ D*Uf for non-ideal)
        double expectedVo = AnalyticalSolutions.buckOutputVoltage(VIN, DUTY);
        // Allow 15% tolerance due to diode drop and non-ideal components
        assertEquals(expectedVo, vout, expectedVo * 0.15,
                String.format("Buck output after %d periods: expected ~%.1f V, got %.2f V",
                        totalPeriods, expectedVo, vout));
    }

    @Test
    void inductorCurrentIsContinuous() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.buckConverter(
                VIN, L, C, R_LOAD, R_SW_ON, R_SW_OFF, UF, R_D_ON, R_D_OFF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];
        int stepsPerPeriod = (int) (PERIOD / DT);
        int onSteps = (int) (DUTY * stepsPerPeriod);

        // Run 30 periods to reach steady state
        for (int period = 0; period < 30; period++) {
            for (int stepInPeriod = 0; stepInPeriod < stepsPerPeriod; stepInPeriod++) {
                int globalStep = period * stepsPerPeriod + stepInPeriod;
                double time = globalStep * DT;

                applyGateSignal(netlist, stepInPeriod < onSteps);
                solver.buildMatrixA(netlist, DT, time, false);
                solver.buildVectorB(netlist, DT, time, false);
                solver.solve();
                doDiodeIteration(netlist, solver);
                calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
                solver.shiftHistory();
                System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
            }
        }

        // Check last period: inductor current should stay positive (CCM)
        double iLmin = Double.MAX_VALUE;
        double iLmax = Double.MIN_VALUE;

        for (int stepInPeriod = 0; stepInPeriod < stepsPerPeriod; stepInPeriod++) {
            int globalStep = 30 * stepsPerPeriod + stepInPeriod;
            double time = globalStep * DT;

            applyGateSignal(netlist, stepInPeriod < onSteps);
            solver.buildMatrixA(netlist, DT, time, false);
            solver.buildVectorB(netlist, DT, time, false);
            solver.solve();
            doDiodeIteration(netlist, solver);
            calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);

            double iL = currents[3]; // element 3 = inductor
            iLmin = Math.min(iLmin, iL);
            iLmax = Math.max(iLmax, iL);
        }

        // In CCM, minimum inductor current should be positive
        assertTrue(iLmin > 0,
                String.format("CCM: inductor current min should be >0, got %.4f A", iLmin));

        // Inductor current should have ripple
        double ripple = iLmax - iLmin;
        assertTrue(ripple > 0.01,
                String.format("Inductor should have measurable ripple, got %.4f A", ripple));
    }

    @Test
    void switchNodeVoltageToggles() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.buckConverter(
                VIN, L, C, R_LOAD, R_SW_ON, R_SW_OFF, UF, R_D_ON, R_D_OFF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];
        int stepsPerPeriod = (int) (PERIOD / DT);
        int onSteps = (int) (DUTY * stepsPerPeriod);

        // Run to steady state (20 periods)
        for (int period = 0; period < 20; period++) {
            for (int stepInPeriod = 0; stepInPeriod < stepsPerPeriod; stepInPeriod++) {
                int globalStep = period * stepsPerPeriod + stepInPeriod;
                double time = globalStep * DT;
                applyGateSignal(netlist, stepInPeriod < onSteps);
                solver.buildMatrixA(netlist, DT, time, false);
                solver.buildVectorB(netlist, DT, time, false);
                solver.solve();
                doDiodeIteration(netlist, solver);
                calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
                solver.shiftHistory();
                System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
            }
        }

        // Check switch node voltage during ON phase (should be ~Vin)
        applyGateSignal(netlist, true);
        solver.buildMatrixA(netlist, DT, 20 * PERIOD, false);
        solver.buildVectorB(netlist, DT, 20 * PERIOD, false);
        solver.solve();
        doDiodeIteration(netlist, solver);
        double vSwOn = solver.getP()[2]; // node 2 = switch node

        // Check switch node during OFF phase (should be ~-Uf due to freewheeling diode)
        applyGateSignal(netlist, false);
        calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, 20 * PERIOD, true);
        solver.shiftHistory();
        System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);

        solver.buildMatrixA(netlist, DT, 20 * PERIOD + DT, false);
        solver.buildVectorB(netlist, DT, 20 * PERIOD + DT, false);
        solver.solve();
        doDiodeIteration(netlist, solver);
        double vSwOff = solver.getP()[2];

        // During ON: switch node ≈ Vin
        assertTrue(vSwOn > VIN * 0.8,
                String.format("Switch ON: V_sw should be ~Vin (%.1f), got %.2f", VIN, vSwOn));

        // During OFF: switch node ≈ -Uf (freewheeling diode clamps to ~-0.7V)
        assertTrue(vSwOff < VIN * 0.5,
                String.format("Switch OFF: V_sw should drop below Vin/2, got %.2f", vSwOff));
    }
}

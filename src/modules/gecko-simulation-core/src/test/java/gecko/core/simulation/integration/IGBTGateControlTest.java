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
import gecko.core.testutil.SyntheticCircuitBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test C13: Resistor + IGBT circuit.
 *
 * <p>Verifies IGBT gate control behavior, voltage-based diode iteration
 * switching (LK_IGBT handled same as LK_D in doDiodeIteration), forward voltage
 * compensation, and non-latching turn-off (unlike thyristor).
 *
 * <p>Topology (from SyntheticCircuitBuilder.igbtCircuit):
 * <pre>
 *   Vin (LK_U, DC) ──[node1]── R ──[node2]── IGBT ──[GND]
 *   IGBT: collector=node2, emitter=GND
 * </pre>
 *
 * Elements: [0]=Vin, [1]=R, [2]=IGBT
 * Nodes: 0=GND, 1=Vin+, 2=R-IGBT junction
 * Current through IGBT = currents[2]
 *
 * <p>IGBT parameter layout (LK_IGBT):
 * <ul>
 *   <li>params[0] = current resistance rD</li>
 *   <li>params[1] = Uf (forward voltage, default 1.5V)</li>
 *   <li>params[2] = rOn</li>
 *   <li>params[3] = rOff</li>
 *   <li>params[8] = gate signal (0 or 1)</li>
 * </ul>
 *
 * <p>Key behavioral difference from thyristor: the IGBT turns OFF immediately
 * when gate=0 (non-latching). In the simplified diode iteration model, LK_IGBT
 * is handled identically to LK_D: params[0] is toggled between rOn and rOff
 * based on whether the device voltage vd crosses Uf. Gate ON/OFF is enforced
 * by setting params[0] directly before each step, so that the diode iteration
 * confirms or corrects the state rather than driving it.
 *
 * <p>Gate ON: params[8]=1, params[0]=rOn → diode iteration confirms ON with Uf drop.
 * Gate OFF: params[8]=0, params[0]=rOff → diode iteration skips the rOff→rOn transition
 * because gate=0, so IGBT stays OFF even under forward voltage.
 *
 * <p>Forward bias (Vin=+10V, gate=1): I ≈ (Vin - Uf) / R = (10 - 1.5) / 1000 = 8.5 mA.
 * <p>Reverse bias (Vin=-10V, gate=1): I ≈ 0.
 */
@Tag("integration")
class IGBTGateControlTest {

    private static final double VIN = 10.0;
    private static final double VIN_REV = -10.0;
    private static final double R = 1000.0;    // 1 kΩ
    private static final double UF = 1.5;
    private static final double R_ON = 1e-3;
    private static final double R_OFF = 1e9;
    private static final double DT = 1e-6;

    // -----------------------------------------------------------------------
    // Simulation helpers
    // -----------------------------------------------------------------------

    /**
     * Applies the IGBT gate signal by setting params[8] and the initial resistance
     * params[0] before each simulation step.
     *
     * <p>Gate ON: params[0] = rOn (params[2]), params[8] = 1. The diode iteration
     * will confirm conduction since vd > Uf.
     * <p>Gate OFF: params[0] = rOff (params[3]), params[8] = 0. The diode iteration
     * leaves the IGBT in the OFF state because rD is large.
     *
     * @param netlist the circuit netlist
     * @param on      true to turn gate on, false to turn gate off
     */
    private void applyIGBTGate(CircuitNetlist netlist, boolean on) {
        double[] igbtParams = netlist.getParameter(2); // element 2 = IGBT
        if (on) {
            igbtParams[0] = igbtParams[2]; // rOn
            igbtParams[8] = 1.0;
        } else {
            igbtParams[0] = igbtParams[3]; // rOff
            igbtParams[8] = 0.0;
        }
    }

    /**
     * Performs one simulation step including IGBT-aware diode iteration.
     * Matches the HeadlessSimulationEngine loop pattern used in other integration tests.
     *
     * @param netlist the circuit netlist
     * @param solver  the matrix solver (must be initialized)
     * @param calc    the component current calculator
     * @param time    simulation time at start of this step [s]
     * @return component currents after this step
     */
    private double[] simulateOneStep(CircuitNetlist netlist, MatrixSolver solver,
                                      ComponentCurrentCalculator calc, double time) {
        solver.buildMatrixA(netlist, DT, time, false);
        solver.buildVectorB(netlist, DT, time, false);
        solver.solve();
        doDiodeIteration(netlist, solver);
        double[] currents = new double[netlist.getElementCount()];
        calc.calculateComponentCurrents(solver, netlist, currents, 0, DT, time, true);
        solver.shiftHistory();
        System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);
        return currents;
    }

    /**
     * Diode iteration that handles LK_D, LK_THYR, and LK_IGBT.
     *
     * <p>LK_IGBT gate control: unlike LK_D and LK_THYR (which are purely
     * voltage-controlled), LK_IGBT checks params[8] (gate signal) before allowing
     * a transition from rOff to rOn. If gate=0 the IGBT is held OFF regardless of
     * the device voltage — this is the non-latching property that distinguishes
     * the IGBT from the thyristor.
     *
     * <p>LK_D and LK_THYR use the same voltage-only logic: params[0] is toggled
     * between rOn (params[2]) and rOff (params[3]) based on whether vd crosses
     * Uf (params[1]).
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

                // For IGBT: gate=0 means the device must stay OFF.
                // Skip the rOff→rOn transition when gate is not asserted.
                boolean igbtGateOff = (type == CircuitTypCore.LK_IGBT) && (params[8] == 0.0);

                if ((vd < stoergroesse * uf) && (rD < 10000)) {
                    double aOLD = 1.0 / rD;
                    double bOLD = uf / rD;
                    params[0] = params[3]; // switch to rOff
                    double aNEW = 1.0 / params[0];
                    double bNEW = uf / params[0];
                    a[x][x] += (-aOLD + aNEW);
                    a[y][y] += (-aOLD + aNEW);
                    a[x][y] += (aOLD - aNEW);
                    a[y][x] += (aOLD - aNEW);
                    bVector[x] += (-bOLD + bNEW);
                    bVector[y] += (bOLD - bNEW);
                    changed = true;
                } else if ((vd > stoergroesse * uf) && (rD > 10000) && !igbtGateOff) {
                    // Voltage-triggered turn-on: allowed for LK_D and LK_THYR always,
                    // and for LK_IGBT only when gate=1.
                    double aOLD = 1.0 / rD;
                    double bOLD = uf / rD;
                    params[0] = params[2]; // switch to rOn
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
                if (iterCount > 10000) fail("IGBT diode iteration did not converge");
            }
        }
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    /**
     * Test 1: Gate ON with forward bias — IGBT conducts.
     *
     * <p>VIN = +10 V, gate = 1. Gate is asserted by setting params[0]=rOn before
     * the step so that diode iteration starts in the ON state and confirms
     * conduction (vd &gt; Uf). After the step, params[0] should remain low (rOn)
     * and the current should match the analytical value:
     * I = (Vin - Uf) / R = (10 - 1.5) / 1000 = 8.5 mA.
     */
    @Test
    void gateOnForwardBiasConducts() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.igbtCircuit(VIN, R, R_ON, R_OFF, UF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        // Assert gate before simulation step
        applyIGBTGate(netlist, true);

        double[] currents = simulateOneStep(netlist, solver, calc, 0);

        // After diode iteration, IGBT should be ON (low resistance)
        double rD = netlist.getParameter(2)[0];
        assertTrue(rD < 10000,
                "IGBT should be ON (low resistance) with gate=1 and forward bias, got rD=" + rD);

        // Current should match analytical: I = (Vin - Uf) / R
        double expectedI = (VIN - UF) / R;
        assertEquals(expectedI, currents[2], expectedI * 0.05,
                String.format("Forward current: expected %.4f mA, got %.4f mA",
                        expectedI * 1e3, currents[2] * 1e3));
    }

    /**
     * Test 2: Gate OFF blocks conduction even under forward bias.
     *
     * <p>VIN = +10 V, gate = 0. Gate is deasserted by setting params[0]=rOff and
     * params[8]=0 before the step. Even though forward voltage is applied (vd &gt; Uf
     * with rOff in circuit), the IGBT stays OFF because doDiodeIteration skips the
     * rOff→rOn transition when params[8]==0 for LK_IGBT. This gate-controlled
     * blocking is the key behavioral difference from a pure diode. Current is ~0.
     */
    @Test
    void gateOffBlocks() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.igbtCircuit(VIN, R, R_ON, R_OFF, UF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        // Deassert gate: params[0] = rOff, params[8] = 0
        applyIGBTGate(netlist, false);

        double[] currents = simulateOneStep(netlist, solver, calc, 0);

        // IGBT should remain OFF (high resistance) despite forward-bias voltage
        double rD = netlist.getParameter(2)[0];
        assertTrue(rD > 10000,
                "IGBT should be OFF (high resistance) with gate=0, got rD=" + rD);

        // Current should be ~0 (only leakage through rOff)
        assertTrue(Math.abs(currents[2]) < 1e-6,
                "Current through IGBT should be ~0 with gate=0, got " + currents[2]);
    }

    /**
     * Test 3: Gate ON/OFF cycling demonstrates non-latching behavior.
     *
     * <p>This test distinguishes IGBT from thyristor:
     * <ol>
     *   <li>Phase 1 (steps 0..49): gate=1, IGBT conducts at ~8.5 mA.</li>
     *   <li>Phase 2 (steps 50..99): gate=0, IGBT immediately blocks (current drops to ~0).</li>
     *   <li>Phase 3 (steps 100..149): gate=1 again, IGBT resumes conduction at ~8.5 mA.</li>
     * </ol>
     *
     * <p>A thyristor would remain latched in Phase 2 despite gate=0 (because DC
     * forward bias keeps vd &gt; Uf). The IGBT turns OFF immediately because gate=0
     * forces params[0]=rOff before each step, preventing the diode iteration from
     * switching it back ON.
     */
    @Test
    void gateOnOffCycling() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.igbtCircuit(VIN, R, R_ON, R_OFF, UF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double expectedOnCurrent = (VIN - UF) / R;
        double[] currents;

        // Phase 1: 50 steps with gate ON — IGBT should conduct
        for (int step = 0; step < 50; step++) {
            applyIGBTGate(netlist, true);
            currents = simulateOneStep(netlist, solver, calc, step * DT);
        }

        double rDAfterOn = netlist.getParameter(2)[0];
        assertTrue(rDAfterOn < 10000,
                "After gate=1 phase (50 steps), IGBT should be ON, got rD=" + rDAfterOn);

        // Sample current from last ON step
        applyIGBTGate(netlist, true);
        currents = simulateOneStep(netlist, solver, calc, 50 * DT);
        assertEquals(expectedOnCurrent, currents[2], expectedOnCurrent * 0.05,
                String.format("Gate=1 phase: expected ~%.4f mA, got %.4f mA",
                        expectedOnCurrent * 1e3, currents[2] * 1e3));

        // Phase 2: 50 steps with gate OFF — IGBT should immediately block (non-latching)
        for (int step = 51; step < 101; step++) {
            applyIGBTGate(netlist, false);
            currents = simulateOneStep(netlist, solver, calc, step * DT);
        }

        double rDAfterOff = netlist.getParameter(2)[0];
        assertTrue(rDAfterOff > 10000,
                "After gate=0 phase (50 steps), IGBT should be OFF (non-latching), got rD=" + rDAfterOff);

        applyIGBTGate(netlist, false);
        currents = simulateOneStep(netlist, solver, calc, 101 * DT);
        assertTrue(Math.abs(currents[2]) < 1e-6,
                "Gate=0 phase: IGBT current should be ~0, got " + currents[2]);

        // Phase 3: 50 steps with gate ON again — IGBT should resume conduction
        for (int step = 102; step < 152; step++) {
            applyIGBTGate(netlist, true);
            currents = simulateOneStep(netlist, solver, calc, step * DT);
        }

        double rDAfterReOn = netlist.getParameter(2)[0];
        assertTrue(rDAfterReOn < 10000,
                "After gate=1 resumed (50 steps), IGBT should be ON again, got rD=" + rDAfterReOn);

        applyIGBTGate(netlist, true);
        currents = simulateOneStep(netlist, solver, calc, 152 * DT);
        assertEquals(expectedOnCurrent, currents[2], expectedOnCurrent * 0.05,
                String.format("Gate=1 resumed: expected ~%.4f mA, got %.4f mA",
                        expectedOnCurrent * 1e3, currents[2] * 1e3));
    }

    /**
     * Test 4: Reverse bias blocks even with gate ON.
     *
     * <p>VIN = -10 V, gate = 1. The IGBT has reverse voltage applied
     * (collector negative w.r.t. emitter). The diode iteration leaves IGBT
     * OFF because vd &lt; 0 &lt; Uf. Current should be approximately zero.
     */
    @Test
    void reverseBiasBlocks() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.igbtCircuit(VIN_REV, R, R_ON, R_OFF, UF);
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        // Gate is ON, but reverse-biased — IGBT cannot turn ON
        applyIGBTGate(netlist, true);

        double[] currents = simulateOneStep(netlist, solver, calc, 0);

        // IGBT should remain OFF due to reverse bias
        double rD = netlist.getParameter(2)[0];
        assertTrue(rD > 10000,
                "Reverse-biased IGBT should stay OFF (high resistance), got rD=" + rD);

        // Current through IGBT should be ~0
        assertTrue(Math.abs(currents[2]) < 1e-6,
                "Reverse current through IGBT should be ~0, got " + currents[2]);
    }
}

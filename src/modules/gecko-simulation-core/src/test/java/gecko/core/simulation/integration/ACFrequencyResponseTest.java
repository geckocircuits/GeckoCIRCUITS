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
 * Integration test C9: AC frequency response of RL and RC low-pass filters.
 *
 * <p>Verifies that the simulator correctly reproduces the gain roll-off of first-order
 * passive filters driven by a sinusoidal voltage source across a sweep of frequencies
 * spanning two decades around the -3 dB corner frequency.
 *
 * <h2>RL low-pass filter (output across R)</h2>
 * <pre>
 *   Vsin (LK_U, SIN) ──[node1]── R ──[node2]── L ──[GND]
 * </pre>
 * Elements: [0]=Vsin, [1]=R, [2]=L
 * Nodes: 0=GND, 1=Vsin+, 2=R-L junction (voltage across L)
 * Gain across R = I_peak * R / V_in_peak = R / sqrt(R^2 + (omega*L)^2)
 * Corner frequency: f_c = R / (2*pi*L)
 *
 * <h2>RC low-pass filter (output across C)</h2>
 * <pre>
 *   Vsin (LK_U, SIN) ──[node1]── R ──[node2]── C ──[GND]
 * </pre>
 * Elements: [0]=Vsin, [1]=R, [2]=C
 * Nodes: 0=GND, 1=Vsin+, 2=R-C junction (capacitor voltage)
 * Gain across C = V_C_peak / V_in_peak = 1 / sqrt(1 + (omega*R*C)^2)
 * Corner frequency: f_c = 1 / (2*pi*R*C)
 *
 * <p>Each test simulates 10 complete cycles at each frequency using 200 time points
 * per cycle, then measures the steady-state peak amplitude in the last 2 cycles.
 * Backward Euler introduces first-order phase lag and amplitude error that grow
 * with frequency; a 5% tolerance accommodates this numerical dissipation.
 */
@Tag("integration")
class ACFrequencyResponseTest {

    // ========================================================================
    // RL low-pass filter parameters
    // ========================================================================

    /** Series resistance for RL filter [Omega]. */
    private static final double RL_R = 100.0;

    /** Inductance for RL filter [H]. */
    private static final double RL_L = 10e-3;  // 10 mH

    /**
     * Corner frequency of RL filter [Hz].
     * f_c = R / (2*pi*L) = 100 / (2*pi*0.01) ≈ 1591.5 Hz
     */
    private static final double RL_FC = RL_R / (2.0 * Math.PI * RL_L);

    // ========================================================================
    // RC low-pass filter parameters
    // ========================================================================

    /** Series resistance for RC filter [Omega]. */
    private static final double RC_R = 1000.0;

    /** Capacitance for RC filter [F]. */
    private static final double RC_C = 100e-9;  // 100 nF

    /**
     * Corner frequency of RC filter [Hz].
     * f_c = 1 / (2*pi*R*C) = 1 / (2*pi*1000*1e-7) ≈ 1591.5 Hz
     */
    private static final double RC_FC = 1.0 / (2.0 * Math.PI * RC_R * RC_C);

    /** Peak amplitude of the sine source [V]. */
    private static final double AMPLITUDE = 10.0;

    /** Number of complete cycles to simulate (allows transients to die out). */
    private static final int TOTAL_CYCLES = 10;

    /**
     * Number of time points per cycle (200 gives <0.5% discretisation error
     * for Backward Euler at the frequencies of interest).
     */
    private static final int POINTS_PER_CYCLE = 200;

    /** Number of cycles at the end of the simulation used for peak detection. */
    private static final int MEASUREMENT_CYCLES = 2;

    /**
     * Maximum relative tolerance for gain comparison.
     * Backward Euler introduces frequency-dependent amplitude damping;
     * 5% covers the worst case within the tested frequency range.
     */
    private static final double TOLERANCE = 0.05;

    // ========================================================================
    // Helper: run simulation and return steady-state peak of a node voltage
    // ========================================================================

    /**
     * Runs a full simulation for the given netlist at the specified frequency,
     * then returns the peak absolute value observed in {@code solver.getP()[nodeIndex]}
     * during the last {@link #MEASUREMENT_CYCLES} cycles.
     *
     * <p>Time step: DT = 1 / (freq * POINTS_PER_CYCLE)
     * <p>Total steps: TOTAL_CYCLES * POINTS_PER_CYCLE
     *
     * @param netlist   fully configured circuit netlist
     * @param freq      sine source frequency [Hz]
     * @param nodeIndex MNA node index whose voltage peak is sought
     * @return peak absolute node voltage in the last MEASUREMENT_CYCLES cycles
     */
    private double measureNodeVoltagePeak(CircuitNetlist netlist, double freq, int nodeIndex) {
        double dt = 1.0 / (freq * POINTS_PER_CYCLE);
        int totalSteps = TOTAL_CYCLES * POINTS_PER_CYCLE;
        int measureStart = (TOTAL_CYCLES - MEASUREMENT_CYCLES) * POINTS_PER_CYCLE;

        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];
        double peakVoltage = 0.0;

        for (int step = 0; step < totalSteps; step++) {
            double time = step * dt;
            solver.buildMatrixA(netlist, dt, time, false);
            solver.buildVectorB(netlist, dt, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, dt, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);

            if (step >= measureStart) {
                double nodeVoltage = Math.abs(solver.getP()[nodeIndex]);
                if (nodeVoltage > peakVoltage) {
                    peakVoltage = nodeVoltage;
                }
            }
        }

        return peakVoltage;
    }

    /**
     * Runs a full simulation for the given netlist at the specified frequency,
     * then returns the peak absolute value of the current through element at
     * {@code elementIndex} during the last {@link #MEASUREMENT_CYCLES} cycles.
     *
     * @param netlist       fully configured circuit netlist
     * @param freq          sine source frequency [Hz]
     * @param elementIndex  index of the element whose current peak is sought
     * @return peak absolute current through the element in the last MEASUREMENT_CYCLES cycles
     */
    private double measureCurrentPeak(CircuitNetlist netlist, double freq, int elementIndex) {
        double dt = 1.0 / (freq * POINTS_PER_CYCLE);
        int totalSteps = TOTAL_CYCLES * POINTS_PER_CYCLE;
        int measureStart = (TOTAL_CYCLES - MEASUREMENT_CYCLES) * POINTS_PER_CYCLE;

        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        ComponentCurrentCalculator calc = new ComponentCurrentCalculator();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());

        double[] currents = new double[netlist.getElementCount()];
        double peakCurrent = 0.0;

        for (int step = 0; step < totalSteps; step++) {
            double time = step * dt;
            solver.buildMatrixA(netlist, dt, time, false);
            solver.buildVectorB(netlist, dt, time, false);
            solver.solve();
            calc.calculateComponentCurrents(solver, netlist, currents, 0, dt, time, true);
            solver.shiftHistory();
            System.arraycopy(currents, 0, solver.getIALT(), 0, currents.length);

            if (step >= measureStart) {
                double current = Math.abs(currents[elementIndex]);
                if (current > peakCurrent) {
                    peakCurrent = current;
                }
            }
        }

        return peakCurrent;
    }

    // ========================================================================
    // RL low-pass filter tests
    // ========================================================================

    /**
     * Verifies that the RL low-pass gain at f_c/10 (well below the corner frequency)
     * is close to unity — the filter passes low-frequency signals with little attenuation.
     *
     * <p>Analytical gain at f_c/10: R / sqrt(R^2 + (omega*L)^2) ≈ 0.995
     */
    @Test
    void rlLowPassGain_farBelowCorner() {
        double freq = RL_FC / 10.0;
        double analyticalGain = AnalyticalSolutions.rlGainAtFrequency(RL_R, RL_L, freq);

        CircuitNetlist netlist = SyntheticCircuitBuilder.acSineSource(AMPLITUDE, freq, RL_R, RL_L);
        // Element [1] = R; peak current through R gives V_R_peak = I_R_peak * R
        double iPeak = measureCurrentPeak(netlist, freq, 1);
        double measuredGain = (iPeak * RL_R) / AMPLITUDE;

        System.out.printf("RL f=%.1f Hz (fc/10): analytical=%.4f, measured=%.4f%n",
                freq, analyticalGain, measuredGain);

        assertEquals(analyticalGain, measuredGain, analyticalGain * TOLERANCE,
                String.format("RL gain at f=%.1f Hz (fc/10): expected %.4f, got %.4f",
                        freq, analyticalGain, measuredGain));
    }

    /**
     * Verifies the RL low-pass gain at f_c/2 (below the corner frequency).
     *
     * <p>Analytical gain: R / sqrt(R^2 + (omega*L)^2) ≈ 0.894
     */
    @Test
    void rlLowPassGain_halfCornerFrequency() {
        double freq = RL_FC / 2.0;
        double analyticalGain = AnalyticalSolutions.rlGainAtFrequency(RL_R, RL_L, freq);

        CircuitNetlist netlist = SyntheticCircuitBuilder.acSineSource(AMPLITUDE, freq, RL_R, RL_L);
        double iPeak = measureCurrentPeak(netlist, freq, 1);
        double measuredGain = (iPeak * RL_R) / AMPLITUDE;

        System.out.printf("RL f=%.1f Hz (fc/2): analytical=%.4f, measured=%.4f%n",
                freq, analyticalGain, measuredGain);

        assertEquals(analyticalGain, measuredGain, analyticalGain * TOLERANCE,
                String.format("RL gain at f=%.1f Hz (fc/2): expected %.4f, got %.4f",
                        freq, analyticalGain, measuredGain));
    }

    /**
     * Verifies the RL low-pass gain at the corner frequency f_c.
     *
     * <p>At the -3 dB point, the analytical gain is exactly 1/sqrt(2) ≈ 0.707.
     */
    @Test
    void rlLowPassGain_atCornerFrequency() {
        double freq = RL_FC;
        double analyticalGain = AnalyticalSolutions.rlGainAtFrequency(RL_R, RL_L, freq);

        CircuitNetlist netlist = SyntheticCircuitBuilder.acSineSource(AMPLITUDE, freq, RL_R, RL_L);
        double iPeak = measureCurrentPeak(netlist, freq, 1);
        double measuredGain = (iPeak * RL_R) / AMPLITUDE;

        System.out.printf("RL f=%.1f Hz (fc): analytical=%.4f, measured=%.4f%n",
                freq, analyticalGain, measuredGain);

        // At corner: analytical is 1/sqrt(2) ≈ 0.707
        assertEquals(1.0 / Math.sqrt(2.0), analyticalGain, 0.001,
                "RL analytical gain at fc must equal 1/sqrt(2)");
        assertEquals(analyticalGain, measuredGain, analyticalGain * TOLERANCE,
                String.format("RL gain at corner f=%.1f Hz: expected %.4f (0.707), got %.4f",
                        freq, analyticalGain, measuredGain));
    }

    /**
     * Verifies the RL low-pass gain at 2*f_c (above the corner frequency).
     *
     * <p>Analytical gain: R / sqrt(R^2 + (omega*L)^2) ≈ 0.447
     */
    @Test
    void rlLowPassGain_twiceCornerFrequency() {
        double freq = 2.0 * RL_FC;
        double analyticalGain = AnalyticalSolutions.rlGainAtFrequency(RL_R, RL_L, freq);

        CircuitNetlist netlist = SyntheticCircuitBuilder.acSineSource(AMPLITUDE, freq, RL_R, RL_L);
        double iPeak = measureCurrentPeak(netlist, freq, 1);
        double measuredGain = (iPeak * RL_R) / AMPLITUDE;

        System.out.printf("RL f=%.1f Hz (2*fc): analytical=%.4f, measured=%.4f%n",
                freq, analyticalGain, measuredGain);

        assertEquals(analyticalGain, measuredGain, analyticalGain * TOLERANCE,
                String.format("RL gain at f=%.1f Hz (2*fc): expected %.4f, got %.4f",
                        freq, analyticalGain, measuredGain));
    }

    /**
     * Verifies the RL low-pass gain at 10*f_c (well above the corner frequency).
     *
     * <p>Analytical gain: R / sqrt(R^2 + (omega*L)^2) ≈ 0.0995.
     * This is the steepest roll-off point in the test sweep; Backward Euler
     * adds extra damping at high frequencies, but 5% tolerance is sufficient.
     */
    @Test
    void rlLowPassGain_farAboveCorner() {
        double freq = 10.0 * RL_FC;
        double analyticalGain = AnalyticalSolutions.rlGainAtFrequency(RL_R, RL_L, freq);

        CircuitNetlist netlist = SyntheticCircuitBuilder.acSineSource(AMPLITUDE, freq, RL_R, RL_L);
        double iPeak = measureCurrentPeak(netlist, freq, 1);
        double measuredGain = (iPeak * RL_R) / AMPLITUDE;

        System.out.printf("RL f=%.1f Hz (10*fc): analytical=%.4f, measured=%.4f%n",
                freq, analyticalGain, measuredGain);

        assertEquals(analyticalGain, measuredGain, analyticalGain * TOLERANCE,
                String.format("RL gain at f=%.1f Hz (10*fc): expected %.4f, got %.4f",
                        freq, analyticalGain, measuredGain));
    }

    /**
     * Verifies the entire RL frequency sweep in a single parameterised pass.
     *
     * <p>Sweeps f_c/10, f_c/2, f_c, 2*f_c, and 10*f_c and asserts each measured
     * gain is within {@link #TOLERANCE} relative to the analytical value.
     */
    @Test
    void rlLowPassFrequencySweep() {
        double[] freqs = {
            RL_FC / 10.0,
            RL_FC / 2.0,
            RL_FC,
            2.0 * RL_FC,
            10.0 * RL_FC
        };

        for (double freq : freqs) {
            double analyticalGain = AnalyticalSolutions.rlGainAtFrequency(RL_R, RL_L, freq);

            CircuitNetlist netlist = SyntheticCircuitBuilder.acSineSource(
                    AMPLITUDE, freq, RL_R, RL_L);
            double iPeak = measureCurrentPeak(netlist, freq, 1);
            double measuredGain = (iPeak * RL_R) / AMPLITUDE;

            System.out.printf("RL sweep f=%.1f Hz: analytical=%.4f, measured=%.4f%n",
                    freq, analyticalGain, measuredGain);

            assertEquals(analyticalGain, measuredGain, analyticalGain * TOLERANCE,
                    String.format("RL frequency sweep at f=%.1f Hz: expected gain %.4f, got %.4f",
                            freq, analyticalGain, measuredGain));
        }
    }

    // ========================================================================
    // RC low-pass filter tests
    // ========================================================================

    /**
     * Verifies that the RC low-pass gain at f_c/10 (well below the corner frequency)
     * is close to unity — the filter passes low-frequency signals with little attenuation.
     *
     * <p>Analytical gain: 1 / sqrt(1 + (omega*R*C)^2) ≈ 0.995
     */
    @Test
    void rcLowPassGain_farBelowCorner() {
        double freq = RC_FC / 10.0;
        double analyticalGain = AnalyticalSolutions.rcGainAtFrequency(RC_R, RC_C, freq);

        CircuitNetlist netlist = SyntheticCircuitBuilder.acSineSourceRC(
                AMPLITUDE, freq, RC_R, RC_C);
        // Node 2 = R-C junction = voltage across C
        double vcPeak = measureNodeVoltagePeak(netlist, freq, 2);
        double measuredGain = vcPeak / AMPLITUDE;

        System.out.printf("RC f=%.1f Hz (fc/10): analytical=%.4f, measured=%.4f%n",
                freq, analyticalGain, measuredGain);

        assertEquals(analyticalGain, measuredGain, analyticalGain * TOLERANCE,
                String.format("RC gain at f=%.1f Hz (fc/10): expected %.4f, got %.4f",
                        freq, analyticalGain, measuredGain));
    }

    /**
     * Verifies the RC low-pass gain at f_c/2 (below the corner frequency).
     *
     * <p>Analytical gain: 1 / sqrt(1 + (omega*R*C)^2) ≈ 0.894
     */
    @Test
    void rcLowPassGain_halfCornerFrequency() {
        double freq = RC_FC / 2.0;
        double analyticalGain = AnalyticalSolutions.rcGainAtFrequency(RC_R, RC_C, freq);

        CircuitNetlist netlist = SyntheticCircuitBuilder.acSineSourceRC(
                AMPLITUDE, freq, RC_R, RC_C);
        double vcPeak = measureNodeVoltagePeak(netlist, freq, 2);
        double measuredGain = vcPeak / AMPLITUDE;

        System.out.printf("RC f=%.1f Hz (fc/2): analytical=%.4f, measured=%.4f%n",
                freq, analyticalGain, measuredGain);

        assertEquals(analyticalGain, measuredGain, analyticalGain * TOLERANCE,
                String.format("RC gain at f=%.1f Hz (fc/2): expected %.4f, got %.4f",
                        freq, analyticalGain, measuredGain));
    }

    /**
     * Verifies the RC low-pass gain at the corner frequency f_c.
     *
     * <p>At the -3 dB point, the analytical gain is exactly 1/sqrt(2) ≈ 0.707.
     */
    @Test
    void rcLowPassGain_atCornerFrequency() {
        double freq = RC_FC;
        double analyticalGain = AnalyticalSolutions.rcGainAtFrequency(RC_R, RC_C, freq);

        CircuitNetlist netlist = SyntheticCircuitBuilder.acSineSourceRC(
                AMPLITUDE, freq, RC_R, RC_C);
        double vcPeak = measureNodeVoltagePeak(netlist, freq, 2);
        double measuredGain = vcPeak / AMPLITUDE;

        System.out.printf("RC f=%.1f Hz (fc): analytical=%.4f, measured=%.4f%n",
                freq, analyticalGain, measuredGain);

        // At corner: analytical is 1/sqrt(2) ≈ 0.707
        assertEquals(1.0 / Math.sqrt(2.0), analyticalGain, 0.001,
                "RC analytical gain at fc must equal 1/sqrt(2)");
        assertEquals(analyticalGain, measuredGain, analyticalGain * TOLERANCE,
                String.format("RC gain at corner f=%.1f Hz: expected %.4f (0.707), got %.4f",
                        freq, analyticalGain, measuredGain));
    }

    /**
     * Verifies the RC low-pass gain at 2*f_c (above the corner frequency).
     *
     * <p>Analytical gain: 1 / sqrt(1 + (omega*R*C)^2) ≈ 0.447
     */
    @Test
    void rcLowPassGain_twiceCornerFrequency() {
        double freq = 2.0 * RC_FC;
        double analyticalGain = AnalyticalSolutions.rcGainAtFrequency(RC_R, RC_C, freq);

        CircuitNetlist netlist = SyntheticCircuitBuilder.acSineSourceRC(
                AMPLITUDE, freq, RC_R, RC_C);
        double vcPeak = measureNodeVoltagePeak(netlist, freq, 2);
        double measuredGain = vcPeak / AMPLITUDE;

        System.out.printf("RC f=%.1f Hz (2*fc): analytical=%.4f, measured=%.4f%n",
                freq, analyticalGain, measuredGain);

        assertEquals(analyticalGain, measuredGain, analyticalGain * TOLERANCE,
                String.format("RC gain at f=%.1f Hz (2*fc): expected %.4f, got %.4f",
                        freq, analyticalGain, measuredGain));
    }

    /**
     * Verifies the RC low-pass gain at 10*f_c (well above the corner frequency).
     *
     * <p>Analytical gain: 1 / sqrt(1 + (omega*R*C)^2) ≈ 0.0995.
     * Backward Euler adds extra damping at high frequencies; 5% tolerance is sufficient.
     */
    @Test
    void rcLowPassGain_farAboveCorner() {
        double freq = 10.0 * RC_FC;
        double analyticalGain = AnalyticalSolutions.rcGainAtFrequency(RC_R, RC_C, freq);

        CircuitNetlist netlist = SyntheticCircuitBuilder.acSineSourceRC(
                AMPLITUDE, freq, RC_R, RC_C);
        double vcPeak = measureNodeVoltagePeak(netlist, freq, 2);
        double measuredGain = vcPeak / AMPLITUDE;

        System.out.printf("RC f=%.1f Hz (10*fc): analytical=%.4f, measured=%.4f%n",
                freq, analyticalGain, measuredGain);

        assertEquals(analyticalGain, measuredGain, analyticalGain * TOLERANCE,
                String.format("RC gain at f=%.1f Hz (10*fc): expected %.4f, got %.4f",
                        freq, analyticalGain, measuredGain));
    }

    /**
     * Verifies the entire RC frequency sweep in a single parameterised pass.
     *
     * <p>Sweeps f_c/10, f_c/2, f_c, 2*f_c, and 10*f_c and asserts each measured
     * gain is within {@link #TOLERANCE} relative to the analytical value.
     */
    @Test
    void rcLowPassFrequencySweep() {
        double[] freqs = {
            RC_FC / 10.0,
            RC_FC / 2.0,
            RC_FC,
            2.0 * RC_FC,
            10.0 * RC_FC
        };

        for (double freq : freqs) {
            double analyticalGain = AnalyticalSolutions.rcGainAtFrequency(RC_R, RC_C, freq);

            CircuitNetlist netlist = SyntheticCircuitBuilder.acSineSourceRC(
                    AMPLITUDE, freq, RC_R, RC_C);
            double vcPeak = measureNodeVoltagePeak(netlist, freq, 2);
            double measuredGain = vcPeak / AMPLITUDE;

            System.out.printf("RC sweep f=%.1f Hz: analytical=%.4f, measured=%.4f%n",
                    freq, analyticalGain, measuredGain);

            assertEquals(analyticalGain, measuredGain, analyticalGain * TOLERANCE,
                    String.format("RC frequency sweep at f=%.1f Hz: expected gain %.4f, got %.4f",
                            freq, analyticalGain, measuredGain));
        }
    }

    // ========================================================================
    // Cross-validation: gain ratio between RL and RC at same corner frequency
    // ========================================================================

    /**
     * Validates that both RL and RC filters produce the same gain at their
     * respective corner frequencies (both equal 1/sqrt(2) by definition).
     *
     * <p>This cross-check confirms the measurement methodology is symmetric
     * and not biased towards a particular filter topology.
     */
    @Test
    void bothFiltersHaveHalfPowerAtCornerFrequency() {
        // RL at its corner frequency
        double rlFreq = RL_FC;
        CircuitNetlist rlNetlist = SyntheticCircuitBuilder.acSineSource(
                AMPLITUDE, rlFreq, RL_R, RL_L);
        double rlIPeak = measureCurrentPeak(rlNetlist, rlFreq, 1);
        double rlGain = (rlIPeak * RL_R) / AMPLITUDE;

        // RC at its corner frequency
        double rcFreq = RC_FC;
        CircuitNetlist rcNetlist = SyntheticCircuitBuilder.acSineSourceRC(
                AMPLITUDE, rcFreq, RC_R, RC_C);
        double rcVcPeak = measureNodeVoltagePeak(rcNetlist, rcFreq, 2);
        double rcGain = rcVcPeak / AMPLITUDE;

        double halfPower = 1.0 / Math.sqrt(2.0);

        System.out.printf("RL gain at fc=%.1f Hz: %.4f (expected %.4f)%n",
                rlFreq, rlGain, halfPower);
        System.out.printf("RC gain at fc=%.1f Hz: %.4f (expected %.4f)%n",
                rcFreq, rcGain, halfPower);

        assertEquals(halfPower, rlGain, halfPower * TOLERANCE,
                String.format("RL gain at corner must be ~0.707, got %.4f", rlGain));
        assertEquals(halfPower, rcGain, halfPower * TOLERANCE,
                String.format("RC gain at corner must be ~0.707, got %.4f", rcGain));
    }
}

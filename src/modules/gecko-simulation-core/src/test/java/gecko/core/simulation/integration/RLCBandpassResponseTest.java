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
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.simulation.solver.ComponentCurrentCalculator;
import gecko.core.simulation.solver.MatrixSolver;
import gecko.core.testutil.AnalyticalSolutions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: Series RLC bandpass filter frequency response.
 *
 * <p>A series RLC circuit acts as a bandpass filter for current. At resonance
 * f₀ = 1/(2π√(LC)), the impedance is minimized (|Z| = R) and current is maximized.
 * At frequencies far from resonance, current drops significantly.
 *
 * <p>Topology:
 * <pre>
 *   Vsin (LK_U, SIN) ──[node1]── R ──[node2]── L ──[node3]── C ──[GND]
 * </pre>
 *
 * Elements: [0]=Vsin, [1]=R, [2]=L, [3]=C
 * Nodes: 0=GND, 1=Vsin+, 2=R-L junction, 3=L-C junction
 *
 * <p>Circuit parameters:
 * <ul>
 *   <li>R = 10 Ω</li>
 *   <li>L = 10 mH</li>
 *   <li>C = 10 μF</li>
 *   <li>f₀ = 1/(2π√(LC)) ≈ 503 Hz</li>
 *   <li>Q = √(L/C)/R ≈ 3.16</li>
 *   <li>BW = R/(2πL) ≈ 159 Hz</li>
 *   <li>Amplitude = 10 V</li>
 * </ul>
 *
 * <p>The normalized current gain G(f) = (I_peak * R) / V_amplitude equals 1 at resonance
 * and is compared to the analytical formula rlcBandpassGain(r, l, c, f) = R / |Z(f)|.
 *
 * <p>A high-Q (Q ≈ 3.16) circuit requires more cycles to settle; 30 cycles are simulated
 * with measurement in the last 3 cycles to ensure transients are negligible.
 */
@Tag("integration")
class RLCBandpassResponseTest {

    // ========================================================================
    // Circuit parameters
    // ========================================================================

    /** Series resistance [Omega]. */
    private static final double R = 10.0;

    /** Inductance [H]. */
    private static final double L = 10e-3;   // 10 mH

    /** Capacitance [F]. */
    private static final double C = 10e-6;   // 10 uF

    /**
     * Resonant (centre) frequency [Hz].
     * f0 = 1 / (2*pi*sqrt(L*C)) = 1 / (2*pi*sqrt(10e-3 * 10e-6)) ≈ 503 Hz
     */
    private static final double F0 = AnalyticalSolutions.rlcResonantFrequency(L, C);

    /** Peak amplitude of the sine source [V]. */
    private static final double AMPLITUDE = 10.0;

    /**
     * Number of complete cycles to simulate.
     * Q ≈ 3.16 means the transient envelope decays as exp(-pi*f0*t/Q).
     * 30 cycles gives exp(-pi*30/3.16) ≈ 4e-14 — fully settled.
     */
    private static final int TOTAL_CYCLES = 30;

    /**
     * Number of time points per cycle.
     * 200 points/cycle gives &lt;0.5% discretisation error for Backward Euler
     * at the frequencies of interest (up to 10*f0 ≈ 5 kHz).
     */
    private static final int POINTS_PER_CYCLE = 200;

    /** Number of measurement cycles at the end of the simulation window. */
    private static final int MEASUREMENT_CYCLES = 3;

    /**
     * Relative tolerance for gain comparison.
     * A wider 10% tolerance is used because:
     * (a) Backward Euler introduces frequency-dependent amplitude damping; and
     * (b) at off-resonance frequencies the current is very small and the
     *     numerical noise is proportionally larger.
     */
    private static final double TOLERANCE = 0.10;

    // ========================================================================
    // Netlist builder: Vsin → R → L → C → GND
    // ========================================================================

    /**
     * Builds a fresh series RLC netlist driven by a sinusoidal voltage source.
     *
     * <p>Topology:
     * <pre>
     *   Vsin (LK_U, SIN) ──[node1]── R ──[node2]── L ──[node3]── C ──[GND]
     * </pre>
     *
     * Elements: [0]=Vsin, [1]=R, [2]=L, [3]=C
     * Nodes: 0=GND, 1=Vsin+, 2=R-L junction, 3=L-C junction
     *
     * @param amplitude peak amplitude of the sine source [V]
     * @param freq      frequency of the sine source [Hz]
     * @param r         series resistance [Omega]
     * @param l         series inductance [H]
     * @param c         series capacitance [F]
     * @return fully configured CircuitNetlist
     */
    private CircuitNetlist buildSeriesRLC_AC(double amplitude, double freq,
                                              double r, double l, double c) {
        int elementCount = 4;
        int maxNode = 3;
        int maxVSrc = 1;

        CircuitTypCore[] types = {
            CircuitTypCore.LK_U,
            CircuitTypCore.LK_R,
            CircuitTypCore.LK_L,
            CircuitTypCore.LK_C
        };

        // Vsin: node1(+) to GND(-)
        // R:    node1 to node2
        // L:    node2 to node3
        // C:    node3 to GND
        int[] nodeX = {1, 1, 2, 3};
        int[] nodeY = {0, 2, 3, 0};
        int[] vSrcNr = {1, -1, -1, -1};

        double[][] params = new double[elementCount][];

        // [0] Sine voltage source: [0]=QUELLE_SIN_NEW, [2]=freq, [3]=offset, [4]=phase, [20]=amplitude
        params[0] = new double[21];
        params[0][0] = SourceType.QUELLE_SIN_NEW;
        params[0][2] = freq;
        params[0][3] = 0.0;   // DC offset
        params[0][4] = 0.0;   // initial phase
        params[0][20] = amplitude;

        // [1] Resistor: [0]=R
        params[1] = new double[21];
        params[1][0] = r;

        // [2] Inductor: [0]=L
        params[2] = new double[21];
        params[2][0] = l;

        // [3] Capacitor: [0]=C (A-matrix stamper), [6]=C, [7]=C (linear), [10]=0 (correction)
        params[3] = new double[21];
        params[3][0] = c;
        params[3][6] = c;
        params[3][7] = c;
        params[3][10] = 0.0;

        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc, elementCount);
        return netlist;
    }

    // ========================================================================
    // Simulation helper
    // ========================================================================

    /**
     * Simulates a series RLC circuit at the given frequency and returns the
     * peak current magnitude through element [1] (the resistor) measured in
     * the last measurement cycles.
     *
     * <p>The measured peak current through R equals the series current, so
     * normalized gain = (I_peak * R) / amplitude.
     *
     * @param freq            frequency of the sine source [Hz]
     * @param pointsPerCycle  number of simulation steps per period
     * @param totalCycles     total number of periods to simulate
     * @param measureCycles   number of final periods used for peak detection
     * @return peak current magnitude [A] in the last measureCycles cycles
     */
    private double measureCurrentPeak(double freq, int pointsPerCycle,
                                       int totalCycles, int measureCycles) {
        double dt = 1.0 / (freq * pointsPerCycle);
        int totalSteps = totalCycles * pointsPerCycle;
        int measureStart = (totalCycles - measureCycles) * pointsPerCycle;

        CircuitNetlist netlist = buildSeriesRLC_AC(AMPLITUDE, freq, R, L, C);

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
                // Element [1] = R; current through R is the series current
                double iAbs = Math.abs(currents[1]);
                if (iAbs > peakCurrent) {
                    peakCurrent = iAbs;
                }
            }
        }

        return peakCurrent;
    }

    /**
     * Convenience overload using the default simulation parameters
     * ({@link #POINTS_PER_CYCLE}, {@link #TOTAL_CYCLES}, {@link #MEASUREMENT_CYCLES}).
     *
     * @param freq frequency of the sine source [Hz]
     * @return peak current magnitude [A] in the last MEASUREMENT_CYCLES cycles
     */
    private double measureCurrentPeak(double freq) {
        return measureCurrentPeak(freq, POINTS_PER_CYCLE, TOTAL_CYCLES, MEASUREMENT_CYCLES);
    }

    // ========================================================================
    // Test 1: Peak gain at resonance is approximately 1.0
    // ========================================================================

    /**
     * At resonance f₀, the series RLC impedance is minimized to |Z| = R, so the
     * normalized gain G = (I_peak * R) / amplitude = R / |Z(f0)| should equal 1.0.
     *
     * <p>Analytical: rlcBandpassGain(R, L, C, f0) = R / R = 1.0
     *
     * <p>Backward Euler amplitude error scales roughly as (omega*dt)^2 / 2 per step.
     * With 1000 points/cycle at f0 ≈ 503 Hz, dt ≈ 2 µs and the error is well below 1%.
     * A 5% tolerance provides comfortable headroom.
     */
    @Test
    void peakGainAtResonance() {
        // Use 1000 points/cycle at resonance to reduce Backward Euler amplitude damping
        // to under 1% so the 5% tolerance is comfortably met.
        final int finePointsPerCycle = 1000;

        double analyticalGain = AnalyticalSolutions.rlcBandpassGain(R, L, C, F0);

        double iPeak = measureCurrentPeak(F0, finePointsPerCycle, TOTAL_CYCLES, MEASUREMENT_CYCLES);
        double measuredGain = (iPeak * R) / AMPLITUDE;

        System.out.printf("RLC bandpass f0=%.1f Hz: Q=%.2f, analytical gain=%.4f, measured=%.4f%n",
                F0, AnalyticalSolutions.rlcQualityFactor(R, L, C), analyticalGain, measuredGain);

        // Analytical gain at resonance must equal 1.0 by definition
        assertEquals(1.0, analyticalGain, 1e-10,
                "Analytical bandpass gain at resonance must equal 1.0");

        // Simulated gain should be close to 1.0 within 5% tolerance
        assertEquals(analyticalGain, measuredGain, 0.05,
                String.format("RLC gain at resonance f0=%.1f Hz: expected %.4f, got %.4f",
                        F0, analyticalGain, measuredGain));
    }

    // ========================================================================
    // Test 2: Gain drops significantly at off-resonance frequencies
    // ========================================================================

    /**
     * At frequencies far from resonance (f0/10 and 10*f0), the series RLC impedance
     * is dominated by the reactive terms, so the normalized gain must be well below 0.5.
     *
     * <p>At f0/10: |Z| >> R because the capacitive reactance dominates.
     * At 10*f0:  |Z| >> R because the inductive reactance dominates.
     * Both produce gain well below the -3 dB level.
     */
    @Test
    void gainDropsAtOffResonance() {
        double freqLow  = F0 / 10.0;
        double freqHigh = F0 * 10.0;

        double iPeakLow  = measureCurrentPeak(freqLow);
        double iPeakHigh = measureCurrentPeak(freqHigh);

        double gainLow  = (iPeakLow  * R) / AMPLITUDE;
        double gainHigh = (iPeakHigh * R) / AMPLITUDE;

        double analyticalLow  = AnalyticalSolutions.rlcBandpassGain(R, L, C, freqLow);
        double analyticalHigh = AnalyticalSolutions.rlcBandpassGain(R, L, C, freqHigh);

        System.out.printf("RLC bandpass f=f0/10=%.1f Hz: analytical=%.4f, measured=%.4f%n",
                freqLow, analyticalLow, gainLow);
        System.out.printf("RLC bandpass f=10*f0=%.1f Hz: analytical=%.4f, measured=%.4f%n",
                freqHigh, analyticalHigh, gainHigh);

        assertTrue(gainLow < 0.5,
                String.format("RLC gain at f0/10=%.1f Hz must be < 0.5, got %.4f",
                        freqLow, gainLow));
        assertTrue(gainHigh < 0.5,
                String.format("RLC gain at 10*f0=%.1f Hz must be < 0.5, got %.4f",
                        freqHigh, gainHigh));
    }

    // ========================================================================
    // Test 3: Frequency sweep matches analytical formula within tolerance
    // ========================================================================

    /**
     * Sweeps five frequencies around resonance and verifies the simulated gain
     * matches the analytical bandpass gain formula within {@link #TOLERANCE} (10%).
     *
     * <p>Test frequencies: f0/10, f0/3, f0, 3*f0, 10*f0
     *
     * <p>A 10% tolerance is used to accommodate:
     * <ul>
     *   <li>Backward Euler amplitude damping (grows with frequency);</li>
     *   <li>Slower transient decay at off-resonance frequencies where the
     *       resonant modes are less well-excited.</li>
     * </ul>
     */
    @Test
    void frequencySweepMatchesAnalytical() {
        double[] freqs = {
            F0 / 10.0,
            F0 / 3.0,
            F0,
            F0 * 3.0,
            F0 * 10.0
        };

        for (double freq : freqs) {
            double analyticalGain = AnalyticalSolutions.rlcBandpassGain(R, L, C, freq);

            double iPeak = measureCurrentPeak(freq);
            double measuredGain = (iPeak * R) / AMPLITUDE;

            System.out.printf("RLC sweep f=%.1f Hz: analytical=%.4f, measured=%.4f%n",
                    freq, analyticalGain, measuredGain);

            // Use absolute delta = analyticalGain * TOLERANCE to handle near-zero gains gracefully
            assertEquals(analyticalGain, measuredGain, analyticalGain * TOLERANCE,
                    String.format("RLC frequency sweep at f=%.1f Hz: expected gain %.4f, got %.4f",
                            freq, analyticalGain, measuredGain));
        }
    }

    // ========================================================================
    // Test 4: Approximate symmetry of rolloff around resonance
    // ========================================================================

    /**
     * The series RLC bandpass is symmetric on a logarithmic frequency axis.
     * Therefore the gain at f0/3 and at 3*f0 should be approximately equal.
     *
     * <p>Exact symmetry: rlcBandpassGain(R,L,C,f0/3) == rlcBandpassGain(R,L,C,3*f0)
     * (provable by substituting ω = ω0/3 and 3ω0 and noting |ωL - 1/(ωC)| is
     * symmetric around ω0 in geometric terms).
     *
     * <p>The test allows 30% relative difference in simulated gains to account for
     * residual transients and Backward Euler numerical dissipation which are
     * asymmetric in frequency.
     */
    @Test
    void symmetricRolloff() {
        double freqBelow = F0 / 3.0;
        double freqAbove = F0 * 3.0;

        double analyticalBelow = AnalyticalSolutions.rlcBandpassGain(R, L, C, freqBelow);
        double analyticalAbove = AnalyticalSolutions.rlcBandpassGain(R, L, C, freqAbove);

        double iPeakBelow = measureCurrentPeak(freqBelow);
        double iPeakAbove = measureCurrentPeak(freqAbove);

        double measuredGainBelow = (iPeakBelow * R) / AMPLITUDE;
        double measuredGainAbove = (iPeakAbove * R) / AMPLITUDE;

        System.out.printf("RLC rolloff f=f0/3=%.1f Hz: analytical=%.4f, measured=%.4f%n",
                freqBelow, analyticalBelow, measuredGainBelow);
        System.out.printf("RLC rolloff f=3*f0=%.1f Hz: analytical=%.4f, measured=%.4f%n",
                freqAbove, analyticalAbove, measuredGainAbove);

        // Verify analytical symmetry (exact)
        assertEquals(analyticalBelow, analyticalAbove, 1e-10,
                String.format("Analytical gain must be symmetric: f0/3 gave %.6f, 3*f0 gave %.6f",
                        analyticalBelow, analyticalAbove));

        // Verify simulated gains are within 30% of each other
        double higherGain = Math.max(measuredGainBelow, measuredGainAbove);
        double lowerGain  = Math.min(measuredGainBelow, measuredGainAbove);
        double relDiff = (higherGain - lowerGain) / higherGain;

        assertTrue(relDiff < 0.30,
                String.format("Simulated gains at f0/3 (%.4f) and 3*f0 (%.4f) differ by %.1f%% "
                        + "(expected < 30%% for approximate symmetry)",
                        measuredGainBelow, measuredGainAbove, relDiff * 100.0));
    }
}

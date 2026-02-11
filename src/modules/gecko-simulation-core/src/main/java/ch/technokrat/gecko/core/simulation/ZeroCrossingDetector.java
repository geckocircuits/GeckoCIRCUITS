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
package ch.technokrat.gecko.core.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Detects zero-crossing events in simulation signals and locates the exact crossing time
 * using bisection. This is critical for power electronics simulation accuracy, where switch
 * events (MOSFET/IGBT gate transitions, diode conduction changes, thyristor firing) create
 * discontinuities that must be resolved within the time step.
 *
 * <p>In the main simulation loop ({@code SimulationsKern}), the detector would be called
 * after each time step to check gate/control signals for threshold crossings. When a
 * crossing is detected, the simulation steps back and splits the time step at the crossing
 * point, then re-stamps the MNA matrix with the new switch states.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * ZeroCrossingDetector detector = new ZeroCrossingDetector(1e-12, 50);
 *
 * // After each simulation step, check signals for crossings
 * List<ZeroCrossingEvent> events = detector.detectCrossings(
 *     signalsPrev, signalsCurr, 0.5, tPrev, tCurr);
 *
 * if (!events.isEmpty()) {
 *     // Split time step at the earliest crossing
 *     double tCross = events.get(0).getCrossingTime();
 *     // Re-stamp MNA matrix with new switch states at tCross
 * }
 * }</pre>
 */
public final class ZeroCrossingDetector {

    /** Default time tolerance for bisection convergence (1 ps) */
    public static final double DEFAULT_TOLERANCE = 1e-12;

    /** Default maximum bisection iterations */
    public static final int DEFAULT_MAX_ITERATIONS = 50;

    private final double tolerance;
    private final int maxIterations;

    /**
     * Creates a detector with default tolerance (1e-12) and max iterations (50).
     */
    public ZeroCrossingDetector() {
        this(DEFAULT_TOLERANCE, DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Creates a detector with configurable parameters.
     *
     * @param tolerance     time tolerance for bisection convergence (seconds)
     * @param maxIterations maximum number of bisection iterations
     * @throws IllegalArgumentException if tolerance is not positive or maxIterations < 1
     */
    public ZeroCrossingDetector(double tolerance, int maxIterations) {
        if (tolerance <= 0 || !Double.isFinite(tolerance)) {
            throw new IllegalArgumentException("Tolerance must be a finite positive value, got: " + tolerance);
        }
        if (maxIterations < 1) {
            throw new IllegalArgumentException("Max iterations must be >= 1, got: " + maxIterations);
        }
        this.tolerance = tolerance;
        this.maxIterations = maxIterations;
    }

    /**
     * Detects threshold crossings across all signal channels between two time steps.
     * Returns events sorted by crossing time (earliest first).
     *
     * @param signalsPrev signal values at the previous time step
     * @param signalsCurr signal values at the current time step
     * @param threshold   the crossing threshold (e.g., 0.5 for gate signals)
     * @param tPrev       time at the previous step
     * @param tCurr       time at the current step
     * @return list of crossing events sorted by time, empty if no crossings detected
     * @throws IllegalArgumentException if arrays are null, have different lengths, or times are invalid
     */
    public List<ZeroCrossingEvent> detectCrossings(double[] signalsPrev, double[] signalsCurr,
                                                    double threshold, double tPrev, double tCurr) {
        validateInputs(signalsPrev, signalsCurr, tPrev, tCurr);

        List<ZeroCrossingEvent> events = new ArrayList<>();

        for (int i = 0; i < signalsPrev.length; i++) {
            double prev = signalsPrev[i] - threshold;
            double curr = signalsCurr[i] - threshold;

            // Check for sign change (crossing)
            if (prev * curr < 0) {
                // Crossing detected - determine direction
                ZeroCrossingEvent.Direction direction = curr > 0
                        ? ZeroCrossingEvent.Direction.RISING
                        : ZeroCrossingEvent.Direction.FALLING;

                // Bisect to find exact crossing time
                double crossingTime = bisectCrossingTime(prev, curr, tPrev, tCurr);

                events.add(new ZeroCrossingEvent(i, crossingTime, direction,
                        signalsPrev[i], signalsCurr[i]));
            }
        }

        // Sort by crossing time so the simulation processes the earliest event first
        events.sort(Comparator.comparingDouble(ZeroCrossingEvent::getCrossingTime));
        return Collections.unmodifiableList(events);
    }

    /**
     * Detects a single threshold crossing for one signal.
     *
     * @param valuePrev previous signal value
     * @param valueCurr current signal value
     * @param threshold the crossing threshold
     * @return true if a crossing occurred between the two values
     */
    public boolean hasCrossing(double valuePrev, double valueCurr, double threshold) {
        double prev = valuePrev - threshold;
        double curr = valueCurr - threshold;
        return prev * curr < 0;
    }

    /**
     * Uses bisection to locate the exact crossing time within a time step.
     * Assumes a linear interpolation of the signal between the two time points.
     * For linear interpolation, this computes the exact root in one step,
     * but the iterative bisection handles any numerical edge cases.
     *
     * @param shiftedPrev signal value minus threshold at tPrev
     * @param shiftedCurr signal value minus threshold at tCurr
     * @param tPrev       time at the previous step
     * @param tCurr       time at the current step
     * @return estimated crossing time
     */
    double bisectCrossingTime(double shiftedPrev, double shiftedCurr, double tPrev, double tCurr) {
        double tLow = tPrev;
        double tHigh = tCurr;
        double fLow = shiftedPrev;
        double fHigh = shiftedCurr;

        for (int iter = 0; iter < maxIterations; iter++) {
            if ((tHigh - tLow) < tolerance) {
                break;
            }

            // Linear interpolation for the midpoint estimate (secant method first, then bisect)
            double tMid;
            double denom = fHigh - fLow;
            if (Math.abs(denom) > 1e-30) {
                // Secant/linear interpolation: t where f(t) = 0
                tMid = tLow - fLow * (tHigh - tLow) / denom;
                // Clamp to interval to ensure convergence
                tMid = Math.max(tLow, Math.min(tHigh, tMid));
            } else {
                // Degenerate case: simple midpoint
                tMid = (tLow + tHigh) / 2.0;
            }

            // Evaluate signal at midpoint via linear interpolation
            double alpha = (tMid - tPrev) / (tCurr - tPrev);
            double fMid = shiftedPrev + alpha * (shiftedCurr - shiftedPrev);

            if (Math.abs(fMid) < 1e-30) {
                return tMid; // Exact crossing found
            }

            // Standard bisection: narrow the bracket
            if (fLow * fMid < 0) {
                tHigh = tMid;
                fHigh = fMid;
            } else {
                tLow = tMid;
                fLow = fMid;
            }
        }

        return (tLow + tHigh) / 2.0;
    }

    /**
     * Gets the configured tolerance.
     *
     * @return tolerance in seconds
     */
    public double getTolerance() {
        return tolerance;
    }

    /**
     * Gets the configured maximum iterations.
     *
     * @return max iterations
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    private static void validateInputs(double[] signalsPrev, double[] signalsCurr,
                                        double tPrev, double tCurr) {
        if (signalsPrev == null || signalsCurr == null) {
            throw new IllegalArgumentException("Signal arrays must not be null");
        }
        if (signalsPrev.length != signalsCurr.length) {
            throw new IllegalArgumentException(
                    "Signal arrays must have the same length: " + signalsPrev.length + " vs " + signalsCurr.length);
        }
        if (tCurr <= tPrev) {
            throw new IllegalArgumentException("tCurr must be greater than tPrev: " + tPrev + " >= " + tCurr);
        }
        if (!Double.isFinite(tPrev) || !Double.isFinite(tCurr)) {
            throw new IllegalArgumentException("Time values must be finite");
        }
    }
}

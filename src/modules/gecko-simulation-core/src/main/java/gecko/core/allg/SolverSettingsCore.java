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
package gecko.core.allg;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * GUI-free solver configuration for headless simulation.
 * This class provides simulation parameters without any GUI dependencies.
 *
 * Used by HeadlessSimulationEngine and SimulationsKern when running
 * in headless mode (REST API, CLI, batch processing).
 */
public final class SolverSettingsCore {

    // Solver type (backward-euler, trapezoidal, gear-shichman)
    private SolverType solverType;

    // Pre-calculation settings
    private double preCalculationTime;
    private double preCalculationStepWidth;

    // Main simulation settings
    private double stepWidth;          // dt
    private double simulationDuration; // total simulation time
    private double pauseTime;          // optional pause time (-1 means no pause)
    private double controlSamplePeriod; // CONTROL sample period Ts in seconds; 0 runs CONTROL every step
    private Map<String, Double> controlSubsystemPeriods = Map.of(); // sample period per CONTROL subsystem sheet path
    private boolean zeroCrossing;      // locate diode-family switching inside a step; off by default
    private boolean dcOperatingPoint;  // seed the first step from a solved DC operating point; off by default
    private PeriodicSteadyStateSettings periodicSteadyState; // periodic steady-state search; null means off
    private AdaptiveStepSettings adaptiveStep; // adaptive electrical sub-step inside the fixed grid; null means off
    private SparseSolverSettings sparseSolver; // sparse LU selection, where null means the dense LU

    // State tracking
    private boolean inPreCalculationMode;

    /**
     * Creates solver settings with default values.
     * Default: Backward-Euler solver, 0.1us step width, 20ms duration
     */
    public SolverSettingsCore() {
        this.solverType = SolverType.SOLVER_BE;
        this.preCalculationTime = -100e-3;
        this.preCalculationStepWidth = 100e-9;
        this.stepWidth = 0.1e-6;
        this.simulationDuration = 20e-3;
        this.pauseTime = -1.0;
        this.inPreCalculationMode = false;
    }

    /**
     * Creates solver settings with specified parameters.
     *
     * @param solverType integration method to use
     * @param stepWidth simulation time step in seconds
     * @param simulationDuration total simulation time in seconds
     */
    public SolverSettingsCore(SolverType solverType, double stepWidth, double simulationDuration) {
        this();
        this.solverType = solverType;
        this.stepWidth = stepWidth;
        this.simulationDuration = simulationDuration;
    }

    // Builder-style fluent API

    public SolverSettingsCore withSolverType(SolverType type) {
        this.solverType = type;
        return this;
    }

    public SolverSettingsCore withStepWidth(double dt) {
        this.stepWidth = dt;
        return this;
    }

    public SolverSettingsCore withSimulationDuration(double duration) {
        this.simulationDuration = duration;
        return this;
    }

    public SolverSettingsCore withPreCalculation(double time, double dt) {
        this.preCalculationTime = time;
        this.preCalculationStepWidth = dt;
        return this;
    }

    public SolverSettingsCore withPauseTime(double pause) {
        this.pauseTime = pause;
        return this;
    }

    /**
     * Runs CONTROL at the first electrical step at or after each multiple of the period,
     * holding its outputs between samples. Zero keeps the default of one CONTROL run per step.
     * Validation happens when a simulation starts.
     */
    public SolverSettingsCore withControlSamplePeriod(double period) {
        this.controlSamplePeriod = period;
        return this;
    }

    /**
     * Gives each named CONTROL subsystem its own sample period. A key is a sheet path, joined by "/".
     * Blocks outside every named subsystem keep the global period. An empty map keeps the single clock.
     * The settings keep a detached, sorted, unmodifiable copy. Validation happens when a simulation starts.
     *
     * @param periods sample period in seconds by sheet path, or null for none
     */
    public SolverSettingsCore withControlSubsystemPeriods(Map<String, Double> periods) {
        this.controlSubsystemPeriods = periods == null || periods.isEmpty() ? Map.of()
                : Collections.unmodifiableMap(new TreeMap<>(periods));
        return this;
    }

    /**
     * Locates diode-family switching events inside each electrical step. Off by default.
     * Gear-Shichman is rejected when a simulation starts while this is on.
     */
    public SolverSettingsCore withZeroCrossing(boolean enabled) {
        this.zeroCrossing = enabled;
        return this;
    }

    /**
     * Seeds the first electrical step from the circuit's DC operating point. Off by default.
     * Capacitors open, inductors short, sources take their t=0 value, switches keep their initial gate state.
     */
    public SolverSettingsCore withDcOperatingPoint(boolean enabled) {
        this.dcOperatingPoint = enabled;
        return this;
    }

    /**
     * Searches for the periodic steady state before the main phase. Null, the default, turns it off.
     * The search seeds itself from the DC operating point without changing {@link #isDcOperatingPoint()}.
     * Validation happens when a simulation starts.
     */
    public SolverSettingsCore withPeriodicSteadyState(PeriodicSteadyStateSettings settings) {
        this.periodicSteadyState = settings;
        return this;
    }

    /**
     * Adapts the electrical sub-step between the fixed grid points from a local-error estimate. Null, the default,
     * turns it off. A non-null value also turns on zero-crossing location. Validation happens when a simulation starts.
     */
    public SolverSettingsCore withAdaptiveStep(AdaptiveStepSettings settings) {
        this.adaptiveStep = settings;
        return this;
    }

    /**
     * Chooses the sparse LU for the electrical system. Null, the default, keeps the dense LU with bit-identical results.
     * See {@link SparseSolverSettings} for the automatic threshold and the explicit overrides.
     */
    public SolverSettingsCore withSparseSolver(SparseSolverSettings settings) {
        this.sparseSolver = settings;
        return this;
    }

    // Getters

    /** @return the sparse-solver settings, or null when the dense LU is used */
    public SparseSolverSettings getSparseSolver() {
        return sparseSolver;
    }

    /** @return the adaptive-step settings, or null when the feature is off */
    public AdaptiveStepSettings getAdaptiveStep() {
        return adaptiveStep;
    }

    /** @return the periodic steady-state settings, or null when the search is off */
    public PeriodicSteadyStateSettings getPeriodicSteadyState() {
        return periodicSteadyState;
    }

    public boolean isDcOperatingPoint() {
        return dcOperatingPoint;
    }

    public boolean isZeroCrossing() {
        return zeroCrossing;
    }

    public double getControlSamplePeriod() {
        return controlSamplePeriod;
    }

    /** @return the unmodifiable sample period by CONTROL subsystem sheet path, empty for the single clock */
    public Map<String, Double> getControlSubsystemPeriods() {
        return controlSubsystemPeriods;
    }

    public SolverType getSolverType() {
        return solverType;
    }

    public double getPreCalculationTime() {
        return preCalculationTime;
    }

    public double getPreCalculationStepWidth() {
        return preCalculationStepWidth;
    }

    public double getStepWidth() {
        return stepWidth;
    }

    public double getSimulationDuration() {
        return simulationDuration;
    }

    public double getPauseTime() {
        return pauseTime;
    }

    public boolean isInPreCalculationMode() {
        return inPreCalculationMode;
    }

    // Setters

    public void setSolverType(SolverType solverType) {
        this.solverType = solverType;
    }

    public void setStepWidth(double stepWidth) {
        this.stepWidth = stepWidth;
    }

    public void setSimulationDuration(double simulationDuration) {
        this.simulationDuration = simulationDuration;
    }

    public void setInPreCalculationMode(boolean inPreCalculationMode) {
        this.inPreCalculationMode = inPreCalculationMode;
    }

    /**
     * Creates a copy of these settings.
     * @return a new SolverSettingsCore with the same values
     */
    public SolverSettingsCore copy() {
        SolverSettingsCore copy = new SolverSettingsCore();
        copy.solverType = this.solverType;
        copy.preCalculationTime = this.preCalculationTime;
        copy.preCalculationStepWidth = this.preCalculationStepWidth;
        copy.stepWidth = this.stepWidth;
        copy.simulationDuration = this.simulationDuration;
        copy.pauseTime = this.pauseTime;
        copy.controlSamplePeriod = this.controlSamplePeriod;
        copy.controlSubsystemPeriods = this.controlSubsystemPeriods;
        copy.zeroCrossing = this.zeroCrossing;
        copy.dcOperatingPoint = this.dcOperatingPoint;
        copy.periodicSteadyState = this.periodicSteadyState;
        copy.adaptiveStep = this.adaptiveStep;
        copy.sparseSolver = this.sparseSolver;
        copy.inPreCalculationMode = this.inPreCalculationMode;
        return copy;
    }

    @Override
    public String toString() {
        return String.format("SolverSettingsCore[solver=%s, dt=%.2e, duration=%.2e]",
                solverType, stepWidth, simulationDuration);
    }
}

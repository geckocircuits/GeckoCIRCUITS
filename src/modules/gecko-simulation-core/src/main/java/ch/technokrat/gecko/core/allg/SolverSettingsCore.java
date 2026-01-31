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
package ch.technokrat.gecko.core.allg;

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

    // Getters

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
        copy.inPreCalculationMode = this.inPreCalculationMode;
        return copy;
    }

    @Override
    public String toString() {
        return String.format("SolverSettingsCore[solver=%s, dt=%.2e, duration=%.2e]",
                solverType, stepWidth, simulationDuration);
    }
}

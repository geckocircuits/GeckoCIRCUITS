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
package gecko.core.simulation;

/**
 * Detailed progress information for a running simulation.
 * Provides comprehensive metrics including time, steps, and estimated completion.
 */
public class SimulationProgress {

    private final double overallProgress;       // 0.0 to 1.0
    private final double preCalcProgress;       // 0.0 to 1.0 (pre-calculation phase)
    private final double mainSimProgress;       // 0.0 to 1.0 (main simulation phase)
    private final int currentStep;
    private final int totalSteps;
    private final double currentTime;
    private final double endTime;
    private final Long estimatedRemainingMs;    // ETA in milliseconds
    private final HeadlessSimulationEngine.EngineState state;

    /**
     * Creates a new simulation progress snapshot.
     *
     * @param overallProgress overall completion (0.0 to 1.0)
     * @param preCalcProgress pre-calculation progress (0.0 to 1.0)
     * @param mainSimProgress main simulation progress (0.0 to 1.0)
     * @param currentStep current time step number
     * @param totalSteps total expected steps
     * @param currentTime current simulation time [s]
     * @param endTime simulation end time [s]
     * @param estimatedRemainingMs estimated remaining time [ms], null if unknown
     * @param state current engine state
     */
    public SimulationProgress(double overallProgress, double preCalcProgress, double mainSimProgress,
                             int currentStep, int totalSteps, double currentTime, double endTime,
                             Long estimatedRemainingMs, HeadlessSimulationEngine.EngineState state) {
        this.overallProgress = overallProgress;
        this.preCalcProgress = preCalcProgress;
        this.mainSimProgress = mainSimProgress;
        this.currentStep = currentStep;
        this.totalSteps = totalSteps;
        this.currentTime = currentTime;
        this.endTime = endTime;
        this.estimatedRemainingMs = estimatedRemainingMs;
        this.state = state;
    }

    // Getters

    public double getOverallProgress() {
        return overallProgress;
    }

    public double getPreCalcProgress() {
        return preCalcProgress;
    }

    public double getMainSimProgress() {
        return mainSimProgress;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public double getCurrentTime() {
        return currentTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public Long getEstimatedRemainingMs() {
        return estimatedRemainingMs;
    }

    public HeadlessSimulationEngine.EngineState getState() {
        return state;
    }

    /**
     * Gets progress as a percentage string.
     *
     * @return formatted progress (e.g., "45.2%")
     */
    public String getProgressPercentage() {
        return String.format("%.1f%%", overallProgress * 100.0);
    }

    @Override
    public String toString() {
        return String.format("SimulationProgress{progress=%.1f%%, step=%d/%d, time=%.6fs/%.6fs, state=%s, eta=%dms}",
            overallProgress * 100.0, currentStep, totalSteps, currentTime, endTime, state,
            estimatedRemainingMs != null ? estimatedRemainingMs : -1);
    }
}

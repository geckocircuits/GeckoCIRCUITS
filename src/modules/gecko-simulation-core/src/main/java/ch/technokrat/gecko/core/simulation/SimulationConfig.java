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

import ch.technokrat.gecko.core.allg.SolverSettingsCore;
import ch.technokrat.gecko.core.allg.SolverType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a headless simulation run.
 * Encapsulates all parameters needed to execute a simulation without GUI.
 */
public final class SimulationConfig {

    private final SolverSettingsCore solverSettings;
    private final String circuitFilePath;
    private final Map<String, Double> parameterOverrides;
    private final boolean enableDataLogging;
    private final int dataLoggingInterval;

    private SimulationConfig(Builder builder) {
        this.solverSettings = builder.solverSettings.copy();
        this.circuitFilePath = builder.circuitFilePath;
        this.parameterOverrides = Collections.unmodifiableMap(new HashMap<>(builder.parameterOverrides));
        this.enableDataLogging = builder.enableDataLogging;
        this.dataLoggingInterval = builder.dataLoggingInterval;
    }

    /**
     * Gets the solver settings for this simulation.
     *
     * @return solver settings
     */
    public SolverSettingsCore getSolverSettings() {
        return solverSettings.copy();
    }

    /**
     * Gets the path to the circuit file (.ipes).
     *
     * @return circuit file path, or null if circuit is provided programmatically
     */
    public String getCircuitFilePath() {
        return circuitFilePath;
    }

    /**
     * Gets parameter overrides for the simulation.
     * Keys are parameter names, values are the override values.
     *
     * @return unmodifiable map of parameter overrides
     */
    public Map<String, Double> getParameterOverrides() {
        return parameterOverrides;
    }

    /**
     * Checks if data logging is enabled.
     *
     * @return true if data should be logged during simulation
     */
    public boolean isDataLoggingEnabled() {
        return enableDataLogging;
    }

    /**
     * Gets the data logging interval (every Nth time step).
     *
     * @return logging interval
     */
    public int getDataLoggingInterval() {
        return dataLoggingInterval;
    }

    /**
     * Creates a new builder for SimulationConfig.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for SimulationConfig.
     */
    public static class Builder {
        private SolverSettingsCore solverSettings = new SolverSettingsCore();
        private String circuitFilePath;
        private Map<String, Double> parameterOverrides = new HashMap<>();
        private boolean enableDataLogging = true;
        private int dataLoggingInterval = 1;

        private Builder() {
        }

        /**
         * Sets the solver settings.
         *
         * @param solverSettings the solver configuration
         * @return this builder
         */
        public Builder solverSettings(SolverSettingsCore solverSettings) {
            if (solverSettings == null) {
                throw new IllegalArgumentException("solverSettings cannot be null");
            }
            this.solverSettings = solverSettings.copy();
            return this;
        }

        /**
         * Sets the solver type.
         *
         * @param solverType the integration method
         * @return this builder
         */
        public Builder solverType(SolverType solverType) {
            this.solverSettings.setSolverType(solverType);
            return this;
        }

        /**
         * Sets the simulation step width (dt).
         *
         * @param dt time step in seconds
         * @return this builder
         */
        public Builder stepWidth(double dt) {
            this.solverSettings.setStepWidth(dt);
            return this;
        }

        /**
         * Sets the total simulation duration.
         *
         * @param duration simulation time in seconds
         * @return this builder
         */
        public Builder simulationDuration(double duration) {
            this.solverSettings.setSimulationDuration(duration);
            return this;
        }

        /**
         * Sets the circuit file path.
         *
         * @param filePath path to .ipes circuit file
         * @return this builder
         */
        public Builder circuitFile(String filePath) {
            this.circuitFilePath = filePath;
            return this;
        }

        /**
         * Adds a parameter override.
         *
         * @param parameterName the parameter name
         * @param value the override value
         * @return this builder
         */
        public Builder withParameter(String parameterName, double value) {
            this.parameterOverrides.put(parameterName, value);
            return this;
        }

        /**
         * Sets all parameter overrides.
         *
         * @param parameters map of parameter names to values
         * @return this builder
         */
        public Builder withParameters(Map<String, Double> parameters) {
            this.parameterOverrides.putAll(parameters);
            return this;
        }

        /**
         * Enables or disables data logging.
         *
         * @param enable true to enable data logging
         * @return this builder
         */
        public Builder enableDataLogging(boolean enable) {
            this.enableDataLogging = enable;
            return this;
        }

        /**
         * Sets the data logging interval.
         *
         * @param interval log every Nth time step
         * @return this builder
         */
        public Builder dataLoggingInterval(int interval) {
            this.dataLoggingInterval = Math.max(1, interval);
            return this;
        }

        /**
         * Builds the SimulationConfig instance.
         *
         * @return new SimulationConfig
         */
        public SimulationConfig build() {
            return new SimulationConfig(this);
        }
    }

    @Override
    public String toString() {
        return String.format("SimulationConfig[circuit=%s, solver=%s, dt=%.2e, duration=%.2e]",
                circuitFilePath, solverSettings.getSolverType(),
                solverSettings.getStepWidth(), solverSettings.getSimulationDuration());
    }
}

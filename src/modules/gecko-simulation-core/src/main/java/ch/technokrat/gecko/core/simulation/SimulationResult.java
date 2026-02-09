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

import ch.technokrat.gecko.core.datacontainer.DataContainerGlobal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Results from a headless simulation run.
 * Contains simulation data, timing information, and any errors that occurred.
 */
public final class SimulationResult {

    /**
     * Status of the simulation result.
     */
    public enum Status {
        /** Simulation completed successfully */
        SUCCESS,
        /** Simulation failed due to an error */
        FAILED,
        /** Simulation was cancelled */
        CANCELLED,
        /** Simulation timed out */
        TIMEOUT
    }

    private final Status status;
    private final DataContainerGlobal dataContainer;
    private final long executionTimeMs;
    private final int totalTimeSteps;
    private final double simulatedTime;
    private final String errorMessage;
    private final List<String> warnings;
    private final Map<String, Object> metadata;

    private SimulationResult(Builder builder) {
        this.status = builder.status;
        this.dataContainer = builder.dataContainer;
        this.executionTimeMs = builder.executionTimeMs;
        this.totalTimeSteps = builder.totalTimeSteps;
        this.simulatedTime = builder.simulatedTime;
        this.errorMessage = builder.errorMessage;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(builder.warnings));
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
    }

    /**
     * Gets the simulation status.
     *
     * @return status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Checks if the simulation was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Gets the data container with simulation results.
     *
     * @return data container, or null if simulation failed
     */
    public DataContainerGlobal getDataContainer() {
        return dataContainer;
    }

    /**
     * Gets the wall-clock execution time in milliseconds.
     *
     * @return execution time in ms
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    /**
     * Gets the total number of time steps simulated.
     *
     * @return time step count
     */
    public int getTotalTimeSteps() {
        return totalTimeSteps;
    }

    /**
     * Gets the total simulated time in seconds.
     *
     * @return simulated time
     */
    public double getSimulatedTime() {
        return simulatedTime;
    }

    /**
     * Gets the error message if simulation failed.
     *
     * @return error message, or null if successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets any warnings generated during simulation.
     *
     * @return list of warnings
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Gets additional metadata about the simulation.
     *
     * @return metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Gets the signal names from the data container.
     *
     * @return array of signal names, or empty array if no data
     */
    public String[] getSignalNames() {
        if (dataContainer == null || !dataContainer.isInitialized()) {
            return new String[0];
        }
        int rowCount = dataContainer.getRowLength();
        String[] names = new String[rowCount];
        for (int i = 0; i < rowCount; i++) {
            names[i] = dataContainer.getSignalName(i);
        }
        return names;
    }

    /**
     * Gets a specific signal's data array.
     *
     * @param signalIndex the signal row index
     * @return array of values, or null if invalid index
     */
    public float[] getSignalData(int signalIndex) {
        if (dataContainer == null || !dataContainer.isInitialized()) {
            return null;
        }
        if (signalIndex < 0 || signalIndex >= dataContainer.getRowLength()) {
            return null;
        }

        int maxIndex = dataContainer.getMaximumTimeIndex(signalIndex);
        if (maxIndex < 0) {
            return new float[0];
        }

        float[] data = new float[maxIndex + 1];
        for (int i = 0; i <= maxIndex; i++) {
            data[i] = dataContainer.getValue(signalIndex, i);
        }
        return data;
    }

    /**
     * Gets the time array.
     *
     * @return array of time values, or empty array if no data
     */
    public double[] getTimeArray() {
        if (dataContainer == null || !dataContainer.isInitialized()) {
            return new double[0];
        }
        if (dataContainer.getRowLength() == 0) {
            return new double[0];
        }

        int maxIndex = dataContainer.getMaximumTimeIndex(0);
        if (maxIndex < 0) {
            return new double[0];
        }

        double[] times = new double[maxIndex + 1];
        for (int i = 0; i <= maxIndex; i++) {
            times[i] = dataContainer.getTimeValue(i, 0);
        }
        return times;
    }

    /**
     * Creates a new builder for SimulationResult.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a failed result with an error message.
     *
     * @param errorMessage the error message
     * @return failed result
     */
    public static SimulationResult failed(String errorMessage) {
        return builder()
                .status(Status.FAILED)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Creates a cancelled result.
     *
     * @return cancelled result
     */
    public static SimulationResult cancelled() {
        return builder()
                .status(Status.CANCELLED)
                .build();
    }

    /**
     * Builder class for SimulationResult.
     */
    public static class Builder {
        private Status status = Status.SUCCESS;
        private DataContainerGlobal dataContainer;
        private long executionTimeMs;
        private int totalTimeSteps;
        private double simulatedTime;
        private String errorMessage;
        private List<String> warnings = new ArrayList<>();
        private Map<String, Object> metadata = new HashMap<>();

        private Builder() {
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder dataContainer(DataContainerGlobal dataContainer) {
            this.dataContainer = dataContainer;
            return this;
        }

        public Builder executionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public Builder totalTimeSteps(int totalTimeSteps) {
            this.totalTimeSteps = totalTimeSteps;
            return this;
        }

        public Builder simulatedTime(double simulatedTime) {
            this.simulatedTime = simulatedTime;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder addWarning(String warning) {
            this.warnings.add(warning);
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = new ArrayList<>(warnings);
            return this;
        }

        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        public SimulationResult build() {
            return new SimulationResult(this);
        }
    }

    @Override
    public String toString() {
        return String.format("SimulationResult[status=%s, time=%.2es, steps=%d, wallClock=%dms]",
                status, simulatedTime, totalTimeSteps, executionTimeMs);
    }
}

/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.api;

import java.io.FileNotFoundException;

/**
 * Unified interface for accessing GeckoCIRCUITS simulator.
 *
 * This interface provides a common contract for different access modes:
 * - Standalone (direct in-process access)
 * - Remote RMI (network-based access)
 * - Memory-Mapped File (MMF) (shared memory access)
 * - Pipe-based (inter-process communication)
 *
 * The interface focuses on the core simulation operations, abstracting
 * away the underlying transport mechanism.
 *
 * @see ch.technokrat.gecko.GeckoRemoteInterface for the full RMI interface
 * @author GeckoCIRCUITS Team
 */
public interface ISimulatorAccess {

    /**
     * Access mode types for simulator connection.
     */
    enum AccessMode {
        /** Direct in-process access (standalone mode) */
        STANDALONE,
        /** Remote Method Invocation (network) */
        RMI,
        /** Memory-Mapped File (shared memory) */
        MMF,
        /** Named pipe (inter-process) */
        PIPE
    }

    //=== Connection Management ===

    /**
     * Connects to the simulator.
     *
     * @return session ID for this connection
     * @throws SimulatorAccessException if connection fails
     */
    long connect() throws SimulatorAccessException;

    /**
     * Disconnects from the simulator.
     *
     * @param sessionId the session to disconnect
     * @throws SimulatorAccessException if disconnection fails
     */
    void disconnect(long sessionId) throws SimulatorAccessException;

    /**
     * Checks if the simulator is available for new connections.
     *
     * @return true if available, false otherwise
     * @throws SimulatorAccessException if check fails
     */
    boolean isAvailable() throws SimulatorAccessException;

    /**
     * Gets the current access mode.
     *
     * @return the access mode type
     */
    AccessMode getAccessMode();

    //=== Model Loading/Saving ===

    /**
     * Opens a circuit model file.
     *
     * @param fileName path to the model file
     * @throws FileNotFoundException if file does not exist
     * @throws SimulatorAccessException if loading fails
     */
    void openFile(String fileName) throws FileNotFoundException, SimulatorAccessException;

    /**
     * Saves the current model to a file.
     *
     * @param fileName path to save to
     * @throws SimulatorAccessException if saving fails
     */
    void saveFileAs(String fileName) throws SimulatorAccessException;

    //=== Simulation Control ===

    /**
     * Initializes the simulation with default parameters.
     *
     * @throws SimulatorAccessException if initialization fails
     */
    void initSimulation() throws SimulatorAccessException;

    /**
     * Initializes the simulation with specified parameters.
     *
     * @param deltaT time step size
     * @param endTime simulation end time
     * @throws SimulatorAccessException if initialization fails
     */
    void initSimulation(double deltaT, double endTime) throws SimulatorAccessException;

    /**
     * Runs the full simulation.
     *
     * @throws SimulatorAccessException if simulation fails
     */
    void runSimulation() throws SimulatorAccessException;

    /**
     * Advances simulation by one time step.
     *
     * @throws SimulatorAccessException if step fails
     */
    void simulateStep() throws SimulatorAccessException;

    /**
     * Advances simulation by specified number of steps.
     *
     * @param steps number of steps to advance
     * @throws SimulatorAccessException if simulation fails
     */
    void simulateSteps(int steps) throws SimulatorAccessException;

    /**
     * Advances simulation by specified time duration.
     *
     * @param time duration to simulate
     * @throws SimulatorAccessException if simulation fails
     */
    void simulateTime(double time) throws SimulatorAccessException;

    /**
     * Ends the current simulation.
     *
     * @throws SimulatorAccessException if ending fails
     */
    void endSimulation() throws SimulatorAccessException;

    /**
     * Gets the current simulation time.
     *
     * @return current simulation time in seconds
     * @throws SimulatorAccessException if retrieval fails
     */
    double getSimulationTime() throws SimulatorAccessException;

    //=== Parameter Access ===

    /**
     * Sets a component parameter value.
     *
     * @param elementName name of the component
     * @param parameterName name of the parameter
     * @param value new parameter value
     * @throws SimulatorAccessException if setting fails
     */
    void setParameter(String elementName, String parameterName, double value)
            throws SimulatorAccessException;

    /**
     * Gets a component parameter value.
     *
     * @param elementName name of the component
     * @param parameterName name of the parameter
     * @return the parameter value
     * @throws SimulatorAccessException if retrieval fails
     */
    double getParameter(String elementName, String parameterName)
            throws SimulatorAccessException;

    /**
     * Gets an output signal value.
     *
     * @param elementName name of the component
     * @param outputName name of the output
     * @return the output value
     * @throws SimulatorAccessException if retrieval fails
     */
    double getOutput(String elementName, String outputName)
            throws SimulatorAccessException;

    //=== Time Parameters ===

    /**
     * Gets the simulation time step.
     *
     * @return time step in seconds
     * @throws SimulatorAccessException if retrieval fails
     */
    double get_dt() throws SimulatorAccessException;

    /**
     * Sets the simulation time step.
     *
     * @param dt time step in seconds
     * @throws SimulatorAccessException if setting fails
     */
    void set_dt(double dt) throws SimulatorAccessException;

    /**
     * Gets the simulation end time.
     *
     * @return end time in seconds
     * @throws SimulatorAccessException if retrieval fails
     */
    double get_Tend() throws SimulatorAccessException;

    /**
     * Sets the simulation end time.
     *
     * @param tEnd end time in seconds
     * @throws SimulatorAccessException if setting fails
     */
    void set_Tend(double tEnd) throws SimulatorAccessException;
}

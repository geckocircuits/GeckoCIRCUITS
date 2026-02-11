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

/**
 * Interface for the circuit simulation engine.
 *
 * Defines the contract for simulation lifecycle management including initialization,
 * execution, pausing, and time control. This interface decouples simulation consumers
 * from the concrete SimulationsKern implementation.
 *
 * <p>Typical usage:
 * <pre>
 * ISimulationEngine engine = ...;
 * engine.setZeiten(0.0, 1.0, 1e-6);
 * engine.initSimulation(...);
 * engine.runSimulation();
 * </pre>
 *
 * @see ch.technokrat.gecko.geckocircuits.circuit.SimulationsKern
 */
public interface ISimulationEngine {

    /**
     * Simulation status enum representing the current state of the simulation.
     */
    enum SimulationStatus {
        /** Simulation has not been initialized */
        NOT_INIT,
        /** Simulation is currently running */
        RUNNING,
        /** Simulation is paused (can be resumed) */
        PAUSED,
        /** Simulation has completed */
        FINISHED
    }

    /**
     * Gets the current simulation status.
     *
     * @return Current simulation status
     */
    SimulationStatus getSimulationStatus();

    /**
     * Gets the current simulation time.
     *
     * @return Current time in seconds
     */
    double getZeitAktuell();

    /**
     * Gets the simulation time step.
     *
     * @return Time step (dt) in seconds
     */
    double getdt();

    /**
     * Gets the simulation end time.
     *
     * @return End time in seconds
     */
    double getTEND();

    /**
     * Gets the simulation start time.
     *
     * @return Start time in seconds
     */
    double getTSTART();

    /**
     * Sets the simulation time parameters.
     *
     * @param tSTART Start time in seconds
     * @param tEND End time in seconds
     * @param dt Time step in seconds
     */
    void setZeiten(double tSTART, double tEND, double dt);

    /**
     * Runs the simulation until the end time or pause condition.
     */
    void runSimulation();

    /**
     * Simulates a single time step.
     *
     * @throws Exception if the simulation end time has been reached
     */
    void simulateOneStep() throws Exception;

    /**
     * Simulates for a specified duration.
     *
     * @param time Duration to simulate in seconds
     * @throws Exception if the duration exceeds remaining simulation time
     */
    void simulateTime(double time) throws Exception;

    /**
     * Pauses the simulation.
     */
    void pauseSimulation();

    /**
     * Ends the simulation and saves state for potential continuation.
     */
    void endSim();

    /**
     * Restores initial conditions from a previous simulation run (CONTINUE mode).
     */
    void setInitialConditionsFromContinue();
}

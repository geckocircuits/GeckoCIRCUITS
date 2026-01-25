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
package ch.technokrat.gecko.geckocircuits.allg;

import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.circuit.SimulationsKern;
import ch.technokrat.gecko.geckocircuits.circuit.SolverSettings;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller class for simulation operations.
 *
 * This class provides a clean facade for simulation control, coordinating
 * between SimulationRunner and UI state listeners. It decouples simulation
 * logic from direct UI manipulation.
 *
 * Extracted from Fenster.java to improve separation of concerns and reduce
 * the size of the main window class.
 *
 * @see SimulationRunner
 * @see SimulationStateListener
 * @see Fenster
 */
public class SimulationController {

    private final SimulationRunner simulationRunner;
    private final SolverSettings solverSettings;
    private final List<SimulationStateListener> listeners = new ArrayList<>();

    /**
     * Creates a new simulation controller.
     *
     * @param fenster The main window
     * @param schematicEntry The circuit editor
     * @param solverSettings The solver settings
     */
    public SimulationController(Fenster fenster, SchematischeEingabe2 schematicEntry, SolverSettings solverSettings) {
        this.simulationRunner = new SimulationRunner(fenster, schematicEntry);
        this.solverSettings = solverSettings;
    }

    /**
     * Adds a listener for simulation state changes.
     */
    public void addSimulationStateListener(SimulationStateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a simulation state listener.
     */
    public void removeSimulationStateListener(SimulationStateListener listener) {
        listeners.remove(listener);
    }

    /**
     * Starts a new simulation with error dialog handling.
     *
     * @return true if simulation started successfully
     */
    public boolean initAndStart() {
        try {
            notifyStarted();
            simulationRunner.startCalculation(true, solverSettings);
            return true;
        } catch (Exception ex) {
            notifyAborted(ex.getMessage());
            showErrorDialog("Error during simulation init", ex);
            return false;
        }
    }

    /**
     * Continues a paused simulation with error dialog handling.
     *
     * @return true if simulation continued successfully
     */
    public boolean continueSimulation() {
        try {
            notifyStarted();
            simulationRunner.continueCalculation(true, solverSettings);
            return true;
        } catch (Exception ex) {
            notifyAborted(ex.getMessage());
            showErrorDialog("Error continuing simulation", ex);
            return false;
        }
    }

    /**
     * Pauses the current simulation.
     */
    public void pauseSimulation() {
        simulationRunner.pauseSimulation();
        notifyPaused();
    }

    /**
     * Ends the current simulation.
     */
    public void endSimulation() {
        if (simulationRunner.simKern != null) {
            simulationRunner.simKern.endSim();
        }
        notifyStatusUpdate("Stopped");
        pauseSimulation();
    }

    /**
     * Gets the simulation kernel for direct access when needed.
     *
     * @return The simulation kernel, or null if no simulation is running
     */
    public SimulationsKern getSimulationKernel() {
        return simulationRunner.simKern;
    }

    /**
     * Gets the underlying simulation runner for backward compatibility.
     *
     * @return The simulation runner
     */
    public SimulationRunner getSimulationRunner() {
        return simulationRunner;
    }

    /**
     * Checks if a simulation is currently running.
     *
     * @return true if simulation is running
     */
    public boolean isSimulationRunning() {
        if (simulationRunner.simKern == null) {
            return false;
        }
        return simulationRunner.simKern.getSimulationStatus() ==
               ch.technokrat.gecko.geckocircuits.api.ISimulationEngine.SimulationStatus.RUNNING;
    }

    /**
     * Initializes simulation for step-by-step control from GeckoSCRIPT.
     */
    public void initSimForScripting() {
        simulationRunner.initSim();
    }

    /**
     * Initializes simulation with custom time step and duration.
     *
     * @param dt Time step
     * @param tEnd End time
     */
    public void initSimForScripting(double dt, double tEnd) {
        simulationRunner.initSim(dt, tEnd);
    }

    /**
     * External initialization for Simulink coupling.
     *
     * @param tEnd End time for simulation
     */
    public void externalInit(double tEnd) {
        simulationRunner.external_init(tEnd);
    }

    // Notification methods

    private void notifyStarted() {
        for (SimulationStateListener listener : listeners) {
            listener.onSimulationStarted();
        }
    }

    private void notifyPaused() {
        for (SimulationStateListener listener : listeners) {
            listener.onSimulationPaused();
        }
    }

    private void notifyFinished(long elapsedTimeMs) {
        for (SimulationStateListener listener : listeners) {
            listener.onSimulationFinished(elapsedTimeMs);
        }
    }

    private void notifyAborted(String message) {
        for (SimulationStateListener listener : listeners) {
            listener.onSimulationAborted(message);
        }
    }

    private void notifyStatusUpdate(String message) {
        for (SimulationStateListener listener : listeners) {
            listener.onStatusUpdate(message);
        }
    }

    private void showErrorDialog(String title, Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null,
                ex.getMessage(),
                title,
                JOptionPane.ERROR_MESSAGE);
    }
}

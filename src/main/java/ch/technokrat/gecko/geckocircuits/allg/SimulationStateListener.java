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

/**
 * Listener interface for simulation state changes.
 *
 * This interface decouples the simulation engine from UI components,
 * allowing simulation state changes to be communicated without direct
 * dependencies on Fenster or menu classes.
 *
 * @see SimulationRunner
 * @see Fenster
 */
public interface SimulationStateListener {

    /**
     * Called when a simulation starts.
     */
    void onSimulationStarted();

    /**
     * Called when a simulation is paused.
     */
    void onSimulationPaused();

    /**
     * Called when a simulation finishes normally.
     *
     * @param elapsedTimeMs Total simulation time in milliseconds
     */
    void onSimulationFinished(long elapsedTimeMs);

    /**
     * Called when a simulation is aborted due to error.
     *
     * @param errorMessage Description of the error
     */
    void onSimulationAborted(String errorMessage);

    /**
     * Called when simulation status message should be updated.
     *
     * @param message Status message to display
     */
    void onStatusUpdate(String message);
}

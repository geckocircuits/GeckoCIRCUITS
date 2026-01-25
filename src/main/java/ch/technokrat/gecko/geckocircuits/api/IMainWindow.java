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
import java.io.IOException;

/**
 * Interface for the main application window.
 *
 * Defines the contract for primary application operations including file management,
 * simulation control, and window state. This interface decouples dependent code from
 * the concrete Fenster implementation.
 *
 * @see ch.technokrat.gecko.geckocircuits.allg.Fenster
 */
public interface IMainWindow {

    /**
     * Checks if a simulation is currently running.
     *
     * @return true if simulation is running, false otherwise
     */
    boolean isSimulationRunning();

    /**
     * Gets the currently open file name.
     *
     * @return Current file name or "Untitled" if no file is open
     */
    String getOpenFileName();

    /**
     * Opens a file by name.
     *
     * @param fileName Path to the file to open
     * @throws FileNotFoundException if file doesn't exist
     */
    void openFile(String fileName) throws FileNotFoundException;

    /**
     * Saves the current file.
     */
    void saveFile();

    /**
     * Opens a "Save As" dialog and saves to a new file.
     */
    void saveFileAs();

    /**
     * Creates a new empty circuit file.
     */
    void createNewFile();

    /**
     * Updates the window title to reflect current file and state.
     */
    void updateWindowTitle();

    /**
     * Enables or disables simulator controls.
     *
     * @param enabled true to enable simulation controls
     */
    void setSimulatorEnabled(boolean enabled);

    /**
     * Opens the file selection dialog.
     */
    void openFileDialog();

    /**
     * Opens the simulation parameters dialog.
     */
    void openParameterMenu();

    /**
     * Saves scope/graph data to a file.
     *
     * @param scopeId Identifier for the scope
     * @param fileName Path to save the data
     * @return true if save was successful
     */
    boolean saveScopeData(String scopeId, String fileName);
}

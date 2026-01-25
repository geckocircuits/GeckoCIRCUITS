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

import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.control.RegelBlock;
import java.util.List;

/**
 * Interface for the circuit schematic editor.
 *
 * Defines the contract for circuit editing operations including component management,
 * selection, and state tracking. This interface decouples dependent code from
 * the concrete SchematischeEingabe2 implementation.
 *
 * @see ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2
 */
public interface ICircuitEditor {

    /**
     * Gets all electrical circuit (LK) components.
     *
     * @return List of electrical circuit components
     */
    List<AbstractCircuitBlockInterface> getElementLK();

    /**
     * Gets all control blocks.
     *
     * @return List of control blocks
     */
    List<RegelBlock> getElementCONTROL();

    /**
     * Gets all thermal components.
     *
     * @return List of thermal circuit components
     */
    List<AbstractCircuitBlockInterface> getElementTHERM();

    /**
     * Gets all block interface components.
     *
     * @return List of all block interface components
     */
    List<AbstractBlockInterface> getBlockInterfaceComponents();

    /**
     * Deletes a single component from the circuit.
     *
     * @param component Component to delete
     */
    void deleteComponent(AbstractBlockInterface component);

    /**
     * Deletes all currently selected components.
     */
    void deleteSelectedComponents();

    /**
     * Selects all components in the visible circuit sheet.
     */
    void selectAll();

    /**
     * Deselects all components.
     */
    void deselect();

    /**
     * Checks if the model has been modified since last save.
     *
     * @return true if model has unsaved changes
     */
    boolean isModelModified();

    /**
     * Clears the modified state flag.
     */
    void resetModelModified();

    /**
     * Marks the model as modified (sets dirty flag).
     */
    void setDirtyFlag();

    /**
     * Updates all component coupling references throughout the circuit.
     */
    void updateAllComponentReferences();

    /**
     * Updates component couplings when a component is renamed.
     *
     * @param oldName Original component name
     * @param newName New component name
     */
    void updateComponentCouplings(String oldName, String newName);

    /**
     * Resets all circuit sheets for a new file.
     */
    void resetCircuitSheetsForNewFile();

    /**
     * Enables or disables simulator integration.
     *
     * @param enabled true to enable simulation controls
     */
    void setSimulatorEnabled(boolean enabled);

    /**
     * Sets the font used for circuit display.
     *
     * @param fontSize Font size in points
     * @param fontType Font family name
     */
    void setFont(int fontSize, String fontType);

    /**
     * Enables or disables antialiasing in circuit rendering.
     *
     * @param enabled true to enable antialiasing
     */
    void setAntialiasing(boolean enabled);
}

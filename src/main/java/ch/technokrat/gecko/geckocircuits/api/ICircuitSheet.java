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

import ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent;
import java.util.List;

/**
 * Interface for a circuit sheet (drawing canvas for circuit components).
 *
 * Defines the contract for managing circuit sheet contents including components
 * and connections. This interface decouples dependent code from the concrete
 * CircuitSheet implementation.
 *
 * @see ch.technokrat.gecko.geckocircuits.circuit.CircuitSheet
 */
public interface ICircuitSheet {

    /**
     * Gets all elements on this circuit sheet.
     *
     * @return List of all circuit sheet components
     */
    List<AbstractCircuitSheetComponent> getAllElements();

    /**
     * Adds a component to this circuit sheet.
     *
     * @param component Component to add
     */
    void addElement(AbstractCircuitSheetComponent component);

    /**
     * Removes a component from this circuit sheet.
     *
     * @param component Component to remove
     */
    void removeElement(AbstractCircuitSheetComponent component);

    /**
     * Clears all elements from this circuit sheet.
     */
    void clearAllElements();

    /**
     * Requests a repaint of the circuit sheet.
     */
    void repaint();

    /**
     * Gets the name of this circuit sheet.
     *
     * @return Sheet name
     */
    String getSheetName();

    /**
     * Sets the name of this circuit sheet.
     *
     * @param name New sheet name
     */
    void setSheetName(String name);

    /**
     * Checks if this sheet is a subcircuit.
     *
     * @return true if this is a subcircuit sheet
     */
    boolean isSubcircuit();

    /**
     * Gets the parent circuit sheet (for subcircuits).
     *
     * @return Parent sheet, or null if this is the root sheet
     */
    ICircuitSheet getParentSheet();
}

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
package gecko.core.allg;

import gecko.core.circuit.TokenMap;

/**
 * Core interface for parameter values in headless simulation contexts.
 * Extracts 9 essential methods needed by loss curve classes.
 *
 * This interface enables loss curve classes to work in GUI-free environments
 * while maintaining backward compatibility with the full GUI-enabled UserParameter class.
 *
 * Design pattern: Interface extraction with adapter pattern for GUI integration.
 * See: LossFileAccessor pattern used in VerlustBerechnungDetailed.
 */
public interface UserParameterCore<T> {

    /**
     * Get the current value of this parameter.
     * @return Current parameter value
     */
    T getValue();

    /**
     * Get the parameter value as a double.
     * @return Parameter value converted to double
     * @throws ClassCastException if value cannot be converted to double
     */
    double getDoubleValue();

    /**
     * Set the parameter value without triggering undo/redo mechanism.
     * Used during circuit file loading and programmatic updates.
     * @param value New parameter value
     */
    void setValueWithoutUndo(T value);

    /**
     * Read parameter value from circuit file token map.
     * @param tokenMap Token map containing serialized parameter data
     */
    void readFromTokenMap(TokenMap tokenMap);

    /**
     * Write parameter to ASCII format for .ipes circuit file.
     * @param ascii String buffer to append XML representation
     */
    void writeXMLToFile(StringBuffer ascii);

    /**
     * Get unique identifier for serialization (e.g., "tj", "uBlock").
     * @return Save identifier string
     */
    String getSaveIdentifier();

    /**
     * Get short display name (e.g., "Tj", "Ub").
     * @return Short name for UI display
     */
    String getShortName();

    /**
     * Get measurement unit (e.g., "°C", "V", "A").
     * @return Unit string
     */
    String getUnit();

    /**
     * Get long descriptive name (e.g., "Junction Temperature", "Blocking Voltage").
     * @return Long description for tooltips/documentation
     */
    String getLongName();
}

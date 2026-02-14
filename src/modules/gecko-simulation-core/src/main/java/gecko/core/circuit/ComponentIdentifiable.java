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
package gecko.core.circuit;

/**
 * Interface for components that can be identified by string ID and numeric identifier.
 * Used for backward compatibility when loading old .ipes file formats.
 *
 * @since Sprint 4a - GeckoFile Migration
 */
public interface ComponentIdentifiable {
    /**
     * Get the string-based identifier for this component (used in older file formats).
     *
     * @return the string ID
     */
    String getStringID();

    /**
     * Get the unique numeric identifier for this component.
     *
     * @return the unique object identifier
     */
    long getUniqueObjectIdentifier();
}

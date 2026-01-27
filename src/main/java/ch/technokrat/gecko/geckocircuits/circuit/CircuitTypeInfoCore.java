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

package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitTypeInfoCore;

/**
 * Concrete circuit type information - NO GUI/I18n dependencies.
 * Pure circuit component type, extractable to gecko-simulation-core.
 */
public class CircuitTypeInfoCore extends AbstractCircuitTypeInfoCore {

    public CircuitTypeInfoCore(Class<? extends AbstractBlockInterface> typeClass, String idString) {
        super(typeClass, idString);
    }

    @Override
    public ConnectorType getSimulationDomain() {
        return ConnectorType.LK;
    }    
}

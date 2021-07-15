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

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitTypeInfo;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andy
 */
public class CircuitTypeInfo extends AbstractCircuitTypeInfo {

    public CircuitTypeInfo(Class<? extends AbstractBlockInterface> typeClass, String idString, I18nKeys typeDescription) {
        super(typeClass, idString, typeDescription);
    }

    public CircuitTypeInfo(Class<? extends AbstractBlockInterface> typeClass, String idString, I18nKeys typeDescription, I18nKeys typeDescriptionVerbose) {
        super(typeClass, idString, typeDescription, typeDescriptionVerbose);
    }

    @Override
    public ConnectorType getSimulationDomain() {
        return ConnectorType.LK;
    }    
}

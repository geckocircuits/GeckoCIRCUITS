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
package gecko.geckocircuits.circuit.circuitcomponents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.circuit.ConnectorType;
import gecko.geckocircuits.circuit.SpecialType;
import gecko.i18n.resources.I18nKeys;
public class ThermalTypeInfo extends AbstractTypeInfo {
    private static final Logger LOGGER = LogManager.getLogger(ThermalTypeInfo.class);


    public ThermalTypeInfo(final Class<? extends AbstractBlockInterface> typeClass,
            final String idString, final I18nKeys typeDescription) {
        super(typeClass, idString, typeDescription);
    }

    public ThermalTypeInfo(final Class<? extends AbstractBlockInterface> typeClass, final String idString,
            final I18nKeys typeDescription, final I18nKeys typeDescriptionVerbose) {
        super(typeClass, idString, typeDescription, typeDescriptionVerbose);
    }

    @Override
    public final ConnectorType getSimulationDomain() {
        return ConnectorType.THERMAL;
    }

    @Override
    public final String getExportImportCharacters() {
        return "eTH";
    }

    @Override
    public final String getSaveIdentifier() {
        return "ElementTHERM";
    }

    @Override
    public final AbstractBlockInterface fabric() {
        try {
            return _typeClass.getDeclaredConstructor().newInstance();
        } catch (Throwable ex) {
            LOGGER.error("error: " + _typeClass);LogManager.getLogger(SpecialType.class).error("Exception occurred", ex);
        }
        return null;

    }


}

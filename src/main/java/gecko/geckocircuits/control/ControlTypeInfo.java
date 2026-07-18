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
package gecko.geckocircuits.control;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.circuit.ConnectorType;
import gecko.geckocircuits.circuit.SpecialType;
import gecko.i18n.resources.I18nKeys;
public final class ControlTypeInfo extends AbstractTypeInfo {
    private static final Logger LOGGER = LogManager.getLogger(ControlTypeInfo.class);


    public ControlTypeInfo(final Class<? extends AbstractBlockInterface> typeClass, final String idString, final I18nKeys typeDescription, final I18nKeys typeDescriptionVerbose) {
        super(typeClass, idString, typeDescription);
    }

    public ControlTypeInfo(final Class<? extends AbstractBlockInterface> typeClass, final String idString, final I18nKeys typeDescription) {
        super(typeClass, idString, typeDescription);
    }

    @Override
    public ConnectorType getSimulationDomain() {
        return ConnectorType.CONTROL;
    }

    @Override
    public String getExportImportCharacters() {
        return "c";
    }

    @Override
    public String getSaveIdentifier() {
        return "ElementCONTROL";
    }

    @Override
    public AbstractBlockInterface fabric() {
        try {
            return _typeClass.getDeclaredConstructor().newInstance();
        } catch (Throwable ex) {
            LOGGER.error("error: " + _typeClass);
            ex.printStackTrace();LogManager.getLogger(SpecialType.class).error("Exception occurred", ex);
        }
        return null;

    }
}
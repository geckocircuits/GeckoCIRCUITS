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
import gecko.i18n.resources.I18nKeys;

/**
 * Circuit type information with GUI/I18n support.
 * Extends AbstractTypeInfo to add internationalization for circuit-specific types.
 */
public abstract class AbstractCircuitTypeInfo extends AbstractTypeInfo {
    private static final Logger LOGGER = LogManager.getLogger(AbstractCircuitTypeInfo.class);


    public AbstractCircuitTypeInfo(Class<? extends AbstractBlockInterface> typeClass, String idString, I18nKeys typeDescription) {
        super(typeClass, idString, typeDescription);
    }

    public AbstractCircuitTypeInfo(Class<? extends AbstractBlockInterface> typeClass, String idString, I18nKeys typeDescription, I18nKeys typeDescriptionVerbose) {
        super(typeClass, idString, typeDescription, typeDescriptionVerbose);
    }

    @Override
    public final String getExportImportCharacters() {
        return "e";
    }

    @Override
    public final String getSaveIdentifier() {
        return "ElementLK";
    }

    @Override
    public final AbstractBlockInterface fabric() {
        try {
            return _typeClass.getDeclaredConstructor().newInstance();
        } catch (Throwable ex) {
            LOGGER.error("error: " + _typeClass);
            ex.printStackTrace();
        }
        return null;
    }
}

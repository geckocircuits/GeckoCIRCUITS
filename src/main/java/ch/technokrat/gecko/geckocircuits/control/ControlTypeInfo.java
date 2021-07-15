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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.SpecialTyp;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ControlTypeInfo extends AbstractTypeInfo {

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
            return _typeClass.newInstance();
        } catch (Throwable ex) {
            System.err.println("error: " + _typeClass);
            ex.printStackTrace();
            Logger.getLogger(SpecialTyp.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }
}
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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.TypeInfoCore;
import ch.technokrat.gecko.geckocircuits.circuit.SpecialTyp;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core circuit type information - NO GUI/I18n dependencies.
 * Pure circuit component type registration logic.
 */
public abstract class AbstractCircuitTypeInfoCore extends TypeInfoCore {

    public AbstractCircuitTypeInfoCore(Class<? extends AbstractBlockInterface> typeClass, String idString) {
        super(typeClass, idString);
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
            System.err.println("error: " + _typeClass);
            ex.printStackTrace();
            Logger.getLogger(SpecialTyp.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    @Override
    public ConnectorType getSimulationDomain() {
        return ConnectorType.LK;
    }
}

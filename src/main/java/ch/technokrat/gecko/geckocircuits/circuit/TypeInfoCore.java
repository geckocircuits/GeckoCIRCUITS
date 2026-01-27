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

import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;

/**
 * Core type information interface - NO GUI/I18n dependencies.
 * Pure type registration and factory contract extractable to gecko-simulation-core.
 * Provides basic interface for component type registration and creation.
 */
public abstract class TypeInfoCore {            
    
    public final String _fixedIDString;
    public final Class<? extends AbstractBlockInterface> _typeClass;
    public AbstractComponentTyp _parentType;

    /**
     * Constructor for type information
     */
    public TypeInfoCore(final Class<? extends AbstractBlockInterface> typeClass, final String idString) {
        _fixedIDString = idString;
        _typeClass = typeClass;
    }
    
    /**
     * Abstract method for creating component instances
     */
    public abstract AbstractBlockInterface fabric();
    
    /**
     * Get export/import character code for this type
     */
    public abstract String getExportImportCharacters();
    
    /**
     * Get save identifier for serialization
     */
    public abstract String getSaveIdentifier();
    
    /**
     * Get simulation domain for this component type
     */
    public abstract ConnectorType getSimulationDomain();
}

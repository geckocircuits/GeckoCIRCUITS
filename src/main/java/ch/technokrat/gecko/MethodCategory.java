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
package ch.technokrat.gecko;

import ch.technokrat.gecko.i18n.resources.I18nKeys;

public enum MethodCategory {
    SIMULATION_START(I18nKeys.SIM_START_CATEGORY),
    LOAD_SAVE_MODEL(I18nKeys.LOAD_SAVE_CATEGORY),
    SIGNAL_PROCESSING(I18nKeys.SIGNAL_PROCESSING),
    COMPONENT_PROPERTIES(I18nKeys.COMPONENT_PROPERTIES),
    COMPONENT_CREATION_LISTING(I18nKeys.COMPONENT_CREATION),
    ALL_CATEGORIES(I18nKeys.ALL_CATEGORIES);
    
    private I18nKeys _tranlsationKey;

    private MethodCategory(final I18nKeys translationKey) {
        _tranlsationKey = translationKey;
    }    

    @Override
    public String toString() {
        return _tranlsationKey.getTranslation();
    }
    
    
    
}

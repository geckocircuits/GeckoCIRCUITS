/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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

import gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import gecko.geckocircuits.control.calculators.PT1Calculator;
import gecko.i18n.resources.I18nKeys;

public final class ReglerPT1 extends AbstractReglerPT {                
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerPT1.class, "PT1", I18nKeys.PT1);
    @Override
    public String[] getOutputNames() {
        return new String[]{"pt1"};
    }    
    
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new PT1Calculator(_TVal.getValue(), _a1Val.getValue());
    }
        
    
    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.PT1_CONTROL_DESCRIPTION};
    }                
}

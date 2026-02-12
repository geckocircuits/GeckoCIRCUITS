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
import gecko.geckocircuits.control.calculators.MaxCalculatorMultiInputs;
import gecko.geckocircuits.control.calculators.MaxCalculatorTwoInputs;
import gecko.i18n.resources.I18nKeys;

public final class ReglerMAX extends AbstractReglerVariableInputs {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerMAX.class, "MAX", I18nKeys.MAXIMUM);
    
    public ReglerMAX() {
        super(2);
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"max"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.LARGEST_VALUE_OF_THE_INPUTS};
    }        

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {        
        if (XIN.size() == 2) {            
            return new MaxCalculatorTwoInputs();
        } else {
            return new MaxCalculatorMultiInputs(XIN.size());
        }

    }
        
}

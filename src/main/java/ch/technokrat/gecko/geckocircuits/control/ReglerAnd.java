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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.AndMultiInputCalculator;
import ch.technokrat.gecko.geckocircuits.control.calculators.AndTwoPortCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

public final class ReglerAnd extends AbstractReglerVariableInputs {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerAnd.class, "AND", I18nKeys.AND);

    public ReglerAnd() {
        super(2);
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"and"};
    }    
    

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        if (XIN.size() == 2) {
            return new AndTwoPortCalculator();            
        } else {
            return new AndMultiInputCalculator(XIN.size());            
        }

    }
    

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[] {I18nKeys.LOGICAL_AND_OPERATION_OF_INPUTS};
    }    
}

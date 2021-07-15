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

import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.i18n.resources.I18nKeys;


public abstract class AbstractVoltageDropSwitch extends AbstractSwitch implements ForwardVoltageDropable {
    UserParameter<Double> _forwardVoltageDrop = UserParameter.Builder.
            <Double>start("forwardVoltageDrop", AbstractSwitch.UF_DEFAULT).                       
            longName(I18nKeys.FORWARD_VOLTAGE_DROP).
            shortName("uF").
            addAlternativeShortName("uDS").
            unit("V").            
            arrayIndex(this, 1).
            build();     
    

    @Override
    public UserParameter<Double> getForwardVoltageDropParameter() {
        return _forwardVoltageDrop;
    }
    
    
}

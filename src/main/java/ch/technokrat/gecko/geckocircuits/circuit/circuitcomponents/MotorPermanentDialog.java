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
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;

class MotorPermanentDialog extends AbstractMotorDialog<MotorPermanent> {
    public MotorPermanentDialog(final MotorPermanent parent) { 
        super(parent);
    }        
    
    List<UserParameter<Double>> getDialogSortedParameters() {
        return Arrays.asList(element._frictionParameter, element._inertiaParameter,
                element._polePairsParameter,
                element._armatureResistancePar, element._armatureInductancePar, 
                element._psiParameter, element._initialArmatureCurrentParam, element._initialRotationalSpeed,
                element._initialRotorPosition);        
    }        
        
    @Override
    JPanel buildPanelParameters() { 
        return super.buildPanelParameters(0, 6, new int[]{3,4}, true);        
    }

    @Override
    List<UserParameter<Double>> getInitPanelParameters() {
        return Arrays.asList(element._initialArmatureCurrentParam, element._initialRotationalSpeed,
                element._initialRotorPosition);        
    }    
    
    
    @Override
    JPanel buildPanelInitParameter() {
        return super.buildPanelParameters(6, 9, new int[0], false);                        
    }               
}

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

import gecko.geckocircuits.allg.UserParameter;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;

class MotorPMSMDialog extends AbstractMotorDialog<MotorPMSM> {
    
    public MotorPMSMDialog(final MotorPMSM parent) {
        super(parent);
    }
    

    @Override
    JPanel buildPanelParameters() {
        return super.buildPanelParameters(0, 7, new int[]{4,8}, true);        
    }

    @Override
    JPanel buildPanelInitParameter() {
        return super.buildPanelParameters(7, 11, new int[]{2}, false);        
    }

    @Override
    List getDialogSortedParameters() {
        return Arrays.asList(element._fluxLinkagePar, element._frictionParameter, element._inertiaParameter,
                element._polePairsParameter, element._statorResistancePar, element._statorInductanceDPar, element._statorInductanceQPar,
                element.initialStatorCurrentA, element.initialStatorCurrentB, element._initialRotationalSpeed,
                element._initialRotorPosition);
    }

    @Override
    List<UserParameter<Double>> getInitPanelParameters() {
        return Arrays.asList(element.initialStatorCurrentA, element.initialStatorCurrentB, element._initialRotationalSpeed,
                element._initialRotorPosition);
    
    }
        
    
    
    
}

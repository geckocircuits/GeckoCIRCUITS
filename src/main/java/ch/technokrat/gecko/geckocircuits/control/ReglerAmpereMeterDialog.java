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

import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossComponent;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

class ReglerAmpereMeterDialog extends DialogElementCONTROL {
    
    private JComboBox _comboLossType;
    
    public ReglerAmpereMeterDialog(final AbstractCurrentMeasurement reglerAMP) {
        super(reglerAMP);
    }

    @Override
    protected void baueGuiIndividual() {
        if (element instanceof ComponentCoupable) {
            jpM = createComponentCouplingPanel((AbstractBlockInterface) element);
            if (element instanceof ReglerFlowMeter) {
                //extra panel for flow measurement
                final JPanel psw2 = new JPanel();
                psw2.setLayout(new BorderLayout());
                _comboLossType = new JComboBox();
                final LossComponent[] components = LossComponent.values();
                for (int i = 0; i < components.length; i++) {
                    _comboLossType.addItem(components[i]);
                }
                _comboLossType.setSelectedItem(((ReglerFlowMeter)element).getLossComponentBeingMeasured());
                final JLabel lossLabel = new JLabel("Loss component to measure:");
                jpM.add(lossLabel);
                psw2.add(_comboLossType);
                jpM.setLayout(new GridLayout(3, 1));
                jpM.add(psw2);                                                
            }
        }        
    }
    
    @Override
    public void processInputs() {
        if (_comboLossType != null && element instanceof ReglerFlowMeter) {
            ((ReglerFlowMeter)element).setLossComponentBeingMeasured((LossComponent)_comboLossType.getSelectedItem());
        }
    }
}

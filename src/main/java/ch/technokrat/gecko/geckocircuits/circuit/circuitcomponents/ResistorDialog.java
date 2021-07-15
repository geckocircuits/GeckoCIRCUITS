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

import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.circuit.DialogNonLinearity;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

class ResistorDialog extends DialogElementLK<AbstractResistor> {

    public ResistorDialog(AbstractResistor parent) {
        super(parent);
    }

    @Override
    protected void baueGUIIndividual() {
        JPanel pIN = createParameterPanel(element._resistance);        
        
        
        final FormatJTextField capacitanceField = tf.get(0);
                
        final JPanel pINyC = new JPanel();
        pINyC.setLayout(new BorderLayout());
        pINyC.add(pIN, BorderLayout.NORTH);
        con.add(pINyC, BorderLayout.CENTER);
    }        
    
}

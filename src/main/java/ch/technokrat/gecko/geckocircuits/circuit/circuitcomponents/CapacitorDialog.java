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
import javax.swing.*;
import javax.swing.border.TitledBorder;

class CapacitorDialog extends DialogElementLK<AbstractCapacitor> {

    public CapacitorDialog(final AbstractCapacitor parent) {
        super(parent);
    }

    @Override
    protected void baueGUIIndividual() {
        final JPanel pIN = createParameterPanel(element._capacitance, element._initialValue);
        
        final JButton jbNonLinC = GuiFabric.getJButton(I18nKeys.DEFINE_CHARACTERISTIC);
        jbNonLinC.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
               DialogNonLinearity dialog = new DialogNonLinearity(element, true);
               dialog.setVisible(true);
            }
        });
        
        final FormatJTextField capacitanceField = tf.get(0);
        
        final JCheckBox jcbNonLinC = new JCheckBox("Non-Linear Behavior");
        jcbNonLinC.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                if (jcbNonLinC.isSelected()) {
                    jbNonLinC.setEnabled(true);
                    capacitanceField.setEnabled(false);
                } else {
                    jbNonLinC.setEnabled(false);
                    capacitanceField.setEnabled(true);
                }
                element._isNonlinear.setUserValue(jcbNonLinC.isSelected());
            }
        });
        jcbNonLinC.setSelected(element._isNonlinear.getValue());
        if (jcbNonLinC.isSelected()) {
            jbNonLinC.setEnabled(true);
            capacitanceField.setEnabled(false);
        } else {
            jbNonLinC.setEnabled(false);
            capacitanceField.setEnabled(true);
        }
        
        final JPanel pNonLinC = new JPanel();
        pNonLinC.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Non-Linear", TitledBorder.LEFT, TitledBorder.TOP));
        pNonLinC.setLayout(new BorderLayout());
        pNonLinC.add(jcbNonLinC, BorderLayout.CENTER);
        final JPanel pNonLinxC = new JPanel();
        pNonLinxC.add(jbNonLinC);
        pNonLinC.add(pNonLinxC, BorderLayout.SOUTH);
        //
        final JPanel pINyC = new JPanel();
        pINyC.setLayout(new BorderLayout());
        pINyC.add(pIN, BorderLayout.NORTH);
        pINyC.add(pNonLinC, BorderLayout.CENTER);        
        con.add(pINyC, BorderLayout.CENTER);
    }                        
}

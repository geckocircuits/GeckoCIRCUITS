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

import ch.technokrat.gecko.geckocircuits.allg.Fenster;
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

class InductorDialog extends DialogElementLK<AbstractInductor> {
    private JCheckBox jcbLossL;  // soll GeckoMAGNETICS aktiviert werden? 
    
    public InductorDialog(final AbstractInductor parent) {
        super(parent);
    }

    @Override
    protected void baueGUIIndividual() {
        jcbLossL = new JCheckBox("Calculate Losses");
        JPanel pIN = createParameterPanel(element._inductance, element._initialCurrent);
        
        final JButton jbNonLinL = GuiFabric.getJButton(I18nKeys.DEFINE_CHARACTERISTIC);
        jbNonLinL.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                DialogNonLinearity dialogNonLinearity = new DialogNonLinearity(element, false);
                dialogNonLinearity.setVisible(true);
            }
        });
        
        final FormatJTextField inductanceField = tf.get(0);
        
        final JCheckBox jcbNonLinL = new JCheckBox("Non-Linear Behavior");
        jcbNonLinL.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (jcbNonLinL.isSelected()) {
                    jbNonLinL.setEnabled(true);
                    inductanceField.setEnabled(false);
                } else {
                    jbNonLinL.setEnabled(false);
                    inductanceField.setEnabled(true);
                }
                element._isNonlinear.setUserValue(jcbNonLinL.isSelected());
            }
        });
        jcbNonLinL.setSelected(element._isNonlinear.getValue());
        if (jcbNonLinL.isSelected()) {
            jbNonLinL.setEnabled(true);
            inductanceField.setEnabled(false);
        } else {
            jbNonLinL.setEnabled(false);
            inductanceField.setEnabled(true);
        }
        // 
        JPanel pNonLinL = new JPanel();
        pNonLinL.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Non-Linear", TitledBorder.LEFT, TitledBorder.TOP));
        pNonLinL.setLayout(new BorderLayout());
        pNonLinL.add(jcbNonLinL, BorderLayout.CENTER);
        JPanel pNonLinxL = new JPanel();
        pNonLinxL.add(jbNonLinL);
        pNonLinL.add(pNonLinxL, BorderLayout.SOUTH);
        //------------------
        final JButton jbMAG1 = new JButton("GeckoMAGNETICS");
        jcbLossL.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (jcbLossL.isSelected()) {
                    jbMAG1.setEnabled(true);
                } else {
                    jbMAG1.setEnabled(false);
                }
                
            }
        });
        if (Fenster.INCLUDE_GeckoMAGNETICS) {
            
            if (jcbLossL.isSelected()) {
                jbMAG1.setEnabled(true);
            } else {
                jbMAG1.setEnabled(false);
            }
        } else {
            jcbLossL.setEnabled(false);
            jbMAG1.setEnabled(false);
        }
        // 
        JPanel pMAG1 = new JPanel();
        pMAG1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Losses", TitledBorder.LEFT, TitledBorder.TOP));
        pMAG1.setLayout(new BorderLayout());
        pMAG1.add(jcbLossL, BorderLayout.CENTER);
        JPanel pMAG1x = new JPanel();
        pMAG1x.add(jbMAG1);
        pMAG1.add(pMAG1x, BorderLayout.SOUTH);
        //
        JPanel pINy = new JPanel();
        pINy.setLayout(new BorderLayout());
        pINy.add(pIN, BorderLayout.NORTH);
        pINy.add(pNonLinL, BorderLayout.CENTER);
        pINy.add(pMAG1, BorderLayout.SOUTH);
        con.add(pINy, BorderLayout.CENTER);

    }        
}

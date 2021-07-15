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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.DialogCircuitComponent;
import ch.technokrat.gecko.geckocircuits.circuit.PotentialCoupable;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;

/**
 * DialogElementLK is package private by intention! Every circuit component
 * should be defined within this packagse, and DialogElementLK should only be
 * called from within this package.
 *
 * @author andy
 */
public abstract class DialogElementLK<T extends AbstractCircuitBlockInterface> extends DialogCircuitComponent<T> {

    private GridBagConstraints gbc = new GridBagConstraints();                

    DialogElementLK(final T elementLK) {
        super(GeckoSim._win, true, elementLK);                        
        getContentPane().setLayout(new BorderLayout());        
    }            

    abstract void baueGUIIndividual();

    @Override
    public void baueGUI() {        
        con = this.getContentPane();
        con.setLayout(new BorderLayout());        
        gbc.fill = GridBagConstraints.BOTH;                        
        con.add(jPanelName, BorderLayout.NORTH);                         
        baueGUIIndividual();
        con.add(jPanelButtonOkCancel, BorderLayout.SOUTH);        
    }   

    static Component createControlLabelCombo(final AbstractCircuitBlockInterface elementLK) {
        Component returnValue = null;
        final List<String> labelListeReglerKnotenTemp = elementLK.getParentCircuitSheet().getLocalLabels(ConnectorType.CONTROL);
        if (!labelListeReglerKnotenTemp.isEmpty()) {
            final JComboBox combo = new JComboBox(labelListeReglerKnotenTemp.toArray());

            for (String search : labelListeReglerKnotenTemp) {
                if (elementLK.getParameterString()[0].equals(search)) {
                    combo.setSelectedIndex(labelListeReglerKnotenTemp.indexOf(search));
                    break;
                }
            }

            combo.setForeground(GlobalColors.farbeFertigElementCONTROL);
            combo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent actionEvent) {
                    if (elementLK instanceof PotentialCoupable) {
                        ((PotentialCoupable) elementLK).getPotentialCoupling().setNewCouplingLabel(0, combo.getSelectedItem().toString());
                    } else {
                        elementLK.getParameterString()[0] = combo.getSelectedItem().toString();
                    }
                }
            });
            returnValue = combo;
        } else {
            final JLabel labelMissing = new JLabel("No signal nodes defined.");
            labelMissing.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
            returnValue = labelMissing;
        }
        return returnValue;
    }                
}

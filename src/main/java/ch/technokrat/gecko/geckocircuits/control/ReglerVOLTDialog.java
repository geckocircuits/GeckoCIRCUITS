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

import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.PotentialCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalInterface;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JPanel;

class ReglerVOLTDialog extends DialogElementCONTROL {

    private DialogPanelVoltageMeasurement dpvm;
    JComboBox componentCombo;

    public ReglerVOLTDialog(AbstractPotentialMeasurement potMeas) {
        super(potMeas);
    }

    @Override
    protected void baueGuiIndividual() {
        dpvm = new DialogPanelVoltageMeasurement();
        PotentialCoupable potCoupable = (PotentialCoupable) element;
        jpM = dpvm;
        final JPanel compMeasurement = createComponentCouplingPanel(element);
        dpvm.jPanelComponentMeasurement.add(compMeasurement);
        List<String> labelListe = new ArrayList<String>();
        ConnectorType conType = potCoupable.getPotentialCoupling().getType();

        for (AbstractBlockInterface block : element.getParentCircuitSheet().allElements.getClassFromContainer(AbstractBlockInterface.class)) {
            for (TerminalInterface term : block.getAllTerminals()) {
                if (term.getCategory() == conType) {
                    final String label = term.getLabelObject().getLabelString();
                    if (!label.isEmpty() && !labelListe.contains(label)) {
                        labelListe.add(label);
                    }
                }
            }
        }
        Collections.sort(labelListe);


        for (String lab : labelListe) {
            dpvm.jComboBoxLabel1.addItem(lab);
            dpvm.jComboBoxLabel2.addItem(lab);
        }

        dpvm.jComboBoxLabel1.setSelectedIndex(-1);
        dpvm.jComboBoxLabel2.setSelectedIndex(-1);

        for (String lab : labelListe) {
            if (element.getParameterString()[0].equals(lab)) {
                dpvm.jComboBoxLabel1.setSelectedIndex(labelListe.indexOf(lab));
            }
            if (element.getParameterString()[1].equals(lab)) {
                dpvm.jComboBoxLabel2.setSelectedIndex(labelListe.indexOf(lab));
            }
        }

        Color potentialColor = GlobalColors.farbeFertigElementLK;
        switch (potCoupable.getPotentialCoupling().getType()) {
            case CONTROL:
                potentialColor = GlobalColors.farbeFertigElementCONTROL;
                break;
            case THERMAL:
                potentialColor = GlobalColors.farbeFertigElementTHERM;
                break;
            case LK:
            case LK_AND_RELUCTANCE:
                potentialColor = GlobalColors.farbeFertigElementLK;
                break;
            case RELUCTANCE:
                potentialColor = GlobalColors.farbeFertigElementRELUCTANCE;
                break;
            default:
                assert false;
        }

        dpvm.jComboBoxLabel1.setForeground(potentialColor);
        Object testCombo = ((JPanel) compMeasurement.getComponent(0)).getComponent(0);
        if (testCombo instanceof JComboBox) {
            componentCombo = (JComboBox) ((JPanel) compMeasurement.getComponent(0)).getComponent(0);
            dpvm.jComboBoxLabel1.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    if (dpvm.jComboBoxLabel1.getSelectedItem() != null) {
                        componentCombo.setSelectedIndex(-1);
                    }
                }
            });


            componentCombo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    if (componentCombo.getSelectedItem() != null) {
                        dpvm.jComboBoxLabel1.setSelectedIndex(-1);
                        dpvm.jComboBoxLabel2.setSelectedIndex(-1);
                    }
                }
            });
        }



        dpvm.jComboBoxLabel2.setForeground(potentialColor);
        dpvm.jComboBoxLabel2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String[] parStr = element.getParameterString();
                if (dpvm.jComboBoxLabel2.getSelectedItem() != null) {
                    final String newLabel = dpvm.jComboBoxLabel2.getSelectedItem().toString();
                    ((PotentialCoupable) element).getPotentialCoupling().setNewCouplingLabelUserDialog(1, newLabel);
                    parStr[2] = "";
                }
            }
        });
    }

    @Override
    public void processInputs() {
        if (dpvm.jComboBoxLabel1.getSelectedItem() != null) {
            final String newLabel = dpvm.jComboBoxLabel1.getSelectedItem().toString();
            ((ComponentCoupable) element).getComponentCoupling().setNewCouplingElementInvisibleUndoable(0, null);
            ((PotentialCoupable) element).getPotentialCoupling().setNewCouplingLabelUserDialog(0, newLabel);
        }

        if (dpvm.jComboBoxLabel2.getSelectedItem() != null) {
            final String newLabel = dpvm.jComboBoxLabel2.getSelectedItem().toString();
            ((ComponentCoupable) element).getComponentCoupling().setNewCouplingElementInvisibleUndoable(0, null);
            ((PotentialCoupable) element).getPotentialCoupling().setNewCouplingLabelUserDialog(1, newLabel);
        }
        
        if (componentCombo != null && componentCombo.getSelectedItem() != null) {
            ((PotentialCoupable) element).getPotentialCoupling().setNewCouplingLabel(0, "");
            ((PotentialCoupable) element).getPotentialCoupling().setNewCouplingLabel(1, "");
        }
    }
}

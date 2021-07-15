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
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractMotor;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

class ReglerViewMotDialog extends DialogElementCONTROL {

    public ReglerViewMotDialog(final ReglerVIEWMOT aThis) {
        super(aThis);
    }

    @Override
    protected void baueGuiIndividual() {
        JPanel psw2 = new JPanel();
        psw2.setLayout(new BorderLayout());
        List<String> labelListeElementLK2 = new ArrayList<String>();
        final List<AbstractCircuitBlockInterface> possibleElements = element.getParentCircuitSheet().allElements.
                getClassFromContainer(AbstractCircuitBlockInterface.class);
        for (AbstractCircuitBlockInterface elem : possibleElements) {
            if (elem instanceof AbstractMotor) {
                for (String parameterString : ((AbstractCircuitBlockInterface) (elem)).getParameterStringIntern()) {
                    labelListeElementLK2.add(elem.getStringID() + "." + parameterString);
                }
            }
        }

        //
        if (labelListeElementLK2.size() > 0) {
            final JComboBox combo = new JComboBox();
            for (String label : labelListeElementLK2) { 
                combo.addItem(label);
            }
            int indexCombo = -1;
            for (int i1 = 0; i1 < labelListeElementLK2.size(); i1++) {
                if (element.getParameterString()[0].equals(labelListeElementLK2.get(i1))) {
                    indexCombo = i1;
                }
            }
            combo.setSelectedIndex(indexCombo);
            combo.setForeground(GlobalColors.farbeFertigElementLK);
            combo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent actionEvent) {
                    String[] parStr = element.getParameterString();
                    String selectedString = combo.getSelectedItem().toString();
                    parStr[0] = selectedString;
                    parStr[1] = selectedString.substring(0, selectedString.lastIndexOf("."));
                    parStr[2] = selectedString.substring(selectedString.lastIndexOf(".") + 1);
                    for (AbstractBlockInterface search : possibleElements) {
                        if (search.getStringID().equals(parStr[1])) {
                            ((ReglerVIEWMOT) element).getComponentCoupling().setNewCouplingElementUndoable(0, search);
                        }
                    }
                }
            });
            psw2.add(combo);
        } else {
            JLabel txtNo = new JLabel("No machine in power circuit defined.");
            txtNo.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
            psw2.add(txtNo);
        }
        jpM.add(psw2);
    }
}

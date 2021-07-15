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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public abstract class DialogElementCONTROL<T extends RegelBlock> extends DialogCircuitComponent<T> {    
    JPanel jpM = new JPanel();                    

    public DialogElementCONTROL(final T element) {
        super(GeckoSim._win, true, element);                
        
    }    
    

    @Override
    public void baueGUI() {
        this.setLocationRelativeTo(GeckoSim._win);                
        con = this.getContentPane();
        con.setLayout(new BorderLayout());                                        
        try {
        con.add(jPanelName, BorderLayout.NORTH);
        } catch (Exception ex) {
            // sometimes, I git an XException here... don't know the reason.
            ex.printStackTrace();
        }  
        jpM.setLayout(new BorderLayout());
        jpM.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "Parameter", TitledBorder.LEFT, TitledBorder.TOP));
        
        baueGuiIndividual();
        con.add(jpM, BorderLayout.CENTER);
        con.add(jPanelButtonOkCancel, BorderLayout.SOUTH);
    }

    
    public static JPanel createComponentCouplingPanel(final AbstractBlockInterface couplingElement) {
        if (!(couplingElement instanceof ComponentCoupable)) {
            return null;
        }
        final ComponentCoupable coupable = (ComponentCoupable) couplingElement;
        final JPanel jpM = new JPanel();
        jpM.setLayout(new GridLayout(1, 1));
        jpM.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), coupable.getCouplingTitle().getTranslation(),
                TitledBorder.LEFT, TitledBorder.TOP));

        final JPanel psw1 = new JPanel();
        psw1.setLayout(new BorderLayout());

        final List<AbstractCircuitSheetComponent> allPossibleElements = couplingElement.getParentCircuitSheet().allElements;
        final List<AbstractBlockInterface> selectionList = new ArrayList<AbstractBlockInterface>();

        for (AbstractCircuitSheetComponent elem : allPossibleElements) {
            coupable.checkComponentCompatibility(elem, selectionList);
        }

        Collections.sort(selectionList, ALPHABETIC_SORT);

        final JComboBox combo = new JComboBox();

        int comboSelectionIndex = -1;
        for (int i = 0; i < selectionList.size(); i++) {
            combo.addItem(selectionList.get(i).getStringID());
            final AbstractBlockInterface connectedTo = coupable.getComponentCoupling()._coupledElements[0];
            if (connectedTo != null && selectionList.get(i).getStringID().equals(connectedTo.getStringID())) {
                comboSelectionIndex = i;
            }
        }
        combo.setSelectedIndex(comboSelectionIndex);

        if (selectionList.isEmpty()) {
            final JLabel txtNo = new JLabel(coupable.getMissingComponentsString().getTranslation());
            txtNo.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
            psw1.add(txtNo);
        } else {
            combo.setForeground(selectionList.get(0).getForeGroundColor());
            addSelectionComboListener(combo, selectionList, coupable);
            psw1.add(combo);
        }
        jpM.add(psw1);
                      
        return jpM;
    }

    private static void addSelectionComboListener(final JComboBox combo, final List<AbstractBlockInterface> selectionList,
            final ComponentCoupable coupable) {
        combo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                if (combo.getSelectedIndex() < 0) {
                    coupable.getComponentCoupling().setNewCouplingElementUndoable(0, null);
                } else {
                    final AbstractBlockInterface selectedElement = selectionList.get(combo.getSelectedIndex());
                    final String comboString = combo.getSelectedItem().toString();
                    assert selectedElement.getStringID().equals(comboString);
                    coupable.getComponentCoupling().setNewCouplingElementUndoable(0, selectedElement);
                }

            }
        });
    }
    private static final Comparator<AbstractBlockInterface> ALPHABETIC_SORT = new Comparator<AbstractBlockInterface>() {
        @Override
        public int compare(final AbstractBlockInterface first, final AbstractBlockInterface second) {
            final String firstName = first.getStringID();
            final String secondName = second.getStringID();
            return firstName.compareTo(secondName);
        }
    };

    abstract void baueGuiIndividual();

    @Override
    public void processInputIndividual() {
        processInputs();
        processRegisteredParameters();
        schliesseFenster();
    }        

    protected void processInputs() {
    }

        
}

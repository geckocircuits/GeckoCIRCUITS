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

import ch.technokrat.gecko.geckocircuits.circuit.ControlSourceType;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

class ReglerSignalSourceDialog extends AbstractDialogWithExternalOption<ReglerSignalSource> {
    
    private JCheckBox jCheckBoxShowDetails;  // wieviel Info soll im SchematicEntry angezeigt werden (Uebersicht vs. Info)?
    // Unterdialoge fuer Preview-SIGNAL -->         
    private boolean displayDetails = false;  // wieviel Info soll im SchematicEntry angezeigt werden (Uebersicht vs. Info)?
    private final JComboBox jComboShape = new JComboBox();    
    private final GridBagConstraints gbc = new GridBagConstraints();
    JPanel parameterPanel;

    public ReglerSignalSourceDialog(final ReglerSignalSource reglerSignal) {
        super(reglerSignal);
        gbc.fill = GridBagConstraints.BOTH;        
        jComboShape.addItem(ControlSourceType.QUELLE_RECHTECK);
        jComboShape.addItem(ControlSourceType.QUELLE_SIN);
        jComboShape.addItem(ControlSourceType.QUELLE_DREIECK);
    }
        

    @Override
    protected void baueGuiIndividual() {
        jpM = new JPanel();
        jpM.setLayout(new BorderLayout());
        
        parameterPanel = createParameterPanel(element._amplitudeAC, element._frequency, element._offsetDC, element._phase, element._dutyRatio);        
                                  
        jComboShape.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean visible = jComboShape.getSelectedItem() != ControlSourceType.QUELLE_SIN;
                tf.get(tf.size()-1).setVisible(visible);
                for(Component comp : parameterPanel.getComponents()) {
                    if(comp instanceof JLabel) {
                        JLabel lab = (JLabel) comp;
                        if(lab.getText().startsWith("d =")) {
                        lab.setVisible(visible);
                    }
                }
                }
            }            
        });
        
        jComboShape.setSelectedItem(element._typQuelle.getValue());        
                
        
        jCheckBoxShowDetails = new JCheckBox("Display Details");
        if (element.getParameter()[7] == 0) {  // init
            displayDetails = false;
            jCheckBoxShowDetails.setSelected(false);
        } else {
            displayDetails = true;
            jCheckBoxShowDetails.setSelected(true);
        }
        jCheckBoxShowDetails.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (jCheckBoxShowDetails.isSelected()) {
                    displayDetails = true;
                } else {
                    displayDetails = false;
                }
            }
        });
        //------------
                
        JPanel jpMM = new JPanel();
        jpMM.setLayout(new BorderLayout());
        
        JPanel upperGrid = new JPanel();
        upperGrid.setLayout(new GridLayout(3,1));
        jpMM.add(upperGrid, BorderLayout.NORTH);
        upperGrid.add(_jCheckBoxUseExternal);
        upperGrid.add(jCheckBoxShowDetails);
                        
        JLabel shapeLabel = new JLabel("Signal shape: ");
        JPanel shapePanel = new JPanel();
        shapePanel.add(shapeLabel);
        shapePanel.add(jComboShape);
        upperGrid.add(shapePanel);
        jpMM.add(new JLabel(" "), BorderLayout.SOUTH);  // Abstand 
        jpM.add(jpMM, BorderLayout.NORTH);
        jpM.add(parameterPanel, BorderLayout.CENTER);
                                
    }           

    @Override
    protected void processInputs() {
        super.processInputs();                
        element._displayDetails.setUserValue(displayDetails);                
        element._typQuelle.setUserValue((ControlSourceType) jComboShape.getSelectedItem());
    }
    

    @Override
    JComponent[] getComponentsDisabledExternal() {
        return tf.toArray(new JComponent[tf.size()]);                
    }        
    
}

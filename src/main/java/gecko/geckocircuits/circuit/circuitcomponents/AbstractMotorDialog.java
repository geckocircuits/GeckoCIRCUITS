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

import gecko.geckocircuits.allg.FormatJTextField;
import gecko.geckocircuits.allg.UserParameter;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

abstract class AbstractMotorDialog<T extends AbstractMotor> extends DialogElementLK<T> {
    final static String trenner = "   ";
    final static String separator = "-------";
    JLabel[] variableNames;
    JLabel[] explanationStrings;

    public AbstractMotorDialog(final T elementLK) {
        super(elementLK);
    }
    

    @Override
    public void processInputIndividual() {                
        
        List<UserParameter<Double>> orderedUserParameters = getDialogSortedParameters();
        
        for (UserParameter<Double> param : orderedUserParameters) {  
            final FormatJTextField textField = tf.get(orderedUserParameters.indexOf(param));
            double number = textField.getNumberFromField();  // cM
            if (number == FormatJTextField.IS_VARIABLE) {
                param.setNameOpt(textField.getText());                
            } else {
                param.setNameOpt("");
                param.setUserValue(number);
            }
        }        
    }

    @Override
    protected final void baueGUIIndividual() {
        JTabbedPane tabber = this.createMotorGUI();
        con.add(tabber, BorderLayout.CENTER);
    }

    JTabbedPane createMotorGUI() {
        createOrderedComponents();
        JPanel jpParMOTORx_IMSAT = wrap(buildPanelParameters());
        JPanel jpInitMOTORx_IMSAT = wrap(buildPanelInitParameter());
        JTabbedPane tabberMOTOR_IMSAT = new JTabbedPane();
        tabberMOTOR_IMSAT.addTab("Parameter", jpParMOTORx_IMSAT);
        tabberMOTOR_IMSAT.addTab("Initial", jpInitMOTORx_IMSAT);
        return tabberMOTOR_IMSAT;
    }

    JPanel wrap(final JPanel content) {
        JPanel jpParMOTORx_SMSALIENT = new JPanel();
        jpParMOTORx_SMSALIENT.setLayout(new BorderLayout());
        jpParMOTORx_SMSALIENT.add(content, BorderLayout.NORTH);
        return jpParMOTORx_SMSALIENT;
    }

    abstract JPanel buildPanelParameters();

    abstract JPanel buildPanelInitParameter();
    abstract List<UserParameter<Double>> getInitPanelParameters();
    
    void createOrderedComponents() {
        List<UserParameter<Double>> orderedUserPars = getDialogSortedParameters();
        
        variableNames = new JLabel[orderedUserPars.size()];
        explanationStrings = new JLabel[orderedUserPars.size()];

        for (int i = 0; i < orderedUserPars.size(); i++) {
            String unitString = orderedUserPars.get(i).getUnit();
            if ("unitless".equals(unitString)) {
                unitString = " - ";
            }
            variableNames[i] = labelFabric(orderedUserPars.get(i).getShortName()
                    + " [" + unitString + "] ");
            explanationStrings[i] = labelFabric(trenner + orderedUserPars.get(i).getLongName());            
            tf.add(fabricFormatTextField(orderedUserPars.get(i)));
        }
    }

    protected void addTorqueCombo(int i, JPanel jpParMOTOR, GridBagConstraints gbc) {
        JLabel jlMoSMb15 = labelFabric("Tm [Nm] =  ");
        JLabel jlMoSMb15t = labelFabric(trenner + "mech. torque (load)");
        JLabel jlMoSMb15x = labelFabric(trenner + "If not defined:  Tm = 0");
        gbc.gridx = 0;
        gbc.gridy = 19;
        jpParMOTOR.add(jlMoSMb15, gbc);
        gbc.gridx = 1;
        gbc.gridy = 19;
        jpParMOTOR.add(createControlLabelCombo(element), gbc);
        gbc.gridx = 2;
        gbc.gridy = 19;
        jpParMOTOR.add(jlMoSMb15t, gbc);
        gbc.gridx = 2;
        gbc.gridy = 20;
        jpParMOTOR.add(jlMoSMb15x, gbc);
    }

    static JPanel panelFabric() {
        JPanel jPanelMOTOR = new JPanel();
        jPanelMOTOR.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Initial values at t=0", TitledBorder.LEFT, TitledBorder.TOP));
        jPanelMOTOR.setLayout(new GridBagLayout());
        return jPanelMOTOR;
    }
    
    abstract List<UserParameter<Double>> getDialogSortedParameters();

    JPanel buildPanelParameters(int startIndex, int endIndex, int[] separatorIndices, boolean addTorqueCombo) {
        JPanel jpParMOTOR = panelFabric();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = gbc.BOTH;

        gbc.gridy = 0;
        for (int i = startIndex; i < endIndex; i++) {
            gbc.gridx = 0;
            jpParMOTOR.add(variableNames[i], gbc);
            gbc.gridx = 1;
            jpParMOTOR.add(tf.get(i), gbc);
            gbc.gridx = 2;
            jpParMOTOR.add(explanationStrings[i], gbc);
            gbc.gridy++;
            for (int sepIndex : separatorIndices) {
                if (gbc.gridy == sepIndex) {
                    gbc.gridx = 0;
                    jpParMOTOR.add(new JLabel(" "), gbc);
                    gbc.gridy++;
                }
            }
        }
        if(addTorqueCombo) {
            addTorqueCombo(gbc.gridy + 1, jpParMOTOR, gbc);
        }        
        return jpParMOTOR;
    }
}

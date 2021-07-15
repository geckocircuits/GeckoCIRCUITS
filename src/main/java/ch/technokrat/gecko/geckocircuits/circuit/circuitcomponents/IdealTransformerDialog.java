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
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

/**
 *
 * @author andy
 */
class IdealTransformerDialog extends DialogElementLK<IdealTransformer> {

    private JCheckBox reverse;

    public IdealTransformerDialog(IdealTransformer parent) {
        super(parent);
    }

    @Override
    protected void baueGUIIndividual() {
        JPanel pIN2 = new JPanel();
        pIN2.setLayout(new GridLayout(3, 3));
        pIN2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "", TitledBorder.LEFT, TitledBorder.TOP));
        //------------------
        JLabel jln = labelFabric("n : 1 =  ");
        reverse = new JCheckBox("Reversed");
        reverse.setFont(GlobalFonts.LAB_FONT_DIALOG_1);

        if (element._reversed.getDoubleValue() == 1) {
            reverse.setSelected(true);
        }
        
        final FormatJTextField windingsField1 = getRegisteredTextField(element._windings1);
        final FormatJTextField windingsField2 = getRegisteredTextField(element._windings2);
        final FormatJTextField windingsRatio = getRegisteredTextField(element._windingsRatio);
        
        pIN2.add(jln);                

        windingsRatio.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent ke) {
            }

            public void keyPressed(KeyEvent ke) {
            }

            public void keyReleased(KeyEvent ke) {
                windingsField1.setText(windingsRatio.getText());
                windingsField2.setText("1");
            }
        });
        pIN2.add(windingsRatio);

        JLabel m_n = labelFabric("m : n =  ");
        pIN2.add(new JLabel(" "));
        pIN2.add(m_n);
        
        
        windingsField1.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent ke) {
            }

            public void keyPressed(KeyEvent ke) {
            }

            public void keyReleased(KeyEvent ke) {
                try {
                    if (windingsField2.getNumberFromField() != 0) {
                        windingsRatio.setNumberToField(windingsField1.getNumberFromField() / windingsField2.getNumberFromField());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        pIN2.add(windingsField1);       
        pIN2.add(windingsField2);
        pIN2.add(reverse);

        windingsField2.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent ke) {
            }

            public void keyPressed(KeyEvent ke) {
            }

            public void keyReleased(KeyEvent ke) {
                try {
                    if (windingsField2.getNumberFromField() != 0) {
                        windingsRatio.setNumberToField(windingsField1.getNumberFromField() / windingsField2.getNumberFromField());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        if (!element._windings1.getNameOpt().isEmpty() || !element._windings2.getNameOpt().isEmpty()) {
            windingsRatio.setForeground(GlobalColors.farbeOPT);
            windingsRatio.setText(windingsField1.getText() + " / " + windingsField2.getText());
        } else {
            windingsRatio.setNumberToField(element._windings1.getValue() / element._windings2.getValue());
        }

        con.add(pIN2, BorderLayout.CENTER);
    }

    @Override
    public void processInputIndividual() {        
        if (reverse.isSelected()) {
            element._reversed.setUserValue(1.0);
        } else {
            element._reversed.setUserValue(-1.0);
        }
        ((IdealTransformer) element).initPar();
    }
}

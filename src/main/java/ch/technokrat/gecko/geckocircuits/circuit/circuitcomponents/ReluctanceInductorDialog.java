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

class ReluctanceInductorDialog extends DialogElementLK<ReluctanceInductor> {

    public ReluctanceInductorDialog(final ReluctanceInductor parent) {
        super(parent);
    }

    @Override
    protected void baueGUIIndividual() {
        JPanel pIN2 = new JPanel();
        pIN2.setLayout(new GridLayout(5, 2));
        pIN2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "", TitledBorder.LEFT, TitledBorder.TOP));
        JLabel jln = labelFabric("Winding number =  ");
        final JCheckBox reverseInput = new JCheckBox("Input reversed");
        final JCheckBox reverseOutput = new JCheckBox("Output reversed");
        reverseInput.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
        reverseOutput.setFont(GlobalFonts.LAB_FONT_DIALOG_1);

        if (element._inputReversed.getValue()) {
            reverseInput.setSelected(true);
        }

        if (element._outputReversed.getValue()) {
            reverseOutput.setSelected(true);
        }

        final FormatJTextField ampTurnsField = new FormatJTextField();
        
        reverseInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                if (reverseInput.isSelected()) {
                    element._inputReversed.setUserValue(true);
                } else {
                    element._inputReversed.setUserValue(false);
                }
            }
        });

        reverseOutput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                if (reverseOutput.isSelected()) {
                    element._outputReversed.setUserValue(true);
                } else {
                    element._outputReversed.setUserValue(false);
                }
            }
        });

        pIN2.add(jln);
        
        final FormatJTextField windingsField  = getRegisteredTextField(element._windings);
        final FormatJTextField initCurrentField = getRegisteredTextField(element._initCurrent);
        
        windingsField.addKeyListener(new KeyListener() {
            public void keyTyped(final KeyEvent event) {
            }

            public void keyPressed(final KeyEvent event) {
            }

            public void keyReleased(final KeyEvent event) {
                ampTurnsField.setNumberToField(windingsField.getNumberFromField() * ampTurnsField.getNumberFromField());
            }
        });
        pIN2.add(windingsField);

        final JLabel m_n = labelFabric("Initial current =  ");
        pIN2.add(m_n);
        
        
        initCurrentField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(final KeyEvent event) {
                // nothing todo
            }

            @Override
            public void keyPressed(final KeyEvent event) {
                // nothing todo
            }

            @Override
            public void keyReleased(final KeyEvent event) {
                try {
                    ampTurnsField.setNumberToField(initCurrentField.getNumberFromField() * windingsField.getNumberFromField());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        pIN2.add(initCurrentField);
        
        JLabel jLabAmpTurns = labelFabric("Initial AmpTurns =");
        pIN2.add(jLabAmpTurns);
        pIN2.add(ampTurnsField);
        pIN2.add(reverseInput);
        pIN2.add(reverseOutput);
        ampTurnsField.setNumberToField(element._initCurrent.getValue() * element._windings.getValue());
        ampTurnsField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(final KeyEvent event) {
            }

            @Override
            public void keyPressed(final KeyEvent event) {
            }

            @Override
            public void keyReleased(final KeyEvent event) {
                try {
                    if (windingsField.getNumberFromField() != 0) {
                        initCurrentField.setNumberToField(ampTurnsField.getNumberFromField() / windingsField.getNumberFromField());
                    }
                } catch (Exception ex) {
                    // not output here. During typing, there may be unreadable numbers, e.g. "1e-" for 1e-10"
                }
            }
        });

        con.add(pIN2, BorderLayout.CENTER);

    }        
}

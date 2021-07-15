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
package ch.technokrat.gecko.geckocircuits.newscope;

import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import ch.technokrat.gecko.geckocircuits.allg.TechFormat;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

/**
 *
 * @author andy
 */
public final class PowerAnalysisPanel extends JPanel {

    private final JComboBox[] _comboU = new JComboBox[3];
    private final JComboBox[] _comboI = new JComboBox[3];
    private FormatJTextField[][] _pqTextFields;  // Textfelder fuer Leistungswerte A und B
    private final TechFormat _cf = new TechFormat();
    private final GridBagConstraints _gbc = new GridBagConstraints();
    private final String[] signalListe;

    public PowerAnalysisPanel(final AbstractDataContainer worksheet, final PowerAnalysisSettings powerAnalSettings) {
        super();
        JPanel[] pUIa = new JPanel[3];
        final String[] titles = new String[]{"A", "B", "C"};
        for(int i = 0; i < pUIa.length; i++) {
            pUIa[i] = new JPanel();
            pUIa[i].setLayout(new GridBagLayout());
            pUIa[i].setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), titles[i], 
                    TitledBorder.LEFT, TitledBorder.TOP));
        }
        
        //
        signalListe = new String[worksheet.getRowLength()+1];
        signalListe[0] = "deactivated";
        
        for(int i = 0; i < worksheet.getRowLength(); i++) {
            signalListe[i+1] = worksheet.getSignalName(i);
        }
        
        setComboUIActions(powerAnalSettings);
        
        setPQTextFields();


        _gbc.gridx = 0;
        _gbc.gridy = 0;
        final JLabel[][] powerAnalLabels = new JLabel[8][3];
        for (int i = 0; i < powerAnalLabels[0].length; i++) {
            powerAnalLabels[0][i] = new JLabel("u(t) = ");
            powerAnalLabels[1][i] = new JLabel("   P = ");
            powerAnalLabels[2][i] = new JLabel("   D = ");
            powerAnalLabels[3][i] = new JLabel("   lambda = ");
            powerAnalLabels[4][i] = new JLabel("i(t) = ");
            powerAnalLabels[5][i] = new JLabel("   Q = ");
            powerAnalLabels[6][i] = new JLabel("   S = ");
            powerAnalLabels[7][i] = new JLabel("   cos(p1) = ");

            for (int j = 0; j < powerAnalLabels.length; j++) {
                final JLabel tmp = powerAnalLabels[j][i];
                tmp.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
                tmp.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);

            }

        }

        /*
         * todo: refactor this mess with some more intelligent indexing!
         */
        for (int ii = 0; ii < 3; ii++) {
            _gbc.gridx = 0;
            _gbc.gridy = 0;
            pUIa[ii].add(powerAnalLabels[0][ii], _gbc);
            _gbc.gridx = 1;
            _gbc.gridy = 0;
            pUIa[ii].add(_comboU[ii], _gbc);
            _gbc.gridx = 2;
            _gbc.gridy = 0;
            pUIa[ii].add(powerAnalLabels[1][ii], _gbc);
            _gbc.gridx = 3;
            _gbc.gridy = 0;
            pUIa[ii].add(_pqTextFields[ii][0], _gbc);
            _gbc.gridx = 4;
            _gbc.gridy = 0;
            pUIa[ii].add(powerAnalLabels[2][ii], _gbc);
            _gbc.gridx = 5;
            _gbc.gridy = 0;
            pUIa[ii].add(_pqTextFields[ii][2], _gbc);
            _gbc.gridx = 6;
            _gbc.gridy = 0;
            pUIa[ii].add(powerAnalLabels[3][ii], _gbc);
            _gbc.gridx = 7;
            _gbc.gridy = 0;
            pUIa[ii].add(_pqTextFields[ii][4], _gbc);
            _gbc.gridx = 0;
            _gbc.gridy = 1;
            pUIa[ii].add(powerAnalLabels[4][ii], _gbc);
            _gbc.gridx = 1;
            _gbc.gridy = 1;
            pUIa[ii].add(_comboI[ii], _gbc);
            _gbc.gridx = 2;
            _gbc.gridy = 1;
            pUIa[ii].add(powerAnalLabels[5][ii], _gbc);
            _gbc.gridx = 3;
            _gbc.gridy = 1;
            pUIa[ii].add(_pqTextFields[ii][1], _gbc);
            _gbc.gridx = 4;
            _gbc.gridy = 1;
            pUIa[ii].add(powerAnalLabels[6][ii], _gbc);
            _gbc.gridx = 5;
            _gbc.gridy = 1;
            pUIa[ii].add(_pqTextFields[ii][3], _gbc);
            _gbc.gridx = 6;
            _gbc.gridy = 1;
            pUIa[ii].add(powerAnalLabels[7][ii], _gbc);
            _gbc.gridx = 7;
            _gbc.gridy = 1;
            pUIa[ii].add(_pqTextFields[ii][5], _gbc);
        }


        setLayout(new GridLayout(3, 1));
        for(int i = 0; i < 3; i++) {
            add(pUIa[i]);
        }

    }

    public void calculate(final AbstractDataContainer _worksheet, final double startTimeValue, final double stopTimeValue) {
        final PowerCalculatorSelectionIndex pcsi = new PowerCalculatorSelectionIndex(startTimeValue, stopTimeValue);
        pcsi.calculate(_worksheet);
    }

    private void setPQTextFields() {
        _pqTextFields = new FormatJTextField[3][6];
        for (int i = 0; i < _pqTextFields.length; i++) {
            for (int j = 0; j < _pqTextFields[0].length; j++) {
                final FormatJTextField textField = new FormatJTextField();
                _pqTextFields[i][j] = textField;
                textField.setColumns(10);
                textField.setEditable(false);
                textField.setFont(new java.awt.Font("Arial", 0, 11));
            }
        }
    }

    private void setComboUIActions(final PowerAnalysisSettings powerAnalSettings) {
        for (int i = 0; i < _comboU.length; i++) {
            final int index = i;
            _comboU[i] = new JComboBox(signalListe);

            if (powerAnalSettings._powerAnalVoltageIndices[i] >= 0) {
                _comboU[i].setSelectedIndex(powerAnalSettings._powerAnalVoltageIndices[i]);
            }

            _comboU[i].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent actionEvent) {
                    powerAnalSettings._powerAnalVoltageIndices[index] = _comboU[index].getSelectedIndex();
                }
            });

            _comboI[i] = new JComboBox(signalListe);

            if (powerAnalSettings._powerAnalCurrentIndices[i] >= 0) {
                _comboI[i].setSelectedIndex(powerAnalSettings._powerAnalCurrentIndices[i]);
            }

            _comboI[i].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent actionEvent) {
                    powerAnalSettings._powerAnalCurrentIndices[index] = _comboI[index].getSelectedIndex();
                }
            });

        }

    }

    public class PowerCalculatorSelectionIndex {

        /**
         * points to the right analysis selection (A, B, C, ...)
         */
        public final List<Integer> _selPowerAnal = new ArrayList<Integer>();
        public final List<Integer> _selCurrentInd = new ArrayList<Integer>();
        public final List<Integer> _selVoltageInd = new ArrayList<Integer>();
        public final double startTime;
        public final double stopTime;

        public PowerCalculatorSelectionIndex(final double rng1, final double rng2) {

            for (int i = 0; i < _comboU.length; i++) {
                if (_comboU[i].getSelectedIndex() > 0 && _comboI[i].getSelectedIndex() > 0) {
                    _selPowerAnal.add(i);
                    _selCurrentInd.add(_comboI[i].getSelectedIndex());
                    _selVoltageInd.add(_comboU[i].getSelectedIndex());
                }
            }

            startTime = rng1;
            stopTime = rng2;
        }

        public void calculate(final AbstractDataContainer worksheet) {
            try {
                final PowerCalculator ergP = PowerCalculator.calculatorFabric(worksheet, this);
                if (this._selCurrentInd.size() != 0) {  // dh. keine Leistungsberechnung durchzufuehren weil deaktiviert
                    
                    for (int i = 0; i < _pqTextFields.length; i++) {
                        for (int j = 0; j < _pqTextFields[0].length; j++) {
                            _pqTextFields[i][j].setText("    -");
                        }
                    }

                    int counter = 0;
                    for (int i : _selPowerAnal) {
                        _pqTextFields[i][0].setText(_cf.formatT(ergP.getPowerP(counter), TechFormat.FORMAT_AUTO));
                        _pqTextFields[i][1].setText(_cf.formatT(ergP.getPowerQ(counter), TechFormat.FORMAT_AUTO));
                        _pqTextFields[i][2].setText(_cf.formatT(ergP.getPowerD(counter), TechFormat.FORMAT_AUTO));
                        _pqTextFields[i][3].setText(_cf.formatT(ergP.getPowerS(counter), TechFormat.FORMAT_AUTO));
                        _pqTextFields[i][4].setText(_cf.formatT(ergP.getLambda(counter), TechFormat.FORMAT_AUTO));
                        _pqTextFields[i][5].setText(_cf.formatT(ergP.getCosPhi(counter), TechFormat.FORMAT_AUTO));
                        counter++;
                    }

                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}

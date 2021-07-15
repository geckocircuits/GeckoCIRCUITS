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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

class JPanelLossDataInterpolationSettings extends JPanel {

    private final JLabel _jlUTest = new JLabel("    U =");
    private final DialogVerlusteDetail _parentDialog;
    private final FormatJTextField _jtfTemperature = new FormatJTextField(100);
    private final JPanel _jpTt = new JPanel();
    private final FormatJTextField _jtfVoltage = new FormatJTextField(300);
    private final ActionListener _createCurveListener = new ActionListener() { 
        @Override
        public void actionPerformed(final ActionEvent actionEvent) {
            try {
                final double temperature = _jtfTemperature.getNumberFromField();
                final double uMeas = _jtfVoltage.getNumberFromField();
                _parentDialog.createTestCurve(temperature, uMeas);
            } catch (Exception exc) {
                System.out.println(exc.getMessage());                
            }
        }
    };

    JPanelLossDataInterpolationSettings(final DialogVerlusteDetail parentDialog) {
        super();
        _parentDialog = parentDialog;

        setLayouts();
        createTemperatureComponents();
        createVoltageComponents();
        add(_jpTt, BorderLayout.CENTER);
        JButton jbTest = GuiFabric.getJButton(I18nKeys.SHOW);
        jbTest.addActionListener(_createCurveListener);
        add(jbTest, BorderLayout.SOUTH);
    }

    private void setLayouts() {
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "Data Test", TitledBorder.LEFT, TitledBorder.TOP));
        this.setLayout(new BorderLayout());

        _jpTt.setLayout(new GridLayout(1, 2));

    }

    private void createTemperatureComponents() {
        JLabel jltx = new JLabel(I18nKeys.CHECK_CURVES_AT.getTranslation());
        jltx.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        add(jltx, BorderLayout.NORTH);

        JLabel jpTt1 = new JLabel("Tj [°C] = ");
        jpTt1.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);

        _jtfTemperature.addActionListener(_createCurveListener);
        _jpTt.add(jpTt1);
        _jpTt.add(_jtfTemperature);
    }

    private void createVoltageComponents() {
        _jtfVoltage.addActionListener(_createCurveListener);
        _jlUTest.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        _jpTt.add(_jlUTest);
        _jpTt.add(_jtfVoltage);
    }

    void setVoltageSelectionVisible(final boolean isVisible) {
        _jtfVoltage.setVisible(isVisible);
        _jlUTest.setVisible(isVisible);
    }
}

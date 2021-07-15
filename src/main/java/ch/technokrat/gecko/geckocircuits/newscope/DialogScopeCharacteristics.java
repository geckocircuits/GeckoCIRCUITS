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

import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.allg.TechFormat;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class DialogScopeCharacteristics extends GeckoDialog {

    private GridBagConstraints _gridBagConst = new GridBagConstraints();
    private final TechFormat _cf = new TechFormat();
    private FormatJTextField _jTextFieldStatus;  // Staus-Anzeige der Berechnung
    private final JPanelDialogRange _panelDialRange;
    private final PanelCharacteristicsResult _characErgPanel;
    private PowerAnalysisPanel _powAnalPanel;
    private JPanel _jPanCharacInfo;
    private JTabbedPane _charactTab;
    private JPanel _jPanPowAnalInfo;
    private JPanel _panelPowAnal;
    private JTabbedPane _powerAnalPane;
    private JPanel _jpCalc;
    private final AbstractDataContainer _worksheet;
    private JButton _jbCALC;
    private static final double LARGE_VAL = 1e99;
    private static final int TEXT_FIELD_COLS = 16;
    private static final int RECT_SIZE = 999;
    private static final int POW_AN_X = 379, POW_AN_Y = 224;
    private static final int CHAR_X_SIZE = 788, CHAR_Y_SIZE = 173;

    public DialogScopeCharacteristics(final JFrame parent, final AbstractDataContainer worksheet,
            final PowerAnalysisSettings powerAnalSettings, final double[] sliderValues) {
        super(parent, true);


        _worksheet = worksheet;
        setWindowProperties();
        _panelDialRange = new JPanelDialogRange(worksheet, sliderValues);
        _characErgPanel = new PanelCharacteristicsResult(worksheet);
        setPanelCharacteristicsInformation();
        setCharactericsticsTabber();
        setPanelPowerAnalysis(powerAnalSettings);

        setPowerAnalysisInformation();
        setPowerAnalysisTabber();
        setPanelCalcCancel();
        setPanelAll();
        this.getRootPane().setDefaultButton(_jbCALC);
    }

    private void setWindowProperties() {
        setTitle(" Characteristics & Power Analysis");
        getContentPane().setLayout(new BorderLayout());
        _gridBagConst = new GridBagConstraints();
        _gridBagConst.fill = GridBagConstraints.BOTH;
    }

    private void setPanelCharacteristicsInformation() {
        _jPanCharacInfo = new JPanel();

        final JComponent comp = new JComponent() {

            @Override
            public void paintComponent(final java.awt.Graphics graphics) {
                try {
                    graphics.setColor(java.awt.Color.white);
                    graphics.fillRect(0, 0, RECT_SIZE, RECT_SIZE);  // weisser Hintergrund
                    java.awt.Image equ1 = new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "equ1.png")).getImage();
                    graphics.drawImage(equ1, 0, 0, null);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        };
        comp.setPreferredSize(new java.awt.Dimension(CHAR_X_SIZE, CHAR_Y_SIZE));
        _jPanCharacInfo.add(comp);
    }

    private void setCharactericsticsTabber() {
        _charactTab = new JTabbedPane();
        _charactTab.addTab("Characteristics", _characErgPanel);
        _charactTab.addTab("Definitions", _jPanCharacInfo);
    }

    private void setPowerAnalysisInformation() {
        _jPanPowAnalInfo = new JPanel();
        final JComponent comp = new JComponent() {

            @Override
            public void paintComponent(final java.awt.Graphics graphics) {
                try {
                    graphics.setColor(java.awt.Color.white);
                    graphics.fillRect(0, 0, RECT_SIZE, RECT_SIZE);  // weisser Hintergrund
                    java.awt.Image equ1 = new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "equ2b.png")).getImage();
                    graphics.drawImage(equ1, 0, 0, null);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        };
        comp.setPreferredSize(new java.awt.Dimension(POW_AN_X, POW_AN_Y));
        _jPanPowAnalInfo.add(comp);

    }

    private void setPowerAnalysisTabber() {
        _powerAnalPane = new JTabbedPane();
        _powerAnalPane.addTab("Power Analysis", _panelPowAnal);
        _powerAnalPane.addTab("Definitions", _jPanPowAnalInfo);
    }

    private void setPanelPowerAnalysis(final PowerAnalysisSettings powerAnalSettings) {
        _panelPowAnal = new JPanel();
        _panelPowAnal.setLayout(new BorderLayout());
        _powAnalPanel = new PowerAnalysisPanel(_worksheet, powerAnalSettings);
        _panelPowAnal.add(_powAnalPanel, BorderLayout.NORTH);

    }

    private void setPanelCalcCancel() {
        final JPanel pOK = new JPanel();
        _jbCALC = GuiFabric.getJButton(I18nKeys.CALCULATE);
        _jbCALC.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                final double xDef1 = _panelDialRange.getStartTimeValue();
                final double xDef2 = _panelDialRange.getStopTimeValue();
                final Thread rechner = new CalculationThread();
                rechner.setPriority(Thread.MIN_PRIORITY);
                rechner.start();
            }
        });
        final JButton jbCancel = GuiFabric.getJButton(I18nKeys.CANCEL);
        jbCancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                dispose();
            }
        });
        pOK.add(_jbCALC);
        pOK.add(jbCancel);
        _jpCalc = new JPanel();
        _jpCalc.setLayout(new BorderLayout());
        _jpCalc.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "Status", TitledBorder.LEFT, TitledBorder.TOP));
        _jTextFieldStatus = new FormatJTextField();
        _jTextFieldStatus.setColumns(TEXT_FIELD_COLS);
        _jTextFieldStatus.setEditable(false);
        _jpCalc.add(pOK, BorderLayout.SOUTH);
        _jpCalc.add(_jTextFieldStatus, BorderLayout.NORTH);
    }

    private void setPanelAll() {
        final JPanel pALL = new JPanel();
        pALL.setLayout(new java.awt.GridBagLayout());
        pALL.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "",
                TitledBorder.LEFT, TitledBorder.TOP));
        _gridBagConst.fill = GridBagConstraints.BOTH;
        _gridBagConst.gridx = 0;
        _gridBagConst.gridy = 0;
        _gridBagConst.gridwidth = 2;
        _gridBagConst.gridheight = 1;
        pALL.add(_charactTab, _gridBagConst);
        _gridBagConst.gridx = 0;
        _gridBagConst.gridy = 1;
        _gridBagConst.gridwidth = 1;
        _gridBagConst.gridheight = 1;
        pALL.add(_panelDialRange, _gridBagConst);
        _gridBagConst.gridx = 0;
        _gridBagConst.gridy = 2;
        _gridBagConst.gridwidth = 1;
        _gridBagConst.gridheight = 1;
        pALL.add(_jpCalc, _gridBagConst);
        _gridBagConst.gridx = 1;
        _gridBagConst.gridy = 1;
        _gridBagConst.gridwidth = 1;
        _gridBagConst.gridheight = 2;
        pALL.add(_powerAnalPane, _gridBagConst);
        //
        getContentPane().add(pALL);

        this.pack();
        this.setResizable(false);

    }

    class CalculationThread extends Thread {

        @Override
        public void run() {
            try {
                _jbCALC.setEnabled(false);
                _jTextFieldStatus.setForeground(Color.blue);
                _jTextFieldStatus.setText("Calculation running ... ");



                final CharacteristicsCalculator characteristics = CharacteristicsCalculator.calculateFabric(_worksheet,
                        _panelDialRange.getStartTimeValue(), _panelDialRange.getStopTimeValue());

                for (int i1 = 0; i1 < _worksheet.getRowLength(); i1++) {
                    _characErgPanel.setAvgText(_cf.formatT(characteristics.getAVGValue(i1), TechFormat.FORMAT_AUTO), i1);
                    _characErgPanel.setRmsText(_cf.formatT(characteristics.getRMS2Value(i1), TechFormat.FORMAT_AUTO), i1);
                    if (characteristics.getMinValue(i1) == LARGE_VAL) {
                        _characErgPanel.setMinText(_cf.formatT(0, TechFormat.FORMAT_AUTO), i1);
                    } else {
                        _characErgPanel.setMinText(_cf.formatT(characteristics.getMinValue(i1), TechFormat.FORMAT_AUTO), i1);
                    }

                    if (characteristics.getMaxValue(i1) == -LARGE_VAL) {
                        _characErgPanel.setMaxText(_cf.formatT(0, TechFormat.FORMAT_AUTO), i1);
                    } else {
                        _characErgPanel.setMaxText(_cf.formatT(characteristics.getMaxValue(i1), TechFormat.FORMAT_AUTO), i1);
                    }

                    _characErgPanel.setCRestText(_cf.formatT(characteristics.getKlirrValue(i1), TechFormat.FORMAT_AUTO), i1);
                    _characErgPanel.setShapeText(_cf.formatT(characteristics.getShapeValue(i1), TechFormat.FORMAT_AUTO), i1);
                    _characErgPanel.setThdText(_cf.formatT(characteristics.getTHDValue(i1), TechFormat.FORMAT_AUTO), i1);
                    _characErgPanel.setRippleText(_cf.formatT(characteristics.getRippleValue(i1), TechFormat.FORMAT_AUTO), i1);
                    _characErgPanel.setPeakPeakText(_cf.formatT(characteristics.getPeakToPeakValue(i1), TechFormat.FORMAT_AUTO), i1);
                }



                _powAnalPanel.calculate(_worksheet, _panelDialRange.getStartTimeValue(),
                        _panelDialRange.getStopTimeValue());
                _jTextFieldStatus.setForeground(Color.black);
                _jTextFieldStatus.setText("Calculation OK");

            } catch (IndexOutOfBoundsException ex) {
                ex.printStackTrace();
                _jTextFieldStatus.setForeground(Color.red);
                _jTextFieldStatus.setText(ex.getMessage());

            } catch (Exception exc) {
                exc.printStackTrace();
                _jTextFieldStatus.setForeground(Color.red);
                _jTextFieldStatus.setText("Data-Error");
            } finally {
                _jbCALC.setEnabled(true);
            }
        }
    };
}

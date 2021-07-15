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

import ch.technokrat.gecko.geckocircuits.scope.DialogFourierDiagramm;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import ch.technokrat.gecko.geckocircuits.allg.TechFormat;
import ch.technokrat.gecko.geckocircuits.scope.FourierPlotFrame;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import javax.swing.JCheckBox;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import java.net.URL;
import javax.swing.JOptionPane;

public class DialogFourier extends JDialog {

    //-------------
    private AbstractDataContainer worksheet;
    private boolean[] signalFourierAnalysiert;  // gibt zu jedem Header (= Signalnamen) an, ob Fourier-Analyse durchgefuehrt wurde
    private GridBagConstraints gbc = new GridBagConstraints();
    //-------------
    private TechFormat cf = new TechFormat();
    private FormatJTextField rngSc1, rngSc2, rngDf1, rngDf2, rngSl1, rngSl2;  // Angaben Zeitbereiche
    private FormatJTextField ftfnMax, ftff1;  // Textfelder fuer Fourier-Daten
    private double f1;  // Grundfrequenz fuer Fourieranalyse
    private int nMin, nMax;  // Grundfrequenz-Vielfache fuer Fourieranalyse
    private JCheckBox[] jcbZV;   // Auswahl der ZV-Kurven, die Fourier-analysiert werden soll
    private JButton jbCALC;  // Berechnung starten
    //-------------
    //-------------
    //-------------
    public int NN;
    private final JPanelDialogRange _jPanelRange;
    private final NewScope _newScope;

    public DialogFourier(AbstractDataContainer worksheet, double[] sliderValues, final NewScope newScope) {
        _newScope = newScope;
        super.setModal(true);
        try {
            this.setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif"))).getImage());
        } catch (Exception e) {
        }
        this.worksheet = worksheet;
        signalFourierAnalysiert = new boolean[worksheet.getRowLength() + 1];
        //--------------------------
        nMin = 0;
        nMax = 100;  // default
        //--------------------------
        this.setTitle(" Fourier-Transform");
        this.getContentPane().setLayout(new BorderLayout());
        _jPanelRange = new JPanelDialogRange(worksheet, sliderValues);
        this.baueGUI();

        _jPanelRange.registerActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                f1 = 1.0 / (_jPanelRange.getStopTimeValue() - _jPanelRange.getStartTimeValue());
                ftff1.setText(cf.formatT(f1, TechFormat.FORMAT_AUTO));
            }
        });

        this.pack();
        this.setResizable(false);
        //------------------------
    }

    private void baueGUI() {
        //------------------
        Container con = this.getContentPane();
        con.setLayout(new BorderLayout());
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        int cols = 7;


        //
        int maximumFractionDigits = 9;
        JPanel pHAR = new JPanel();
        pHAR.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);

        pHAR.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Harmonics", TitledBorder.LEFT, TitledBorder.TOP));
        //
        Dimension tfDimension = new Dimension(100, 15);
        JLabel jlH2 = new JLabel("f_1 [Hz] =  ");
        jlH2.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
        jlH2.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        c.gridy = 0;
        c.gridx = 0;
        pHAR.add(jlH2, c);
        //
        ftff1 = new FormatJTextField();
        ftff1.setPreferredSize(tfDimension);
        ftff1.setText(cf.formatT((1.0 / (_jPanelRange.getStopTimeValue() - _jPanelRange.getStartTimeValue())), TechFormat.FORMAT_AUTO));
        ftff1.setEditable(false);

        c.gridy = 0;
        c.gridx = 1;
        pHAR.add(ftff1, c);

        //
        JLabel jlH3 = new JLabel("n_min =  ");
        jlH3.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
        jlH3.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);

        JLabel jlH4 = new JLabel("n_max =  ");
        jlH4.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
        jlH4.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);

        c.gridy = 1;
        c.gridx = 0;
        pHAR.add(jlH4, c);
        //
        ftfnMax = new FormatJTextField();
        ftfnMax.setPreferredSize(tfDimension);
        ftfnMax.setText(nMax + "");

        c.gridx = 1;
        pHAR.add(ftfnMax, c);

        //===========================================================
        //===========================================================
        JPanel pSEL = new JPanel();
        pSEL.setLayout(new GridBagLayout());
        pSEL.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Select Curve(s)", TitledBorder.LEFT, TitledBorder.TOP));
        //
        jcbZV = new JCheckBox[worksheet.getRowLength()];
        gbc.fill = gbc.BOTH;
        for (int i1 = 1; i1 < worksheet.getRowLength() + 1; i1++) {
            gbc.gridx = 0;
            gbc.gridy = i1 - 1;
            jcbZV[i1 - 1] = new JCheckBox();
            if (i1 == 1) {
                jcbZV[i1 - 1].setSelected(true);  // default-init
            }
            jcbZV[i1 - 1].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                }
            });
            pSEL.add(jcbZV[i1 - 1], gbc);
            //
            gbc.gridx = 1;
            gbc.gridy = i1 - 1;
            JLabel jlZV = new JLabel(worksheet.getSignalName(i1 - 1));
            jlZV.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
            jlZV.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
            pSEL.add(jlZV, gbc);
        }

        final JDialog ich = this;  // fuer Referenz in innerer Klasse
        //
        JPanel pOK = new JPanel();
        jbCALC = GuiFabric.getJButton(I18nKeys.CALCULATE);
        jbCALC.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                f1 = 1.0 / (_jPanelRange.getStopTimeValue() - _jPanelRange.getStartTimeValue());
                ftff1.setText(cf.formatT(f1, TechFormat.FORMAT_AUTO));

                nMin = 0;  // (int)ftfnMin.getNumberFromField();
                nMax = (int) ftfnMax.getNumberFromField();
                if ((nMin < 0) || (nMax < 0) || (nMin > nMax)) {
                    return;
                }
                if (f1 <= 0) {
                    return;
                }
                //----------------------------------
                Thread rechner = new Thread() {
                    private double[][][] erg;

                    public void run() {
                        jbCALC.setEnabled(false);  // damit man nicht mehrere Berechnungen durch versehentliches Druecken startet
                        try {
                            erg = calculate();
                            //-----------------
                            jbCALC.setEnabled(true);
                            if (0 > 1) {
                                FourierPlotFrame plotFrame = new FourierPlotFrame(_newScope, f1, erg);
                                DialogFourier.this.setVisible(false);
                                plotFrame.setVisible(true);
                            }
                            //-----------------
                            // fertige Grafik nach Rechenende hochfahren ..
                            DialogFourierDiagramm diagramm = new DialogFourierDiagramm(
                                    erg, signalFourierAnalysiert, nMin, f1, worksheet, _jPanelRange.getStartTimeValue(),
                                    _jPanelRange.getStopTimeValue());
                            diagramm.setLocationRelativeTo(ich);
                            diagramm.setVisible(true);
                            //-----------------
                        } catch (java.lang.OutOfMemoryError er) {
                            JOptionPane.showMessageDialog(null,
                                    "Could not allocate enough memory for Fourier transformation!",
                                    "Memory error!",
                                    JOptionPane.ERROR_MESSAGE);
                        } catch (Error e0) {
                            e0.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                            jbCALC.setEnabled(true);
                        }
                    }
                };
                //----------------------------------
                rechner.setPriority(Thread.MIN_PRIORITY);
                rechner.start();
                //
            }
        });
        JButton jbCancel = GuiFabric.getJButton(I18nKeys.CANCEL);
        jbCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                dispose();
            }
        });
        pOK.add(jbCALC);
        pOK.add(jbCancel);
        JPanel jpCalc = new JPanel();
        jpCalc.setLayout(new BorderLayout());
        jpCalc.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Calculate", TitledBorder.LEFT, TitledBorder.TOP));
        jpCalc.add(pOK, BorderLayout.SOUTH);
        //
        //===========================================================
        //===========================================================
        JPanel jpERGx = new JPanel();
        jpERGx.setLayout(new BorderLayout());
        jpERGx.add(_jPanelRange, BorderLayout.NORTH);
        jpERGx.add(pHAR, BorderLayout.CENTER);
        //
        JPanel jpERGx2 = new JPanel();
        jpERGx2.setLayout(new BorderLayout());
        jpERGx2.add(pSEL, BorderLayout.NORTH);
        jpERGx2.add(jpCalc, BorderLayout.SOUTH);
        //
        JPanel pALL = new JPanel();
        pALL.setLayout(new BorderLayout());
        pALL.add(jpERGx, BorderLayout.WEST);
        pALL.add(jpERGx2, BorderLayout.EAST);
        con.add(pALL);
        //===========================================================
        //===========================================================
    }

    private double[][][] calculate() throws Exception, Error {  // eventuell OutOfMemoryError bei zuvielen Oberschwingungen

        double[][] an = new double[worksheet.getRowLength()][nMax - nMin + 1];
        double[][] bn = new double[worksheet.getRowLength()][nMax - nMin + 1];
        double[][] cn = new double[worksheet.getRowLength()][nMax - nMin + 1];  // Amplitude
        double[][] jn = new double[worksheet.getRowLength()][nMax - nMin + 1];  // Winkel [rad]

        for (int i1 = 0; i1 < an.length; i1++) {
            for (int i2 = 0; i2 < an[0].length; i2++) {
                an[i1][i2] = 0;
                bn[i1][i2] = 0;
                cn[i1][i2] = 0;
                jn[i1][i2] = 0;
            }
        }

        double rng1 = _jPanelRange.getStartTimeValue();
        double rng2 = _jPanelRange.getStopTimeValue();

        int i1 = 0;
        while (worksheet.getTimeValue(i1, 0) <= rng1) {
            i1++;
        }
        int startIndex = i1;

        int stopIndex = 0;
        while ((i1 < worksheet.getMaximumTimeIndex(0)) && (worksheet.getTimeValue(i1 + 1, 0) > worksheet.getTimeValue(i1, 0))
                && (rng1 <= worksheet.getTimeValue(i1, 0)) && (worksheet.getTimeValue(i1, 0) <= rng2)) {  // Schleife Zeitbereich [t1...t2]
            stopIndex = i1;
            i1++;
        }

        int numberOfSamples = stopIndex - startIndex;
        NN = 1;
        while (NN < numberOfSamples) {
            NN *= 2;
        }

        if (NN > numberOfSamples) {
            NN /= 2;
        }

        float stopTime = (float) worksheet.getTimeValue(stopIndex, 0);
        float startTime = (float) worksheet.getTimeValue(startIndex, 0);

        double timeSpan = stopTime - startTime;


        i1 = startIndex;
        double dT = rng2 - rng1;
        double dt = worksheet.getTimeValue(i1 + 1, 0) - worksheet.getTimeValue(i1, 0);
        //-------------------
        // Rechnen bis zum Endpunkt:
        double q = 2 * Math.PI * f1;  // Hilfskonstante
        while ((i1 < worksheet.getMaximumTimeIndex(0)) && (worksheet.getTimeValue(i1 + 1, 0) > worksheet.getTimeValue(i1, 0))
                && (rng1 <= worksheet.getTimeValue(i1, 0)) && (worksheet.getTimeValue(i1, 0) <= rng2)) {  // Schleife Zeitbereich [t1...t2]
            try {
                dt = worksheet.getTimeValue(i1 + 1, 0) - worksheet.getTimeValue(i1, 0);
            } catch (Exception e) {
            }  // wenn wir ganz am Ende sind --> Exception --> altes 'dt' wird verwendet
            for (int i2 = 1; i2 < worksheet.getRowLength() + 1; i2++) {  // Schleife ueber alle Fourier-zu-zerlegenden Kurven
                if (jcbZV[i2 - 1].isSelected()) {
                    signalFourierAnalysiert[i2] = true;
                    double wert = worksheet.getValue(i2 - 1, i1);
                    double arg = q * worksheet.getTimeValue(i1, 0);
//                    for (int n = nMin; n <= nMax; n++) {  // Schleife ueber alle Grundfrequenz-Vielfachen [nMin...nMax]
//                        an[i2 - 1][n - nMin] += (wert * Math.cos(arg * n) * dt) / (0.5 * dT);
//                        bn[i2 - 1][n - nMin] += (wert * Math.sin(arg * n) * dt) / (0.5 * dT);
//                    }
                } else {
                    signalFourierAnalysiert[i2] = false;
                }
            }
            i1++;
        }

        for (int i2 = 1; i2 < worksheet.getRowLength() + 1; i2++) {  // Schleife ueber alle Fourier-zu-zerlegenden Kurven
            if (jcbZV[i2 - 1].isSelected()) {
                float[] data = new float[NN];
                int j = startIndex;
                for (int i = 0; i < NN; i++) {
                    while (worksheet.getTimeValue(j, 0) < startTime + i * timeSpan / NN) {
                        j++;
                    }
                    data[i] = (float) worksheet.getValue(i2 - 1, j);
                }

                Cispr16Fft.realft(data, 1);
                for (int n = nMin; n <= nMax; n++) {
                    an[i2 - 1][n - nMin] = data[2 * n] / (NN / 2);
                    bn[i2 - 1][n - nMin] = data[2 * n + 1] / (NN / 2);
                }
            }
        }

        //-------------------
        // Auswertung:

        for (int i2 = 1; i2 < worksheet.getRowLength() + 1; i2++) {
            for (int n = nMin; n <= nMax; n++) {

                if (n == 0) {  // DC-Gleichanteil
                    cn[i2 - 1][n - nMin] = 0.5 * an[i2 - 1][n - nMin];
                    jn[i2 - 1][n - nMin] = 0;
                } else {
                    cn[i2 - 1][n - nMin] = Math.sqrt(an[i2 - 1][n - nMin] * an[i2 - 1][n - nMin] + bn[i2 - 1][n - nMin] * bn[i2 - 1][n - nMin]);
                    jn[i2 - 1][n - nMin] = Math.atan2(an[i2 - 1][n - nMin], bn[i2 - 1][n - nMin]);
                }
            }
        }


        double[][][] erg = new double[4][][];
        erg[0] = an;
        erg[1] = bn;
        erg[2] = cn;
        erg[3] = jn;
        return erg;
    }
}

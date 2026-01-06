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
package ch.technokrat.gecko.geckocircuits.scope;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import ch.technokrat.gecko.geckocircuits.allg.GeckoFileChooser;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

@SuppressWarnings({"deprecation", "serial", "this-escape"})
public class DialogFourierDiagramm extends JDialog implements ComponentListener {

    //-------------------
    private JTabbedPane tabber;
    private FourierDiagramm[] diag;
    private DisplayFourierWorksheet[] sheet;
    private FourierKurvenRekonstruktion[] rekonstruktion;
    //
    private final AbstractDataContainer _worksheet;
    private final double[][] _cn, _jn, _an, _bn;
    private final double _f1;
    private final int _nMin;
    private final double _rng1, _rng2;
    private final boolean[] _signalFourierAnalysiert;
    //
    private int mausModus = GraferImplementation.MAUSMODUS_NIX;
    private int iconONnummerALT = 0;
    //-------------------
    private ImageIcon[] iconOFF, iconON;
    private JButton[] jbMaus;
    private JToolBar jtb1;
    private JMenuItem mItemF3;
    //-----------------------

    public DialogFourierDiagramm(
            double[][][] erg, boolean[] sngFourierAnalysiert, int nMin, double f1, AbstractDataContainer worksheet,
            double rng1, double rng2) {
        super.setModal(true);
        try {
            URL picsUrl = GlobalFilePathes.PFAD_PICS_URL;
            setIconImage(new ImageIcon(new URL(picsUrl, "gecko.gif")).getImage());
        } catch (Exception e) {
        }
        _an = erg[0];
        _bn = erg[1];
        _cn = erg[2];
        _jn = erg[3];
        _signalFourierAnalysiert = sngFourierAnalysiert;
        _nMin = nMin;
        _f1 = f1;
        _rng1 = rng1;
        _rng2 = rng2;
        _worksheet = worksheet;

        setTitle("Diagram Fourier-Transform");
        getContentPane().setLayout(new BorderLayout());
        baueGUItoolbar();
        baueGUI();
        pack();
        addComponentListener(this);
    }

    private void baueGUI() {
        tabber = new JTabbedPane();
        diag = new FourierDiagramm[_worksheet.getRowLength()+1];
        sheet = new DisplayFourierWorksheet[_worksheet.getRowLength()+1];
        rekonstruktion = new FourierKurvenRekonstruktion[_worksheet.getRowLength()+1];
        //
        for (int i1 = 1; i1 < _worksheet.getRowLength()+1; i1++) {
            if (_signalFourierAnalysiert[i1]) {
                JPanel jpSg = new JPanel();
                jpSg.setLayout(new BorderLayout());
                JTabbedPane tabberLok = new JTabbedPane();
                //---------
                diag[i1 - 1] = new FourierDiagramm(_cn[i1 - 1], _nMin, _f1); 
                tabberLok.addTab("Fourier Analysis", diag[i1 - 1]); 
                sheet[i1 - 1] = new DisplayFourierWorksheet(_cn[i1 - 1], _jn[i1 - 1], _nMin);
                tabberLok.addTab("Worksheet Data", sheet[i1 - 1]);
                rekonstruktion[i1 - 1] = new FourierKurvenRekonstruktion(_an[i1 - 1], _bn[i1 - 1], _nMin, _f1, _worksheet, i1, _rng1, _rng2);
                tabberLok.addTab("Reconstruction", rekonstruktion[i1 - 1]);
                //---------
                jpSg.add(tabberLok, BorderLayout.CENTER);
                tabber.addTab(_worksheet.getSignalName(i1-1) + " ", jpSg);
            }
        }
        //------------------------
        JPanel jpIN = new JPanel();  // User-Input:  n-Bereich-Einschraenkung, OK usw ...
        //------------------------
        Container con = this.getContentPane();
        con.setLayout(new BorderLayout());
        con.add(tabber, BorderLayout.CENTER);
        con.add(jpIN, BorderLayout.SOUTH);
        con.add(jtb1, BorderLayout.WEST);
        //=======================================
        //-----------
        JMenu dataMenu = GuiFabric.getJMenu(I18nKeys.FILE);
        mItemF3 = GuiFabric.getJMenuItem(I18nKeys.WRITE_DATA_TO_FILE);
        mItemF3.setMnemonic(KeyEvent.VK_S);
        mItemF3.addActionListener((ActionEvent ae) -> {
            GeckoFileChooser fileChooser = GeckoFileChooser.createSimpleSaveFileChooser(".dat", DialogFourierDiagramm.this);
            if (fileChooser.getUserResult() == GeckoFileChooser.FileChooserResult.CANCEL) {
                return;
            }

            StringBuffer sb = new StringBuffer();
            sb.append("N ");
            for (int i1 = 1; i1 < _worksheet.getRowLength()+1; i1++) {
                if (_signalFourierAnalysiert[i1]) {
                    sb.append(_worksheet.getSignalName(i1-1) + "_cN " + _worksheet.getSignalName(i1-1) + "_phiN[rad] ");
                }
            }
            sb.append("\n");
            for (int i1 = 0; i1 < _cn[0].length; i1++) {
                sb.append(i1 + " ");
                for (int i2 = 1; i2 < _worksheet.getRowLength()+1; i2++) {
                    if (_signalFourierAnalysiert[i2]) {
                        sb.append(_cn[i2 - 1][i1] + " " + _jn[i2 - 1][i1] + " ");
                    }

                }
                sb.append("\n");
            }
            try {
                BufferedWriter fkaku = new BufferedWriter(new FileWriter(fileChooser.getFileWithCheckedEnding()));
                fkaku.write(sb.toString());
                fkaku.flush();
                fkaku.close();
            } catch (Exception e) {
                System.out.println(e + "   qe90r8gn03g8q");
            }
        });
        JMenuItem mItemF5 = GuiFabric.getJMenuItem(I18nKeys.EXIT);
        mItemF5.setMnemonic(KeyEvent.VK_X);
        mItemF5.addActionListener((ActionEvent ae) -> DialogFourierDiagramm.this.dispose());
        dataMenu.add(mItemF3);
        mItemF3.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        dataMenu.add(mItemF5);
        mItemF5.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        //
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(dataMenu);
        this.setJMenuBar(menuBar);
        //=======================================
    }

    private void baueGUItoolbar() {
        //--------------------
        try {
            String[] iconFiles = {"iconON_off.png", "iconON_zoomFit2.png", "iconON_zoomFenster.png", 
                                  "iconON_getXYschieber.png", "iconON_log.png"};
            
            iconON = new ImageIcon[iconFiles.length];
            for (int i = 0; i < iconFiles.length; i++) {
                URL iconUrl = DialogFourierDiagramm.class.getResource("/gecko/geckocircuits/allg/" + iconFiles[i]);
                iconON[i] = new ImageIcon(iconUrl);
                if (iconON[i].getImageLoadStatus() != java.awt.MediaTracker.COMPLETE) {
                    Logger.getLogger(DialogFourierDiagramm.class.getName()).log(Level.WARNING, 
                        "Failed to load icon: " + iconFiles[i]);
                }
            }
            
            String[] iconFilesOFF = {"iconOFF_off.png", "iconOFF_zoomFit2.png", "iconOFF_zoomFenster.png",
                                      "iconOFF_getXYschieber.png", "iconOFF_log.png"};
            
            iconOFF = new ImageIcon[iconFilesOFF.length];
            for (int i = 0; i < iconFilesOFF.length; i++) {
                URL iconUrl = DialogFourierDiagramm.class.getResource("/gecko/geckocircuits/allg/" + iconFilesOFF[i]);
                iconOFF[i] = new ImageIcon(iconUrl);
                if (iconOFF[i].getImageLoadStatus() != java.awt.MediaTracker.COMPLETE) {
                    Logger.getLogger(DialogFourierDiagramm.class.getName()).log(Level.WARNING, 
                        "Failed to load icon: " + iconFilesOFF[i]);
                }
            }
        } catch (Exception e) {
            Logger.getLogger(DialogFourierDiagramm.class.getName()).log(Level.WARNING, e.getMessage());
        }
        //
        jbMaus = new JButton[iconOFF.length];
        for (int i1 = 0; i1 < jbMaus.length; i1++) {
            jbMaus[i1] = new JButton();
            jbMaus[i1].setIcon(iconOFF[i1]);
            jbMaus[i1].setActionCommand("mausModus " + i1);
            jbMaus[i1].addActionListener((final ActionEvent actionEvent) -> aktualisiereMausModus(actionEvent));

            //--------------------
            switch (i1) {
                case 0:
                    jbMaus[i1].setToolTipText("Deactivate mouse");
                    break;
                case 1:
                    jbMaus[i1].setToolTipText("Autoscale");
                    break;
                case 2:
                    jbMaus[i1].setToolTipText("Zoom rectangle");
                    break;
                case 3:
                    jbMaus[i1].setToolTipText("Set slider for X/Y-values");
                    break;
                case 4:
                    jbMaus[i1].setToolTipText("set logarithmic y-axis");
                    break;
                default:
                    Logger.getLogger(DialogFourierDiagramm.class.getName()).log(Level.WARNING, "Error: 49ugnw3grjgtfzj");
                    break;
            }
            //--------------------
        }
        jbMaus[0].setIcon(iconON[0]);  // default: Maus deaktiviert
        
        //--------------------
        jtb1 = new JToolBar("Mouse Options");
        jtb1.setOrientation(JToolBar.VERTICAL);
        jtb1.setFloatable(false);
        for (int i1 = 0; i1 < jbMaus.length; i1++) {
            jtb1.add(jbMaus[i1]);
        }
        //--------------------
    }

    private int yAchseTyp = GraferV3.ACHSE_LIN;

    private void aktualisiereMausModus(final ActionEvent actionEvent) {
        //--------------------
        // alten Zustand speichern: 
        int mausModusALT = -1;
        //--------------------
        final StringTokenizer stk = new StringTokenizer(actionEvent.getActionCommand(), " ");
        stk.nextToken();
        final int indexKnopfGedrueckt = Integer.valueOf(stk.nextToken());
        for (int i1 = 0; i1 < jbMaus.length; i1++) {
            jbMaus[i1].setIcon(iconOFF[i1]);
        }
        jbMaus[indexKnopfGedrueckt].setIcon(iconON[indexKnopfGedrueckt]);
        //--------------------
        switch (indexKnopfGedrueckt) {
            case 0:
                mausModus = GraferImplementation.MAUSMODUS_NIX;
                break;
            case 1:
                mausModusALT = mausModus;
                mausModus = GraferImplementation.MAUSMODUS_ZOOM_AUTOFIT;
                break;
            case 2:
                mausModus = GraferImplementation.MAUSMODUS_ZOOM_FENSTER;
                break;
            case 3:
                mausModus = GraferImplementation.MAUSMODUS_WERTANZEIGE_SCHIEBER;
                break;
            case 4:
                if(yAchseTyp == GraferV3.ACHSE_LIN) {
                   yAchseTyp = GraferV3.ACHSE_LOG;
                   jbMaus[4].setIcon(iconON[4]);
                } else {
                    yAchseTyp = GraferV3.ACHSE_LIN;
                    jbMaus[4].setIcon(iconOFF[4]);
                }

                for (int i = 0; i < diag.length; i++) {
                    if (diag[i] != null) {
                        diag[i].setzeAchsenTyp(new int[]{GraferV3.ACHSE_LIN}, new int[]{yAchseTyp});
                        diag[i].repaint();
                    }
                    

                }
                
                break;
            default:
                Logger.getLogger(DialogFourierDiagramm.class.getName()).log(Level.WARNING, "Fehler: 98n3gweggtq5t");                
                break;
        }
        for (int i1 = 1; i1 < _worksheet.getRowLength()+1; i1++) {
            if (_signalFourierAnalysiert[i1]) {
                diag[i1 - 1].setMausModus(mausModus);
                rekonstruktion[i1 - 1].setMausModus(mausModus);
            }
        }
        if (mausModus == GraferImplementation.MAUSMODUS_ZOOM_AUTOFIT) {
            mausModus = mausModusALT;
            jbMaus[1].setIcon(iconOFF[1]);
            jbMaus[iconONnummerALT].setIcon(iconON[iconONnummerALT]);
            for (int i1 = 1; i1 < _worksheet.getRowLength()+1; i1++) {
                if (_signalFourierAnalysiert[i1]) {
                    diag[i1 - 1].setMausModus(mausModus);
                    rekonstruktion[i1 - 1].setMausModus(mausModus);
                }
            }
        } else {
            iconONnummerALT = indexKnopfGedrueckt;
        }
    }

    @Override
    public void componentResized(final ComponentEvent compEvent) {
        for (int i1 = 1; i1 < _worksheet.getRowLength()+1; i1++) {
            if (_signalFourierAnalysiert[i1]) {
                diag[i1 - 1].resize();
                diag[i1 - 1].repaint();
                rekonstruktion[i1 - 1].resize();
                rekonstruktion[i1 - 1].repaint();
            }
        }
    }

    @Override
    public void componentMoved(final ComponentEvent compEvent) {
        // nothing todo here
    }

    @Override
    public void componentShown(final ComponentEvent compEvent) {
        // nothing todo here
    }

    @Override
    public void componentHidden(final ComponentEvent compEvent) {
        // nothing todo here
    }
}



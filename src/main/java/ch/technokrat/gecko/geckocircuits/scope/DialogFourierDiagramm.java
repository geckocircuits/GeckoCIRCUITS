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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressWarnings({"deprecation", "serial", "this-escape"})
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Dialog stores Fourier data arrays and worksheet reference for analysis")
public class DialogFourierDiagramm extends JDialog implements ComponentListener {

    //-------------------
    private JTabbedPane tabbedPane;
    private FourierDiagramm[] diagrams;
    private DisplayFourierWorksheet[] sheet;
    private FourierKurvenRekonstruktion[] reconstructions;
    //
    private final AbstractDataContainer _worksheet;
    private final double[][] _cn, _jn, _an, _bn;
    private final double _f1;
    private final int _nMin;
    private final double _rng1, _rng2;
    private final boolean[] _signalFourierAnalysiert;
    //
    private int mouseMode = GraferImplementation.MAUSMODUS_NIX;
    private int previousActiveIconIndex = 0;
    //-------------------
    private ImageIcon[] iconOFF, iconON;
    private JButton[] mouseButtons;
    private JToolBar toolBar;
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

        setTitle("Fourier Transform Diagram");
        getContentPane().setLayout(new BorderLayout());
        buildToolbar();
        baueGUI();
        pack();
        addComponentListener(this);
    }

    private void baueGUI() {
        tabbedPane = new JTabbedPane();
        diagrams = new FourierDiagramm[_worksheet.getRowLength()+1];
        sheet = new DisplayFourierWorksheet[_worksheet.getRowLength()+1];
        reconstructions = new FourierKurvenRekonstruktion[_worksheet.getRowLength()+1];
        //
        for (int i1 = 1; i1 < _worksheet.getRowLength()+1; i1++) {
            if (_signalFourierAnalysiert[i1]) {
                JPanel jpSg = new JPanel();
                jpSg.setLayout(new BorderLayout());
                JTabbedPane localTabbedPane = new JTabbedPane();
                //---------
                diagrams[i1 - 1] = new FourierDiagramm(_cn[i1 - 1], _nMin, _f1);
                localTabbedPane.addTab("Fourier Analysis", diagrams[i1 - 1]);
                sheet[i1 - 1] = new DisplayFourierWorksheet(_cn[i1 - 1], _jn[i1 - 1], _nMin);
                localTabbedPane.addTab("Worksheet Data", sheet[i1 - 1]);
                reconstructions[i1 - 1] = new FourierKurvenRekonstruktion(_an[i1 - 1], _bn[i1 - 1], _nMin, _f1, _worksheet, i1, _rng1, _rng2);
                localTabbedPane.addTab("Reconstruction", reconstructions[i1 - 1]);
                //---------
                jpSg.add(localTabbedPane, BorderLayout.CENTER);
                tabbedPane.addTab(_worksheet.getSignalName(i1-1) + " ", jpSg);
            }
        }
        //------------------------
        JPanel jpIN = new JPanel();  // User-Input: n-range restriction, OK, etc.
        //------------------------
        Container con = this.getContentPane();
        con.setLayout(new BorderLayout());
        con.add(tabbedPane, BorderLayout.CENTER);
        con.add(jpIN, BorderLayout.SOUTH);
        con.add(toolBar, BorderLayout.WEST);
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
                BufferedWriter fkaku = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileChooser.getFileWithCheckedEnding()), StandardCharsets.UTF_8));
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

    private void buildToolbar() {
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
        mouseButtons = new JButton[iconOFF.length];
        for (int i1 = 0; i1 < mouseButtons.length; i1++) {
            mouseButtons[i1] = new JButton();
            mouseButtons[i1].setIcon(iconOFF[i1]);
            mouseButtons[i1].setActionCommand("mouseMode " + i1);
            mouseButtons[i1].addActionListener((final ActionEvent actionEvent) -> updateMouseMode(actionEvent));

            //--------------------
            switch (i1) {
                case 0:
                    mouseButtons[i1].setToolTipText("Deactivate mouse");
                    break;
                case 1:
                    mouseButtons[i1].setToolTipText("Autoscale");
                    break;
                case 2:
                    mouseButtons[i1].setToolTipText("Zoom rectangle");
                    break;
                case 3:
                    mouseButtons[i1].setToolTipText("Set slider for X/Y-values");
                    break;
                case 4:
                    mouseButtons[i1].setToolTipText("set logarithmic y-axis");
                    break;
                default:
                    Logger.getLogger(DialogFourierDiagramm.class.getName()).log(Level.WARNING, "Error: 49ugnw3grjgtfzj");
                    break;
            }
            //--------------------
        }
        mouseButtons[0].setIcon(iconON[0]);  // default: Mouse deactivated

        //--------------------
        toolBar = new JToolBar("Mouse Options");
        toolBar.setOrientation(JToolBar.VERTICAL);
        toolBar.setFloatable(false);
        for (int i1 = 0; i1 < mouseButtons.length; i1++) {
            toolBar.add(mouseButtons[i1]);
        }
        //--------------------
    }

    private int yAxisType = GraferV3.ACHSE_LIN;

    private void updateMouseMode(final ActionEvent actionEvent) {
        //--------------------
        // save previous state:
        int previousMouseMode = -1;
        //--------------------
        final StringTokenizer stk = new StringTokenizer(actionEvent.getActionCommand(), " ");
        stk.nextToken();
        final int buttonPressedIndex = Integer.parseInt(stk.nextToken());
        for (int i1 = 0; i1 < mouseButtons.length; i1++) {
            mouseButtons[i1].setIcon(iconOFF[i1]);
        }
        mouseButtons[buttonPressedIndex].setIcon(iconON[buttonPressedIndex]);
        //--------------------
        switch (buttonPressedIndex) {
            case 0:
                mouseMode = GraferImplementation.MAUSMODUS_NIX;
                break;
            case 1:
                previousMouseMode = mouseMode;
                mouseMode = GraferImplementation.MAUSMODUS_ZOOM_AUTOFIT;
                break;
            case 2:
                mouseMode = GraferImplementation.MAUSMODUS_ZOOM_FENSTER;
                break;
            case 3:
                mouseMode = GraferImplementation.MAUSMODUS_WERTANZEIGE_SCHIEBER;
                break;
            case 4:
                if(yAxisType == GraferV3.ACHSE_LIN) {
                   yAxisType = GraferV3.ACHSE_LOG;
                   mouseButtons[4].setIcon(iconON[4]);
                } else {
                    yAxisType = GraferV3.ACHSE_LIN;
                    mouseButtons[4].setIcon(iconOFF[4]);
                }

                for (int i = 0; i < diagrams.length; i++) {
                    if (diagrams[i] != null) {
                        diagrams[i].setzeAchsenTyp(new int[]{GraferV3.ACHSE_LIN}, new int[]{yAxisType});
                        diagrams[i].repaint();
                    }


                }

                break;
            default:
                Logger.getLogger(DialogFourierDiagramm.class.getName()).log(Level.WARNING, "Error: 98n3gweggtq5t");
                break;
        }
        for (int i1 = 1; i1 < _worksheet.getRowLength()+1; i1++) {
            if (_signalFourierAnalysiert[i1]) {
                diagrams[i1 - 1].setMausModus(mouseMode);
                reconstructions[i1 - 1].setMausModus(mouseMode);
            }
        }
        if (mouseMode == GraferImplementation.MAUSMODUS_ZOOM_AUTOFIT) {
            mouseMode = previousMouseMode;
            mouseButtons[1].setIcon(iconOFF[1]);
            mouseButtons[previousActiveIconIndex].setIcon(iconON[previousActiveIconIndex]);
            for (int i1 = 1; i1 < _worksheet.getRowLength()+1; i1++) {
                if (_signalFourierAnalysiert[i1]) {
                    diagrams[i1 - 1].setMausModus(mouseMode);
                    reconstructions[i1 - 1].setMausModus(mouseMode);
                }
            }
        } else {
            previousActiveIconIndex = buttonPressedIndex;
        }
    }

    @Override
    public void componentResized(final ComponentEvent compEvent) {
        for (int i1 = 1; i1 < _worksheet.getRowLength()+1; i1++) {
            if (_signalFourierAnalysiert[i1]) {
                diagrams[i1 - 1].resize();
                diagrams[i1 - 1].repaint();
                reconstructions[i1 - 1].resize();
                reconstructions[i1 - 1].repaint();
            }
        }
    }

    @Override
    public void componentMoved(final ComponentEvent compEvent) {
        // nothing to do here
    }

    @Override
    public void componentShown(final ComponentEvent compEvent) {
        // nothing to do here
    }

    @Override
    public void componentHidden(final ComponentEvent compEvent) {
        // nothing to do here
    }
}



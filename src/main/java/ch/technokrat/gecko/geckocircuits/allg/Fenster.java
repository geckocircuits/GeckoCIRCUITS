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
package ch.technokrat.gecko.geckocircuits.allg;

import ch.technokrat.gecko.ExternalGeckoCustom;
import ch.technokrat.gecko.GeckoCustomMMF;
import ch.technokrat.gecko.GeckoExternal;
import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.control.*;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataJunkCompressable;
import ch.technokrat.gecko.geckocircuits.newscope.ScopeFrame;
import ch.technokrat.gecko.geckoscript.SimulationAccess;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import ch.technokrat.modelviewcontrol.AbstractUndoGenericModel;

public final class Fenster extends JFrame implements WindowListener, ActionListener, ComponentListener {
    

    int _simMenuIndex = 2; // simulation menu is third in bar.
    JMenuBar _menuBar; // to provide dynamic changing of the menu bar, need to make this a member.
    //--------------------------------------
    private boolean simulatorAktiviert = false;
    //
    public static SchematischeEingabe2 _se;
    public SchematischeEingabeAuswahl2 sea;
    public static final boolean INCLUDE_GeckoMAGNETICS = false;
    public static final boolean INCLUDE_GeckoHEAT = false;
    public static final boolean INCLUDE_GeckoEMC = false;
    private static final String UNTITLED = "Untitled";  // wird in der Fenster-Leiste angezeigt, wenn eine neue Datei gestartet wird 
    public static String aktuellerDateiName = UNTITLED;  // ohne Pfadangabe! - nur zur Anzeige in der Fensterleiste (so wie bei Windows ueblich)
    private String dateiEndung = "ipes";
    //--------------------------------------
    // 
    private JSplitPane split;
    public static int seaBREITE = 110;  // Breite der 'SchematischeEingabeAuswahl2()'-Komponente am rechten Rand
    public JCheckBoxMenuItem vItemShowParLK, vItemShowFlowLK, vItemShowNameLK, vItemShowTextLineLK;
    public JCheckBoxMenuItem vItemShowParCONTROL, vItemShowNameCONTROL, vItemShowTextLineCONTROL;
    public JCheckBoxMenuItem aliasingCONTROL;
    public JCheckBoxMenuItem vItemShowParTHERM, vItemShowFlowTHERM, vItemShowNameTHERM, vItemShowTextLineTHERM;
    JMenuItem mItemNew, mItemOpen, mItemSave, mItemSaveAs, mItemExit, mItemSaveView, mItemMemorySettings, mItemRemoteSettings,
            mItemUpdateSettings;
    private JMenuItem mItemCheckModel, mItemFindString, mItemExport, mItemImport, mItemImportFromFile, mItemSetPar, mItemSetOrder;
    private JCheckBoxMenuItem mItemConnectorTest;
    private JMenuItem mItemRECENT_1, mItemRECENT_2, mItemRECENT_3, mItemRECENT_4;
    private JMenuItem mItemUndo, mItemRedo, mItemCopy, mItemMove, mItemDelete, mItemEscape, mItemSelectAll, mItemDisable, mItemDisableShort;
    private JMenuItem mItemParameter, mItemRun, mItemStop, mItemContinue;
    //private JMenuItem mItemGoSteadyState, mItemSaveState, mItemLoadState, mItemResetState, mItemClearStates;
    //--------------------------------------    
    //--------------------------------------
    final GeckoStatusBar jtfStatus = new GeckoStatusBar("Ready ...", this);  // Status-Anzeige der Schaltungs-Simulation
    public static final int RECENT_FILE_SPACE = -1;
    //--------------------------------------
    private boolean speicherVorgangLaeuft = false;
    //--------------------------------------
    // Falls als Applet gestartet wird, dann koennen folgenden Dateien geladen werden: 
    private String[] datnamExampleApplet;
    //-------------------------
    // simple parameter-set for GeckoOPTIMIZER --> 
    public static final OptimizerParameterData optimizerParameterData = new OptimizerParameterData();
    public static final JScrollPane seScroll = new JScrollPane();
    //-------------------------        
    private JMenuItem mItemSaveApplet;
    private int uniqueFileID = 0;
    public static SimulationAccess _scripter = null;
    public static final SolverSettings _solverSettings = new SolverSettings();
    private JMenuItem _mItemScriptingTool;
    public static GeckoFileManager _fileManager = null;
    public static ExternalGeckoCustom _external = null;
    public static GeckoCustomMMF _mmf_access = null;
    public final SimulationRunner _simRunner;
    public final KeyAdapter keyAdapter;
    private static final String spTitleX = "  -  ";
    public static boolean IS_BRANDED = false;
    public static boolean IS_APPLET = true;  // set at runtime
    private SuggestionField _searchTestField;
    public LastComponentButton _lastComponentButton;
    public final static JPanel _northPanel = new JPanel();

    /*
     * // steady-state analysis >> private int numberOfDomains= 3; // LK=0, THERM=1, CONTR=2 private double Tss=20e-3; // to be
     * set by the user, e.g. 20ms for a PFC n a 50Hz-grid private int maxIterationsNewton=100; private double
     * maxErrorNewton=0.01; private boolean useIterationNewton=true, useIterationSecant=false; // save user-defined initial
     * values before stateady-state analysis >> private double[][][] states, statesAtFileload; private boolean[][] noStates,
     * noStatesAtFileload;
     */
    public boolean isSimulationRunning() {
        return !mItemRun.isEnabled();
    }

    public static String getOpenFileName() {
        return aktuellerDateiName;
    }

    public boolean saveZVData(String stringID_scope, String fileName) {
        List<RegelBlock> eC = _se.getElementCONTROL();
        for (RegelBlock ec : eC) {
            if (ec.getStringID().equals(stringID_scope) && ec instanceof ReglerOSZI) {
                ScopeFrame scope = ((ReglerOSZI) ec)._scopeFrame;
                scope.saveZVData(fileName);
                return true;
            }
        }
        return false;
    }

    public OptimizerParameterData getOptimizerParameterData() {
        return optimizerParameterData;
    }

    /**
     * damit kann man den Simulator aktivieren bzw. de-aktivieren -->
     *
     * @param simulatorAktiviert
     */
    public void setActivationOfSimulator(boolean simulatorAktiviert) {
        this.simulatorAktiviert = simulatorAktiviert;
        _se.setActivationOfSimulator(simulatorAktiviert);
    }

    public void setAppletFiles(String[] datnamExampleApplet) {
        this.datnamExampleApplet = datnamExampleApplet;
    }

    public Fenster() {

//        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//            System.out.println("look and feel: " + info.getClassName());
//        }

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            //UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (InstantiationException ex) {
            Logger.getLogger(Fenster.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Fenster.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(Fenster.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
        }

        /*
         * try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (ClassNotFoundException ex) {
         * Logger.getLogger(Fenster.class.getName()).log(Level.SEVERE, null, ex); } catch (InstantiationException ex) {
         * Logger.getLogger(Fenster.class.getName()).log(Level.SEVERE, null, ex); } catch (IllegalAccessException ex) {
         * Logger.getLogger(Fenster.class.getName()).log(Level.SEVERE, null, ex); } catch (UnsupportedLookAndFeelException ex) {
         * Logger.getLogger(Fenster.class.getName()).log(Level.SEVERE, null, ex); }
         */
        try {
            this.setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif"))).getImage());
        } catch (Exception e) {
        }

        if (Fenster.IS_APPLET) {
            this.setTitle("*** Applet-Mode *** GeckoCIRCUITS ***");
        } else {
            this.setTitle(aktuellerDateiName + spTitleX + "GeckoCIRCUITS");
        }

        this.addWindowListener(this);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);  // damit man den 'QuitWithoutSaving'-Dialog hochfahren kann ...
        this.addComponentListener(this);
        //---------------------------------

        keyAdapter = new KeyAdapter() {
            public void keyTyped(KeyEvent ke) {
                processKeyEvents(ke);
            }
        };

        _se = new SchematischeEingabe2(this);

        _simRunner = new SimulationRunner(this, _se);

        sea = new SchematischeEingabeAuswahl2();
        _se.setSchematischeEingabeAuswahl(sea);
        sea.anmeldenSchematischeEingabe(_se);
        //sea.setEnabledAt(1, false);                                 
        //---------------------------------
        //this.baueGUI();
        baueGUI();

        Timer timer = new Timer();
        // start after 10 sek, and then save a backup after every 20 sek
        if (!IS_BRANDED) {
            timer.schedule(new BackupTask(), 10000, 20000);
        }

        _scripter = new SimulationAccess(this);

        if (!_scripter.isScripterEnabled()) {
            _mItemScriptingTool.setEnabled(false);
        }

        _fileManager = new GeckoFileManager();
        //default file for unsaved work
        String jarPath = GetJarPath.getJarFilePath();
        if (!IS_BRANDED && !Fenster.IS_APPLET) {
            if (jarPath != null) {
                File jarFile = new File(jarPath);
                GlobalFilePathes.DATNAM = jarFile.getParent() + System.getProperty("file.separator") + "unsavedFile.ipes";
            } else {
                GlobalFilePathes.DATNAM = System.getProperty("user.dir") + System.getProperty("file.separator") + "unsavedFile.ipes";
            }
        } else {
            GlobalFilePathes.DATNAM = "Applet";
        }

        if (!Fenster.IS_APPLET && !Fenster.IS_BRANDED) {
            //StartupWindow.fabricUnBlocking();
        }

        DialogUpdate.checkForUpdateInterval();

    }

    private void baueGUI() {
        JMenu fileMenu = GuiFabric.getJMenu(I18nKeys.FILE);
        mItemNew = GuiFabric.getJMenuItem(I18nKeys.NEW);
        mItemNew.setActionCommand("New");
        mItemNew.addActionListener(this);
        mItemNew.setMnemonic(KeyEvent.VK_N);
        mItemOpen = GuiFabric.getJMenuItem(I18nKeys.OPEN);
        mItemOpen.setActionCommand("Open");
        mItemOpen.addActionListener(this);
        mItemOpen.setMnemonic(KeyEvent.VK_O);
        mItemSave = GuiFabric.getJMenuItem(I18nKeys.SAVE);
        mItemSave.setActionCommand("Save");
        mItemSave.addActionListener(this);
        mItemSave.setMnemonic(KeyEvent.VK_S);
        mItemSaveAs = GuiFabric.getJMenuItem(I18nKeys.SAVE_AS);
        mItemSaveAs.setActionCommand("Save As");
        mItemSaveAs.addActionListener(this);

        mItemSaveApplet = GuiFabric.getJMenuItem(I18nKeys.SAVE_AS_APPLET);
        mItemSaveApplet.setActionCommand("SaveApplet");
        mItemSaveApplet.addActionListener(this);

        if (IS_BRANDED) {
            mItemNew.setEnabled(false);
            mItemOpen.setEnabled(false);
            mItemSaveApplet.setEnabled(false);
        }

        if (Fenster.IS_APPLET) {
            mItemSaveApplet.setEnabled(false);
        }

        mItemSaveView = GuiFabric.getJMenuItem(I18nKeys.SAVE_VIEW_AS_IMAGE);
        mItemSaveView.setActionCommand("Save View as Image");
        mItemSaveView.addActionListener(this);
        mItemExit = GuiFabric.getJMenuItem(I18nKeys.EXIT);
        mItemExit.setActionCommand("Exit");
        mItemExit.addActionListener(this);

        if (Fenster.IS_APPLET) {
            mItemSave.setEnabled(false);
            mItemSaveAs.setEnabled(false);
            mItemSaveView.setEnabled(false);
        }
        // 
        fileMenu.add(mItemNew);
        mItemNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK));
        fileMenu.add(mItemOpen);
        mItemOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
        fileMenu.add(mItemSave);
        mItemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));
        fileMenu.add(mItemSaveAs);
        fileMenu.add(mItemSaveApplet);
        fileMenu.add(mItemSaveView);
        fileMenu.add(mItemExit);
        // die zuletzt bearbeiteten Dateien: 
        fileMenu.addSeparator();
        mItemRECENT_1 = new JMenuItem(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_1, RECENT_FILE_SPACE));
        mItemRECENT_1.addActionListener(this);
        mItemRECENT_1.setActionCommand("RECENT_1");
        mItemRECENT_2 = new JMenuItem(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_2, RECENT_FILE_SPACE));
        mItemRECENT_2.addActionListener(this);
        mItemRECENT_2.setActionCommand("RECENT_2");
        mItemRECENT_3 = new JMenuItem(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_3, RECENT_FILE_SPACE));
        mItemRECENT_3.addActionListener(this);
        mItemRECENT_3.setActionCommand("RECENT_3");
        mItemRECENT_4 = new JMenuItem(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_4, RECENT_FILE_SPACE));
        mItemRECENT_4.addActionListener(this);
        mItemRECENT_4.setActionCommand("RECENT_4");
        fileMenu.add(mItemRECENT_1);
        fileMenu.add(mItemRECENT_2);
        fileMenu.add(mItemRECENT_3);
        fileMenu.add(mItemRECENT_4);
        //=======================================
        JMenu editMenu = GuiFabric.getJMenu(I18nKeys.EDIT);
        mItemUndo = GuiFabric.getJMenuItem(I18nKeys.UNDO);
        mItemUndo.setActionCommand("Undo");
        mItemUndo.addActionListener(this);
        mItemUndo.setMnemonic(KeyEvent.VK_Z);

        mItemRedo = GuiFabric.getJMenuItem(I18nKeys.REDO);
        mItemRedo.setActionCommand("Redo");
        mItemRedo.addActionListener(this);
        mItemRedo.setMnemonic(KeyEvent.VK_Y);

        mItemCopy = GuiFabric.getJMenuItem(I18nKeys.COPY_ELEMENTS);
        mItemCopy.setActionCommand("Copy Elements");
        mItemCopy.addActionListener(this);
        mItemCopy.setMnemonic(KeyEvent.VK_C);
        mItemMove = GuiFabric.getJMenuItem(I18nKeys.MOVE_ELEMENTS);
        mItemMove.setActionCommand("Move Elements");
        mItemMove.addActionListener(this);
        mItemMove.setMnemonic(KeyEvent.VK_X);
        mItemDelete = GuiFabric.getJMenuItem(I18nKeys.DELETE_ELEMENTS);
        mItemDelete.setActionCommand("Delete Elements");
        mItemDelete.addActionListener(this);
        mItemDelete.setMnemonic(KeyEvent.VK_DELETE);
        mItemEscape = GuiFabric.getJMenuItem(I18nKeys.DESELECT);
        mItemEscape.setActionCommand("Deselect");
        mItemEscape.addActionListener(this);
        mItemEscape.setMnemonic(KeyEvent.VK_ESCAPE);
        mItemSelectAll = GuiFabric.getJMenuItem(I18nKeys.SELECT_ALL);
        mItemSelectAll.setActionCommand("SelectAll");
        mItemSelectAll.addActionListener(this);
        mItemSelectAll.setMnemonic(KeyEvent.VK_ESCAPE);
        mItemDisable = GuiFabric.getJMenuItem(I18nKeys.ENABLE_DISABLE);
        mItemDisable.setActionCommand("Disable");
        mItemDisable.addActionListener(this);
        mItemDisable.setMnemonic(KeyEvent.VK_A);

        mItemDisableShort = GuiFabric.getJMenuItem(I18nKeys.SHORT_CIRCUIT_COMPONENT);
        mItemDisableShort.setActionCommand("DisableShort");
        mItemDisableShort.addActionListener(this);
        //mItemDisableShort.setMnemonic(KeyEvent.VK_ALT);

        mItemImport = GuiFabric.getJMenuItem(I18nKeys.IMPORT);
        mItemImport.setActionCommand("Import");
        mItemImport.addActionListener(this);

        mItemImportFromFile = GuiFabric.getJMenuItem(I18nKeys.IMPORT_FROM_FILE);
        mItemImportFromFile.setActionCommand("ImportFromFile");
        mItemImportFromFile.addActionListener(this);

        mItemExport = GuiFabric.getJMenuItem(I18nKeys.EXPORT);
        mItemExport.setActionCommand("Export");
        mItemExport.addActionListener(this);

        editMenu.addSeparator();
        editMenu.add(mItemUndo);
        editMenu.add(mItemRedo);
        editMenu.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                boolean canUndo = AbstractUndoGenericModel.undoManager.canUndo();
                mItemUndo.setEnabled(canUndo);
                String undoText = AbstractUndoGenericModel.undoManager.getUndoPresentationName();
                if (canUndo) {
                    mItemUndo.setText("Undo: " + undoText);
                } else {
                    mItemUndo.setText("Undo (not available)");
                }

                boolean canRedo = AbstractUndoGenericModel.undoManager.canRedo();
                mItemRedo.setEnabled(canRedo);
                if (canRedo) {
                    String redoText = AbstractUndoGenericModel.undoManager.getRedoPresentationName();
                    mItemRedo.setText("Redo: " + redoText);
                } else {
                    mItemRedo.setText("Redo (not available)");
                }

            }
        });
        mItemRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK));
        mItemUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK));
        editMenu.addSeparator();
        editMenu.add(mItemMove);
        mItemMove.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Event.CTRL_MASK));
        editMenu.add(mItemCopy);
        mItemCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK));
        editMenu.add(mItemDelete);    //mItemDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        editMenu.add(mItemEscape);
        mItemEscape.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

        editMenu.add(mItemSelectAll);
        mItemSelectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK));

        mItemDisable.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK));
        mItemDisableShort.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.ALT_MASK));

        editMenu.add(mItemDisable);
        editMenu.add(mItemDisableShort);

        editMenu.addSeparator();
        editMenu.add(mItemImport);

        editMenu.add(mItemExport);
        editMenu.add(mItemImportFromFile);

        aliasingCONTROL = new JCheckBoxMenuItem("Use Antialiasing");
        aliasingCONTROL.addActionListener(this);
        aliasingCONTROL.setActionCommand("aliasingCommand");

        DataJunkCompressable.setMemoryPrecision();

        String valueString = GeckoSim.applicationProps.getProperty("ANTI_ALIASING");
        if (valueString != null) {
            if (new Boolean(valueString)) {
                _se.setAntialiasing(true);
                aliasingCONTROL.setSelected(true);
                jtfStatus.setAliasing(aliasingCONTROL.isSelected());
            }
        }

        vItemShowNameLK = new JCheckBoxMenuItem("Name");
        vItemShowNameLK.addActionListener(this);
        vItemShowNameLK.setActionCommand("vItemShowNameLK");
        vItemShowNameLK.setSelected(SchematischeEingabe2._lkDisplayMode.showName);
        vItemShowNameLK.setForeground(GlobalColors.farbeFertigElementLK);
        vItemShowParLK = new JCheckBoxMenuItem("Show Parameter");
        vItemShowParLK.addActionListener(this);
        vItemShowParLK.setActionCommand("vItemShowParLK");
        vItemShowParLK.setSelected(SchematischeEingabe2._lkDisplayMode.showParameter);
        vItemShowParLK.setForeground(GlobalColors.farbeFertigElementLK);
        vItemShowTextLineLK = new JCheckBoxMenuItem("Show Text-Line");
        vItemShowTextLineLK.addActionListener(this);
        vItemShowTextLineLK.setActionCommand("vItemShowTextLineLK");
        vItemShowTextLineLK.setSelected(SchematischeEingabe2._lkDisplayMode.showParameter);
        vItemShowTextLineLK.setForeground(GlobalColors.farbeFertigElementLK);
        vItemShowFlowLK = new JCheckBoxMenuItem("Flow Direction");
        vItemShowFlowLK.addActionListener(this);
        vItemShowFlowLK.setActionCommand("vItemShowFlowLK");
        vItemShowFlowLK.setSelected(SchematischeEingabe2._lkDisplayMode.showFlowSymbol);
        vItemShowFlowLK.setForeground(GlobalColors.farbeFertigElementLK);
        //
        vItemShowNameCONTROL = new JCheckBoxMenuItem("Name");
        vItemShowNameCONTROL.addActionListener(this);
        vItemShowNameCONTROL.setActionCommand("vItemShowNameCONTROL");
        vItemShowNameCONTROL.setSelected(SchematischeEingabe2._controlDisplayMode.showName);
        vItemShowNameCONTROL.setForeground(GlobalColors.farbeFertigElementCONTROL);
        vItemShowParCONTROL = new JCheckBoxMenuItem("Show Parameter");
        vItemShowParCONTROL.addActionListener(this);
        vItemShowParCONTROL.setActionCommand("vItemShowParCONTROL");
        vItemShowParCONTROL.setSelected(SchematischeEingabe2._controlDisplayMode.showParameter);
        vItemShowParCONTROL.setForeground(GlobalColors.farbeFertigElementCONTROL);
        vItemShowTextLineCONTROL = new JCheckBoxMenuItem("Show Text-Line");
        vItemShowTextLineCONTROL.addActionListener(this);
        vItemShowTextLineCONTROL.setActionCommand("vItemShowTextLineCONTROL");
        vItemShowTextLineCONTROL.setSelected(SchematischeEingabe2._controlDisplayMode.showParameter);
        vItemShowTextLineCONTROL.setForeground(GlobalColors.farbeFertigElementCONTROL);
        //
        vItemShowNameTHERM = new JCheckBoxMenuItem("Name");
        vItemShowNameTHERM.addActionListener(this);
        vItemShowNameTHERM.setActionCommand("vItemShowNameTHERM");

        vItemShowNameTHERM.setSelected(SchematischeEingabe2._thermDisplayMode.showName);
        vItemShowNameTHERM.setForeground(GlobalColors.farbeFertigElementTHERM);
        vItemShowParTHERM = new JCheckBoxMenuItem("Show Parameter");
        vItemShowParTHERM.addActionListener(this);
        vItemShowParTHERM.setActionCommand("vItemShowParTHERM");
        vItemShowParTHERM.setSelected(SchematischeEingabe2._thermDisplayMode.showParameter);
        vItemShowParTHERM.setForeground(GlobalColors.farbeFertigElementTHERM);
        vItemShowTextLineTHERM = new JCheckBoxMenuItem("Show Text-Line");
        vItemShowTextLineTHERM.addActionListener(this);
        vItemShowTextLineTHERM.setActionCommand("vItemShowTextLineTHERM");
        vItemShowTextLineTHERM.setSelected(SchematischeEingabe2._thermDisplayMode.showParameter);
        vItemShowTextLineTHERM.setForeground(GlobalColors.farbeFertigElementTHERM);
        vItemShowFlowTHERM = new JCheckBoxMenuItem("Flow Direction");
        vItemShowFlowTHERM.addActionListener(this);
        vItemShowFlowTHERM.setActionCommand("vItemShowFlowTHERM");
        vItemShowFlowTHERM.setSelected(SchematischeEingabe2._thermDisplayMode.showFlowSymbol);
        vItemShowFlowTHERM.setForeground(GlobalColors.farbeFertigElementTHERM);
        //
        JMenuItem menueGroesse = GuiFabric.getJMenuItem(I18nKeys.WORKSHEET_SIZE);
        menueGroesse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                DialogSheetSize.fabric(Fenster.this, _se._visibleCircuitSheet._worksheetSize);
            }
        });

        JMenu menueSkalierung = GuiFabric.getJMenu(I18nKeys.SCALING);
        JMenuItem mItemSkal10 = GuiFabric.getJMenuItem(I18nKeys.POINT_10);
        mItemSkal10.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                AbstractCircuitSheetComponent.dpixValue.setValue(10);
            }
        });
        JMenuItem mItemSkal12 = GuiFabric.getJMenuItem(I18nKeys.POINT_12);
        mItemSkal12.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                AbstractCircuitSheetComponent.dpixValue.setValue(12);
            }
        });
        JMenuItem mItemSkal14 = GuiFabric.getJMenuItem(I18nKeys.POINT_14);
        mItemSkal14.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                AbstractCircuitSheetComponent.dpixValue.setValue(14);
            }
        });
        JMenuItem mItemSkal16 = GuiFabric.getJMenuItem(I18nKeys.POINT_16);
        mItemSkal16.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                AbstractCircuitSheetComponent.dpixValue.setValue(16);
            }
        });
        JMenuItem mItemSkal18 = GuiFabric.getJMenuItem(I18nKeys.POINT_18);
        mItemSkal18.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                AbstractCircuitSheetComponent.dpixValue.setValue(18);
            }
        });
        menueSkalierung.add(mItemSkal10);
        menueSkalierung.add(mItemSkal12);
        menueSkalierung.add(mItemSkal14);
        menueSkalierung.add(mItemSkal16);
        menueSkalierung.add(mItemSkal18);
        //
        JMenu menueFontSize = GuiFabric.getJMenu(I18nKeys.FONT_SIZE);
        JMenuItem mItemFS06 = GuiFabric.getJMenuItem(I18nKeys.POINT_6);
        mItemFS06.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _se.setzeFont(6, "Arial");
            }
        });
        JMenuItem mItemFS08 = GuiFabric.getJMenuItem(I18nKeys.POINT_8);
        mItemFS08.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _se.setzeFont(8, "Arial");
            }
        });
        JMenuItem mItemFS10 = GuiFabric.getJMenuItem(I18nKeys.POINT_10);
        mItemFS10.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _se.setzeFont(10, "Arial");
            }
        });
        JMenuItem mItemFS12 = GuiFabric.getJMenuItem(I18nKeys.POINT_12);
        mItemFS12.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _se.setzeFont(12, "Arial");
            }
        });
        JMenuItem mItemFS14 = GuiFabric.getJMenuItem(I18nKeys.POINT_14);
        mItemFS14.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _se.setzeFont(14, "Arial");
            }
        });
        JMenuItem mItemFS16 = GuiFabric.getJMenuItem(I18nKeys.POINT_16);
        mItemFS16.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _se.setzeFont(16, "Arial");
            }
        });
        JMenuItem mItemFS18 = GuiFabric.getJMenuItem(I18nKeys.POINT_18);
        mItemFS18.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _se.setzeFont(18, "Arial");
            }
        });
        menueFontSize.add(mItemFS06);
        menueFontSize.add(mItemFS08);
        menueFontSize.add(mItemFS10);
        menueFontSize.add(mItemFS12);
        menueFontSize.add(mItemFS14);
        menueFontSize.add(mItemFS16);
        menueFontSize.add(mItemFS18);
        //
        JMenu viewMenu = GuiFabric.getJMenu(I18nKeys.VIEW);
        viewMenu.add(menueGroesse);
        viewMenu.add(menueSkalierung);
        viewMenu.add(menueFontSize);
        viewMenu.add(aliasingCONTROL);
        viewMenu.addSeparator();
        viewMenu.add(vItemShowNameLK);
        viewMenu.add(vItemShowParLK);
        viewMenu.add(vItemShowTextLineLK);
        viewMenu.add(vItemShowFlowLK);
        viewMenu.addSeparator();
        viewMenu.add(vItemShowNameCONTROL);
        viewMenu.add(vItemShowParCONTROL);
        viewMenu.add(vItemShowTextLineCONTROL);
        viewMenu.addSeparator();
        viewMenu.add(vItemShowNameTHERM);
        viewMenu.add(vItemShowParTHERM);
        viewMenu.add(vItemShowTextLineTHERM);
        viewMenu.add(vItemShowFlowTHERM);
        //=======================================
        JMenu toolsMenu = GuiFabric.getJMenu(I18nKeys.TOOLS);

        mItemMemorySettings = GuiFabric.getJMenuItem(I18nKeys.MEMORY_SETTINGS);
        mItemMemorySettings.setActionCommand("memorySettings");
        mItemMemorySettings.addActionListener(this);

        mItemUpdateSettings = GuiFabric.getJMenuItem(I18nKeys.UPDATE_SETTINGS);
        mItemUpdateSettings.setActionCommand("updateSettings");
        mItemUpdateSettings.addActionListener(this);

        mItemRemoteSettings = GuiFabric.getJMenuItem(I18nKeys.REMOTE_ACCESS_SETTINGS);
        mItemRemoteSettings.setActionCommand("remoteSettings");
        mItemRemoteSettings.addActionListener(this);

        //
        mItemConnectorTest = new JCheckBoxMenuItem("Check Connections");
        mItemConnectorTest.addActionListener(this);
        mItemConnectorTest.setActionCommand("mItemConnectorTest");
        mItemConnectorTest.setSelected(false);
        //mItemConnectorTest.setForeground(GlobalColors.farbeConnectorTestMode);

        //
        mItemSetPar = GuiFabric.getJMenuItem(I18nKeys.SET_PARAMETERS);
        mItemSetPar.setActionCommand("setParameters");
        mItemSetPar.addActionListener(this);
        // 
        mItemSetOrder = GuiFabric.getJMenuItem(I18nKeys.SET_ORDER_OF_CONTROL);
        mItemSetOrder.setActionCommand("setOrder");
        mItemSetOrder.addActionListener(this);

        mItemCheckModel = GuiFabric.getJMenuItem(I18nKeys.CHECK_CONTROL_MODEL);
        mItemCheckModel.setActionCommand("mItemCheckModel");
        mItemCheckModel.addActionListener(this);
        mItemCheckModel.setMnemonic(KeyEvent.VK_Q);

        mItemFindString = GuiFabric.getJMenuItem(I18nKeys.FIND_IN_MODEL);
        mItemFindString.setActionCommand("mItemFind");
        mItemFindString.addActionListener(this);
        mItemFindString.setMnemonic(KeyEvent.VK_F);
        mItemFindString.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK));

        //
        toolsMenu.add(mItemConnectorTest);
        mItemConnectorTest.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, Event.CTRL_MASK));
        toolsMenu.add(mItemCheckModel);
        toolsMenu.add(mItemFindString);
        mItemCheckModel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));
        toolsMenu.addSeparator();
        toolsMenu.add(mItemSetPar);
        //toolsMenu.add(mItemSetOrder);
        toolsMenu.addSeparator();
        toolsMenu.add(mItemMemorySettings);
        toolsMenu.add(mItemUpdateSettings);
        toolsMenu.add(mItemRemoteSettings);
        //
        //toolsMenu.add(mItem3DElectromag);
        //mItem3DElectromag.setEnabled(false);
        //toolsMenu.add(mItemOptimize);
        //mItemOptimize.setEnabled(false);
        // 
        //toolsMenu.add(mItemTest);
        JMenu helpMenu = GuiFabric.getJMenu(I18nKeys.HELP);

        JMenuItem mItemAbout = GuiFabric.getJMenuItem(I18nKeys.ABOUT);
        mItemAbout.setActionCommand("About");
        mItemAbout.addActionListener(this);
        //
        helpMenu.add(mItemAbout);

        JMenuItem mItemLicenses = GuiFabric.getJMenuItem(I18nKeys.LICENSING);
        mItemLicenses.setActionCommand("Licensing");
        mItemLicenses.addActionListener(this);
        //
        helpMenu.add(mItemLicenses);

        JMenuItem mItemFeedback = GuiFabric.getJMenuItem(I18nKeys.FEEDBACK);
        mItemFeedback.setActionCommand("Feedback");
        mItemFeedback.addActionListener(this);

        helpMenu.add(mItemFeedback);

        JMenuItem mItemUpdate = GuiFabric.getJMenuItem(I18nKeys.UPDATES);
        mItemUpdate.setActionCommand("Update");
        mItemUpdate.addActionListener(this);

        helpMenu.add(mItemUpdate);

        if (Fenster.IS_APPLET) {
            mItemFeedback.setEnabled(false);
            //mItemUpdate.setEnabled(false);
        }

        //=======================================
        // Simulations-Status-Anzeige:
        JLabel jlSpace1 = new JLabel("       ");
        //=======================================
        JMenu geckoMenu = GuiFabric.getJMenu(I18nKeys.GECKO);
        geckoMenu.setForeground(GlobalColors.farbeGecko);
        _mItemScriptingTool = new JMenuItem("GeckoSCRIPT");  // TxtI.ti_optimizerSimple
        _mItemScriptingTool.setEnabled(true);
        _mItemScriptingTool.setActionCommand("geckoScript");
        _mItemScriptingTool.addActionListener(this);
        JMenuItem mItemOptimizerSimple = new JMenuItem("GeckoOPTIMIZER");  // TxtI.ti_optimizerSimple
        mItemOptimizerSimple.setEnabled(false);
        mItemOptimizerSimple.setActionCommand("geckoOptimizer");
        mItemOptimizerSimple.addActionListener(this);
        JMenuItem mItem3DTherm = new JMenuItem("GeckoHEAT");
        mItem3DTherm.setActionCommand("3Dtherm");
        mItem3DTherm.addActionListener(this);
        JMenuItem mItemMagnet = new JMenuItem("GeckoMAGNETICS");
        mItemMagnet.setActionCommand("magnet");
        mItemMagnet.addActionListener(this);
        JMenuItem mItemEMC = new JMenuItem("GeckoEMC");
        mItemEMC.setActionCommand("geckoEMC");
        mItemEMC.addActionListener(this);
        geckoMenu.add(_mItemScriptingTool);
        geckoMenu.add(mItemOptimizerSimple);
        geckoMenu.add(mItem3DTherm);
        geckoMenu.add(mItemMagnet);
        geckoMenu.add(mItemEMC);

        if (INCLUDE_GeckoMAGNETICS) {
            mItemMagnet.setEnabled(true);
        } else {
            mItemMagnet.setEnabled(false);
        }
        if (INCLUDE_GeckoHEAT) {
            mItem3DTherm.setEnabled(true);
        } else {
            mItem3DTherm.setEnabled(false);
        }
        if (INCLUDE_GeckoEMC) {
            mItemEMC.setEnabled(true);
        } else {
            mItemEMC.setEnabled(false);
        }
        if (Fenster.IS_APPLET) {
            mItem3DTherm.setEnabled(false);
            mItemMagnet.setEnabled(false);
            mItemEMC.setEnabled(false);
        }
        //=======================================
        //
        _menuBar = new JMenuBar();
        _menuBar.add(fileMenu);
        _menuBar.add(editMenu);
        JMenu simMenu = GuiFabric.getJMenu(I18nKeys.SIMULATION);
        _menuBar.add(simMenu);
        //_menuBar.add(simMenu);
        _menuBar.add(viewMenu);
        _menuBar.add(toolsMenu);
        _menuBar.add(helpMenu);
        _menuBar.add(geckoMenu);
        _menuBar.add(jlSpace1);
        _menuBar.add(jtfStatus);
        this.setJMenuBar(_menuBar);
        setSimulationMenu();

        seScroll.getVerticalScrollBar().setUnitIncrement(20);
        seScroll.getHorizontalScrollBar().setUnitIncrement(20);
        JPanel p2 = new JPanel();
        p2.setLayout(new BorderLayout());
        p2.add(sea, BorderLayout.CENTER);
        _northPanel.setBorder(new EmptyBorder(-5, -5, -5, -5));
        _searchTestField = new SuggestionField(this);
        _searchTestField.setCaseSensitive(false);
        sea.registerSearchField(_searchTestField);

        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BorderLayout());
        _lastComponentButton = new LastComponentButton();
        sea.registerLastComponentButton(_lastComponentButton);

        _lastComponentButton.setContentAreaFilled(false);
        _lastComponentButton.setFocusPainted(false);
        Dimension dim = new Dimension(130, 130);
        _lastComponentButton.setPreferredSize(dim);
        _lastComponentButton.setMinimumSize(dim);
        _lastComponentButton.setMaximumSize(dim);
        searchPanel.add(_lastComponentButton);
        searchPanel.add(_searchTestField, BorderLayout.SOUTH);
        JPanel newPanel = new JPanel();
        newPanel.setLayout(new BorderLayout());
        newPanel.add(_northPanel);
        newPanel.add(_northPanel, BorderLayout.NORTH);
        newPanel.add(seScroll, BorderLayout.CENTER);
        p2.add(searchPanel, BorderLayout.SOUTH);
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, newPanel, p2);
        this.getContentPane().add(split, BorderLayout.CENTER);

    }

    private boolean ausserhalb(Point xy1, Point xy2, int worksheetGrenzeX, int worksheetGrenzeY) {
        if (xy1.x > worksheetGrenzeX || xy1.y > worksheetGrenzeY) {
            return true;
        }
        if (xy2.x > worksheetGrenzeX || xy2.y > worksheetGrenzeY) {
            return true;
        }
        return false;
    }

    private boolean imNegativenBereich(Point[] xy1, Point[] xy2) {
        for (int i2 = 0; i2 < xy1.length; i2++) {
            if (xy1[i2].x < 0 || xy1[i2].y < 0) {
                return true;
            }
        }
        for (int i2 = 0; i2 < xy2.length; i2++) {
            if (xy2[i2].x < 0 || xy2[i2].y < 0) {
                return true;
            }
        }
        return false;
    }

    private boolean imNegativenBereich(Point xy1, Point xy2) {
        if (xy1.x < 0 || xy1.y < 0) {
            return true;
        }
        if (xy2.x < 0 || xy2.y < 0) {
            return true;
        }
        return false;
    }

    /**
     * Verarbeitung derKeyEvents fuer DELETE, weil das scheinbar nicht mit den
     * Menu-Accelerators funktionert --> (Java-Bug??)
     */
    private void processKeyEvents(KeyEvent ke) {
        //System.out.println((int)ke.getKeyChar()+"\t"+ke.getKeyText(ke.getKeyCode())+"\t"+ke.getKeyCode()+"\n"+ke); 
        if ((ke.getKeyChar() == KeyEvent.VK_DELETE) || (ke.getKeyCode() == KeyEvent.VK_DELETE) || (ke.getKeyChar() == KeyEvent.VK_BACK_SPACE) || (ke.getKeyChar() == 4)) // '4' entspricht CONTROL_D
        {
            _se.deleteSelectedComponentsWithUndo();
            return;
        }

        if (ke.getKeyChar() == 'r' || ((int) ke.getKeyChar()) == 18) {
            _se.maus_rotiereElement();
            return;
        }

        if (ke.getKeyChar() == 'w' || ((int) ke.getKeyChar()) == 23) {
            _se.maus_umschalten_selectMode_wireMode_connectorTestMode();

        }

        /*
         * else if ((ke.getKeyCode()==0)&&((int)ke.getKeyChar()==3)) // entspricht CONTROL_C
         * se.kopiereAllesImBearbeitungsModus();
         */
    }

    // zur Modifikation der Titelleiste --> wenn Aenderungen noch nicht gespeichert wurden --> 
    public void modifiziereTitel() {
        this.setTitle(aktuellerDateiName + "*" + spTitleX + "GeckoCIRCUITS");
    }

    // wird aufgerufen, nachdem die Fensterabmessungen bekannt sind:
    public void aktualisiereDividerSplitPane(int breiteFenster) {
        split.setDividerLocation(breiteFenster - seaBREITE);
    }

    private void aktualisierePropertiesRECENT(String datnam) {
        if ((datnam.equals(GlobalFilePathes.RECENT_CIRCUITS_1)) || (datnam.isEmpty())) {
            return;
        }
        //--------------
        if (!datnam.equals("LANGUAGE_UPDATE")) {
            if (datnam.equals(GlobalFilePathes.RECENT_CIRCUITS_2)) {
                GlobalFilePathes.RECENT_CIRCUITS_2 = GlobalFilePathes.RECENT_CIRCUITS_1;
                GlobalFilePathes.RECENT_CIRCUITS_1 = datnam;
            } else if (datnam.equals(GlobalFilePathes.RECENT_CIRCUITS_3)) {
                GlobalFilePathes.RECENT_CIRCUITS_3 = GlobalFilePathes.RECENT_CIRCUITS_2;
                GlobalFilePathes.RECENT_CIRCUITS_2 = GlobalFilePathes.RECENT_CIRCUITS_1;
                GlobalFilePathes.RECENT_CIRCUITS_1 = datnam;
            } else if (datnam.equals(GlobalFilePathes.RECENT_CIRCUITS_4)) {
                GlobalFilePathes.RECENT_CIRCUITS_4 = GlobalFilePathes.RECENT_CIRCUITS_3;
                GlobalFilePathes.RECENT_CIRCUITS_3 = GlobalFilePathes.RECENT_CIRCUITS_2;
                GlobalFilePathes.RECENT_CIRCUITS_2 = GlobalFilePathes.RECENT_CIRCUITS_1;
                GlobalFilePathes.RECENT_CIRCUITS_1 = datnam;
            } else {  // voellig neuer Name
                GlobalFilePathes.RECENT_CIRCUITS_4 = GlobalFilePathes.RECENT_CIRCUITS_3;
                GlobalFilePathes.RECENT_CIRCUITS_3 = GlobalFilePathes.RECENT_CIRCUITS_2;
                GlobalFilePathes.RECENT_CIRCUITS_2 = GlobalFilePathes.RECENT_CIRCUITS_1;
                GlobalFilePathes.RECENT_CIRCUITS_1 = datnam;
            }
            //--------------
            mItemRECENT_1.setText(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_1, RECENT_FILE_SPACE));
            mItemRECENT_2.setText(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_2, RECENT_FILE_SPACE));
            mItemRECENT_3.setText(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_3, RECENT_FILE_SPACE));
            mItemRECENT_4.setText(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_4, RECENT_FILE_SPACE));
            //--------------
        }

        GeckoSim.applicationProps.setProperty("RECENT_CIRCUITS_1", GlobalFilePathes.RECENT_CIRCUITS_1);
        GeckoSim.applicationProps.setProperty("RECENT_CIRCUITS_2", GlobalFilePathes.RECENT_CIRCUITS_2);
        GeckoSim.applicationProps.setProperty("RECENT_CIRCUITS_3", GlobalFilePathes.RECENT_CIRCUITS_3);
        GeckoSim.applicationProps.setProperty("RECENT_CIRCUITS_4", GlobalFilePathes.RECENT_CIRCUITS_4);

        DateFormat dFormat = new SimpleDateFormat("yyyy.MM.dd");
    }

    public void saveFileAs() {
        if (speicherVorgangLaeuft) {
            return;
        }
        speicherVorgangLaeuft = true;
        //

        GeckoFileChooser dialog = GeckoFileChooser.createSaveFileChooser(".ipes", "Circuit Simulation Files (*.ipes)", this, new File(GlobalFilePathes.DATNAM));

        if (dialog.getUserResult() == GeckoFileChooser.FileChooserResult.CANCEL) {
            speicherVorgangLaeuft = false;
            return;
        }
        GlobalFilePathes.DATNAM = dialog.getFileWithCheckedEnding().getAbsolutePath();
        aktuellerDateiName = GlobalFilePathes.DATNAM;

        generateNewFileId();
        _fileManager.recomputeRelativePaths(aktuellerDateiName);
        rawSaveFile(new File(aktuellerDateiName));

        speicherVorgangLaeuft = false;
        _se.resetModelModified();  // damit wird verhindert, dass 'QuitWithoutSaving'-Dialog aufgerufen wird, obwohl Datei bereits gespeichert

        this.setTitle(aktuellerDateiName + spTitleX + "GeckoCIRCUITS");
        this.aktualisierePropertiesRECENT(aktuellerDateiName);
    }

    public void rawSaveFile(File file) {

        try {
            DatenSpeicher datLK = new DatenSpeicher(
                    getSize(),
                    this.optimizerParameterData,
                    uniqueFileID, _scripter, _fileManager, _se, _solverSettings);
            //------------
            // Plain-Test Variante in ASCII -->
            // BufferedWriter out= new BufferedWriter(new FileWriter(GlobalFilePathes.DATNAM));
            //
            // Komprimierter Datenstrom --> reduzierte und unleserliche Datei -->
//            DeflaterOutputStream out1= new DeflaterOutputStream(new FileOutputStream(new File(GlobalFilePathes.DATNAM)));
            GZIPOutputStream out1 = new GZIPOutputStream(new FileOutputStream(file));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(out1));
            //            
            out.write(datLK.exportASCII());
            out.flush();
            out.close();
            checkWrittenFileSize(file);
            //------------
            //
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            checkWrittenFileSize(file);
        }
    }
    


    public void saveFile() {
        boolean reworkRelativePaths = false;
        //----------------
        if (speicherVorgangLaeuft) {
            return;
        }
        speicherVorgangLaeuft = true;
        //----
        // falls noch kein Datei-Name gewaehlt wurde -->
        if (aktuellerDateiName.equals(UNTITLED)) {
            GeckoFileChooser fileChooser = GeckoFileChooser.createSaveFileChooser(".ipes", "Circuit Simulation Files (*.ipes)", this, null);

            if (fileChooser.getUserResult() == GeckoFileChooser.FileChooserResult.CANCEL) {
                speicherVorgangLaeuft = false;
                return;
            }
            GlobalFilePathes.DATNAM = fileChooser.getFileWithCheckedEnding().getAbsolutePath();
            reworkRelativePaths = true;
        }
        //----------
        try {
            aktuellerDateiName = GlobalFilePathes.DATNAM;
            generateNewFileId();
            if (aktuellerDateiName.contains("autoBackup")) {
                int response = JOptionPane.showConfirmDialog(null, "Caution: you try to overwrite your auto-backup file. \nYou should rename your filename to prevent data-loss!\n Do you want to continue anyway?", "Warning!",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response == JOptionPane.NO_OPTION) {
                    speicherVorgangLaeuft = false;
                    return;
                } else if (response == JOptionPane.CLOSED_OPTION) {
                    speicherVorgangLaeuft = false;
                    return;

                }
            }
            if (reworkRelativePaths) {
                _fileManager.recomputeRelativePaths(aktuellerDateiName);
            }
            rawSaveFile(new File(aktuellerDateiName));
            //------------
            speicherVorgangLaeuft = false;
            _se.resetModelModified();  // damit wird verhindert, dass 'QuitWithoutSaving'-Dialog aufgerufen wird, obwohl Datei bereits gespeichert
            //
        } catch (Exception e) {
            speicherVorgangLaeuft = false;
            System.out.println(e + " peorkkkg");
        }
        this.setTitle(aktuellerDateiName + spTitleX + "GeckoCIRCUITS");
        this.aktualisierePropertiesRECENT(aktuellerDateiName);
    }

    public void createNewFile() {
        optimizerParameterData.clear();
        this.aktuellerDateiName = UNTITLED;
        this.setTitle(aktuellerDateiName + spTitleX + "GeckoCIRCUITS");
        sea._typElement = null;
        _se.resetCircuitSheetsForNewFile();
        _se.resetModelModified();  // damit wird verhindert, dass 'QuitWithoutSaving'-Dialog aufgerufen wird, obwohl Datei bereits gespeichert
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
    }

    public void openFileDialog() {
        File currentDirectory = null;
        try {
            currentDirectory = new File(GlobalFilePathes.DATNAM);
        } catch (Exception e) {
            try {
                currentDirectory = new File(GlobalFilePathes.PFAD_JAR_HOME);
            } catch (Exception ex) {
                currentDirectory = null;
            }
        }

        GeckoFileChooser fileChooser = GeckoFileChooser.createOpenFileChooser(".ipes", "Circuit Simulation Files (*.ipes)", this, currentDirectory);
        if (fileChooser.getUserResult() == GeckoFileChooser.FileChooserResult.CANCEL) {
            return;
        }
        try {
            openFile(fileChooser.getFileWithCheckedEnding().getAbsolutePath());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Fenster.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setAnsicht() {
        // Ansicht-Menu entsprechend setzen
        vItemShowNameLK.setState(SchematischeEingabe2._lkDisplayMode.showName);
        vItemShowParLK.setState(SchematischeEingabe2._lkDisplayMode.showParameter);
        vItemShowTextLineLK.setState(SchematischeEingabe2._lkDisplayMode.showTextLine);
        vItemShowFlowLK.setState(SchematischeEingabe2._lkDisplayMode.showFlowSymbol);
        vItemShowNameCONTROL.setState(SchematischeEingabe2._controlDisplayMode.showName);
        vItemShowParCONTROL.setState(SchematischeEingabe2._controlDisplayMode.showParameter);
        vItemShowTextLineCONTROL.setState(SchematischeEingabe2._controlDisplayMode.showTextLine);
        vItemShowNameTHERM.setState(SchematischeEingabe2._thermDisplayMode.showName);
        vItemShowParTHERM.setState(SchematischeEingabe2._thermDisplayMode.showParameter);
        vItemShowTextLineTHERM.setState(SchematischeEingabe2._thermDisplayMode.showTextLine);
        vItemShowFlowTHERM.setState(SchematischeEingabe2._thermDisplayMode.showFlowSymbol);
    }

    public void openFile(BufferedReader in) throws IOException {
        _se.resetCircuitSheetsForNewFile();
        Vector datVec = new Vector();
        String z = null;
        while ((z = in.readLine()) != null) {
            datVec.addElement(z);
        }
        in.close();
        String[] zeile = new String[datVec.size()];
        for (int i1 = 0; i1 < datVec.size(); i1++) {
            zeile[i1] = (String) datVec.elementAt(i1);
            //System.out.println("zeile[i1]= "+zeile[i1]);
        }

        DatenSpeicher daten = new DatenSpeicher(zeile, false, null);
        _se.resetCircuitSheetsForNewFile();
        optimizerParameterData.clearAndInitializeWithoutUndo(daten._optimizerNames, daten._optimizerData);
        _se.ladeGespeicherteNetzlisteVonDatenSpeicher(daten, null);
        AbstractCircuitSheetComponent.dpixValue.setValueWithoutUndo(daten.dpix);
        daten.updateSolverSettings(_solverSettings);

        

        int fensterWidth = daten._fensterWidth, fensterHeight = daten._fensterHeight;
        if ((fensterWidth != -1) && (fensterHeight != -1)) {
            this.setSize(fensterWidth, fensterHeight);
        }

        if (daten.fontSize == 0) {
            daten.fontSize = 12;  // default, falls keine Daten vorhanden
        }
        if (daten._fontTyp == null) {
            daten._fontTyp = "Arial";
        }

        //
        _se._circuitSheet._worksheetSize.setNewWorksheetSize(daten.sizeX, daten.sizeY);
        _se.setzeFont(daten.fontSize, daten._fontTyp);
        _se.updateAllComponentReferences();
        _se.resetModelModified();  // damit wird verhindert, dass 'QuitWithoutSaving'-Dialog aufgerufen wird, obwohl Datei bereits gespeichert
        _fileManager = new GeckoFileManager(daten.fileMgrFiles);
        _se.initAdditionalFiles(_se._circuitSheet.getAllElements().getClassFromContainer(AbstractBlockInterface.class));
    }

    public void openFile(String dateiName) throws FileNotFoundException {
        _se.resetCircuitSheetsForNewFile();
        GlobalFilePathes.DATNAM = dateiName;
        _se.resetCircuitSheetsForNewFile();
        DatenSpeicher daten = loadDatenSpeicherFromFile(dateiName, false, optimizerParameterData);        
        daten.updateSolverSettings(_solverSettings);        
        _se.ladeGespeicherteNetzlisteVonDatenSpeicher(daten, null);

        AbstractCircuitSheetComponent.dpixValue.setValue(daten.dpix);

        int fensterWidth = daten._fensterWidth, fensterHeight = daten._fensterHeight;

        if ((fensterWidth != -1) && (fensterHeight != -1)) {
            this.setSize(fensterWidth, fensterHeight);
        }

        if (daten.fontSize == 0) {
            daten.fontSize = 12;  // default, falls keine Daten vorhanden
        }
        if (daten._fontTyp == null) {
            daten._fontTyp = "Arial";
        }
        _se.setzeFont(daten.fontSize, daten._fontTyp);
        try {
            _se._circuitSheet._worksheetSize.setNewWorksheetSize(daten.sizeX, daten.sizeY);
        } catch (IllegalArgumentException ex) {
            _se._circuitSheet._worksheetSize.setNewWorksheetSize(900, 900);
        }

        aktuellerDateiName = dateiName;
        _se.updateAllComponentReferences();
        _se.resetModelModified();  // damit wird verhindert, dass 'QuitWithoutSaving'-Dialog aufgerufen wird, obwohl Datei bereits gespeichert

        _scripter.clearData();
        _scripter.setScriptCode(daten._scripterCode);
        _scripter.setDeclarationCode(daten._scripterDeclarations);
        _scripter.setImportCode(daten._scripterImports);
        _scripter.setExtraFilesHashBlock(daten._scripterExtraFiles);
        _fileManager = new GeckoFileManager(daten.fileMgrFiles);
        _se.initAdditionalFiles(_se._circuitSheet.getAllElements().getClassFromContainer(AbstractBlockInterface.class));
        this.setTitle(aktuellerDateiName + spTitleX + "GeckoCIRCUITS");
        this.aktualisierePropertiesRECENT(aktuellerDateiName);
        uniqueFileID = daten._uniqueFileId;

        // don't do this for two reasons: first - how it is implemented now with 
        // static ReglerFromExternal and ReglerToExtral ArrayLists, the simulink
        // interfaces are loaded twice!
        // second: the window which asks for the autobackup halts the whole simulink system!
        if (GeckoSim.operatingmode == OperatingMode.STANDALONE) {
            try {
                if (!IS_APPLET && !GeckoSim._isTestingMode) {
                    checkAutoBackupFileId(daten._uniqueFileId);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("Warning: Check for auto-backup file disabled!");
        }
        long toc = System.currentTimeMillis();
        //System.out.println("loading time: " + (toc - tic) / 1000.0);        
    }

    public void actionPerformed(ActionEvent ae) {
        if (!simulatorAktiviert) {
            return;
        }
        //
        String befehl = ae.getActionCommand();
        try {
            //========================================================================
            if (befehl.equals("New")) {
                _se.setConnectorTestMode(false);
                if (Fenster.IS_APPLET) {
                    this.createNewFile();
                } else {
                    // 'createNewFile()' wird optional vom Dialog aus aufgerufen
                    if (_se.getZustandGeaendert()) {

                        int returnOption = JOptionPane.showConfirmDialog(
                                this,
                                "The content of the file has changed.\nDo you want to save the changes?\n",
                                "Warning: Create new file",
                                JOptionPane.YES_NO_CANCEL_OPTION);

                        switch (returnOption) {
                            case 0:
                                saveFile();
                            case 1: // just exit, without saving
                                createNewFile();
                                break;
                            case 2: // cancel option
                                break;
                            default:
                                assert false;
                        }
                    } else {
                        this.createNewFile();
                    }
                }
            } else if (befehl.equals("Open")) {
                _se.setConnectorTestMode(false);
                if (Fenster.IS_APPLET) {
                    DialogAppletExamples dialogAppletExamples = new DialogAppletExamples(datnamExampleApplet, this);
                } else {
                    if (_se.getZustandGeaendert()) {
                        int returnOption = JOptionPane.showConfirmDialog(
                                this,
                                "The content of the file has changed.\nDo you want to save the changes?\n",
                                "Warning: Open new file",
                                JOptionPane.YES_NO_CANCEL_OPTION);

                        switch (returnOption) {
                            case 0:
                                saveFile();
                            case 1: // just exit, without saving
                                this.openFileDialog();
                                this.setAnsicht();
                                break;
                            case 2: // cancel option
                                break;
                            default:
                                assert false;
                        }
                    } else {
                        this.openFileDialog();
                        this.setAnsicht();
                    }
                }
            } else if (befehl.equals("Save")) {
                this.saveFile();
            } else if (befehl.equals("Save As")) {
                this.saveFileAs();
            } else if (befehl.equals("SaveApplet")) {
                File currentDirectory = null;
                try {
                    currentDirectory = new File(GlobalFilePathes.DATNAM);
                } catch (Exception e) {
                    currentDirectory = new File(GlobalFilePathes.PFAD_JAR_HOME);
                }

                GeckoFileChooser fileChooser = GeckoFileChooser.createSaveFileChooser(".jar", ".jar Simulation Applet (*.jar)", this, currentDirectory);

                if (fileChooser.getUserResult() == GeckoFileChooser.FileChooserResult.OK) {
                    File appletFile = fileChooser.getFileWithCheckedEnding();
                    saveAsApplet(appletFile);
                    return;
                }
            } else if (befehl.equals("Save View as Image")) {
                try {
                    new SaveViewFrame(this, _se._visibleCircuitSheet).setVisible(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            ex.getMessage(),
                            "Error!",
                            JOptionPane.ERROR_MESSAGE);
                } catch (OutOfMemoryError ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            ex.getMessage(),
                            "Error!",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else if (befehl.equals("Exit")) {
                this.schliesseProgramm();
                //--------------------------------------------
            } else if (befehl.equals("RECENT_1")) {
                _se.setConnectorTestMode(false);
                if (GlobalFilePathes.RECENT_CIRCUITS_1.equals("")) {
                    return;
                }
                if (_se.getZustandGeaendert()) {
                    int returnOption = JOptionPane.showConfirmDialog(
                            this,
                            "The content of the file has changed.\nDo you want to save the changes?\n",
                            "Warning: Open new file",
                            JOptionPane.YES_NO_CANCEL_OPTION);

                    switch (returnOption) {
                        case 0:
                            saveFile();
                        case 1: // just exit, without saving
                            loadFileFromList_withoutSaving(1);
                            this.setAnsicht();
                            break;
                        case 2: // cancel option
                            break;
                        default:
                            assert false;
                    }
                } else {
                    this.openFile(GlobalFilePathes.RECENT_CIRCUITS_1);
                    this.setAnsicht();
                }
            } else if (befehl.equals("RECENT_2")) {
                _se.setConnectorTestMode(false);
                if (GlobalFilePathes.RECENT_CIRCUITS_2.equals("")) {
                    return;
                }
                if (_se.getZustandGeaendert()) {
                    int returnOption = JOptionPane.showConfirmDialog(
                            this,
                            "The content of the file has changed.\nDo you want to save the changes?\n",
                            "Warning: Open new file",
                            JOptionPane.YES_NO_CANCEL_OPTION);

                    switch (returnOption) {
                        case 0:
                            saveFile();
                        case 1: // just exit, without saving
                            loadFileFromList_withoutSaving(2);
                            this.setAnsicht();
                            break;
                        case 2: // cancel option
                            break;
                        default:
                            assert false;
                    }
                } else {
                    this.openFile(GlobalFilePathes.RECENT_CIRCUITS_2);
                    this.setAnsicht();
                }
            } else if (befehl.equals("RECENT_3")) {
                _se.setConnectorTestMode(false);
                if (GlobalFilePathes.RECENT_CIRCUITS_3.equals("")) {
                    return;
                }
                if (_se.getZustandGeaendert()) {
                    int returnOption = JOptionPane.showConfirmDialog(
                            this,
                            "The content of the file has changed.\nDo you want to save the changes?\n",
                            "Warning: Open new file",
                            JOptionPane.YES_NO_CANCEL_OPTION);

                    switch (returnOption) {
                        case 0:
                            saveFile();
                        case 1: // just exit, without saving
                            loadFileFromList_withoutSaving(3);
                            this.setAnsicht();
                            break;
                        case 2: // cancel option
                            break;
                        default:
                            assert false;
                    }
                } else {
                    this.openFile(GlobalFilePathes.RECENT_CIRCUITS_3);
                    this.setAnsicht();
                }
            } else if (befehl.equals("RECENT_4")) {
                _se.setConnectorTestMode(false);
                if (GlobalFilePathes.RECENT_CIRCUITS_4.equals("")) {
                    return;
                }
                if (_se.getZustandGeaendert()) {
                    int returnOption = JOptionPane.showConfirmDialog(
                            this,
                            "The content of the file has changed.\nDo you want to save the changes?\n",
                            "Warning: Open new file",
                            JOptionPane.YES_NO_CANCEL_OPTION);

                    switch (returnOption) {
                        case 0:
                            saveFile();
                        case 1: // just exit, without saving
                            loadFileFromList_withoutSaving(4);
                            this.setAnsicht();
                            break;
                        case 2: // cancel option
                            break;
                        default:
                            assert false;
                    }
                } else {
                    this.openFile(GlobalFilePathes.RECENT_CIRCUITS_4);
                    this.setAnsicht();
                }
                //========================================================================
            } else if (befehl.equals("Move Elements")) {
                _se.verschiebeAllesImBearbeitungsModus();
            } else if (befehl.equals("Copy Elements")) {
                _se.kopiereAllesImBearbeitungsModus();
            } else if (befehl.equals("Undo")) {
                if (AbstractUndoGenericModel.undoManager.canUndo()) {
                    AbstractUndoGenericModel.undoManager.undo();
                    _se._visibleCircuitSheet.repaint();
                }
            } else if (befehl.equals("Redo")) {
                if (AbstractUndoGenericModel.undoManager.canRedo()) {
                    AbstractUndoGenericModel.undoManager.redo();
                    _se._visibleCircuitSheet.repaint();
                }
            } else if (befehl.equals("Delete Elements")) {
                _se.deleteSelectedComponentsWithUndo();
            } else if (befehl.equals("Deselect")) {
                _se.deselect();
            } else if (befehl.equals("SelectAll")) {
                _se.selectAll();
            } else if (befehl.equals("Disable")) {
                _se.toggleEnable(false);
            } else if (befehl.equals("DisableShort")) {
                _se.toggleEnable(true);
            } else if (befehl.equals("Import")) {
                _se.importFromClipboard();
            } else if (befehl.equals("ImportFromFile")) {
                File currentDirectory = null;
                try {
                    currentDirectory = new File(GlobalFilePathes.DATNAM);
                } catch (Exception e) {
                    try {
                        currentDirectory = new File(GlobalFilePathes.PFAD_JAR_HOME);
                    } catch (Exception ex) {
                        currentDirectory = null;
                    }
                }
                GeckoFileChooser fileChooser = GeckoFileChooser.createOpenFileChooser(".ipes", "Circuit Simulation Files (*.ipes)", this, currentDirectory);
                if (fileChooser.getUserResult() == GeckoFileChooser.FileChooserResult.CANCEL) {
                    return;
                }                
                    final String fileName = fileChooser.getFileWithCheckedEnding().getAbsolutePath();                    
                    try {
                        if((new File(fileName)).exists()) {
                            String[] fileLines = getLinesArrayFromIpesFile(fileName);
                            _se.readSelectedElementsFromASCIIString(fileLines);                    
                        } else {
                            throw new FileNotFoundException("File not found " + fileName);
                        }                        
                    } catch(Throwable error) {
                        JOptionPane.showMessageDialog(Fenster.this,
                            "Error during file import : " + fileName + "\n" + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }                                        
            } else if (befehl.equals("Export")) {
                _se.export_allesImBearbeitungsModus();
                //========================================================================
            } else if (befehl.equals("Parameter")) {
                openParameterMenu(this);
            } else if (befehl.equals("Init & Start")) {
                initStartWithErrorDialogMessage();
            } else if (befehl.equals("Pause")) {
                this.pauseSimulation();
            } else if (befehl.equals("Continue")) {
                continueCalculationWithPossibleErrorMessage();
            } else if (befehl.equals(
                    "vItemShowNameLK")) {
                SchematischeEingabe2._lkDisplayMode.showName = vItemShowNameLK.getState();
                if ((!SchematischeEingabe2._lkDisplayMode.showName) && (!SchematischeEingabe2._lkDisplayMode.showParameter)) {
                    vItemShowTextLineLK.setState(false);
                    SchematischeEingabe2._lkDisplayMode.showTextLine = vItemShowTextLineLK.getState();
                }
                _se._visibleCircuitSheet.repaint();
            } else if (befehl.equals(
                    "aliasingCommand")) {
                _se.setAntialiasing(aliasingCONTROL.isSelected());
                jtfStatus.setAliasing(aliasingCONTROL.isSelected());
            } else if (befehl.equals(
                    "vItemShowParLK")) {
                SchematischeEingabe2._lkDisplayMode.showParameter = vItemShowParLK.getState();
                if ((!SchematischeEingabe2._lkDisplayMode.showName) && (!SchematischeEingabe2._lkDisplayMode.showParameter)) {
                    vItemShowTextLineLK.setState(false);
                    SchematischeEingabe2._lkDisplayMode.showTextLine = vItemShowTextLineLK.getState();
                }
                _se._visibleCircuitSheet.repaint();
            } else if (befehl.equals(
                    "vItemShowTextLineLK")) {
                SchematischeEingabe2._lkDisplayMode.showTextLine = vItemShowTextLineLK.getState();
                _se._visibleCircuitSheet.repaint();
            } else if (befehl.equals(
                    "vItemShowFlowLK")) {
                SchematischeEingabe2._lkDisplayMode.showFlowSymbol = vItemShowFlowLK.getState();
                _se._visibleCircuitSheet.repaint();
                //---------------------------------------------------------
            } else if (befehl.equals(
                    "vItemShowNameCONTROL")) {
                SchematischeEingabe2._controlDisplayMode.showName = vItemShowNameCONTROL.getState();
                if ((!SchematischeEingabe2._controlDisplayMode.showName) && (!SchematischeEingabe2._controlDisplayMode.showParameter)) {
                    vItemShowTextLineCONTROL.setState(false);
                    SchematischeEingabe2._controlDisplayMode.showTextLine = vItemShowTextLineCONTROL.getState();
                }
                _se._circuitSheet.repaint();
            } else if (befehl.equals(
                    "vItemShowParCONTROL")) {
                SchematischeEingabe2._controlDisplayMode.showParameter = vItemShowParCONTROL.getState();
                if ((!SchematischeEingabe2._controlDisplayMode.showName) && (!SchematischeEingabe2._controlDisplayMode.showParameter)) {
                    vItemShowTextLineCONTROL.setState(false);
                    SchematischeEingabe2._controlDisplayMode.showTextLine = vItemShowTextLineCONTROL.getState();
                }
                _se._circuitSheet.repaint();
            } else if (befehl.equals(
                    "vItemShowTextLineCONTROL")) {
                SchematischeEingabe2._controlDisplayMode.showTextLine = vItemShowTextLineCONTROL.getState();
                _se._circuitSheet.repaint();
                //---------------------------------------------------------
            } else if (befehl.equals(
                    "vItemShowNameTHERM")) {
                SchematischeEingabe2._thermDisplayMode.showName = vItemShowNameTHERM.getState();
                if ((!SchematischeEingabe2._thermDisplayMode.showName) && (!SchematischeEingabe2._thermDisplayMode.showParameter)) {
                    vItemShowTextLineTHERM.setState(false);
                    SchematischeEingabe2._thermDisplayMode.showTextLine = vItemShowTextLineTHERM.getState();
                }
                _se._circuitSheet.repaint();
            } else if (befehl.equals(
                    "vItemShowParTHERM")) {
                SchematischeEingabe2._thermDisplayMode.showParameter = vItemShowParTHERM.getState();
                if ((!SchematischeEingabe2._thermDisplayMode.showName) && (!SchematischeEingabe2._thermDisplayMode.showParameter)) {
                    vItemShowTextLineTHERM.setState(false);
                    SchematischeEingabe2._thermDisplayMode.showTextLine = vItemShowTextLineTHERM.getState();
                }
                _se._circuitSheet.repaint();
            } else if (befehl.equals(
                    "vItemShowTextLineTHERM")) {
                SchematischeEingabe2._thermDisplayMode.showTextLine = vItemShowTextLineTHERM.getState();
                _se._circuitSheet.repaint();
            } else if (befehl.equals(
                    "vItemShowFlowTHERM")) {
                SchematischeEingabe2._thermDisplayMode.showFlowSymbol = vItemShowFlowTHERM.getState();
                _se._circuitSheet.repaint();
            } else if (befehl.equals(
                    "setParameters")) {
                new DialogOptimizerParameterSettings(optimizerParameterData, _se.getBlockInterfaceComponents());
            } else if (befehl.equals(
                    "setOrder")) {
                NetzlisteAllg nlA = NetzlisteAllg.fabricNetzlistComplete(_se.getConnection(ConnectorType.CONTROL), _se.getElementCONTROL());
                NetzlisteCONTROL.getOptimizedList(nlA);
                new DialogControlOrderN(this, true, NetzlisteCONTROL.getOptimizedList(nlA));
            } else if (befehl.equals(
                    "mItemConnectorTest")) {
                boolean checkConnectors = mItemConnectorTest.getState();
                _se.setConnectorTestMode(checkConnectors);
            } else if (befehl.equals(
                    "mItemFind")) {
                new DialogFindInModel(this, false, _se);
            } else if (befehl.equals(
                    "mItemCheckModel")) {
                NetzlisteAllg nlA = NetzlisteAllg.fabricNetzlistComplete(_se.getConnection(ConnectorType.CONTROL), _se.getElementCONTROL());
                NetListLK nlL = _se.getNetzliste(ConnectorType.LK_AND_RELUCTANCE);
                new DialogControlCheck(this, true, NetzlisteCONTROL.FabricRunSimulation(nlA)).setVisible(true);
            } else if (befehl.equals("memorySettings")) {
                DialogMemory dm = new DialogMemory();

            } else if (befehl.equals("updateSettings")) {
                DialogUpdateSettings du = new DialogUpdateSettings();
                du.setVisible(true);
            } else if (befehl.equals(
                    "remoteSettings")) {
                DialogRemotePort drp = new DialogRemotePort(this, false);
                drp.setVisible(true);
            } else if (befehl.equals(
                    "3Dtherm")) {
            } else if (befehl.equals(
                    "geckoScript")) {
                _scripter.makeVisible();
            } else if (befehl.equals(
                    "magnet")) {
            } else if (befehl.equals(
                    "3Delmag")) {
                System.out.println("Nicht implementiert");
            } else if (befehl.equals(
                    "optimize")) {
                System.out.println("Nicht implementiert");
            } else if (befehl.equals(
                    "About")) {
                doAboutDialog();
            } else if (befehl.equals(
                    "Feedback")) {
                new DialogFeedback(this);
            } else if (befehl.equals(
                    "Licensing")) {
                new DialogLicensing();
            } else if (befehl.equals(
                    "Update")) {
                DialogUpdate du = new DialogUpdate();
                du.setVisible(true);
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public void openParameterMenu(final Frame parent) {
        _solverSettings._dt_ALT = _solverSettings.dt.getValue();
        DialogSimParameter dialogSim = new DialogSimParameter(parent, _solverSettings);
        dialogSim.setLocationRelativeTo(parent);
        dialogSim.setVisible(true);
        //_se._circuitSheet.requestFocus();
    }

    private void doAboutDialog() {
        DialogAbout about = new DialogAbout();
        about.setLocationRelativeTo(this);
        about.setVisible(true);
        _se._circuitSheet.requestFocus();
    }

    // called from DialogNewWithoutSaving --> 
    public void loadFileFromList_withoutSaving(int nrOfFile) {
        try {
            switch (nrOfFile) {
                case 1:
                    this.openFile(GlobalFilePathes.RECENT_CIRCUITS_1);
                    break;
                case 2:
                    this.openFile(GlobalFilePathes.RECENT_CIRCUITS_2);
                    break;
                case 3:
                    this.openFile(GlobalFilePathes.RECENT_CIRCUITS_3);
                    break;
                case 4:
                    this.openFile(GlobalFilePathes.RECENT_CIRCUITS_4);
                    break;
                default:
                    break;
            }
            this.setAnsicht();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Fenster.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setConnectorTestCheckBox(boolean cb) {
        mItemConnectorTest.setState(cb);
    }

    //end simulation - control from GeckoSCRIPT
    public void endSim() {
        _simRunner.simKern.endSim();
        jtfStatus.setText("Stopped after  ");
        //

        if (IS_BRANDED) {
            mItemNew.setEnabled(false);
            mItemOpen.setEnabled(false);
        } else {
            mItemNew.setEnabled(true);
            mItemOpen.setEnabled(true);
        }
        mItemExit.setEnabled(true);
        if (!Fenster.IS_APPLET) {
            mItemSave.setEnabled(true);
            mItemSaveAs.setEnabled(true);
            mItemSaveView.setEnabled(true);
        }
        setzeSTATUS("Stopped after  ");
        pauseSimulation();
        setMenuDuringSimulation(false, true);

    }

    public void continueCalculation(final boolean createNewSimThread) throws Exception {
        _simRunner.continueCalculation(createNewSimThread, _solverSettings);
    }

    public void pauseSimulation() {
        _simRunner.pauseSimulation();
    }

    public void setMenuDuringSimulation(boolean simulationRunning, boolean endReached) {
        if (simulationRunning) {
            mItemNew.setEnabled(false);
            mItemOpen.setEnabled(false);
            mItemSave.setEnabled(false);
            mItemSaveAs.setEnabled(false);
            mItemExit.setEnabled(false);
            mItemSaveView.setEnabled(false);
            mItemCopy.setEnabled(false);
            mItemMove.setEnabled(false);
            mItemDelete.setEnabled(false);
            if (mItemParameter != null) {
                mItemParameter.setEnabled(false);
                mItemRun.setEnabled(false);
                //mItemGoSteadyState.setEnabled(false);
                //mItemSaveState.setEnabled(false);
                //mItemLoadState.setEnabled(false);
                //mItemResetState.setEnabled(false);
                //mItemClearStates.setEnabled(false);
                mItemStop.setEnabled(true);
                mItemContinue.setEnabled(false);
            }

            mItemSaveApplet.setEnabled(false);
        } else if (endReached) {
            if (!IS_BRANDED) {
                mItemNew.setEnabled(true);
                mItemOpen.setEnabled(true);
            }
            mItemExit.setEnabled(true);
            if (!Fenster.IS_APPLET) {
                mItemSave.setEnabled(true);
                mItemSaveAs.setEnabled(true);
                mItemSaveView.setEnabled(true);
                mItemSaveApplet.setEnabled(true);
            }

            /*
             * mItemUndo.setEnabled(true); mItemRedo.setEnabled(true);
             */
            mItemCopy.setEnabled(true);
            mItemMove.setEnabled(true);
            mItemDelete.setEnabled(true);

            if (mItemParameter != null) {
                mItemParameter.setEnabled(true);
                mItemRun.setEnabled(true);
                mItemStop.setEnabled(false);
                mItemContinue.setEnabled(true);
            }
        }

    }

    /**
     * Set up the simulation menu. this gets called both at initial gui
     * generation, and at a provide license event. We look up the simulation
     * menu item, and substitute it with a new JMenuItem created here.
     */
    public void setSimulationMenu() {
        JMenu simMenu = _menuBar.getMenu(_simMenuIndex);
        simMenu.removeAll();
        //---------------
        if (Fenster.IS_APPLET) {
            this.setSimulationMenuOK(simMenu);
            mItemSaveApplet.setEnabled(false);
            return;
        }

        //---------------
            this.setSimulationMenuOK(simMenu);
        
    }

    private void setSimulationMenuOK(JMenu simMenu) {
        mItemParameter = GuiFabric.getJMenuItem(I18nKeys.PARAMETER);
        mItemParameter.setActionCommand("Parameter");
        mItemParameter.addActionListener(this);
        mItemParameter.setMnemonic(KeyEvent.VK_F5);
        mItemRun = GuiFabric.getJMenuItem(I18nKeys.INIT_AND_START);
        mItemRun.setActionCommand("Init & Start");
        mItemRun.addActionListener(this);
        mItemRun.setMnemonic(KeyEvent.VK_F1);
        mItemStop = GuiFabric.getJMenuItem(I18nKeys.PAUSE);
        mItemStop.setActionCommand("Pause");
        mItemStop.addActionListener(this);
        mItemStop.setMnemonic(KeyEvent.VK_F3);
        mItemStop.setEnabled(false);
        mItemContinue = GuiFabric.getJMenuItem(I18nKeys.CONTINUE);
        mItemContinue.setActionCommand("Continue");
        mItemContinue.addActionListener(this);
        mItemContinue.setMnemonic(KeyEvent.VK_F4);
        mItemContinue.setEnabled(false);
        simMenu.add(mItemParameter);
        mItemParameter.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        simMenu.add(mItemRun);
        mItemRun.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        simMenu.add(mItemStop);
        mItemStop.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        simMenu.add(mItemContinue);
        mItemContinue.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
        //simMenu.addSeparator();
        //simMenu.add(mItemGoSteadyState);
        //simMenu.add(mItemLoadState);
        //simMenu.add(mItemSaveState);
        //simMenu.add(mItemResetState);
        //simMenu.add(mItemClearStates);
        if (GeckoSim.operatingmode == OperatingMode.SIMULINK) {
            mItemRun.setEnabled(false);
            mItemStop.setEnabled(false);
            mItemContinue.setEnabled(false);
            //mItemGoSteadyState.setEnabled(false);
        }
    }

    public void setzeSTATUS(String txt) {
        jtfStatus.setText(txt);
    }

    //------------------------------------------------
    public void windowDeactivated(WindowEvent we) {
    }

    public void windowActivated(WindowEvent we) {
    }

    public void windowDeiconified(WindowEvent we) {
    }

    public void windowIconified(WindowEvent we) {
    }

    public void windowClosed(WindowEvent we) {
    }

    public void windowOpened(WindowEvent we) {
    }

    public void windowClosing(WindowEvent we) {
        this.schliesseProgramm();
    }

    public void componentResized(ComponentEvent ce) {
        this.aktualisiereDividerSplitPane(this.getWidth() - seaBREITE);
    }

    public void componentMoved(ComponentEvent ce) {
    }

    public void componentShown(ComponentEvent ce) {
    }

    public void componentHidden(ComponentEvent ce) {
    }
    //------------------------------------------------

    public void schliesseProgramm() {

        if (Fenster.IS_APPLET && !IS_BRANDED) {
            this.setVisible(false);
            return;
        }

        if (IS_BRANDED) {
            try {
                System.exit(0);
            } catch (Exception ex) {
                System.err.println("Cannot exit applet. Setting invisible!");
                this.setVisible(false);
            }

            return;
        }

        if (GeckoSim.operatingmode == OperatingMode.SIMULINK || GeckoSim.operatingmode == OperatingMode.EXTERNAL) {
            JOptionPane.showMessageDialog(this,
                    "You attempted to close GeckoCIRCUITS while running in Simulink-mode.\n"
                    + "Please note that only closing your MATLAB session will close GeckoCIRCUITS.\n",
                    "Aborting program exit!",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (!Fenster.IS_APPLET && !IS_BRANDED) {
            GeckoSim.saveProperties();
        }
        if (_se.getZustandGeaendert()) {
            int returnOption = JOptionPane.showConfirmDialog(
                    this,
                    "The model has changed since last save to the model\nfile. Do you like to save your model "
                    + "to the file:\n" + Fenster.aktuellerDateiName + "\nbefore exiting GeckoCIRCUITS?",
                    "Warning: Exit GeckoCIRCUITS without saving!",
                    JOptionPane.YES_NO_CANCEL_OPTION);

            switch (returnOption) {
                case 0:
                    saveFile();
                case 1: // just exit, without saving
                    if (GeckoSim.operatingmode == OperatingMode.STANDALONE) {
                        System.exit(0);
                    }
                    if (GeckoSim.operatingmode == OperatingMode.SIMULINK || GeckoSim.operatingmode == OperatingMode.EXTERNAL) {
                        dispose();
                    }
                    break;
                case 2: // cancel option
                    break;
                default:
                    assert false;
            }
        } else {
            System.exit(0);
        }
    }

    public void external_end(long tStartSimulink, long tEndSimulink) {
        jtfStatus.setzeStatusRechenzeit(tEndSimulink - tStartSimulink);
    }

    private void saveAsApplet(File outFile) {

        if (!outFile.getAbsolutePath().endsWith(".jar")) {
            outFile = new File(outFile.getAbsolutePath() + ".jar");
        }

        String jarPath = GetJarPath.getJarFilePath();
        File jarFile = new File(jarPath);
        if (jarFile.exists()) {
            copyFile(jarFile, outFile);
        } else {
            System.err.println("Jar-File does not exist:\n"
                    + jarFile.getAbsolutePath());
        }

    }

    void copyFile(final File zipFile, final File newFile) {

        final ProgressMonitor progressMonitor = new ProgressMonitor(Fenster.this, "Saving applet...", "Please wait.", 0, 100);

        class Task extends SwingWorker<Void, Void> {

            private int progress = 0;

            @Override
            public Void doInBackground() {
                final long totalFileSize = zipFile.length();
                long writtenFileSize = 0;

                try {
                    final ZipFile zipSrc = new ZipFile(zipFile);
                    final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(newFile));
                    final Enumeration srcEntries = zipSrc.entries();

                    int counter = 0;
                    while (srcEntries.hasMoreElements() && !progressMonitor.isCanceled()) {
                        counter++;
                        if (counter % 10 == 0) {
                            writtenFileSize = newFile.length();
                            progress = (int) (100.0 * writtenFileSize / totalFileSize);
                            setProgress(progress);
                        }

                        ZipEntry entry = (ZipEntry) srcEntries.nextElement();
                        ZipEntry newEntry = new ZipEntry(entry.getName());
                        zos.putNextEntry(newEntry);

                        BufferedInputStream bis = new BufferedInputStream(zipSrc.getInputStream(entry), 10000);

                        while (bis.available() > 0) {
                            zos.write(bis.read());
                        }

                        zos.closeEntry();
                        bis.close();
                    }

                    zos.putNextEntry(new ZipEntry("brand.ipes"));
                    BufferedOutputStream writer = new BufferedOutputStream(zos);
                    Fenster.this.saveFileAsApplet(writer);

                    zos.close();
                    zipSrc.close();

                    while (progress < 100) {
                        try {
                            Thread.sleep(100);
                            progress++;
                            setProgress(progress);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Fenster.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(Fenster.this,
                            "Could not write to output file: " + newFile.getAbsolutePath() + " \n Error-message: " + ex.getMessage(),
                            "Warning",
                            JOptionPane.WARNING_MESSAGE);

                }
                return null;
            }
        }
        final Task task = new Task();
        task.execute();
        task.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                progressMonitor.setProgress(task.getProgress());
            }
        });

    }

    public void saveFileAsApplet(OutputStream fOut) {
        //----------------
        if (speicherVorgangLaeuft) {
            return;
        }
        speicherVorgangLaeuft = true;
        //----
        // falls noch kein Datei-Name gewaehlt wurde -->
        if (aktuellerDateiName.equals(UNTITLED)) {
            saveFile();
        }
        try {
            aktuellerDateiName = GlobalFilePathes.DATNAM;

            DatenSpeicher datLK = new DatenSpeicher(
                    this.getSize(),
                    this.optimizerParameterData,
                    0, _scripter, _fileManager, _se, _solverSettings);
            //------------
            // Plain-Test Variante in ASCII -->
            // BufferedWriter out= new BufferedWriter(new FileWriter(GlobalFilePathes.DATNAM));
            //
            // Komprimierter Datenstrom --> reduzierte und unleserliche Datei -->
//            DeflaterOutputStream out1= new DeflaterOutputStream(new FileOutputStream(new File(GlobalFilePathes.DATNAM)));
            datLK.saveAsApplet = true;
            GZIPOutputStream out1 = new GZIPOutputStream(fOut);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(out1));
            //
            out.write(datLK.exportASCII());
            datLK.saveAsApplet = false;
            out.flush();
            out.close();
            //------------
            speicherVorgangLaeuft = false;
            _se.resetModelModified();  // damit wird verhindert, dass 'QuitWithoutSaving'-Dialog aufgerufen wird, obwohl Datei bereits gespeichert
            //
        } catch (Exception e) {
            speicherVorgangLaeuft = false;
            System.out.println(e + " peorkkkg");
        }
        this.setTitle(aktuellerDateiName + spTitleX + "GeckoCIRCUITS");
        this.aktualisierePropertiesRECENT(aktuellerDateiName);
    }

    // check if written file is bigger than a given
    // threshold. If not, print a warning message.
    private void checkWrittenFileSize(File file) {
        long size = file.length();

        if (size < 200) {
            String jarPath = GetJarPath.getJarPath();
            JOptionPane.showMessageDialog(this,
                    "GeckoCIRCUITS had a problem to save the model file:"
                    + "\n" + file.getAbsolutePath()
                    + "File size on disk is " + size + " Bytes. Please check your\n"
                    + "model and consider to open the auto-backup file: \n"
                    + jarPath + getAutoBackupFileName(),
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void generateNewFileId() {
        Random random = new Random(System.currentTimeMillis());
        uniqueFileID = random.nextInt();
    }

    private static String getAutoBackupFileName() {
        String userName = System.getProperty("user.name");
        String jarPath = GetJarPath.getJarPath();
        File propertiesFile = GeckoSim.findOrCreateAppDataDirectory();
        String backupFileName = jarPath + "autoBackup" + userName + ".ipes";
        if (propertiesFile != null) {
            backupFileName = propertiesFile.getAbsolutePath() + "/autoBackup.ipes";
        }
        return backupFileName;
    }

    private void checkAutoBackupFileId(int openFileId) {

        DatenSpeicher daten = null;
        String dateiName = getAutoBackupFileName();
        try {
            daten = loadDatenSpeicherFromFile(dateiName, true, null);
        } catch (FileNotFoundException ex) {
            System.err.println("Could not read autobackup-file: " + dateiName);
            return;
        }

        if (openFileId == daten._uniqueFileId) {
            File dateTimeTestOrig = new File(aktuellerDateiName);
            File autoBackupTimeTest = new File(dateiName);
            if (dateTimeTestOrig.lastModified() < autoBackupTimeTest.lastModified()) {
                DateFormat dFormat = new SimpleDateFormat();

                int n = JOptionPane.showConfirmDialog(
                        this,
                        "The automatic backup file has a later modification date: " + new Date(autoBackupTimeTest.lastModified())
                        + "\nthan the current model file, which was last modified on: " + new Date(autoBackupTimeTest.lastModified()) + "\n"
                        + "Do you like to load the backup file " + dateiName + "?",
                        "Warning", JOptionPane.YES_NO_OPTION);

                if (n == 0) {
                    try {
                        openFile(dateiName);

                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(Fenster.class
                                .getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

    }

    public void initStartWithErrorDialogMessage() {
        try {
            this._simRunner.startCalculation(true, _solverSettings);
        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            pauseSimulation();
            _simRunner.simKern._simulationStatus = SimulationsKern.SimulationStatus.FINISHED;
            jtfStatus.setText("Simulation aborted.");
            return;
        }
    }

    public static void importComponentsFromFile(final String fileName, final String importIntoSubCircuitName) throws FileNotFoundException {
        DatenSpeicher daten = loadDatenSpeicherFromFile(fileName, false, null);
        daten.shiftComponentReferences();
        _se.ladeGespeicherteNetzlisteVonDatenSpeicher(daten, importIntoSubCircuitName);
        _se.initAdditionalFiles(daten._allSheetComponents);
        _se.updateAllComponentReferences();
    }

    public static DatenSpeicher loadDatenSpeicherFromFile(String dateiName, boolean isAutoBackupFile, OptimizerParameterData optimizer) throws FileNotFoundException {
        if (!IS_APPLET && !(new File(dateiName).exists())) {
            throw new FileNotFoundException("File: " + GlobalFilePathes.DATNAM + " not found!");
        }
        
        DatenSpeicher daten = null;        
        String[] lines = getLinesArrayFromIpesFile(dateiName);        
        daten = new DatenSpeicher(lines, isAutoBackupFile, optimizer);        
        return daten;

    }

    public void continueCalculationWithPossibleErrorMessage() {
        try {
            this.continueCalculation(true);
        } catch (Throwable ex) {
            pauseSimulation();
            _simRunner.simKern._simulationStatus = SimulationsKern.SimulationStatus.FINISHED;
            jtfStatus.setText("Simulation aborted.");
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    class BackupTask extends TimerTask {

        public void run() {
            if (_se.getZustandGeaendert()) {
                rawSaveFile(new File(getAutoBackupFileName()));
            }

        }
    }

    public static String getTextMenuItemRECENT(String txt, int txtSpace) {
        if (txtSpace == -1) {
            return txt;
        }
        try {
            String dat = txt.substring(txt.lastIndexOf(System.getProperty("file.separator")) + 1);
            if (dat.length() + 5 > txtSpace) {
                return dat;  // nur der Dateiname
            }            // teilweise wird die Pfadangabe mitgegeben:
            int space = txtSpace - (dat.length() + 5);
            String erg = txt.substring(0, space) + " .. " + System.getProperty("file.separator") + " " + dat;
            return erg;
        } catch (Exception e) {
            return txt;
        }
    }
    
    private static String[] getLinesArrayFromIpesFile(String dateiName) {
        String[] lines = null;
        //----------
        // GZIP-Format (March 2009) - ganz neu! --> 
        try {                        
            GZIPInputStream in1 = null;
            if (Fenster.IS_APPLET) {
                in1 = new GZIPInputStream((new URL(GeckoSim.urlApplet, dateiName)).openStream());
            } else {
                in1 = new GZIPInputStream(new FileInputStream(dateiName));
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(in1));
            Vector datVec = new Vector();
            String z = null;

            while ((z = in.readLine()) != null) {
//                if(!isAutoBackupFile) {
//                    System.out.println(z);
//                }
                datVec.addElement(z);
            }
            long readFileEnd = System.currentTimeMillis();

            in.close();

            lines = new String[datVec.size()];
            for (int i1 = 0; i1 < datVec.size(); i1++) {
                lines[i1] = (String) datVec.elementAt(i1);
            }
            

        } catch (Exception e) {
            System.out.println("openFile() - GZIP >> " + e);
            e.printStackTrace();
            //----------
            // neue Version 'gezipt' -->
            try {
                InflaterInputStream in1 = new InflaterInputStream(new FileInputStream(GlobalFilePathes.DATNAM));
                BufferedReader in = new BufferedReader(new InputStreamReader(in1));
                Vector datVec = new Vector();
                String z = null;
                while ((z = in.readLine()) != null) {
                    datVec.addElement(z);
                }
                in.close();
                lines = new String[datVec.size()];
                for (int i1 = 0; i1 < datVec.size(); i1++) {
                    lines[i1] = (String) datVec.elementAt(i1);
                    //System.out.println("zeile[i1]= "+zeile[i1]);
                }                
            } catch (Exception eGZIP) {
                System.out.println("openFile() - A >> " + eGZIP);
                eGZIP.printStackTrace();
            }
        }
        return lines;
    }
    
}

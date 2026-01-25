/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.allg;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import ch.technokrat.modelviewcontrol.AbstractUndoGenericModel;

/**
 * Manages the menu bar creation for the main Fenster window.
 *
 * Extracted from Fenster.java to reduce class size and improve separation of concerns.
 * This class handles all menu item creation, configuration, and initial setup.
 * Menu action handling remains in Fenster for coordination with other components.
 *
 * @see Fenster
 */
public class FensterMenuBar {

    // Menu items - File menu
    private JMenuItem mItemNew;
    private JMenuItem mItemOpen;
    private JMenuItem mItemSave;
    private JMenuItem mItemSaveAs;
    private JMenuItem mItemSaveApplet;
    private JMenuItem mItemSaveView;
    private JMenuItem mItemExit;
    private JMenuItem mItemRECENT_1;
    private JMenuItem mItemRECENT_2;
    private JMenuItem mItemRECENT_3;
    private JMenuItem mItemRECENT_4;

    // Menu items - Edit menu
    private JMenuItem mItemUndo;
    private JMenuItem mItemRedo;
    private JMenuItem mItemCopy;
    private JMenuItem mItemMove;
    private JMenuItem mItemDelete;
    private JMenuItem mItemEscape;
    private JMenuItem mItemSelectAll;
    private JMenuItem mItemDisable;
    private JMenuItem mItemDisableShort;
    private JMenuItem mItemImport;
    private JMenuItem mItemImportFromFile;
    private JMenuItem mItemExport;

    // Menu items - View menu
    private JCheckBoxMenuItem aliasingCONTROL;
    private JCheckBoxMenuItem vItemShowNameLK;
    private JCheckBoxMenuItem vItemShowParLK;
    private JCheckBoxMenuItem vItemShowTextLineLK;
    private JCheckBoxMenuItem vItemShowFlowLK;
    private JCheckBoxMenuItem vItemShowNameCONTROL;
    private JCheckBoxMenuItem vItemShowParCONTROL;
    private JCheckBoxMenuItem vItemShowTextLineCONTROL;
    private JCheckBoxMenuItem vItemShowNameTHERM;
    private JCheckBoxMenuItem vItemShowParTHERM;
    private JCheckBoxMenuItem vItemShowTextLineTHERM;
    private JCheckBoxMenuItem vItemShowFlowTHERM;

    // Menu items - Tools menu
    private JMenuItem mItemMemorySettings;
    private JMenuItem mItemUpdateSettings;
    private JMenuItem mItemRemoteSettings;
    private JCheckBoxMenuItem mItemConnectorTest;
    private JMenuItem mItemSetPar;
    private JMenuItem mItemSetOrder;
    private JMenuItem mItemCheckModel;
    private JMenuItem mItemFindString;

    // Menu items - Simulation menu
    private JMenuItem mItemParameter;
    private JMenuItem mItemRun;
    private JMenuItem mItemStop;
    private JMenuItem mItemContinue;

    // Menu items - Gecko menu
    private JMenuItem mItemScriptingTool;

    // Menu bar and menus
    private final JMenuBar menuBar;
    private final JMenu fileMenu;
    private final JMenu editMenu;
    private final JMenu viewMenu;
    private final JMenu toolsMenu;
    private final JMenu simMenu;
    private final JMenu helpMenu;
    private final JMenu geckoMenu;

    private final int simMenuIndex;
    private final ActionListener actionListener;
    private final SchematischeEingabe2 se;
    private final GeckoStatusBar statusBar;

    /**
     * Creates the menu bar with all menus and menu items.
     *
     * @param actionListener The listener for menu actions (typically Fenster)
     * @param se The circuit editor component
     * @param statusBar The status bar to display in menu bar
     */
    public FensterMenuBar(ActionListener actionListener, SchematischeEingabe2 se, GeckoStatusBar statusBar) {
        this.actionListener = actionListener;
        this.se = se;
        this.statusBar = statusBar;
        this.simMenuIndex = 2;

        menuBar = new JMenuBar();

        fileMenu = createFileMenu();
        editMenu = createEditMenu();
        viewMenu = createViewMenu();
        toolsMenu = createToolsMenu();
        simMenu = GuiFabric.getJMenu(I18nKeys.SIMULATION);
        helpMenu = createHelpMenu();
        geckoMenu = createGeckoMenu();

        assembleMenuBar();
        setupSimulationMenu();
    }

    /**
     * Gets the JMenuBar for adding to the window.
     */
    public JMenuBar getMenuBar() {
        return menuBar;
    }

    /**
     * Gets the simulation menu index in the menu bar.
     */
    public int getSimMenuIndex() {
        return simMenuIndex;
    }

    private JMenu createFileMenu() {
        JMenu menu = GuiFabric.getJMenu(I18nKeys.FILE);

        mItemNew = GuiFabric.getJMenuItem(I18nKeys.NEW);
        mItemNew.setActionCommand("New");
        mItemNew.addActionListener(actionListener);
        mItemNew.setMnemonic(KeyEvent.VK_N);

        mItemOpen = GuiFabric.getJMenuItem(I18nKeys.OPEN);
        mItemOpen.setActionCommand("Open");
        mItemOpen.addActionListener(actionListener);
        mItemOpen.setMnemonic(KeyEvent.VK_O);

        mItemSave = GuiFabric.getJMenuItem(I18nKeys.SAVE);
        mItemSave.setActionCommand("Save");
        mItemSave.addActionListener(actionListener);
        mItemSave.setMnemonic(KeyEvent.VK_S);

        mItemSaveAs = GuiFabric.getJMenuItem(I18nKeys.SAVE_AS);
        mItemSaveAs.setActionCommand("Save As");
        mItemSaveAs.addActionListener(actionListener);

        mItemSaveApplet = GuiFabric.getJMenuItem(I18nKeys.SAVE_AS_APPLET);
        mItemSaveApplet.setActionCommand("SaveApplet");
        mItemSaveApplet.addActionListener(actionListener);

        if (Fenster.IS_BRANDED) {
            mItemNew.setEnabled(false);
            mItemOpen.setEnabled(false);
            mItemSaveApplet.setEnabled(false);
        }

        if (Fenster.IS_APPLET) {
            mItemSaveApplet.setEnabled(false);
        }

        mItemSaveView = GuiFabric.getJMenuItem(I18nKeys.SAVE_VIEW_AS_IMAGE);
        mItemSaveView.setActionCommand("Save View as Image");
        mItemSaveView.addActionListener(actionListener);

        mItemExit = GuiFabric.getJMenuItem(I18nKeys.EXIT);
        mItemExit.setActionCommand("Exit");
        mItemExit.addActionListener(actionListener);

        if (Fenster.IS_APPLET) {
            mItemSave.setEnabled(false);
            mItemSaveAs.setEnabled(false);
            mItemSaveView.setEnabled(false);
        }

        menu.add(mItemNew);
        mItemNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK));
        menu.add(mItemOpen);
        mItemOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
        menu.add(mItemSave);
        mItemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));
        menu.add(mItemSaveAs);
        menu.add(mItemSaveApplet);
        menu.add(mItemSaveView);
        menu.add(mItemExit);

        // Recent files
        menu.addSeparator();
        mItemRECENT_1 = new JMenuItem(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_1, Fenster.RECENT_FILE_SPACE));
        mItemRECENT_1.addActionListener(actionListener);
        mItemRECENT_1.setActionCommand("RECENT_1");
        mItemRECENT_2 = new JMenuItem(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_2, Fenster.RECENT_FILE_SPACE));
        mItemRECENT_2.addActionListener(actionListener);
        mItemRECENT_2.setActionCommand("RECENT_2");
        mItemRECENT_3 = new JMenuItem(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_3, Fenster.RECENT_FILE_SPACE));
        mItemRECENT_3.addActionListener(actionListener);
        mItemRECENT_3.setActionCommand("RECENT_3");
        mItemRECENT_4 = new JMenuItem(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_4, Fenster.RECENT_FILE_SPACE));
        mItemRECENT_4.addActionListener(actionListener);
        mItemRECENT_4.setActionCommand("RECENT_4");
        menu.add(mItemRECENT_1);
        menu.add(mItemRECENT_2);
        menu.add(mItemRECENT_3);
        menu.add(mItemRECENT_4);

        return menu;
    }

    private JMenu createEditMenu() {
        JMenu menu = GuiFabric.getJMenu(I18nKeys.EDIT);

        mItemUndo = GuiFabric.getJMenuItem(I18nKeys.UNDO);
        mItemUndo.setActionCommand("Undo");
        mItemUndo.addActionListener(actionListener);
        mItemUndo.setMnemonic(KeyEvent.VK_Z);

        mItemRedo = GuiFabric.getJMenuItem(I18nKeys.REDO);
        mItemRedo.setActionCommand("Redo");
        mItemRedo.addActionListener(actionListener);
        mItemRedo.setMnemonic(KeyEvent.VK_Y);

        mItemCopy = GuiFabric.getJMenuItem(I18nKeys.COPY_ELEMENTS);
        mItemCopy.setActionCommand("Copy Elements");
        mItemCopy.addActionListener(actionListener);
        mItemCopy.setMnemonic(KeyEvent.VK_C);

        mItemMove = GuiFabric.getJMenuItem(I18nKeys.MOVE_ELEMENTS);
        mItemMove.setActionCommand("Move Elements");
        mItemMove.addActionListener(actionListener);
        mItemMove.setMnemonic(KeyEvent.VK_X);

        mItemDelete = GuiFabric.getJMenuItem(I18nKeys.DELETE_ELEMENTS);
        mItemDelete.setActionCommand("Delete Elements");
        mItemDelete.addActionListener(actionListener);
        mItemDelete.setMnemonic(KeyEvent.VK_DELETE);

        mItemEscape = GuiFabric.getJMenuItem(I18nKeys.DESELECT);
        mItemEscape.setActionCommand("Deselect");
        mItemEscape.addActionListener(actionListener);
        mItemEscape.setMnemonic(KeyEvent.VK_ESCAPE);

        mItemSelectAll = GuiFabric.getJMenuItem(I18nKeys.SELECT_ALL);
        mItemSelectAll.setActionCommand("SelectAll");
        mItemSelectAll.addActionListener(actionListener);
        mItemSelectAll.setMnemonic(KeyEvent.VK_ESCAPE);

        mItemDisable = GuiFabric.getJMenuItem(I18nKeys.ENABLE_DISABLE);
        mItemDisable.setActionCommand("Disable");
        mItemDisable.addActionListener(actionListener);
        mItemDisable.setMnemonic(KeyEvent.VK_A);

        mItemDisableShort = GuiFabric.getJMenuItem(I18nKeys.SHORT_CIRCUIT_COMPONENT);
        mItemDisableShort.setActionCommand("DisableShort");
        mItemDisableShort.addActionListener(actionListener);

        mItemImport = GuiFabric.getJMenuItem(I18nKeys.IMPORT);
        mItemImport.setActionCommand("Import");
        mItemImport.addActionListener(actionListener);

        mItemImportFromFile = GuiFabric.getJMenuItem(I18nKeys.IMPORT_FROM_FILE);
        mItemImportFromFile.setActionCommand("ImportFromFile");
        mItemImportFromFile.addActionListener(actionListener);

        mItemExport = GuiFabric.getJMenuItem(I18nKeys.EXPORT);
        mItemExport.setActionCommand("Export");
        mItemExport.addActionListener(actionListener);

        menu.addSeparator();
        menu.add(mItemUndo);
        menu.add(mItemRedo);
        menu.addChangeListener(new ChangeListener() {
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

        menu.addSeparator();
        menu.add(mItemMove);
        mItemMove.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Event.CTRL_MASK));
        menu.add(mItemCopy);
        mItemCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK));
        menu.add(mItemDelete);
        menu.add(mItemEscape);
        mItemEscape.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

        menu.add(mItemSelectAll);
        mItemSelectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK));

        mItemDisable.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK));
        mItemDisableShort.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.ALT_MASK));

        menu.add(mItemDisable);
        menu.add(mItemDisableShort);

        menu.addSeparator();
        menu.add(mItemImport);
        menu.add(mItemExport);
        menu.add(mItemImportFromFile);

        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = GuiFabric.getJMenu(I18nKeys.VIEW);

        // Antialiasing
        aliasingCONTROL = new JCheckBoxMenuItem("Use Antialiasing");
        aliasingCONTROL.addActionListener(actionListener);
        aliasingCONTROL.setActionCommand("aliasingCommand");

        String valueString = GeckoSim.applicationProps.getProperty("ANTI_ALIASING");
        if (valueString != null) {
            if (Boolean.parseBoolean(valueString)) {
                se.setAntialiasing(true);
                aliasingCONTROL.setSelected(true);
                statusBar.setAliasing(aliasingCONTROL.isSelected());
            }
        }

        // LK display options
        vItemShowNameLK = new JCheckBoxMenuItem("Name");
        vItemShowNameLK.addActionListener(actionListener);
        vItemShowNameLK.setActionCommand("vItemShowNameLK");
        vItemShowNameLK.setSelected(SchematischeEingabe2._lkDisplayMode.showName);
        vItemShowNameLK.setForeground(GlobalColors.farbeFertigElementLK);

        vItemShowParLK = new JCheckBoxMenuItem("Show Parameter");
        vItemShowParLK.addActionListener(actionListener);
        vItemShowParLK.setActionCommand("vItemShowParLK");
        vItemShowParLK.setSelected(SchematischeEingabe2._lkDisplayMode.showParameter);
        vItemShowParLK.setForeground(GlobalColors.farbeFertigElementLK);

        vItemShowTextLineLK = new JCheckBoxMenuItem("Show Text-Line");
        vItemShowTextLineLK.addActionListener(actionListener);
        vItemShowTextLineLK.setActionCommand("vItemShowTextLineLK");
        vItemShowTextLineLK.setSelected(SchematischeEingabe2._lkDisplayMode.showParameter);
        vItemShowTextLineLK.setForeground(GlobalColors.farbeFertigElementLK);

        vItemShowFlowLK = new JCheckBoxMenuItem("Flow Direction");
        vItemShowFlowLK.addActionListener(actionListener);
        vItemShowFlowLK.setActionCommand("vItemShowFlowLK");
        vItemShowFlowLK.setSelected(SchematischeEingabe2._lkDisplayMode.showFlowSymbol);
        vItemShowFlowLK.setForeground(GlobalColors.farbeFertigElementLK);

        // CONTROL display options
        vItemShowNameCONTROL = new JCheckBoxMenuItem("Name");
        vItemShowNameCONTROL.addActionListener(actionListener);
        vItemShowNameCONTROL.setActionCommand("vItemShowNameCONTROL");
        vItemShowNameCONTROL.setSelected(SchematischeEingabe2._controlDisplayMode.showName);
        vItemShowNameCONTROL.setForeground(GlobalColors.farbeFertigElementCONTROL);

        vItemShowParCONTROL = new JCheckBoxMenuItem("Show Parameter");
        vItemShowParCONTROL.addActionListener(actionListener);
        vItemShowParCONTROL.setActionCommand("vItemShowParCONTROL");
        vItemShowParCONTROL.setSelected(SchematischeEingabe2._controlDisplayMode.showParameter);
        vItemShowParCONTROL.setForeground(GlobalColors.farbeFertigElementCONTROL);

        vItemShowTextLineCONTROL = new JCheckBoxMenuItem("Show Text-Line");
        vItemShowTextLineCONTROL.addActionListener(actionListener);
        vItemShowTextLineCONTROL.setActionCommand("vItemShowTextLineCONTROL");
        vItemShowTextLineCONTROL.setSelected(SchematischeEingabe2._controlDisplayMode.showParameter);
        vItemShowTextLineCONTROL.setForeground(GlobalColors.farbeFertigElementCONTROL);

        // THERM display options
        vItemShowNameTHERM = new JCheckBoxMenuItem("Name");
        vItemShowNameTHERM.addActionListener(actionListener);
        vItemShowNameTHERM.setActionCommand("vItemShowNameTHERM");
        vItemShowNameTHERM.setSelected(SchematischeEingabe2._thermDisplayMode.showName);
        vItemShowNameTHERM.setForeground(GlobalColors.farbeFertigElementTHERM);

        vItemShowParTHERM = new JCheckBoxMenuItem("Show Parameter");
        vItemShowParTHERM.addActionListener(actionListener);
        vItemShowParTHERM.setActionCommand("vItemShowParTHERM");
        vItemShowParTHERM.setSelected(SchematischeEingabe2._thermDisplayMode.showParameter);
        vItemShowParTHERM.setForeground(GlobalColors.farbeFertigElementTHERM);

        vItemShowTextLineTHERM = new JCheckBoxMenuItem("Show Text-Line");
        vItemShowTextLineTHERM.addActionListener(actionListener);
        vItemShowTextLineTHERM.setActionCommand("vItemShowTextLineTHERM");
        vItemShowTextLineTHERM.setSelected(SchematischeEingabe2._thermDisplayMode.showParameter);
        vItemShowTextLineTHERM.setForeground(GlobalColors.farbeFertigElementTHERM);

        vItemShowFlowTHERM = new JCheckBoxMenuItem("Flow Direction");
        vItemShowFlowTHERM.addActionListener(actionListener);
        vItemShowFlowTHERM.setActionCommand("vItemShowFlowTHERM");
        vItemShowFlowTHERM.setSelected(SchematischeEingabe2._thermDisplayMode.showFlowSymbol);
        vItemShowFlowTHERM.setForeground(GlobalColors.farbeFertigElementTHERM);

        // Worksheet size menu item
        JMenuItem menueGroesse = GuiFabric.getJMenuItem(I18nKeys.WORKSHEET_SIZE);
        menueGroesse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                DialogSheetSize.fabric((Fenster) actionListener, se._visibleCircuitSheet._worksheetSize);
            }
        });

        // Scaling submenu
        JMenu menueSkalierung = GuiFabric.getJMenu(I18nKeys.SCALING);
        addScalingMenuItem(menueSkalierung, I18nKeys.POINT_10, 10);
        addScalingMenuItem(menueSkalierung, I18nKeys.POINT_12, 12);
        addScalingMenuItem(menueSkalierung, I18nKeys.POINT_14, 14);
        addScalingMenuItem(menueSkalierung, I18nKeys.POINT_16, 16);
        addScalingMenuItem(menueSkalierung, I18nKeys.POINT_18, 18);

        // Font size submenu
        JMenu menueFontSize = GuiFabric.getJMenu(I18nKeys.FONT_SIZE);
        addFontSizeMenuItem(menueFontSize, I18nKeys.POINT_6, 6);
        addFontSizeMenuItem(menueFontSize, I18nKeys.POINT_8, 8);
        addFontSizeMenuItem(menueFontSize, I18nKeys.POINT_10, 10);
        addFontSizeMenuItem(menueFontSize, I18nKeys.POINT_12, 12);
        addFontSizeMenuItem(menueFontSize, I18nKeys.POINT_14, 14);
        addFontSizeMenuItem(menueFontSize, I18nKeys.POINT_16, 16);
        addFontSizeMenuItem(menueFontSize, I18nKeys.POINT_18, 18);

        // Assemble view menu
        menu.add(menueGroesse);
        menu.add(menueSkalierung);
        menu.add(menueFontSize);
        menu.add(aliasingCONTROL);
        menu.addSeparator();
        menu.add(vItemShowNameLK);
        menu.add(vItemShowParLK);
        menu.add(vItemShowTextLineLK);
        menu.add(vItemShowFlowLK);
        menu.addSeparator();
        menu.add(vItemShowNameCONTROL);
        menu.add(vItemShowParCONTROL);
        menu.add(vItemShowTextLineCONTROL);
        menu.addSeparator();
        menu.add(vItemShowNameTHERM);
        menu.add(vItemShowParTHERM);
        menu.add(vItemShowTextLineTHERM);
        menu.add(vItemShowFlowTHERM);

        return menu;
    }

    private void addScalingMenuItem(JMenu menu, I18nKeys key, final int size) {
        JMenuItem item = GuiFabric.getJMenuItem(key);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                AbstractCircuitSheetComponent.dpixValue.setValue(size);
            }
        });
        menu.add(item);
    }

    private void addFontSizeMenuItem(JMenu menu, I18nKeys key, final int size) {
        JMenuItem item = GuiFabric.getJMenuItem(key);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                se.setzeFont(size, "Arial");
            }
        });
        menu.add(item);
    }

    private JMenu createToolsMenu() {
        JMenu menu = GuiFabric.getJMenu(I18nKeys.TOOLS);

        mItemMemorySettings = GuiFabric.getJMenuItem(I18nKeys.MEMORY_SETTINGS);
        mItemMemorySettings.setActionCommand("memorySettings");
        mItemMemorySettings.addActionListener(actionListener);

        mItemUpdateSettings = GuiFabric.getJMenuItem(I18nKeys.UPDATE_SETTINGS);
        mItemUpdateSettings.setActionCommand("updateSettings");
        mItemUpdateSettings.addActionListener(actionListener);

        mItemRemoteSettings = GuiFabric.getJMenuItem(I18nKeys.REMOTE_ACCESS_SETTINGS);
        mItemRemoteSettings.setActionCommand("remoteSettings");
        mItemRemoteSettings.addActionListener(actionListener);

        mItemConnectorTest = new JCheckBoxMenuItem("Check Connections");
        mItemConnectorTest.addActionListener(actionListener);
        mItemConnectorTest.setActionCommand("mItemConnectorTest");
        mItemConnectorTest.setSelected(false);

        mItemSetPar = GuiFabric.getJMenuItem(I18nKeys.SET_PARAMETERS);
        mItemSetPar.setActionCommand("setParameters");
        mItemSetPar.addActionListener(actionListener);

        mItemSetOrder = GuiFabric.getJMenuItem(I18nKeys.SET_ORDER_OF_CONTROL);
        mItemSetOrder.setActionCommand("setOrder");
        mItemSetOrder.addActionListener(actionListener);

        mItemCheckModel = GuiFabric.getJMenuItem(I18nKeys.CHECK_CONTROL_MODEL);
        mItemCheckModel.setActionCommand("mItemCheckModel");
        mItemCheckModel.addActionListener(actionListener);
        mItemCheckModel.setMnemonic(KeyEvent.VK_Q);

        mItemFindString = GuiFabric.getJMenuItem(I18nKeys.FIND_IN_MODEL);
        mItemFindString.setActionCommand("mItemFind");
        mItemFindString.addActionListener(actionListener);
        mItemFindString.setMnemonic(KeyEvent.VK_F);
        mItemFindString.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK));

        menu.add(mItemConnectorTest);
        mItemConnectorTest.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, Event.CTRL_MASK));
        menu.add(mItemCheckModel);
        menu.add(mItemFindString);
        mItemCheckModel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));
        menu.addSeparator();
        menu.add(mItemSetPar);
        menu.addSeparator();
        menu.add(mItemMemorySettings);
        menu.add(mItemUpdateSettings);
        menu.add(mItemRemoteSettings);

        return menu;
    }

    private JMenu createHelpMenu() {
        JMenu menu = GuiFabric.getJMenu(I18nKeys.HELP);

        JMenuItem mItemAbout = GuiFabric.getJMenuItem(I18nKeys.ABOUT);
        mItemAbout.setActionCommand("About");
        mItemAbout.addActionListener(actionListener);

        JMenuItem mItemLicenses = GuiFabric.getJMenuItem(I18nKeys.LICENSING);
        mItemLicenses.setActionCommand("Licensing");
        mItemLicenses.addActionListener(actionListener);

        JMenuItem mItemFeedback = GuiFabric.getJMenuItem(I18nKeys.FEEDBACK);
        mItemFeedback.setActionCommand("Feedback");
        mItemFeedback.addActionListener(actionListener);

        JMenuItem mItemUpdate = GuiFabric.getJMenuItem(I18nKeys.UPDATES);
        mItemUpdate.setActionCommand("Update");
        mItemUpdate.addActionListener(actionListener);

        menu.add(mItemAbout);
        menu.add(mItemLicenses);
        menu.add(mItemFeedback);
        menu.add(mItemUpdate);

        if (Fenster.IS_APPLET) {
            mItemFeedback.setEnabled(false);
        }

        return menu;
    }

    private JMenu createGeckoMenu() {
        JMenu menu = GuiFabric.getJMenu(I18nKeys.GECKO);
        menu.setForeground(GlobalColors.farbeGecko);

        mItemScriptingTool = new JMenuItem("GeckoSCRIPT");
        mItemScriptingTool.setEnabled(true);
        mItemScriptingTool.setActionCommand("geckoScript");
        mItemScriptingTool.addActionListener(actionListener);

        JMenuItem mItemOptimizerSimple = new JMenuItem("GeckoOPTIMIZER");
        mItemOptimizerSimple.setEnabled(false);
        mItemOptimizerSimple.setActionCommand("geckoOptimizer");
        mItemOptimizerSimple.addActionListener(actionListener);

        JMenuItem mItem3DTherm = new JMenuItem("GeckoHEAT");
        mItem3DTherm.setActionCommand("3Dtherm");
        mItem3DTherm.addActionListener(actionListener);

        JMenuItem mItemMagnet = new JMenuItem("GeckoMAGNETICS");
        mItemMagnet.setActionCommand("magnet");
        mItemMagnet.addActionListener(actionListener);

        JMenuItem mItemEMC = new JMenuItem("GeckoEMC");
        mItemEMC.setActionCommand("geckoEMC");
        mItemEMC.addActionListener(actionListener);

        menu.add(mItemScriptingTool);
        menu.add(mItemOptimizerSimple);
        menu.add(mItem3DTherm);
        menu.add(mItemMagnet);
        menu.add(mItemEMC);

        if (Fenster.INCLUDE_GeckoMAGNETICS) {
            mItemMagnet.setEnabled(true);
        } else {
            mItemMagnet.setEnabled(false);
        }
        if (Fenster.INCLUDE_GeckoHEAT) {
            mItem3DTherm.setEnabled(true);
        } else {
            mItem3DTherm.setEnabled(false);
        }
        if (Fenster.INCLUDE_GeckoEMC) {
            mItemEMC.setEnabled(true);
        } else {
            mItemEMC.setEnabled(false);
        }
        if (Fenster.IS_APPLET) {
            mItem3DTherm.setEnabled(false);
            mItemMagnet.setEnabled(false);
            mItemEMC.setEnabled(false);
        }

        return menu;
    }

    private void assembleMenuBar() {
        JLabel jlSpace1 = new JLabel("       ");

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(simMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
        menuBar.add(geckoMenu);
        menuBar.add(jlSpace1);
        menuBar.add(statusBar);
    }

    /**
     * Sets up the simulation menu items.
     */
    public void setupSimulationMenu() {
        simMenu.removeAll();

        if (Fenster.IS_APPLET) {
            setupSimulationMenuItems();
            mItemSaveApplet.setEnabled(false);
            return;
        }

        setupSimulationMenuItems();
    }

    private void setupSimulationMenuItems() {
        mItemParameter = GuiFabric.getJMenuItem(I18nKeys.PARAMETER);
        mItemParameter.setActionCommand("Parameter");
        mItemParameter.addActionListener(actionListener);
        mItemParameter.setMnemonic(KeyEvent.VK_F5);

        mItemRun = GuiFabric.getJMenuItem(I18nKeys.INIT_AND_START);
        mItemRun.setActionCommand("Init & Start");
        mItemRun.addActionListener(actionListener);
        mItemRun.setMnemonic(KeyEvent.VK_F1);

        mItemStop = GuiFabric.getJMenuItem(I18nKeys.PAUSE);
        mItemStop.setActionCommand("Pause");
        mItemStop.addActionListener(actionListener);
        mItemStop.setMnemonic(KeyEvent.VK_F3);
        mItemStop.setEnabled(false);

        mItemContinue = GuiFabric.getJMenuItem(I18nKeys.CONTINUE);
        mItemContinue.setActionCommand("Continue");
        mItemContinue.addActionListener(actionListener);
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

        if (GeckoSim.operatingmode == OperatingMode.SIMULINK) {
            mItemRun.setEnabled(false);
            mItemStop.setEnabled(false);
            mItemContinue.setEnabled(false);
        }
    }

    /**
     * Updates the recent files menu items.
     */
    public void updateRecentFilesMenu() {
        mItemRECENT_1.setText(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_1, Fenster.RECENT_FILE_SPACE));
        mItemRECENT_2.setText(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_2, Fenster.RECENT_FILE_SPACE));
        mItemRECENT_3.setText(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_3, Fenster.RECENT_FILE_SPACE));
        mItemRECENT_4.setText(getTextMenuItemRECENT(GlobalFilePathes.RECENT_CIRCUITS_4, Fenster.RECENT_FILE_SPACE));
    }

    /**
     * Sets the state of menu items during or after simulation.
     *
     * @param simulationRunning true if simulation is currently running
     * @param endReached true if simulation has reached the end
     */
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
                mItemStop.setEnabled(true);
                mItemContinue.setEnabled(false);
            }
            mItemSaveApplet.setEnabled(false);
        } else if (endReached) {
            if (!Fenster.IS_BRANDED) {
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
            mItemCopy.setEnabled(true);
            mItemMove.setEnabled(true);
            mItemDelete.setEnabled(true);
            if (mItemParameter != null) {
                mItemParameter.setEnabled(true);
                mItemRun.setEnabled(true);
                mItemStop.setEnabled(false);
                mItemContinue.setEnabled(false);
            }
        } else {
            // Paused state
            if (!Fenster.IS_BRANDED) {
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
            mItemCopy.setEnabled(true);
            mItemMove.setEnabled(true);
            mItemDelete.setEnabled(true);
            if (mItemParameter != null) {
                mItemParameter.setEnabled(false);
                mItemRun.setEnabled(false);
                mItemStop.setEnabled(false);
                mItemContinue.setEnabled(true);
            }
        }
    }

    /**
     * Sets the connector test checkbox state.
     */
    public void setConnectorTestCheckBox(boolean selected) {
        mItemConnectorTest.setState(selected);
    }

    /**
     * Enables or disables the scripting tool menu item.
     */
    public void setScriptingToolEnabled(boolean enabled) {
        mItemScriptingTool.setEnabled(enabled);
    }

    /**
     * Checks if simulation can be started (Run menu item is enabled).
     */
    public boolean isSimulationRunnable() {
        return mItemRun != null && mItemRun.isEnabled();
    }

    /**
     * Helper method to format recent file path for menu display.
     * Delegates to Fenster.getTextMenuItemRECENT for consistent formatting.
     */
    private String getTextMenuItemRECENT(String datnam, int maxSpaces) {
        return Fenster.getTextMenuItemRECENT(datnam, maxSpaces);
    }

    // Accessors for view menu items (needed by Fenster for state management)
    public JCheckBoxMenuItem getAliasingCONTROL() { return aliasingCONTROL; }
    public JCheckBoxMenuItem getVItemShowNameLK() { return vItemShowNameLK; }
    public JCheckBoxMenuItem getVItemShowParLK() { return vItemShowParLK; }
    public JCheckBoxMenuItem getVItemShowTextLineLK() { return vItemShowTextLineLK; }
    public JCheckBoxMenuItem getVItemShowFlowLK() { return vItemShowFlowLK; }
    public JCheckBoxMenuItem getVItemShowNameCONTROL() { return vItemShowNameCONTROL; }
    public JCheckBoxMenuItem getVItemShowParCONTROL() { return vItemShowParCONTROL; }
    public JCheckBoxMenuItem getVItemShowTextLineCONTROL() { return vItemShowTextLineCONTROL; }
    public JCheckBoxMenuItem getVItemShowNameTHERM() { return vItemShowNameTHERM; }
    public JCheckBoxMenuItem getVItemShowParTHERM() { return vItemShowParTHERM; }
    public JCheckBoxMenuItem getVItemShowTextLineTHERM() { return vItemShowTextLineTHERM; }
    public JCheckBoxMenuItem getVItemShowFlowTHERM() { return vItemShowFlowTHERM; }
}

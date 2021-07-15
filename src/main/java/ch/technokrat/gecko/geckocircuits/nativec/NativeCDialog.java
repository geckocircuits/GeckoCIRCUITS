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

package ch.technokrat.gecko.geckocircuits.nativec;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.circuit.NameAlreadyExistsException;
import ch.technokrat.gecko.geckocircuits.control.javablock.CodeWindow;
import ch.technokrat.gecko.geckocircuits.newscope.GeckoDialog;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.LangInit;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * GUI Dialog to load the Native Libraries
 * @author DIEHL Controls Ricardo Richter
 */
public class NativeCDialog extends GeckoDialog {
    private final DefaultListModel _fileList;
    private final Container _con;
    private final JButton jButtonOK = GuiFabric.getJButton(I18nKeys.OK);
    private final JList jListLibFiles;
    private final JButton jButtonOpenFile = GuiFabric.getJButton(I18nKeys.ADD_NEW);
    private final JButton jButtonRemoveSelection = GuiFabric.getJButton(I18nKeys.REMOVE_SELECTION);
    
    private final JPanel jPanelButtonOk;
    private final JPanel jPanelOpenFile;
    
    private JFileChooser jFileChooser;
    protected final NativeCDialog _thisObj;
    private final ReglerNativeC _regNCObj;
    
    private final NativeCLibraryFile _selectedLibFile;
    


    /**
     * Constructor to initialize File List loaded from File
     * @param regObj        the Object to feedback user data
     * @param parent        parent window
     * @param modal         should window be modal
     * @param libFile       Reference to NativeCLibraryFile, used to fill with user inputs
     * @param libFileList   Reference to List of added Native Libraries, used to fill with user inputs
     */
    public NativeCDialog(ReglerNativeC regObj, Window parent, boolean modal, NativeCLibraryFile libFile, DefaultListModel libFileList) {
        super(parent, modal);
        _selectedLibFile = libFile;
        _fileList = libFileList;
        jListLibFiles = new JList(_fileList);
        if (!isFileNameAlreadyInList(_selectedLibFile.getFileName())) {
            _fileList.addElement(_selectedLibFile.getFileName());
        }
        _thisObj = this;
        _regNCObj = regObj;
        _con = this.getContentPane();
        _con.setLayout(new BorderLayout());
        jButtonOpenFile.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                if (jFileChooser == null) {
                    initFileChooser();
                }
                int returnVal = jFileChooser.showOpenDialog(_thisObj);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        File currentFile = jFileChooser.getSelectedFile();
                        if (!isFileNameAlreadyInList(currentFile.getAbsolutePath())) {
                            _selectedLibFile.setFile(currentFile);
                            String selectedDLLName = _selectedLibFile.getFileName();
                            _fileList.addElement(selectedDLLName);
                            jListLibFiles.setSelectedIndex(_fileList.indexOf(selectedDLLName));
                        }
                    } catch (FileNotFoundException exc) {
                        JOptionPane.showMessageDialog(null, exc.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } );
        
        jButtonRemoveSelection.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                final int index = jListLibFiles.getSelectedIndex();
                if(index < 0) {
                    return;
                }
                _fileList.remove(index);
            }
        } );
        
        jButtonOK.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                // trigger update function to save user data in Paramter Object
                _regNCObj.triggerUpdate();
                if (_selectedLibFile.getFile() != null) {
                    // set name of block to selected Native Library filename
                    try {
                        _regNCObj.setNewNameChecked(_selectedLibFile.getFile().getName());
                    }
                    catch (NameAlreadyExistsException exc) {
                        Logger.getLogger(CodeWindow.class.getName()).log(Level.SEVERE, null, exc);
                        JOptionPane.showMessageDialog(null, "Seems like the selected Native Library is already used by another block!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                _thisObj.dispose();
            }
        });
        jListLibFiles.addListSelectionListener( new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    JList source = (JList) event.getSource();
                    int currentLibNameInd = source.getSelectedIndex();
                    if (currentLibNameInd >= 0 && currentLibNameInd < _fileList.getSize()) {
                        try {
                            _selectedLibFile.setFile((String) _fileList.get(currentLibNameInd));
                        } catch (FileNotFoundException exc) {
                            JOptionPane.showMessageDialog(null, exc.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        } );
        /** add remove option */
        jListLibFiles.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent mouseEvent) {
                //final MouseEvent mouseEvent = e;
                if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    JPopupMenu rightClickMenu = new JPopupMenu();
                    JMenuItem removeOpt = new JMenuItem("remove");
                    removeOpt.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent event) {
                            Point point = mouseEvent.getPoint();
                            int currentInd = jListLibFiles.locationToIndex(point);
                            if (currentInd >= 0 && currentInd < _fileList.size()) {
                                String deletedFileName = (String) _fileList.get(currentInd);
                                // check if selected dll file was removed from list
                                if ( _selectedLibFile.getFileName() != null && deletedFileName.equals(_selectedLibFile.getFileName())) {
                                    _selectedLibFile.setFile(); // remove all references to removed file
                                }
                                _fileList.remove(currentInd);
                                // now check if we need to set a selection again
                                if (jListLibFiles.isSelectionEmpty() && _fileList.size() > 0) {
                                    try {
                                        _selectedLibFile.setFile((String) _fileList.get(0));
                                        jListLibFiles.setSelectedIndex(0);
                                    } catch (Exception exc) {
                                        System.err.println(exc.getStackTrace());
                                        JOptionPane.showMessageDialog(null, exc.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                            }
                        }
                    });
                    rightClickMenu.add(removeOpt);
                    rightClickMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        // set window size of Dialog GUI to 30 % of Main Window ... seemed to be a good choice on my screen
        Dimension winSize = new Dimension( (int) (GeckoSim._win.getWidth() * 0.3), (int) (GeckoSim._win.getHeight() * 0.3));
        jListLibFiles.setPreferredSize(winSize);
        jPanelButtonOk = new JPanel();
        jPanelOpenFile = new JPanel();
        jPanelButtonOk.add(jButtonOK);
        jPanelOpenFile.add(jButtonOpenFile);
        jPanelOpenFile.add(jButtonRemoveSelection);
        _con.add(jPanelButtonOk, BorderLayout.SOUTH);
        _con.add(jPanelOpenFile, BorderLayout.NORTH);
        _con.add(jListLibFiles, BorderLayout.CENTER);
        if (_fileList != null) {
            int index = _fileList.indexOf(_selectedLibFile.getFileName());
            if (index < 0) {
                index = 0;
            }
            jListLibFiles.setSelectedIndex(index);
        }
        this.pack();
    }
    
    private void initFileChooser () {
        FileFilter filterLibrary = new FileNameExtensionFilter("Shared Library (*.dll, *.so, *.jnilib)", 
                "dll", "so", "jnilib"); // Windows: *.dll, Linux: *.so, Mac OS: *.jnilib
        jFileChooser = new JFileChooser();
        jFileChooser.setAcceptAllFileFilterUsed(false);
        jFileChooser.setMultiSelectionEnabled(false);
        jFileChooser.addChoosableFileFilter(filterLibrary);
        if (_selectedLibFile.getFile() != null) {
            jFileChooser.setCurrentDirectory(_selectedLibFile.getFile());
        } else {
            jFileChooser.setCurrentDirectory(new File(GlobalFilePathes.DATNAM));
        }
    }
    
    private boolean isFileNameAlreadyInList (final String name) {
        if (name == null) {
            return true;
        }
        for (int i=0; i<_fileList.size(); i++) {
            if (name.equals(_fileList.get(i))) {
                return true;
            }
        }
        return false;
    }
}

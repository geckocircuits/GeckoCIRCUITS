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

import ch.technokrat.gecko.geckocircuits.allg.GeckoFile.StorageType;
import ch.technokrat.gecko.geckocircuits.circuit.DialogNonLinearity;
import ch.technokrat.gecko.geckocircuits.circuit.GeckoFileable;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossProperties;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.VerlustBerechnungDetailed;
import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author anstupar
 */
public class GeckoFileManagerWindow extends javax.swing.JDialog {

    private VerlustBerechnungDetailed _losses = null;
    private final String _fileExtension;
    private final String _fileType;
    private final StorageType _newFileType = StorageType.EXTERNAL;
    private final JFileChooser _addFilesDialog = new JFileChooser();
    private final List<GeckoFile> _newFilesToAdd = new ArrayList<GeckoFile>();
    private final DefaultListModel _existingFilesList = new DefaultListModel();
    private final DefaultListModel _selectedFilesList = new DefaultListModel();
    private boolean _isLossElement = false;
    private boolean _singleFileOnly = true;
    private final List<GeckoFile> _filesToRemove = new ArrayList<GeckoFile>();
    private DialogNonLinearity nonLinearDialog = null;
    /**
     * pointing to ElementLKDialog text fields, for setting the loss file
     */
    private FormatJTextField _jlS2;
    private FormatJTextField _jlS2b;
    private javax.swing.JButton _jbS2edit;
    private final GeckoFileable _geckoFileable;

    /**
     * Creates new form GeckoFileManagerWindow should make a different
     * constructor for using circuit blocks (i.e. for loss files)
     */
    public GeckoFileManagerWindow(final GeckoFileable fileable, final String extension, final String type, final boolean singleFileOnly) {
        initComponents();
        _geckoFileable = fileable;
        _fileExtension = extension;
        _fileType = type;

        //add existing files of this extension, NOT including those already in this block
        List<GeckoFile> filesAlreadyAddedToElement = _geckoFileable.getFiles();
        _singleFileOnly = singleFileOnly;
        init(filesAlreadyAddedToElement, _fileExtension);
    }

    /**
     * constructor for creating a GeckoFileManager window for semiconductor loss
     * files
     *
     * @param losses
     * @param extension
     * @param type
     * @param jlS2
     * @param jlS2b
     * @param jbS2edit
     */
    public GeckoFileManagerWindow(final VerlustBerechnungDetailed losses, final String extension, final String type,
            final FormatJTextField jlS2, final FormatJTextField jlS2b, final javax.swing.JButton jbS2edit) {

        initComponents();
        _losses = losses;
        _jlS2 = jlS2;
        _jlS2b = jlS2b;
        _jbS2edit = jbS2edit;
        _fileExtension = extension;
        _fileType = type;        
        _geckoFileable = losses;        
        
        //add existing files of this extension, NOT including those already in this block        
        final List<GeckoFile> filesAlreadyAddedToElement = _geckoFileable.getFiles();        
        _singleFileOnly = true;
        _isLossElement = true;
        jListAvailableFiles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        init(filesAlreadyAddedToElement, _fileExtension);
        
    }

    private void init(final List<GeckoFile> alreadyUsedFiles, final String extension) {        
        super.setModal(true);
        try {
            this.setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif"))).getImage());
        } catch (Exception ex) {
            System.err.println("could not load image icon!");
        }

        jLabelFileType.setText("*" + _fileExtension);
        jLabelFileType2.setText("(" + _fileType + ")");

        ListSelectionModel lsmNew = jListSelectedFilesToAdd.getSelectionModel();
        lsmNew.addListSelectionListener(new GeckoFileNewListSelectionHandler());
        ListSelectionModel lsmExisting = jListAvailableFiles.getSelectionModel();
        lsmExisting.addListSelectionListener(new GeckoFileExistingListSelectionHandler());


        List<GeckoFile> existingFiles = Fenster._fileManager.getFilesByExtension(extension);

        if (alreadyUsedFiles.size() > 0) {
            existingFiles.removeAll(alreadyUsedFiles);
        }

        for (GeckoFile availableFile : existingFiles) {
            _existingFilesList.addElement(availableFile);
        }

        for (GeckoFile alreadyUsedFile : alreadyUsedFiles) {
            _selectedFilesList.addElement(alreadyUsedFile);
        }

        getRootPane().setDefaultButton(jButtonOK);
    }

    private void addNewFileToList(final File newSelectedFile) {
        try {
            GeckoFile newFile = new GeckoFile(newSelectedFile, _newFileType, Fenster.getOpenFileName());
            addGeckoFileToList(newFile);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "ERROR: File not found", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addGeckoFileToList(final GeckoFile file) {
        if (_singleFileOnly) { //check if this block allows only adding one file - e.g. external data for signal element
            _newFilesToAdd.clear();
            _selectedFilesList.clear();
        }
        _newFilesToAdd.add(file);
        _selectedFilesList.addElement(file);
        assert !_newFilesToAdd.isEmpty();
    }

    private void removeGeckoFileFromList(final GeckoFile file) {
        int index = _newFilesToAdd.indexOf(file);
        if (index == -1) { //if index is -1, then this is a file which already exists in the block, not a newly added file - add to the list of files to remove
            _filesToRemove.add(file);
        } else { //otherwise this was a new file that the user decided not to add, everything is fine
            for (GeckoFile gFile : _newFilesToAdd) {
                System.out.println(gFile);
            }
            _newFilesToAdd.remove(index);
        }
    }

    //for handling selection events
    class GeckoFileNewListSelectionHandler implements ListSelectionListener {

        @Override
        public void valueChanged(final ListSelectionEvent e) {
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();

            if (lsm.isSelectionEmpty()) {
                jRadioButtonIsExternalNew.setSelected(false);
                jRadioButtonIsInternalNew.setSelected(false);
            } else {

                Object[] selectedFiles = jListSelectedFilesToAdd.getSelectedValues();
                GeckoFile selectedFile = (GeckoFile) selectedFiles[selectedFiles.length - 1];

                if (selectedFile.getStorageType() == StorageType.EXTERNAL) {
                    jRadioButtonIsExternalNew.setSelected(true);
                    jRadioButtonIsInternalNew.setSelected(false);
                } else {
                    jRadioButtonIsExternalNew.setSelected(false);
                    jRadioButtonIsInternalNew.setSelected(true);
                }
            }

        }
    }

    class GeckoFileExistingListSelectionHandler implements ListSelectionListener {

        @Override
        public void valueChanged(final ListSelectionEvent e) {
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();

            if (lsm.isSelectionEmpty()) {
                jRadioButtonIsExternalExisting.setSelected(false);
                jRadioButtonIsInternalExisting.setSelected(false);
            } else {

                Object[] selectedFiles = jListAvailableFiles.getSelectedValues();
                GeckoFile selectedFile = (GeckoFile) selectedFiles[selectedFiles.length - 1];

                if (selectedFile.getStorageType() == StorageType.EXTERNAL) {
                    jRadioButtonIsExternalExisting.setSelected(true);
                    jRadioButtonIsInternalExisting.setSelected(false);
                } else {
                    jRadioButtonIsExternalExisting.setSelected(false);
                    jRadioButtonIsInternalExisting.setSelected(true);
                }
            }

        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupInternalExternalExistingFiles = new javax.swing.ButtonGroup();
        buttonGroupExternalInternalNew = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        jButtonOK = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jButtonRemove = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        jListSelectedFilesToAdd = new javax.swing.JList();
        jButtonAddNewFile = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jRadioButtonIsExternalNew = new javax.swing.JRadioButton();
        jRadioButtonIsInternalNew = new javax.swing.JRadioButton();
        jLabelAlreadyAvailable = new javax.swing.JLabel();
        jLabelFileType = new javax.swing.JLabel();
        jLabelFileType2 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jButtonAddSelectedExistingFiles = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jListAvailableFiles = new javax.swing.JList();
        jPanel5 = new javax.swing.JPanel();
        jRadioButtonIsInternalExisting = new javax.swing.JRadioButton();
        jRadioButtonIsExternalExisting = new javax.swing.JRadioButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Gecko File Manager - add external files to your model");
        setLocationByPlatform(true);

        jButtonOK.setText("OK");
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOKActionPerformed(evt);
            }
        });

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        jButtonRemove.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonRemove.setText("Remove Selection");
        jButtonRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRemoveActionPerformed(evt);
            }
        });

        jListSelectedFilesToAdd.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jListSelectedFilesToAdd.setModel(_selectedFilesList);
        jScrollPane4.setViewportView(jListSelectedFilesToAdd);

        jButtonAddNewFile.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonAddNewFile.setText("Add New File");
        jButtonAddNewFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddNewFileActionPerformed(evt);
            }
        });

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("File Status"));

        buttonGroupExternalInternalNew.add(jRadioButtonIsExternalNew);
        jRadioButtonIsExternalNew.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonIsExternalNew.setText("External");
        jRadioButtonIsExternalNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonIsExternalNewActionPerformed(evt);
            }
        });

        buttonGroupExternalInternalNew.add(jRadioButtonIsInternalNew);
        jRadioButtonIsInternalNew.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonIsInternalNew.setText("Internal");
        jRadioButtonIsInternalNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonIsInternalNewActionPerformed(evt);
            }
        });

        jLabelAlreadyAvailable.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelAlreadyAvailable.setText("File type:");

        jLabelFileType.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelFileType.setText("*.java");

        jLabelFileType2.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelFileType2.setText("Java Source File");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabelAlreadyAvailable)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabelFileType, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jRadioButtonIsExternalNew)
                                    .addComponent(jRadioButtonIsInternalNew))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addComponent(jLabelFileType2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jRadioButtonIsExternalNew)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRadioButtonIsInternalNew)
                .addGap(23, 23, 23)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelAlreadyAvailable)
                    .addComponent(jLabelFileType))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelFileType2)
                .addGap(0, 55, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonRemove, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE)
                    .addComponent(jButtonAddNewFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonRemove)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonAddNewFile))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Files in this element:", jPanel1);

        jButtonAddSelectedExistingFiles.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonAddSelectedExistingFiles.setText("Add Selected File(s) to local block");
        jButtonAddSelectedExistingFiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddSelectedExistingFilesActionPerformed(evt);
            }
        });

        jListAvailableFiles.setModel(_existingFilesList);
        jScrollPane2.setViewportView(jListAvailableFiles);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Modify file type"));

        buttonGroupInternalExternalExistingFiles.add(jRadioButtonIsInternalExisting);
        jRadioButtonIsInternalExisting.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonIsInternalExisting.setText("Internal");
        jRadioButtonIsInternalExisting.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonIsInternalExistingActionPerformed(evt);
            }
        });

        buttonGroupInternalExternalExistingFiles.add(jRadioButtonIsExternalExisting);
        jRadioButtonIsExternalExisting.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonIsExternalExisting.setText("External");
        jRadioButtonIsExternalExisting.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonIsExternalExistingActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jRadioButtonIsExternalExisting)
                    .addComponent(jRadioButtonIsInternalExisting))
                .addContainerGap(16, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(jRadioButtonIsExternalExisting)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRadioButtonIsInternalExisting)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonAddSelectedExistingFiles)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 484, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(23, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonAddSelectedExistingFiles)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Files already available in model (used by other elements):", jPanel2);

        jScrollPane3.setBorder(null);

        jTextArea1.setBackground(java.awt.Color.lightGray);
        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setRows(4);
        jTextArea1.setText("\nFiles specified as \"internal\" are read in by Gecko fully the first time they are added\nto a block and then saved into the Gecko model file (*.ipes). \n\"External\" files are accessed every time a block needs them, and must be distributed \nalong with the model file in order for the simulation to work properly with them.\nYou can use the buttons on the right-hand side to change an existing file's status.");
        jTextArea1.setBorder(null);
        jScrollPane3.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 604, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Info", jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 633, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(7, 7, 7))
            .addGroup(layout.createSequentialGroup()
                .addGap(215, 215, 215)
                .addComponent(jButtonOK, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(293, 293, 293)
                        .addComponent(jLabel1))
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 302, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonOK)
                    .addComponent(jButtonCancel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonAddSelectedExistingFilesActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonAddSelectedExistingFilesActionPerformed
        Object[] selectedFiles = jListAvailableFiles.getSelectedValues();
        for (int i = 0; i < selectedFiles.length; i++) {
            addGeckoFileToList((GeckoFile) selectedFiles[i]);
        }
    }//GEN-LAST:event_jButtonAddSelectedExistingFilesActionPerformed

    private void jButtonRemoveActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonRemoveActionPerformed
        GeckoFile file;
        int selectedIndex = jListSelectedFilesToAdd.getMaxSelectionIndex();
        while (selectedIndex != -1) {
            file = (GeckoFile) _selectedFilesList.getElementAt(selectedIndex);
            removeGeckoFileFromList(file);
            _selectedFilesList.remove(selectedIndex);
            selectedIndex = jListSelectedFilesToAdd.getMaxSelectionIndex();
        }
    }//GEN-LAST:event_jButtonRemoveActionPerformed

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonCancelActionPerformed
        this.dispose();
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jButtonAddNewFileActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonAddNewFileActionPerformed
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(final File f) {
                if (f.isDirectory()) {
                    return true;
                }
                return f.getName().endsWith(_fileExtension);
            }

            @Override
            public String getDescription() {
                return _fileType + " (*" + _fileExtension + ")";
            }
        };
        _addFilesDialog.setAcceptAllFileFilterUsed(false);
        _addFilesDialog.addChoosableFileFilter(filter);
        try {
            _addFilesDialog.setCurrentDirectory(new File(GlobalFilePathes.DATNAM));
        } catch (Exception e) {
            _addFilesDialog.setCurrentDirectory(new File(GlobalFilePathes.PFAD_JAR_HOME));
        }
        int returnVal = _addFilesDialog.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = _addFilesDialog.getSelectedFile();
            addNewFileToList(selectedFile);
        }
    }//GEN-LAST:event_jButtonAddNewFileActionPerformed

    private void jRadioButtonIsExternalNewActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadioButtonIsExternalNewActionPerformed
        Object[] selectedFiles = jListSelectedFilesToAdd.getSelectedValues();
        GeckoFile selectedFile;
        for (int i = 0; i < selectedFiles.length; i++) {
            selectedFile = (GeckoFile) selectedFiles[i];
            try {                
                selectedFile.setStorageType(StorageType.EXTERNAL);
            } catch (FileNotFoundException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "ERROR: File not found", JOptionPane.ERROR_MESSAGE);
            }
        }
        jListSelectedFilesToAdd.repaint();
    }//GEN-LAST:event_jRadioButtonIsExternalNewActionPerformed

    private void jRadioButtonIsInternalNewActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadioButtonIsInternalNewActionPerformed
        Object[] selectedFiles = jListSelectedFilesToAdd.getSelectedValues();
        GeckoFile selectedFile;
        for (int i = 0; i < selectedFiles.length; i++) {
            selectedFile = (GeckoFile) selectedFiles[i];
            try {
                selectedFile.setStorageType(StorageType.INTERNAL);
            } catch (FileNotFoundException e) {
                //this exception is not thrown in this case
                System.err.println(e.getMessage());
            }
        }
        jListSelectedFilesToAdd.repaint();
    }//GEN-LAST:event_jRadioButtonIsInternalNewActionPerformed

    private void jRadioButtonIsExternalExistingActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadioButtonIsExternalExistingActionPerformed
        Object[] selectedFiles = jListAvailableFiles.getSelectedValues();
        GeckoFile selectedFile;
        for (int i = 0; i < selectedFiles.length; i++) {
            selectedFile = (GeckoFile) selectedFiles[i];
            try {
                selectedFile.setStorageType(StorageType.EXTERNAL);
            } catch (FileNotFoundException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "ERROR: File not found", JOptionPane.ERROR_MESSAGE);
            }
        }
        jListAvailableFiles.repaint();
    }//GEN-LAST:event_jRadioButtonIsExternalExistingActionPerformed

    private void jRadioButtonIsInternalExistingActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadioButtonIsInternalExistingActionPerformed
        Object[] selectedFiles = jListAvailableFiles.getSelectedValues();
        GeckoFile selectedFile;
        for (int i = 0; i < selectedFiles.length; i++) {
            selectedFile = (GeckoFile) selectedFiles[i];
            try {
                selectedFile.setStorageType(StorageType.INTERNAL);
            } catch (FileNotFoundException e) {
                //this exception is not thrown in this case
                System.err.println(e.getMessage());
            }
        }
        jListAvailableFiles.repaint();
    }//GEN-LAST:event_jRadioButtonIsInternalExistingActionPerformed

    public void setNonLinearDialog(DialogNonLinearity dialog) {
        nonLinearDialog = dialog;
    }

    private void jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonOKActionPerformed

        if (_isLossElement) {
            if (!_newFilesToAdd.isEmpty()) { //if user has added new file, old one is removed automatically since semiconductor element can have only one loss file
                GeckoFile lossFile = _newFilesToAdd.get(0);
                boolean fileOK = _losses.leseDetailVerlusteVonDatei(lossFile);
                if (fileOK) {
                    String absPath = lossFile.getCurrentAbsolutePath();
                    _jlS2.setText(absPath.substring(0, absPath.lastIndexOf(System.getProperty("file.separator")) + 1));  // Pfad
                    _jlS2b.setText(lossFile.getName());   // Datei
                    _jlS2.setForeground(Color.decode("0x006400"));
                    _jlS2b.setForeground(Color.decode("0x006400"));
                    _jbS2edit.setEnabled(true);
                } else { //error reading in file
                    _jlS2.setText(" ");
                    _jlS2b.setText("File Error");
                    _jlS2.setForeground(Color.red);
                    _jlS2b.setForeground(Color.red);
                    _jbS2edit.setEnabled(false);
                }
            } else if (!_filesToRemove.isEmpty()) { //if user did not select a new file, but just removed the old one
                _losses.removeLossFile();
                _jlS2b.setText("No semiconductor defined");
                _jlS2.setText("-");
                _jlS2.setForeground(Color.red);
                _jlS2b.setForeground(Color.red);
                _jbS2edit.setEnabled(false);
            }
        } else {
            if (!_newFilesToAdd.isEmpty()) {
                if (!_newFilesToAdd.isEmpty()) {
                    _geckoFileable.addFiles(_newFilesToAdd);
                }
                if (nonLinearDialog != null) {
                    nonLinearDialog.setCharacteristicLoadedFromFile(_newFilesToAdd.get(0));
                }
            }

            if (!_filesToRemove.isEmpty()) {
                _geckoFileable.removeLocalComponentFiles(_filesToRemove);
            }

        }
        this.dispose();
    }//GEN-LAST:event_jButtonOKActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupExternalInternalNew;
    private javax.swing.ButtonGroup buttonGroupInternalExternalExistingFiles;
    private javax.swing.JButton jButtonAddNewFile;
    private javax.swing.JButton jButtonAddSelectedExistingFiles;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOK;
    private javax.swing.JButton jButtonRemove;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabelAlreadyAvailable;
    private javax.swing.JLabel jLabelFileType;
    private javax.swing.JLabel jLabelFileType2;
    private javax.swing.JList jListAvailableFiles;
    private javax.swing.JList jListSelectedFilesToAdd;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JRadioButton jRadioButtonIsExternalExisting;
    private javax.swing.JRadioButton jRadioButtonIsExternalNew;
    private javax.swing.JRadioButton jRadioButtonIsInternalExisting;
    private javax.swing.JRadioButton jRadioButtonIsInternalNew;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables
}

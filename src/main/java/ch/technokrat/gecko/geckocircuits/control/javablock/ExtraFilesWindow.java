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
package ch.technokrat.gecko.geckocircuits.control.javablock;

import ch.technokrat.gecko.geckocircuits.allg.GeckoFile;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFileManagerWindow;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.circuit.GeckoFileable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author anstupar
 */
public final class ExtraFilesWindow extends javax.swing.JDialog {

    private final GeckoFileable _geckoFileable;
    private GeckoFileManagerWindow _fileManagerWindow;
    private final DefaultListModel _extraFiles = new DefaultListModel();

    private final ListSelectionListener _listSelectionListener = new ListSelectionListener() {

        @Override
        public void valueChanged(final ListSelectionEvent listSelectionEvent) {
            if (jListExtraSourceFiles.getSelectedIndex() >= 0) {
                jButtonRemoveSelected.setEnabled(true);
            } else {
                jButtonRemoveSelected.setEnabled(false);
            }
        }
    };
    
    public ExtraFilesWindow(final GeckoFileable geckoFileable) {
        super();
        try {
            this.setIconImage(new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif")).getImage());
        } catch (Exception ex) {
            Logger.getLogger(ExtraFilesWindow.class.getName()).log(Level.INFO, "could not load image icon!");            
        }

        _geckoFileable = geckoFileable;
        initComponents();
        jTextArea2.setCaretPosition(0);
        getRootPane().setDefaultButton(jButtonOK);

        jListExtraSourceFiles.addListSelectionListener(_listSelectionListener);

    }

    public void addNewFiles(final List<GeckoFile> newFiles) {        
        for (GeckoFile newFile : newFiles) {
            if(!_extraFiles.contains(newFile)) {
                _extraFiles.addElement(newFile);
            }            
        }
    }
    
    public void removeFilesFromList(final List<GeckoFile> filesToRemove) {
        for (GeckoFile removedFile : filesToRemove) {
            _extraFiles.removeElement(removedFile);
        }
    }
    
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    //CHECKSTYLE:OFF
    @SuppressWarnings("PMD")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButtonOK = new javax.swing.JButton();
        jButtonAddNewFiles = new javax.swing.JButton();
        jButtonRemoveSelected = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jListExtraSourceFiles = new javax.swing.JList();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Specify addional source files");
        setLocationByPlatform(true);

        jButtonOK.setText("OK");
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOKActionPerformed(evt);
            }
        });

        jButtonAddNewFiles.setText("Add / Edit files");
        jButtonAddNewFiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddNewFilesActionPerformed(evt);
            }
        });

        jButtonRemoveSelected.setText("Remove selection");
        jButtonRemoveSelected.setEnabled(false);
        jButtonRemoveSelected.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRemoveSelectedActionPerformed(evt);
            }
        });

        jListExtraSourceFiles.setModel(_extraFiles);
        jScrollPane3.setViewportView(jListExtraSourceFiles);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 519, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Additional .java source code files", jPanel1);

        jTextArea2.setBackground(java.awt.Color.lightGray);
        jTextArea2.setColumns(20);
        jTextArea2.setEditable(false);
        jTextArea2.setFont(new java.awt.Font("Tahoma", 0, 11)); // NOI18N
        jTextArea2.setLineWrap(true);
        jTextArea2.setRows(5);
        jTextArea2.setText("Here you can specify additional Java source code files (*.java) required for the code inside your Java block. If different Java blocks need to use the same external file, it must be specified through this dialog for each of Java block (i.e. specifying a file here includes it only for the Java block you have opened this window from; not for all Java blocks). \n\nIn the previous tab, you can add and remove extra source files. Then whatever functions or objects are defined in those files will be usable inside your Java block with the use of proper syntax. If you are adding many files, it is recommended that you check that they compile and function correctly in an external Java editor.");
        jTextArea2.setWrapStyleWord(true);
        jTextArea2.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jScrollPane2.setViewportView(jTextArea2);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 499, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Info", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 527, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonAddNewFiles, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonRemoveSelected)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButtonOK, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 272, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonRemoveSelected)
                    .addComponent(jButtonAddNewFiles)
                    .addComponent(jButtonOK))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    //CHECKSTYLE:ON
    
    private void jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonOKActionPerformed
        setVisible(false);
    }//GEN-LAST:event_jButtonOKActionPerformed

    private void jButtonAddNewFilesActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonAddNewFilesActionPerformed

        _fileManagerWindow = new GeckoFileManagerWindow(_geckoFileable, ".java", "Java source files",false);
        _fileManagerWindow.setVisible(true);

    }//GEN-LAST:event_jButtonAddNewFilesActionPerformed

    private void jButtonRemoveSelectedActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonRemoveSelectedActionPerformed
        int selectedIndex = jListExtraSourceFiles.getMaxSelectionIndex();
        GeckoFile file;
        final List<GeckoFile> filesToRemove = new ArrayList<GeckoFile>();
        while (selectedIndex != -1) {
            file = (GeckoFile) _extraFiles.getElementAt(selectedIndex);
            filesToRemove.add(file);
            _extraFiles.remove(selectedIndex);
            selectedIndex = jListExtraSourceFiles.getMaxSelectionIndex(); 
        }
        _geckoFileable.removeLocalComponentFiles(filesToRemove);
        
    }//GEN-LAST:event_jButtonRemoveSelectedActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAddNewFiles;
    private javax.swing.JButton jButtonOK;
    private javax.swing.JButton jButtonRemoveSelected;
    private javax.swing.JList jListExtraSourceFiles;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea2;
    // End of variables declaration//GEN-END:variables
}

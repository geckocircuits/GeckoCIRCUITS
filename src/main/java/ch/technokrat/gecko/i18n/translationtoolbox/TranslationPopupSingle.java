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
/*
 * -Translation pop-up GUI class for single-line values.
 * 
 */
package ch.technokrat.gecko.i18n.translationtoolbox;

import ch.technokrat.gecko.i18n.InitParameters;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ProgressMonitor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.SwingWorker;
import ch.technokrat.gecko.i18n.LangInit;
import ch.technokrat.gecko.i18n.bot.UPbot;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

public class TranslationPopupSingle extends javax.swing.JFrame implements PropertyChangeListener {
    
    private boolean confirmed = false;
    private final I18nKeys key; // key of the triggering GUI element
    private String newTranslation;
    private String comment;
    private String buttonName = "";
    
    private static ProgressMonitor progressMonitor; // Progress Monitor GUI
    private Task task; // Background Task Thread
    private Progress progress; // getProgress Thread
    
    /*
     * Inner class used to execute upload instructions from a separate
     * thread to avoid freezing up.
     */
    private class Task extends SwingWorker<Void, Void> {
        @Override
        public Void doInBackground() {
            // upload the suggestion
            UPbot.addTranslationSuggestion_single(key, newTranslation, comment);
            return null;
        }
        
        @Override
        public void done() {} // do nothing
    }
        
    /*
     * Inner class used to acquire progress information from the upload bot 
     * class (UPbot) from a separate thread to avoid freezing up.
     */
    private class Progress extends SwingWorker<Void, Void> {
        @Override
        public Void doInBackground() {
            setProgress(0);
            
            // wait for task thread to get going
            try {
                Thread.sleep(500);
            } catch (Exception e) {}
            
            // keep updating progress until task is finished
            while (!task.isDone()) {
                setProgress(UPbot.getProgress()); // update progress                
            }
            setProgress(100); // indicate task completion
            return null;
        }
        
        @Override
        public void done() {} // do nothing
    }
    
    /**
     * Creates new form TranslationPopupSingle
     * @param key Key of the pop-up triggering element text
     */
    public TranslationPopupSingle(final I18nKeys key) {
        initComponents();
        setTitle(InitParameters.TRANS_TOOL);
        setLocationRelativeTo(null); // display in center screen
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        
        this.key = key;

        jTextField2.setText(LangInit.transMap_single.getValue(key)); // "Current Translation"
        jTextField1.setEditable(false); // "Original English"
        jTextField2.setEditable(false); // "Current Translation"
        jTextField1.setText(LangInit.englishMap_single.getValue(key)); // "Original English"
        
        
        // "Cancel" button
        jButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose(); // dispose of pop-up
            }
        });
        
        // "Done" button
        jButton2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (confirmed) {                    
                    // initialize progressMonitor
                    progressMonitor = new ProgressMonitor(TranslationPopupSingle.this, InitParameters.P_BAR_MESSAGE_UP, "", 0, 100);
                    progressMonitor.setProgress(0); // initialize progress
                    
                    // Create new threads
                    task = new Task();
                    progress = new Progress();
                    progress.addPropertyChangeListener(TranslationPopupSingle.this);
                    
                    // Run threads
                    task.execute();
                    progress.execute();
                    
                    disableButtons();
                } else {
                    new TranslationDialog(InitParameters.CONFIRM_MESSAGE).setVisible(true);
                }
            }
        });
        
        // "Confirm" button
        jButton3.addActionListener(new ActionListener()  {
            public void actionPerformed(ActionEvent e) {
                newTranslation = jTextField3.getText(); // get suggestion
                if (newTranslation.isEmpty()) {
                    new TranslationDialog(InitParameters.EMPTY_CONFIRM_MESSAGE).setVisible(true);
                } else {
                    comment = jTextArea3.getText(); // get comment
                    confirmed = true; // confirm the suggestion
                }
            }
        });
        
        // "Open Translation Toolbox" button
        jButton4.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (confirmed) {
                    buttonName = InitParameters.OTT_BUTTON;
                                        
                    progressMonitor = new ProgressMonitor(TranslationPopupSingle.this, InitParameters.P_BAR_MESSAGE_UP, "", 0, 100);
                    progressMonitor.setProgress(0); // initialize progress
                    
                    // Create new threads
                    task = new Task();
                    progress = new Progress();
                    progress.addPropertyChangeListener(TranslationPopupSingle.this);
                    
                    // Run threads
                    task.execute();
                    progress.execute();
                    
                    disableButtons();
                } else {
                    dispose(); // dispose of the pop-up
                    new TranslationTools().setVisible(true); // open toolbox
                }
            }
        });
    }
    
    /*
     * Method to implement PropertyChangeListener
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int prog = (Integer) evt.getNewValue(); // get updated progress
            progressMonitor.setProgress(prog); // update progress
            String info = "Completed " + prog + "%"; // information for the user
            progressMonitor.setNote(info); // update the message
            
            // check if canceled
            if (progressMonitor.isCanceled()) {
                // cancel threads
                progress.cancel(true);
                task.cancel(true);
                
                enableButtons();
            }
            
            // check if threads completed
            if (progress.isDone()) {
                boolean connected = UPbot.getConnectionStatus();
                if (connected) {
                    dispose();
                    if (buttonName.equals(InitParameters.OTT_BUTTON)) {
                        new TranslationTools().setVisible(true); // open the toolbox
                    }
                } else {
                    enableButtons();
                }
            }
        }
    }
    
    /*
     * Method to enable buttons
     */
    private void enableButtons() {
        jButton1.setEnabled(true); // "Cancel" button
        jButton2.setEnabled(true); // "Done" button
        jButton3.setEnabled(true); // "Confirm" button
        jButton4.setEnabled(true); // "Open Translation Toolbox" button
    }
    
    /*
     * Method to disable buttons
     */
    private void disableButtons() {
        jButton1.setEnabled(false); // "Cancel" button
        jButton2.setEnabled(false); // "Done" button
        jButton3.setEnabled(false); // "Confirm" button
        jButton4.setEnabled(false); // "Open Translation Toolbox" button
    }
       
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel4 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        jButton4 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel2.setText("This tool lets you change a flawed translation");

        jButton1.setText("Cancel");

        jButton2.setText("Done");

        jPanel4.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel1.setText("Original English:");

        jLabel3.setText("Current Translation:");

        jLabel4.setText("Suggest a Translation:");

        jButton3.setText("Confirm");

        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/gecko/i18n/resources/arrow.jpg"))); // NOI18N

        jLabel10.setText("Add a Comment (Optional):");

        jTextArea3.setColumns(20);
        jTextArea3.setRows(5);
        jScrollPane3.setViewportView(jTextArea3);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel10)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton3)
                .addGap(10, 10, 10))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jSeparator1)
                .addGap(10, 10, 10))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 276, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 276, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField3)))
                .addContainerGap())
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jTextField1, jTextField2});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton3)
                .addContainerGap())
        );

        jButton4.setText("Open Translation Toolbox");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButton1, jButton2});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButton2)
                        .addComponent(jButton4))
                    .addComponent(jButton1)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    // End of variables declaration//GEN-END:variables
}

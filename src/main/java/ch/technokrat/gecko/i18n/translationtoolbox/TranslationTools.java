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
 * -Translation toolbox GUI class.
 */
package ch.technokrat.gecko.i18n.translationtoolbox;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ProgressMonitor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.SwingWorker;
import java.util.HashMap;
import ch.technokrat.gecko.i18n.bot.UPbot;
import ch.technokrat.gecko.i18n.LangInit;
import ch.technokrat.gecko.i18n.InitParameters;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

public class TranslationTools extends javax.swing.JFrame implements PropertyChangeListener {
    
    private boolean confirmedSingle = false;
    private boolean confirmedMultiple = false;
    private boolean changed = false;
    private Integer counterSingle;
    private Integer counterMultiple;
    private int maxCounterSingle;
    private int maxCounterMultiple;
    private HashMap<Integer,I18nKeys> keysSingle;
    private HashMap<Integer,I18nKeys> keysMultiple;
    private String newTranslationSingle;
    private String newTranslationMultiple;
    private String commentSingle;
    private String commentMultiple;
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
            if (confirmedSingle) {
                // upload single-line suggestion
                UPbot.addTranslationSuggestion_single(keysSingle.get(counterSingle), newTranslationSingle, commentSingle);
            }
            if (confirmedMultiple) {
                // upload multiple-line suggestion
                UPbot.addTranslationSuggestion_multiple(keysMultiple.get(counterMultiple), newTranslationMultiple, commentMultiple);
            }            
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
     * Creates new form TranslationTools
     */
    public TranslationTools() {
        initComponents();
        setTitle(InitParameters.TRANS_TOOLS);
        setLocationRelativeTo(null); // display in center screen
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        jTextField5.setEditable(false); // "Items Remaining" single-line
        jTextField6.setEditable(false); // "Original English" single-line
        jTextField7.setEditable(false); // "Current Translation" single-line
        jTextField9.setEditable(false); // "Items Remaining" multiple-line
        jTextArea4.setEditable(false); // "Current Translation" multiple-line
        jTextArea5.setEditable(false); // "Original English" multiple-line
        
        maxCounterSingle = LangInit.transMap_single.getSize();
        maxCounterMultiple = LangInit.transMap_multiple.getSize();
        // initialize counters
        counterSingle = maxCounterSingle;
        counterMultiple = maxCounterMultiple;
        
        // "Items Remaining" text field in single-line
        jTextField5.setText(counterSingle.toString());
        
        // "Items Remaining" text field in multiple-line
        jTextField9.setText(counterMultiple.toString());
        
        keysSingle = new HashMap<Integer, I18nKeys>(); // create the keysSingle HashMap
        
        int i = counterSingle;
        
        // initialize single-line keys HashMap
        for (I18nKeys key : LangInit.englishMap_single.getKeySet()) {
            keysSingle.put(i, key);
            i = i - 1;
        }
        
        // "Original English" text field in single-line
        jTextField6.setText(LangInit.englishMap_single.getValue(keysSingle.get(counterSingle)));
        
        // "Current Translation" text field in single-line
        jTextField7.setText(LangInit.transMap_single.getValue(keysSingle.get(counterSingle)));
        
        keysMultiple = new HashMap<Integer,I18nKeys>(); // create the keysMultiple HashMap
        
        i = counterMultiple;
        
        // initialize multiple-line keys HashMap
        for (I18nKeys key : LangInit.englishMap_multiple.getKeySet()) {
            keysMultiple.put(i, key);
            i = i - 1;
        }
        
        // "Original English" text area in multiple-line
        jTextArea5.setText(LangInit.englishMap_multiple.getValue(keysMultiple.get(counterMultiple)));
        
        // "Current Translation" text area in multiple-line
        jTextArea4.setText(LangInit.transMap_multiple.getValue(keysMultiple.get(counterMultiple)));
        
        // "Done" button ActionListener
        jButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (changed) {
                    // make the change in the GUIs
                    // LangInit.app.setText();
                    
                    if (confirmedSingle || confirmedMultiple) {
                        buttonName = InitParameters.DONE_BUTTON;
                        
                        // initialize progressMonitor
                        progressMonitor = new ProgressMonitor(TranslationTools.this, InitParameters.P_BAR_MESSAGE_UP, "", 0, 100);
                        progressMonitor.setProgress(0); // initialize progress
                    
                        // Create new threads
                        task = new Task();
                        progress = new Progress();
                        progress.addPropertyChangeListener(TranslationTools.this);                    
                    
                        // Run threads
                        task.execute();
                        progress.execute();
                        
                        disableButtons();
                    } else {
                        dispose();
                    }
                } else {
                    dispose();
                }
            }
        });
        
        // "Confirm" button in Single-Line
        jButton5.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                newTranslationSingle = jTextField8.getText(); // get new suggestion
                                    
                if (newTranslationSingle.isEmpty()) {
                    new TranslationDialog(InitParameters.EMPTY_CONFIRM_MESSAGE).setVisible(true);
                } else {
                    commentSingle = jTextArea1.getText(); // get new comment
                    confirmedSingle = true; // confirm the suggestion
                    changed = true; // indicates that a change has been made
                }
            } 
        });
        
        // "Confirm" button in Multiple-Line
        jButton8.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                newTranslationMultiple = jTextArea3.getText(); // get new suggestion
                
                if (newTranslationMultiple.isEmpty()) {
                    new TranslationDialog(InitParameters.EMPTY_CONFIRM_MESSAGE).setVisible(true);
                } else {
                    commentMultiple = jTextArea2.getText(); // get new comment
                    confirmedMultiple = true; // confirm the suggestion
                    changed = true; // indicates that a change has been made
                }
            }
        });
        
        // "Next Item" button in Single-Line
        jButton6.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (counterSingle > 1) {
                    if (confirmedSingle) {
                        // save the suggestion on the Wiki
                        
                        // initialize progressMonitor
                        progressMonitor = new ProgressMonitor(TranslationTools.this, InitParameters.P_BAR_MESSAGE_UP, "", 0, 100);
                        progressMonitor.setProgress(0); // initialize progress
                    
                        // Create new threads
                        task = new Task();
                        progress = new Progress();
                        progress.addPropertyChangeListener(TranslationTools.this);                    
                    
                        // Run threads
                        task.execute();
                        progress.execute();
                        
                        disableButtons();
                        
                        jTextField8.setText("");
                        jTextArea1.setText("");
                        confirmedSingle = false;
                    }
                    counterSingle = counterSingle - 1; // decrement counter
                    // "Items Remaining" text field in single-line
                    jTextField5.setText(counterSingle.toString());
                    // "Original English" text field in single-line
                    jTextField6.setText(LangInit.englishMap_single.getValue(keysSingle.get(counterSingle)));
                    // "Current Translation" text field in single-line
                    jTextField7.setText(LangInit.transMap_single.getValue(keysSingle.get(counterSingle)));
                }                
            }
        });
        
        // "Next Item" button in Multiple-Line
        jButton9.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (counterMultiple > 1) {
                    if (confirmedMultiple) {
                        // save suggestion on the Wiki
                        
                        // initialize progressMonitor
                        progressMonitor = new ProgressMonitor(TranslationTools.this, InitParameters.P_BAR_MESSAGE_UP, "", 0, 100);
                        progressMonitor.setProgress(0); // initialize progress
                    
                        // Create new threads
                        task = new Task();
                        progress = new Progress();
                        progress.addPropertyChangeListener(TranslationTools.this);                    
                    
                        // Run threads
                        task.execute();
                        progress.execute();
                        
                        disableButtons();
                        
                        jTextArea2.setText("");
                        jTextArea3.setText("");
                        confirmedMultiple = false;
                    }
                    counterMultiple = counterMultiple - 1; // decrement counter
                    // "Items Remaining" text field in multiple-line
                    jTextField9.setText(counterMultiple.toString());
                    // "Original English" text area in multiple-line
                    jTextArea5.setText(LangInit.englishMap_multiple.getValue(keysMultiple.get(counterMultiple)));
                    // "Current Translation" text area in multiple-line
                    jTextArea4.setText(LangInit.transMap_multiple.getValue(keysMultiple.get(counterMultiple)));
                }
            }
        });
        
        // "Previous Item" button in Single-Line
        jButton7.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (counterSingle < maxCounterSingle) {
                    if (confirmedSingle) {
                        // save the suggestion on the Wiki
                        
                        // initialize progressMonitor
                        progressMonitor = new ProgressMonitor(TranslationTools.this, InitParameters.P_BAR_MESSAGE_UP, "", 0, 100);
                        progressMonitor.setProgress(0); // initialize progress
                    
                        // Create new threads
                        task = new Task();
                        progress = new Progress();
                        progress.addPropertyChangeListener(TranslationTools.this);                    
                    
                        // Run threads
                        task.execute();
                        progress.execute();
                        
                        disableButtons();
                        
                        jTextField8.setText("");
                        jTextArea1.setText("");
                        confirmedSingle = false;
                    }
                    counterSingle = counterSingle + 1; // increment counter
                    // "Items Remaining" text field in single-line
                    jTextField5.setText(counterSingle.toString());
                    // "Original English" text field
                    jTextField6.setText(LangInit.englishMap_single.getValue(keysSingle.get(counterSingle)));
                    // "Current Translation" text field
                    jTextField7.setText(LangInit.transMap_single.getValue(keysSingle.get(counterSingle)));
                }               
            }
        });
      
        // "Previous Item" button in Multiple-Line
        jButton10.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (counterMultiple < maxCounterMultiple) {
                    if (confirmedMultiple) {
                        // save the suggestion on the Wiki
                        
                        // initialize progressMonitor
                        progressMonitor = new ProgressMonitor(TranslationTools.this, InitParameters.P_BAR_MESSAGE_UP, "", 0, 100);
                        progressMonitor.setProgress(0); // initialize progress
                    
                        // Create new threads
                        task = new Task();
                        progress = new Progress();
                        progress.addPropertyChangeListener(TranslationTools.this);                    
                    
                        // Run threads
                        task.execute();
                        progress.execute();
                        
                        disableButtons();
                        
                        jTextArea2.setText("");
                        jTextArea3.setText("");
                        confirmedMultiple = false;
                    }
                    counterMultiple = counterMultiple + 1; // increment counter
                    // "Items Remaining" text field in multiple-line
                    jTextField9.setText(counterMultiple.toString());
                    // "Original English" text area in multiple-line
                    jTextArea5.setText(LangInit.englishMap_multiple.getValue(keysMultiple.get(counterMultiple)));
                    // "Current Translation" text area in multiple-line
                    jTextArea4.setText(LangInit.transMap_multiple.getValue(keysMultiple.get(counterMultiple)));
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
                UPbot.resetProgress();
                boolean connected = UPbot.getConnectionStatus();
                if (connected && buttonName.equals(InitParameters.DONE_BUTTON)) {
                   //GeckoSim._win.resetGUIText();
                   dispose();
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
        jButton1.setEnabled(true); // "Done" button  
        jButton6.setEnabled(true); // "Next Item" button single-line
        jButton7.setEnabled(true); // "Previous Item" button single-line
        jButton5.setEnabled(true); // "Confirm" button single-line
        jButton8.setEnabled(true); // "Confirm" button multiple-line
        jButton9.setEnabled(true); // "Next Item" button multiple-line
        jButton10.setEnabled(true); // "Previous Item" button multiple-line
    }
    
    /*
     * Method to disable buttons
     */
    private void disableButtons() {
        jButton1.setEnabled(false); // "Done" button  
        jButton6.setEnabled(false); // "Next Item" button single-line
        jButton7.setEnabled(false); // "Previous Item" button single-line
        jButton5.setEnabled(false); // "Confirm" button single-line
        jButton8.setEnabled(false); // "Confirm" button multiple-line
        jButton9.setEnabled(false); // "Next Item" button multiple-line
        jButton10.setEnabled(false); // "Previous Item" button multiple-line
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
        jLabel3 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        jSeparator4 = new javax.swing.JSeparator();
        jLabel10 = new javax.swing.JLabel();
        jTextField8 = new javax.swing.JTextField();
        jButton5 = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JSeparator();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel4 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jTextField9 = new javax.swing.JTextField();
        jSeparator6 = new javax.swing.JSeparator();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jSeparator7 = new javax.swing.JSeparator();
        jLabel13 = new javax.swing.JLabel();
        jButton8 = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JSeparator();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextArea4 = new javax.swing.JTextArea();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextArea5 = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel3.setText("This toolbox lets you view/edit all translated items.");

        jButton1.setText("Done");

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel1.setText("Items Remaining:");

        jLabel8.setText("Original English:");

        jLabel9.setText("Current Translation:");

        jLabel10.setText("Suggest a Translation:");

        jButton5.setText("Confirm");

        jButton6.setText("Next Item");

        jButton7.setText("Previous Item");

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/gecko/i18n/resources/arrow.jpg"))); // NOI18N

        jLabel4.setText("Add a Comment (Optional):");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 478, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton5))
                    .addComponent(jSeparator5, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSeparator4, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSeparator3)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField6)
                            .addComponent(jTextField7)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField8))
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton6)
                .addGap(171, 171, 171))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButton6, jButton7});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton6)
                    .addComponent(jButton7))
                .addContainerGap(184, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Single-Line Translations", jPanel3);

        jPanel4.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel5.setText("Items Remaining:");

        jLabel11.setText("Original English:");

        jLabel12.setText("Current Translation:");

        jLabel13.setText("Suggest a Translation:");

        jButton8.setText("Confirm");

        jButton9.setText("Next Item");

        jButton10.setText("Previous Item");

        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/gecko/i18n/resources/arrow.jpg"))); // NOI18N

        jLabel7.setText("Add a Comment (Optional):");

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jScrollPane2.setViewportView(jTextArea2);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTextArea3.setColumns(20);
        jTextArea3.setRows(5);
        jScrollPane3.setViewportView(jTextArea3);

        jTextArea4.setColumns(20);
        jTextArea4.setRows(5);
        jScrollPane4.setViewportView(jTextArea4);

        jTextArea5.setColumns(20);
        jTextArea5.setRows(5);
        jScrollPane5.setViewportView(jTextArea5);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(158, 158, 158)
                        .addComponent(jButton10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton9)
                        .addGap(0, 150, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator7))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton8)))
                .addContainerGap())
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane5)
                            .addComponent(jScrollPane4)
                            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jSeparator6)
                            .addComponent(jSeparator8)
                            .addComponent(jScrollPane3)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel11)
                            .addComponent(jLabel13)
                            .addComponent(jLabel12))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButton10, jButton9});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator7, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator8, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton10)
                    .addComponent(jButton9)))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jScrollPane3, jScrollPane4, jScrollPane5});

        jTabbedPane1.addTab("Multiple-Line Translations", jPanel4);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton1)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addGap(35, 35, 35)
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1))
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
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JTextArea jTextArea4;
    private javax.swing.JTextArea jTextArea5;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    // End of variables declaration//GEN-END:variables
}

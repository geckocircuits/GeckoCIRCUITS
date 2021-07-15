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
 * Language initialization (LangInit) GUI class.
 */
package ch.technokrat.gecko.i18n;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ProgressMonitor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.SwingWorker;
import ch.technokrat.gecko.i18n.bot.DLbot;
import ch.technokrat.gecko.i18n.resources.EnglishMapper;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

public class LangInit extends javax.swing.JDialog implements PropertyChangeListener {

    /**
     * Language chosen by the user
     */
    public static SelectableLanguages language;
    /**
     * Single-Line English map
     */
    public static DoubleMap englishMap_single;
    /**
     * Multiple-Line English map
     */
    public static DoubleMap englishMap_multiple;
    /**
     * Single-Line Translated map
     */
    public static DoubleMap transMap_single;
    /**
     * Multiple-Line Translated map
     */
    public static DoubleMap transMap_multiple;
    private static String[] arguments;
    // Applet-Wiki connection status indicator
    private static boolean connected;
    private static ProgressMonitor progressMonitor; // Progress Monitor GUI
    private Task task; // Background Task Thread
    private Progress progress; // getProgress Thread

    /*
     * Inner class used to execute download instructions from a separate
     * thread so that the LangInit GUI doesn't freeze up.
     */
    private class Task extends SwingWorker<Void, Void> {

        @Override
        public Void doInBackground() {
            transMap_single = DLbot.getTranslations_single(); // get single-line translations from the Wiki
            connected = DLbot.getConnectionStatus(); // see if it worked
            if (connected) {
                transMap_multiple = DLbot.getTranslations_multiple(); // get multiple-line translations from the Wiki
            }
            return null;
        }

        @Override
        public void done() {
        } // do nothing
    }

    /*
     * Inner class used to acquire progress information from the download bot 
     * class (DLbot) from a separate thread so that the LangInit GUI doesn't 
     * freeze up.
     */
    private class Progress extends SwingWorker<Void, Void> {

        @Override
        public Void doInBackground() {
            setProgress(0);

            // wait for task thread to get going
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }

            // keep updating progress until task is finished
            while (!task.isDone()) {
                setProgress(DLbot.getProgress()); // update progress                
            }
            setProgress(100); // indicate task completion
            return null;
        }

        @Override
        public void done() {
        } // do nothing
    }

    /**
     * Creates new form LangInit
     *
     * @param args Arguments given to main method of program
     */
    public LangInit(String[] args) {
        setModalityType(ModalityType.APPLICATION_MODAL);
        initComponents();
        for (SelectableLanguages lang : SelectableLanguages.values()) {
            jComboBoxLanguageSelection.addItem(lang);
        }

        setLocationRelativeTo(null); // display in center-screen
        setResizable(false);
        arguments = args;

        // "Exit" button
        jButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new InitDialog().setVisible(true); // confirm exit
            }
        });

        // "Continue" button
        jButton2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Adjust fonts to correctly display special characters (eg. Japanese chars)
                language = (SelectableLanguages) jComboBoxLanguageSelection.getSelectedItem(); // get language
                englishMap_single = EnglishMapper.initEnglishMap_single(); // initialize englishMap_single
                englishMap_multiple = EnglishMapper.initEnglishMap_multiple(); // initialize englishMap_multiple
                // download current translations if language != English
                if (!language.equals(SelectableLanguages.ENGLISH)) {
                    // initialize progressMonitor
                    progressMonitor = new ProgressMonitor(LangInit.this, InitParameters.P_BAR_MESSAGE_DL, "", 0, 100);
                    progressMonitor.setProgress(0); // initialize progress

                    // Create new threads
                    task = new Task();
                    progress = new Progress();
                    progress.addPropertyChangeListener(LangInit.this);

                    // Run threads
                    task.execute();
                    progress.execute();

                    disableButtons();
                } else {
                    // use englishMap if English is chosen
                    transMap_single = englishMap_single;
                    transMap_multiple = englishMap_multiple;
                    dispose();
                }
            }
        });
    }

    public static void initEnglish() {
        language = SelectableLanguages.ENGLISH;
        englishMap_single = EnglishMapper.initEnglishMap_single(); // initialize englishMap_single
        englishMap_multiple = EnglishMapper.initEnglishMap_multiple(); // initialize englishMap_multiple

        transMap_single = englishMap_single;
        transMap_multiple = englishMap_multiple;
    }

    /*
     * Method to implement PropertyChangeListener
     */
    public void propertyChange(PropertyChangeEvent e) {
        if ("progress".equals(e.getPropertyName())) {
            int prog = (Integer) e.getNewValue(); // get updated progress
            progressMonitor.setProgress(prog); // update progress
            String info = "Completed " + prog + "%"; // information for the user
            progressMonitor.setNote(info); //  update the message

            // check if canceled
            if (progressMonitor.isCanceled()) {
                // cancel threads
                progress.cancel(true);
                task.cancel(true);
                enableButtons();
            }

            // check if threads completed
            if (progress.isDone()) {
                DLbot.resetProgress();
                connected = DLbot.getConnectionStatus();
                if (connected) {
                    dispose();
                } else {
                    enableButtons();
                }
            }
        }
    }

    public static String getTranslatedString(final I18nKeys key) {
        if(transMap_single != null) {
            return transMap_single.getValue(key);
        } else {
            return key.getEnglishString();
        }        
    }

    /*
     * Method to enable buttons
     */
    private void enableButtons() {
        jButton2.setEnabled(true); // "Continue" button
        jButton1.setEnabled(true); // "Exit" buton
    }

    /*
     * Method to disable buttons
     */
    private void disableButtons() {
        jButton2.setEnabled(false); // "Continue" button
        jButton1.setEnabled(false); // "Exit" buton
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
        jLabel1 = new javax.swing.JLabel();
        jComboBoxLanguageSelection = new javax.swing.JComboBox();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("Please choose a language:");

        jButton1.setText("Exit");

        jButton2.setText("Continue");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxLanguageSelection, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(111, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButton1, jButton2});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jComboBoxLanguageSelection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addContainerGap())
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
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox jComboBoxLanguageSelection;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
}

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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.control.QuasiPeakCalculator;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFrame;

public class StartupWindow extends javax.swing.JDialog {

    private static final String SUPPORT_URL = "http://www.gecko-simulations.com/open_source/support.html";

    public static boolean testDialogOpenSourceVersion(final String featureName) {
        try {
           QuasiPeakCalculator.class.getName();
        } catch (NoClassDefFoundError err) {
            StartupWindow window = StartupWindow.fabricDisabledFeature(featureName);
            window.setVisible(true);
            return true;            
        }
        return false;
    }
    
    public static boolean testOpenSourceVersion() {
        try {
            QuasiPeakCalculator.class.getName();
        } catch (NoClassDefFoundError err) {
            return true;            
        }
        return false;
    }
    
    private long DISPOSE_WAIT_TIME_MILLIS = 5000;
    private static final long START_DELAY_TIME_MILLIS = 10000;
    private static final Random rand = new Random(System.currentTimeMillis());
    public static final String DONATE_CODE_KEY = "DONATE_CODE";
   
    private final static Timer delayWindowTimer = new Timer();

    static void fabricUnBlocking() {
        
        if (!testOpenSourceVersion()) {
            return;
        }

        StartupWindow window = new StartupWindow(GeckoSim._win);
        delayWindowTimer.schedule(new DelayWindowVisibleTask(window), START_DELAY_TIME_MILLIS);

    }

    public StartupWindow(final JFrame parentFrame) {
        super(parentFrame, true);
        initComponents();
        this.setTitle("GeckoCIRCUITS Open-Source Information");
        this.setLocationRelativeTo(parentFrame);        
    }
    
    public static StartupWindow fabricDisabledFeature(final String featureName) {
        StartupWindow returnValue = new StartupWindow(null);
        returnValue.jLabel1.setText("<html><font color='red'><b>" + featureName + 
                ": This feature of GeckoCIRCUITS is not included in the "
                + "open-source release. </b><br><font color='black'>" + returnValue.jLabel1.getText().substring(24));
        returnValue.DISPOSE_WAIT_TIME_MILLIS = 0;
        returnValue.pack();
        return returnValue;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButtonOk = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabelSupport = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        jButtonOk.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jButtonOk.setText("Ok");
        jButtonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOkActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jLabel1.setText("<html><body width='750'>This software is published as open-source software under the Gnu Public License (GPL version 3).  For the detailed license text,   please refer to the Licensing Dialog  \"Help -> Licensing\".  Please note that two different releases of GeckoCIRCUITS are available:  <ul> \t  <li> The open-source release that you have currently running, which you can use without any cost.  <li> A professional release, which includes many bug-fixes and additional features. The additional features in the GeckoCIRCUITS professional release are the following: <ul> \t <li> GeckoSCRIPT: A scripting interface which allows full control over the circuit solver.  Using GeckoSCRIPT, \t you can set all circuit parameters, run optimizations from within MATLAB or Java. \t  <li> Matlab/Simulink interface: GeckoCIRCUITS can be combined with Simulink models \t  <li> EMI testreceiver control block \t</ul>  </ul> In order to access the professional release of GeckoCIRCUITS including above mentioned features, you have to  set up a support and maintenance contract with Gecko-Simulations AG. For more information, please contact us via email (contact@gecko-simulations.com)  or visit the following link: </ul></html>  ");

        jLabelSupport.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jLabelSupport.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelSupport.setText("<html><a href=\"www.gecko-simulations.com/open_source/donate.html\">www.gecko-simulations.com/open_source/support.html</a></html>");
        jLabelSupport.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabelSupportMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jLabelSupport))
                        .addGroup(layout.createSequentialGroup()
                            .addGap(12, 12, 12)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 744, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(277, 277, 277)
                        .addComponent(jButtonOk, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 288, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelSupport, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButtonOk)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jLabelSupportMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabelSupportMouseClicked
        LaunchBrowser.launch(SUPPORT_URL);
    }//GEN-LAST:event_jLabelSupportMouseClicked


    private static final class DelayWindowVisibleTask extends TimerTask {

        private final StartupWindow _window;

        public DelayWindowVisibleTask(StartupWindow window) {
            _window = window;
        }

        public void run() {
            _window.setVisible(true);
        }
    }

    private void jButtonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOkActionPerformed
        jButtonOk.setText("Closing Window...");
        jButtonOk.repaint();
        dispose();
    }//GEN-LAST:event_jButtonOkActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonOk;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabelSupport;
    // End of variables declaration//GEN-END:variables

}
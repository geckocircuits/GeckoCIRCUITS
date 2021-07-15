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
import java.io.*;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;


/**
 * Todo: remove all the code duplication in this file! DRY!!!
 * @author andy
 */
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.TooManyFields"})
public final class DialogUpdate extends javax.swing.JFrame {

    private static void doRealUpdateCheck() {
        Thread updateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);                    
                } catch (InterruptedException ex) {
                    Logger.getLogger(DialogUpdate.class.getName()).log(Level.SEVERE, null, ex);
                }                
                
                doUpdateCheck(PRO_URL);
                
            }
        });        
        updateThread.start();
    }
   

    private static void doUpdateCheck(final String urlString) {        
        DialogUpdate window = new DialogUpdate();
        window.getUpdateInformation(urlString);
        String updateString = window.jLabelNewNumber.getText().trim();
        String updateLevelString = GeckoSim.applicationProps.getProperty("UPDATE_LEVEL", "MINOR");
        String lastUpdateString = GeckoSim.applicationProps.getProperty("LAST_UPDATE_INFO", "");
       
        if(lastUpdateString.isEmpty()) {
            lastUpdateString = updateString;
        }
        
        if(updateLevelString.equals("MINOR")) {
            if(!lastUpdateString.equals(updateString)) {
                window.setVisible(true);
            }
        }
        
        if(updateLevelString.equals("MAJOR")) {
            if(!lastUpdateString.substring(0, 13).equals(updateString.substring(0, 13))) {
                window.setVisible(true);
            }
        }        
        
        GeckoSim.applicationProps.setProperty("LAST_UPDATE_INFO", lastUpdateString);
        
    }
        
    
    private final DateFormat _dFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.US);
    private final DateFormat _showFormat = new SimpleDateFormat("MMMMMMMMM yyyy", Locale.US);
    private static final int BUFFER_LENGTH = 1024;
    private static final double RELEASE_DIVISOR = 100.0;
    private static final int TIMEOUT_MILLIS = 4000;
    private static final String PRO_URL = "http://www.gecko-simulations.com/GeckoCIRCUITS/GeckoCIRCUITSreleaseHistory.html";    

    /**
     * Creates new form DialogUpdate
     */
    public DialogUpdate() {
        super();
        try {
            initComponents();            
            jLabelCurrentNumber.setText(Double.toString(DialogAbout.RELEASENUMBER / RELEASE_DIVISOR) + " build " + DialogAbout.BUILD_NUMBER);
            final Date rDate = _dFormat.parse(DialogAbout.RELEASE_DATE);
            jLabelCurrentDate.setText(_showFormat.format(rDate));

        } catch (ParseException ex) {
            Logger.getLogger(DialogUpdate.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private String findVersionString(final String htmlText) {
        final int versionIndex = htmlText.indexOf("Version", 0);
        final int endIndex = htmlText.indexOf(')');
        String returnValue = "";
        if (versionIndex > 0 && endIndex > versionIndex) {
            returnValue = htmlText.substring(versionIndex, endIndex + 1);
        }

        return returnValue;
    }

    public void getUpdateInformation(final String urlString) {
        jLabelInfo.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        loadGeckoUpdateData(urlString);
    }

    private void setErrorMessage() {
        jLabelInfo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelInfo.setText("<html>Communication error! Network transfer was probably blocked by a firewall!</html>");
    }

    private void loadGeckoUpdateData(final String urlString) {
        try {
            final URL geckoReleaseURL = new URL(urlString);
            final URLConnection urlConnection = geckoReleaseURL.openConnection();
            urlConnection.setConnectTimeout(TIMEOUT_MILLIS);


            urlConnection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            urlConnection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            urlConnection.setRequestProperty("User-Agent", "GeckoCIRCUITS " + System.getProperty("os.name") + " "
                    + System.getProperty("os.name") + " " + "no info");
            urlConnection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
            urlConnection.connect();

            final BufferedReader bufIN = new BufferedReader(
                    new InputStreamReader(
                    urlConnection.getInputStream()));
            final StringBuffer htmlText = new StringBuffer("<html>");

            for (String inputLine = bufIN.readLine(); inputLine != null; inputLine = bufIN.readLine()) {
                htmlText.append(inputLine);
            }
            htmlText.append("</html>");
            bufIN.close();
            jLabelInfo.setText(htmlText.toString());
            jLabelNewNumber.setText(findVersionString(htmlText.toString()));

        } catch (SocketTimeoutException timeoutException) {
            setErrorMessage();
        } catch (IOException exception) {
            setErrorMessage();
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

        jButtonClose = new javax.swing.JButton();
        jButtonGetUpdateOS = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabelCurrentNumber = new javax.swing.JLabel();
        jLabelCurrentDate = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabelNewNumber = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jLabelInfo = new javax.swing.JLabel();
        jButtonGetInfosPro = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);

        jButtonClose.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonClose.setText("Close window");
        jButtonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCloseActionPerformed(evt);
            }
        });

        jButtonGetUpdateOS.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonGetUpdateOS.setText("Download update (.zip-file)");
        jButtonGetUpdateOS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGetUpdateOSActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Your GeckoCIRCUITS version"));

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel4.setText("Release number:");

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel5.setText("Release date:");

        jLabelCurrentNumber.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        jLabelCurrentNumber.setText("   ");

        jLabelCurrentDate.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        jLabelCurrentDate.setText("   ");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelCurrentDate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelCurrentNumber, javax.swing.GroupLayout.DEFAULT_SIZE, 583, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabelCurrentNumber))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jLabelCurrentDate))
                .addContainerGap(18, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Available update"));

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel1.setText("Release number:");

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel3.setText("New features / changes:");

        jLabelNewNumber.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        jLabelNewNumber.setText("   ");

        jLabelInfo.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jLabelInfo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelInfo.setText("<html><br>     Waiting for data</html>");
        jScrollPane1.setViewportView(jLabelInfo);

        jButtonGetInfosPro.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonGetInfosPro.setText("Load update information");
        jButtonGetInfosPro.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGetInfosProActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelNewNumber, javax.swing.GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE)
                        .addGap(13, 13, 13))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(0, 554, Short.MAX_VALUE))
                    .addComponent(jScrollPane1))
                .addContainerGap())
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(229, 229, 229)
                .addComponent(jButtonGetInfosPro, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabelNewNumber))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 346, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonGetInfosPro))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(119, 119, 119)
                        .addComponent(jButtonGetUpdateOS, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonClose, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButtonClose, jButtonGetUpdateOS});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonClose)
                    .addComponent(jButtonGetUpdateOS))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    //CHECKSTYLE:ON

    private void jButtonCloseActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonCloseActionPerformed
        dispose();
    }//GEN-LAST:event_jButtonCloseActionPerformed

    private void jButtonGetUpdateOSActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonGetUpdateOSActionPerformed


        final String jarFile = GetJarPath.getJarFilePath();
        File currentDirectoryFile = null;
        if (jarFile != null) {
            currentDirectoryFile = new File(jarFile);
        }

        GeckoFileChooser fileChooser = GeckoFileChooser.createSaveFileChooser(".zip", ".zip archive file", this, currentDirectoryFile);
        if (fileChooser.getUserResult() == GeckoFileChooser.FileChooserResult.CANCEL) {
            return;
        }

        try {
            final URL url = new URL("http://www.gecko-simulations.com/GeckoCIRCUITS/GeckoCIRCUITS.zip");
            final URLConnection urlconnection = url.openConnection();
            if (urlconnection != null) {
                jButtonGetUpdateOS.setText("Downloading update...");
                jButtonGetUpdateOS.setEnabled(false);
                final InputStream inputStream = new BufferedInputStream(url.openStream());

                final OutputStream out = new FileOutputStream(fileChooser.getFileWithCheckedEnding());


                final byte[] buf = new byte[BUFFER_LENGTH];


                for (int len = inputStream.read(buf); len > 0; len = inputStream.read(buf)) {
                    out.write(buf, 0, len);
                }

                out.close();
                inputStream.close();
                jButtonGetUpdateOS.setEnabled(true);
                jButtonGetUpdateOS.setText("Download finished!");
            }

        } catch (IOException ex) {
            Logger.getLogger(DialogUpdate.class.getName()).log(Level.SEVERE, null, ex);
            jButtonGetUpdateOS.setText("Download failed!");
        } catch (Throwable error) {
            JOptionPane.showMessageDialog(this,
                    "Could not download file, the error message is:\n"
                    + error.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            jButtonGetUpdateOS.setText("Download failed!");
        }

    }//GEN-LAST:event_jButtonGetUpdateOSActionPerformed

    private void jButtonGetInfosProActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGetInfosProActionPerformed
        getUpdateInformation(PRO_URL);
    }//GEN-LAST:event_jButtonGetInfosProActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonClose;
    private javax.swing.JButton jButtonGetInfosPro;
    private javax.swing.JButton jButtonGetUpdateOS;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabelCurrentDate;
    private javax.swing.JLabel jLabelCurrentNumber;
    private javax.swing.JLabel jLabelInfo;
    private javax.swing.JLabel jLabelNewNumber;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

    static void checkForUpdateInterval() {
        final String updateFreqString = GeckoSim.applicationProps.getProperty("UPDATE_FREQ", "NEVER");
        final String lastUpdateCheck = GeckoSim.applicationProps.getProperty("LAST_UPDATE_CHECK", "0");
        final long lastUpdateMillis = Long.parseLong(lastUpdateCheck);

        long updateMillisInterval = 0;

        if (updateFreqString.equals("NEVER")) {
            return;
        }

        if (updateFreqString.equals("DAILY")) {
            updateMillisInterval = 1000L * 3600L * 24L;
        }

        if (updateFreqString.equals("WEEKLY")) {
            updateMillisInterval = 1000L * 3600L * 24L * 7L;
        }
        
        
        if (System.currentTimeMillis() > (lastUpdateMillis + updateMillisInterval)) {
            
            doRealUpdateCheck();
        }
        GeckoSim.applicationProps.setProperty("LAST_UPDATE_CHECK", System.currentTimeMillis() + "");
    }
}
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

import javax.swing.ImageIcon; 
import javax.swing.JFrame; 
import java.net.URL; 
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;



public class DialogJavaCompilerOptimizer extends JFrame {
    
    private GeckoJavaCompiler geckoJavaCompiler;
    private javax.swing.JButton jButtonCloseWindow;
    private javax.swing.JButton jButtonCompile;
    private javax.swing.JButton jButtonExecute;
    private javax.swing.JButton jButtonExample1;
    private javax.swing.JButton jButtonExample2;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanelCompilerMessages;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTabbedPane jTabbedPane;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextAreaCode;
    private javax.swing.JTextArea jTextAreaCompilerMessage;
    private javax.swing.JTextArea jTextAreaImports;
    private javax.swing.JTextArea jTextAreaStaticInit;
    private javax.swing.JTextArea jTextAreaVariables;
    //--------------
    private String info= ""
        +"\nSet the parameter values in the GeckoCIRCUITS model: \n\n"
        +"public void setOptimizerParameterData (String[] optName, double[] optValue);\n"
        +"\nControl the Simulation with the following methods:\n\n"
        +"public void setSimParameter (double dt, double tDURATION, double tPAUSE);\n" 
        +"public void startCalculation ();\n" 
        +"public void stopCalculation ();\n" 
        +"public void continueCalculation ();\n" 
        +"public boolean isSimulationRunning ();\n" 
        +"\nGet the simulation results for postprocessing:\n"
        +"(RAM-data might be reduced ,e.g. just every 2nd simulation point recorded,\n"
        +"hard disk data is complete but will slow down the simulation)\n\n"
        +"public double[][] getZV (String stringID_scope);\n" 
        +"public boolean activateWriteToFile (String stringID_scope);\n" 
        +"public boolean saveZVData (String stringID_scope, String fileName);\n" 
        +"public boolean deactivateWriteToFile (String stringID_scope);\n" 
        +""; 
    private String example1= ""
        +"// Define parameter names in the GeckoCIRCUITS model by using the '$'-character in the input dialog, " 
        + "\n// e.g. set resistor value to '$b' instead of '1000' in the dialog window of the resistor. "+"\n"
        +"String[] nameOpt= new String[]{\"$a\",\"$b\",\"$c\",\"$d\",\"$fre\"}; \n"
        +"double[] valueOpt= new double[]{1,1000,3,4, 1600};  \n"
        +"\n// Now set the values of the according parameter names as defined. \n// GeckoCIRCUITS must be referenced as 'GECKO': \n"
        +"GECKO.setOptimizerParameterData(nameOpt,valueOpt); \n"
        +"\n// With these settings the simulation will run with dt=1us, and will make a stop at 4ms: \n"
        +"GECKO.setSimParameter(1e-6,10e-3,4e-3);  \n"
        +"GECKO.startCalculation(); \n"
        +"\n// We have to stop the simulation at t=4ms to set new parameter values before proceeding. " 
        +"\n// This is done with the following code: \n"
        +"while (GECKO.isSimulationRunning()) { try { Thread.sleep(500); } catch (InterruptedException ie) {} } \n"
        +"\n// New values can be set. $b is from now on 350 instead of 1000.\n"
        +"valueOpt[0]= 5;  \n"
        +"valueOpt[1]= 350;  \n"
        +"valueOpt[4]= 350;  \n"
        +"\n// Now we can proceed with the simulation:\n"
        +"GECKO.continueCalculation(); \n" 
        +""; 
    private String example2= ""
        +"String[] nameOpt= new String[]{\"$r\",\"$c\"}; \n"
        +"double rmin=10, rmax=100, r=rmin, cmin=100e-6, cmax=1e-3, c=cmin;\n"
        +"double[] valueOpt= new double[]{r,c};  \n"
        +"GECKO.setOptimizerParameterData(nameOpt,valueOpt); \n"
        +"for (int j1=0;  j1<10;  j1++) {\n"
        +"    r= rmin +j1/10.0*(rmax-rmin);\n"
        +"    for (int j2=0;  j2<10;  j2++) {\n"
        +"        c= cmin +j2/10.0*(cmax-cmin);\n"
        +"        valueOpt[0]= r;\n"
        +"        valueOpt[1]= c;\n"
        +"        GECKO.setSimParameter(1e-6,20e-3,-1);  \n"
        +"        GECKO.startCalculation(); \n"
        +"        while (GECKO.isSimulationRunning()) { try { Thread.sleep(500); } catch (Exception ie) {} } \n"
        +"        double[][] zv= GECKO.getZV(); \n"
        +"        for (int )"
        +"    }\n"
        +"}\n"
        +""; 
    //--------------
    /*
    --------------
    public void opt_setOptimizerParameterData (String[] optName, double[] optValue) { win.setOptimizerParameterData(optName,optValue); } 
    public boolean opt_isSimulationRunning () { return win.isSimulationRunning(); }
    public double[][] opt_getZV (String stringID_scope) { return win.getZV(stringID_scope); } 
    public boolean opt_activateWriteToFile (String stringID_scope) { return win.activateWriteToFile(stringID_scope); }
    public boolean opt_saveZVData (String stringID_scope, String fileName) { return win.saveZVData(stringID_scope,fileName); }
    public boolean opt_deactivateWriteToFile (String stringID_scope) { return win.deactivateWriteToFile(stringID_scope); }
    public void opt_startCalculation () { win.startCalculation(); }
    public void opt_stopCalculation () { win.stopCalculation(); }
    public void opt_continueCalculation () { win.continueCalculation(); } 
    public void opt_setSimParameter (double dt, double tDURATION, double tPAUSE) { win.setSimParameter(dt,tDURATION,tPAUSE); } 
    --------------
    */

    
    
    
    public DialogJavaCompilerOptimizer (GeckoJavaCompiler geckoJavaCompiler) {
        try { this.setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL,"gecko.gif"))).getImage()); } catch (Exception ex) {}
        this.initComponents();        
        //-------
        this.geckoJavaCompiler= geckoJavaCompiler; 
        this.setCodeText(); 
    }

    private void doRun () {
    }

    
    public void setCodeText () {
        jTextAreaCode.setText(geckoJavaCompiler.getSourceCode());
        jTextAreaCompilerMessage.setText(geckoJavaCompiler.getCompilerMessage());
        jTextAreaImports.setText(geckoJavaCompiler.getImportCode());
        jTextAreaStaticInit.setText(geckoJavaCompiler.getStaticInitCode());
        jTextAreaVariables.setText(geckoJavaCompiler.getStaticVariables());
    }
    
    private void initComponents() {
        jFrame1 = new javax.swing.JFrame();
        jButtonCompile = new javax.swing.JButton();
        jButtonExecute = new javax.swing.JButton();
        jButtonCloseWindow = new javax.swing.JButton();
        jTabbedPane = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaCode = new javax.swing.JTextArea();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextAreaImports = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTextAreaStaticInit = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        jTextAreaVariables = new javax.swing.JTextArea();
        jButtonExample2 = new javax.swing.JButton();
        jButtonExample1 = new javax.swing.JButton();
        jPanelCompilerMessages = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextAreaCompilerMessage = new javax.swing.JTextArea();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        javax.swing.GroupLayout jFrame1Layout = new javax.swing.GroupLayout(jFrame1.getContentPane());
        jFrame1.getContentPane().setLayout(jFrame1Layout);
        jFrame1Layout.setHorizontalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jFrame1Layout.setVerticalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(" Optimization Code Processor");
        setLocationByPlatform(true);

        jButtonCompile.setText("Compile Code");
        jButtonCompile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCompileActionPerformed(evt);
            }
        });

        jButtonExecute.setText("Run Optimization");
        jButtonExecute.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExecuteActionPerformed(evt);
            }
        });

        jButtonCloseWindow.setText("Close Window");
        jButtonCloseWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCloseWindowActionPerformed(evt);
            }
        });


        jTextAreaCode.setColumns(20);
        jTextAreaCode.setFont(new java.awt.Font("Courier New", 0, 13));
        jTextAreaCode.setRows(5);
        jTextAreaCode.setTabSize(4);
        jTextAreaCode.setText("out.println(\"sinus of 0.2\" + sin(0.2));");
        jScrollPane1.setViewportView(jTextAreaCode);

        jTextAreaImports.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        jTextAreaImports.setColumns(20);
        jTextAreaImports.setFont(new java.awt.Font("Courier New", 0, 13));
        jTextAreaImports.setRows(5);
        jTextAreaImports.setText("import static java.lang.System.out;\nimport static java.lang.Math.*;\n");
        jScrollPane5.setViewportView(jTextAreaImports);

        jLabel2.setText("Imports:");

        jTextAreaStaticInit.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        jTextAreaStaticInit.setColumns(20);
        jTextAreaStaticInit.setFont(new java.awt.Font("Courier New", 0, 13));
        jTextAreaStaticInit.setRows(5);
        jScrollPane6.setViewportView(jTextAreaStaticInit);

        jLabel3.setText("Static initializer:");

        jLabel4.setFont(new java.awt.Font("Courier New", 0, 14));
        jLabel4.setText("public static void _run_script () { ... }");

        jLabel6.setText("Static variables:");

        jTextAreaVariables.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        jTextAreaVariables.setColumns(20);
        jTextAreaVariables.setFont(new java.awt.Font("Courier New", 0, 13));
        jTextAreaVariables.setRows(5);
        jScrollPane7.setViewportView(jTextAreaVariables);

        jButtonExample2.setText("Example 2");
        jButtonExample2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExample2ActionPerformed(evt);
            }
        });

        jButtonExample1.setText("Example 1");
        jButtonExample1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExample1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 853, Short.MAX_VALUE)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 853, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 405, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabel6)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonExample1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonExample2)
                        .addGap(9, 9, 9)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonExample2)
                    .addComponent(jButtonExample1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane.addTab("Code", jPanel3);

        jTextAreaCompilerMessage.setColumns(20);
        jTextAreaCompilerMessage.setFont(new java.awt.Font("Courier New", 0, 13));
        jTextAreaCompilerMessage.setRows(5);
        jTextAreaCompilerMessage.setTabSize(4);
        jScrollPane3.setViewportView(jTextAreaCompilerMessage);

        javax.swing.GroupLayout jPanelCompilerMessagesLayout = new javax.swing.GroupLayout(jPanelCompilerMessages);
        jPanelCompilerMessages.setLayout(jPanelCompilerMessagesLayout);
        jPanelCompilerMessagesLayout.setHorizontalGroup(
            jPanelCompilerMessagesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCompilerMessagesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 853, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelCompilerMessagesLayout.setVerticalGroup(
            jPanelCompilerMessagesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCompilerMessagesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 580, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane.addTab("Compiler Messages", jPanelCompilerMessages);

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText("Use the following Interface-methods to control GeckoCIRCUITS:\n\n" +info+ "\n");
        jScrollPane2.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 853, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 580, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane.addTab("Interfacing GeckoCIRCUITS: Info", jPanel4);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(275, 275, 275)
                .addComponent(jButtonCompile)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonExecute)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonCloseWindow)
                .addGap(362, 362, 362))
            .addComponent(jTabbedPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonCloseWindow)
                    .addComponent(jButtonExecute)
                    .addComponent(jButtonCompile))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void jButtonCloseWindowActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonCloseWindowActionPerformed
    
    if(! (geckoJavaCompiler.getCompileStatus() == GeckoJavaCompiler.COMPILESTATUS.COMPILED_SUCCESSFULL)) {
        loadCode();
    }
    this.dispose();
}//GEN-LAST:event_jButtonCloseWindowActionPerformed

private void jButtonCompileActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonCompileActionPerformed

        jTabbedPane.setSelectedIndex(1);
        loadCode();
        this.doRun();//GEN-LAST:event_jButtonCompileActionPerformed
}

    private void jButtonExecuteActionPerformed (java.awt.event.ActionEvent evt) {
        try {
            geckoJavaCompiler.startCalculation(); 
        } catch (Exception e) {
            jTabbedPane.setSelectedIndex(1);
        }
    }

private void jButtonExample2ActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonExample2ActionPerformed
    jTextAreaCode.setText(
            "\n//out.println(xIN[0] + \" \" + xIN[1] + \" \" + xIN[2] + \" \" + xIN.length );" +
            "\n\nfor(int i = 0; i < yOUT.length; i++) { " +
            "\n\tyOUT[i] = (i+2.0) * sin(time*1000.0);" +
            "\n}" +
            "\nlabel.setText(\"Time: \" + time );" +
            "\nreturn yOUT;");
    jTextAreaImports.setText(
             "import javax.swing.JFrame;" +
             "\nimport javax.swing.JLabel;" +
             "\nimport static java.lang.System.out;" +
             "\nimport static java.lang.Math.*;");
    jTextAreaStaticInit.setText("helloWindow = new HelloWorld();" + 
                "\nhelloWindow.setVisible(true);");
    jTextAreaVariables.setText(
            "private static final JLabel label = new JLabel(\"Show Simulation Time\");" +
            "\npublic static final class HelloWorld extends JFrame {" +
            "\n\tprivate HelloWorld() {" +
            "\n\tsetDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);" +
            "\n\tgetContentPane().add(label);" +
            "\n\tpack();" +
            "\n\tsetLocationRelativeTo(null);" +
            "\n}}" + 
            "\nprivate static HelloWorld helloWindow;");
 
}//GEN-LAST:event_jButtonExample2ActionPerformed

private void jButtonExample1ActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonExample1ActionPerformed
    jTextAreaCode.setText(example1); 
    jTextAreaImports.setText("import ch.technokrat.gecko.geckocircuits.allg.Fenster;\n");
    jTextAreaStaticInit.setText("");
    jTextAreaVariables.setText("");     
}//GEN-LAST:event_jButtonExample1ActionPerformed
    


private void loadCode () {
        try {
            geckoJavaCompiler.setSourceCode(jTextAreaCode.getText());
            geckoJavaCompiler.setImportCode(jTextAreaImports.getText());
            geckoJavaCompiler.setStaticInitCode(jTextAreaStaticInit.getText());
            geckoJavaCompiler.setStaticVariables(jTextAreaVariables.getText());
            geckoJavaCompiler.doCompilation();
            jTextAreaCompilerMessage.setText(geckoJavaCompiler.getCompilerMessage());
        } catch (IOException ex) {
            Logger.getLogger(DialogJavaCompilerOptimizer.class.getName()).log(Level.SEVERE, null, ex);
        }
}
    
    

    
    
}

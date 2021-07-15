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
package ch.technokrat.gecko.geckoscript;

import ch.technokrat.gecko.Category;
import ch.technokrat.gecko.Declaration;
import ch.technokrat.gecko.Documentation;
import ch.technokrat.gecko.GeckoRemoteInterface;
import ch.technokrat.gecko.MethodCategory;
import ch.technokrat.gecko.geckocircuits.allg.StartupWindow;
import ch.technokrat.gecko.geckocircuits.control.javablock.CodeWindow;
import ch.technokrat.gecko.geckocircuits.control.javablock.CompileStatus;
import ch.technokrat.gecko.geckocircuits.control.javablock.ExtraFilesWindow;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Random;
import java.util.Date;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import javax.swing.*;
import de.sciss.syntaxpane.DefaultSyntaxKit;

public class ScriptWindow extends javax.swing.JFrame {
    ExtraFilesWindow _extSourceWindow;
    private boolean _extWindowInit = false;
    SimulationAccess _circuit;
    Random _nameGenerator;
    String _declarations = "";
    String _className = "GeckoCustom";
    String _sourceCode = "";
    String _advancedVariables = "";
    String _advancedConstructor = "";
    boolean _advancedOption = false;
    HashMap _advancedObjects;

    String _compileSourceCode = "";
    String _workingDirectory;
    CompileStatus _compileStatus = CompileStatus.NOT_COMPILED;    
    AbstractGeckoCustom _scriptObject;
    final JEditorPane _codeTextArea;
    final JEditorPane _declarationsTextArea;
    final JEditorPane _importsTextArea;
    final JEditorPane _sourceCodeCompilerTextArea;
    final JEditorPane _compMessagesTextArea;
    String compilerMessages = "";
    final DefaultListModel _listModel;
    final DefaultComboBoxModel _categoryModel;
    final PrintStream _outputStream;        
    

    /**
     * Creates new form ScriptWindow
     */
    public ScriptWindow(SimulationAccess circuitSim) {
        _extSourceWindow = new ExtraFilesWindow(circuitSim);
        _circuit = circuitSim;
        _nameGenerator = new Random((new Date()).getTime());
        initComponents();
        _listModel = new DefaultListModel();
        jListFunctions.setModel(_listModel);
        _outputStream = new PrintStream(new TextAreaOutputStream(jTextAreaOutput));
        _categoryModel = new DefaultComboBoxModel();
        for (MethodCategory cat : MethodCategory.values()) {
            _categoryModel.addElement(cat);
        }

        jComboBoxCategory.setModel(_categoryModel);
        jComboBoxCategory.setSelectedIndex(_categoryModel.getSize() - 1);

        loadFunctionInfosIntoList();

        jComboBoxCategory.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                loadFunctionInfosIntoList();
            }
        });

        jListFunctions.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = jListFunctions.locationToIndex(evt.getPoint());
                    FunctionDescription funcDes = (FunctionDescription) _listModel.get(index);
                    optionJDialog(funcDes._detailsString);
                }
            }
        });

        DefaultSyntaxKit.initKit();

        _codeTextArea = CodeWindow.createScrollableEditorPane(jPanelCodeEditor);

        _declarationsTextArea = CodeWindow.createScrollableEditorPane(jPanelDeclarations);
        _importsTextArea = CodeWindow.createScrollableEditorPane(jPanelImports);
        _sourceCodeCompilerTextArea = CodeWindow.createScrollableEditorPane(jPanelCompMessages);
        _sourceCodeCompilerTextArea.setEditable(false);

        _compMessagesTextArea = CodeWindow.createScrollableEditorPane(jPanelCompilerErrors);
        _compMessagesTextArea.setEditable(false);

        this.setMinimumSize(new Dimension(1000, 800));

    }

    //Example of using the JOptionPane with
    //the JDialog class
    public void optionJDialog(String displayText) {

        final JDialog optionPaneDialog = new JDialog(this, "Available function details");

        //Note we are creating an instance of a JOptionPane
        //Normally it's just a call to a static method.
        JOptionPane optPane = new JOptionPane(displayText,
                JOptionPane.INFORMATION_MESSAGE, JOptionPane.PLAIN_MESSAGE);

        //Listen for the JOptionPane button click. It comes through as property change 
        //event with the propety called "value". 
        optPane.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals("value")) {
                    optionPaneDialog.dispose();

                }
            }
        });
        optionPaneDialog.setContentPane(optPane);

        //Let the JDialog figure out how big it needs to be
        //based on the size of JOptionPane by calling the 
        //pack() method
        optionPaneDialog.pack();
        optionPaneDialog.setLocationRelativeTo(this);
        optionPaneDialog.setVisible(true);

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButtonCloseWindow = new javax.swing.JButton();
        jButtonCompile = new javax.swing.JButton();
        jTabbedPane = new javax.swing.JTabbedPane();
        jPanelCode = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel2 = new javax.swing.JPanel();
        jPanelCodeEditor = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jListFunctions =         new javax.swing.JList() {

            public String getToolTipText(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (-1 < index) {
                    Object item = getModel().getElementAt(index);
                    if (item instanceof FunctionDescription) {
                        String returnValue = ((FunctionDescription) item)._detailsString;
                        if (!returnValue.isEmpty()) {
                            return returnValue;
                        }
                    }

                }
                return null;
            }
        };
        jComboBoxCategory = new javax.swing.JComboBox();
        jSplitPane3 = new javax.swing.JSplitPane();
        jPanel4 = new javax.swing.JPanel();
        jPanelImports = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jPanelDeclarations = new javax.swing.JPanel();
        jPanelCompilerMessages = new javax.swing.JPanel();
        jPanelCompMessages = new javax.swing.JPanel();
        jPanelCompilerErrors = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanelRunOutput = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextAreaOutput = new javax.swing.JTextArea();
        jPanelInfo = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButtonRun = new javax.swing.JButton();
        jButtonAbort = new javax.swing.JButton();
        jButtonFunctionDetails1 = new javax.swing.JButton();
        jButtonAdditionalSources = new javax.swing.JButton();

        setTitle("GeckoSCRIPT");
        setLocationByPlatform(true);
        setMinimumSize(new java.awt.Dimension(500, 400));

        jButtonCloseWindow.setText("Close window");
        jButtonCloseWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCloseWindowActionPerformed(evt);
            }
        });

        jButtonCompile.setText("Compile code");
        jButtonCompile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCompileActionPerformed(evt);
            }
        });

        jSplitPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(0.1);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("public void runScript() { ... }"));

        jPanelCodeEditor.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelCodeEditor, javax.swing.GroupLayout.DEFAULT_SIZE, 1072, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelCodeEditor, javax.swing.GroupLayout.DEFAULT_SIZE, 539, Short.MAX_VALUE)
        );

        jSplitPane1.setBottomComponent(jPanel2);

        jSplitPane2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jSplitPane2.setDividerLocation(450);
        jSplitPane2.setResizeWeight(0.5);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Available functions"));

        jListFunctions.setFont(jListFunctions.getFont().deriveFont(jListFunctions.getFont().getSize()-1f));
        jListFunctions.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jListFunctions);

        jComboBoxCategory.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 611, Short.MAX_VALUE)
            .addComponent(jComboBoxCategory, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addComponent(jComboBoxCategory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE))
        );

        jSplitPane2.setRightComponent(jPanel3);

        jSplitPane3.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jSplitPane3.setDividerLocation(80);
        jSplitPane3.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane3.setResizeWeight(0.5);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Imports"));

        jPanelImports.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelImports, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelImports, javax.swing.GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
        );

        jSplitPane3.setTopComponent(jPanel4);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Field declarations"));

        jPanelDeclarations.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelDeclarations, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelDeclarations, javax.swing.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
        );

        jSplitPane3.setRightComponent(jPanel5);

        jSplitPane2.setLeftComponent(jSplitPane3);

        jSplitPane1.setLeftComponent(jSplitPane2);

        javax.swing.GroupLayout jPanelCodeLayout = new javax.swing.GroupLayout(jPanelCode);
        jPanelCode.setLayout(jPanelCodeLayout);
        jPanelCodeLayout.setHorizontalGroup(
            jPanelCodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1084, Short.MAX_VALUE)
        );
        jPanelCodeLayout.setVerticalGroup(
            jPanelCodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 772, Short.MAX_VALUE)
        );

        jTabbedPane.addTab("Code", jPanelCode);

        jPanelCompMessages.setLayout(new java.awt.BorderLayout());

        jPanelCompilerErrors.setLayout(new java.awt.BorderLayout());

        jLabel1.setText("Compiler messages:");

        javax.swing.GroupLayout jPanelCompilerMessagesLayout = new javax.swing.GroupLayout(jPanelCompilerMessages);
        jPanelCompilerMessages.setLayout(jPanelCompilerMessagesLayout);
        jPanelCompilerMessagesLayout.setHorizontalGroup(
            jPanelCompilerMessagesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelCompilerMessagesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelCompilerMessagesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanelCompMessages, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1060, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelCompilerErrors, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1011, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelCompilerMessagesLayout.setVerticalGroup(
            jPanelCompilerMessagesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelCompilerMessagesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelCompMessages, javax.swing.GroupLayout.DEFAULT_SIZE, 575, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelCompilerErrors, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane.addTab("Compiler Output", jPanelCompilerMessages);

        jTextAreaOutput.setEditable(false);
        jTextAreaOutput.setColumns(20);
        jTextAreaOutput.setLineWrap(true);
        jTextAreaOutput.setRows(5);
        jScrollPane4.setViewportView(jTextAreaOutput);

        javax.swing.GroupLayout jPanelRunOutputLayout = new javax.swing.GroupLayout(jPanelRunOutput);
        jPanelRunOutput.setLayout(jPanelRunOutputLayout);
        jPanelRunOutputLayout.setHorizontalGroup(
            jPanelRunOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRunOutputLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 1060, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelRunOutputLayout.setVerticalGroup(
            jPanelRunOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRunOutputLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 748, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane.addTab("Script Output", jPanelRunOutput);

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText("GeckoSCRIPT is an interface to GeckoCIRCUITS allowing script based control, modification, and simulation of GeckoCIRCUIT models.\n\nScripting is done using the Java programming language. The full Java API is available. Users may therefore import any Java packages they have on their computer (e.g. math functions, Collections, etc.) via the 'imports' field in GeckoSCRIPT. Furthermore, a set of functions is made available to manipulate GeckoCIRCUIT models and simulations.\n\nThese functions are listed in the upper left corner of the 'Code' tab, and clicking 'Details' will show a short description of each function and how it should be used. For modifying model elements, these functions operate strictly with names - the names of parameters for a model element in GeckoCIRCUITS (seen when double-clicking on an element) and the names of each element instance (modifiable by the user). Clicking 'Available Blocks' will show the all the blocks (by type and name) in the model and their accessible parameters and (if they are control blocks) outputs.\n\n");
        jTextArea1.setWrapStyleWord(true);
        jScrollPane2.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanelInfoLayout = new javax.swing.GroupLayout(jPanelInfo);
        jPanelInfo.setLayout(jPanelInfoLayout);
        jPanelInfoLayout.setHorizontalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1060, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelInfoLayout.setVerticalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 748, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane.addTab("Help", jPanelInfo);

        jButtonRun.setText("Run");
        jButtonRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRunActionPerformed(evt);
            }
        });

        jButtonAbort.setText("Abort script");
        jButtonAbort.setEnabled(false);
        jButtonAbort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAbortActionPerformed(evt);
            }
        });

        jButtonFunctionDetails1.setText("Available Blocks");
        jButtonFunctionDetails1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFunctionDetails1ActionPerformed(evt);
            }
        });

        jButtonAdditionalSources.setText("Additional Sources");
        jButtonAdditionalSources.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAdditionalSourcesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButtonCompile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonRun, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(30, 30, 30)
                        .addComponent(jButtonAbort, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCloseWindow)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonAdditionalSources)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonFunctionDetails1)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButtonAbort, jButtonCloseWindow, jButtonCompile, jButtonRun});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButtonRun)
                        .addComponent(jButtonAbort)
                        .addComponent(jButtonCloseWindow)
                        .addComponent(jButtonFunctionDetails1)
                        .addComponent(jButtonAdditionalSources))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButtonCompile)
                        .addContainerGap())))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonCloseWindowActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonCloseWindowActionPerformed

        this.dispose();
}//GEN-LAST:event_jButtonCloseWindowActionPerformed
    private void jButtonCompileActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonCompileActionPerformed

        if (StartupWindow.testDialogOpenSourceVersion("GeckoSCRIPT")) {
            return;
        }

        jTabbedPane.setSelectedIndex(1);
        compileCode();

}//GEN-LAST:event_jButtonCompileActionPerformed
    private Thread computationThread;

    private void jButtonRunActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonRunActionPerformed

        if (StartupWindow.testDialogOpenSourceVersion("GeckSCRIPT")) {
            return;
        }

        jTabbedPane.setSelectedIndex(2);
        compileCode();

        if (_compileStatus == CompileStatus.COMPILE_ERROR) {
            jTabbedPane.setSelectedIndex(1);
        }

        computationThread = new Thread() {

            @Override
            public void run() {
                jButtonRun.setEnabled(false);
                jButtonAbort.setEnabled(true);
                runCode();
                jButtonRun.setEnabled(true);
                jButtonAbort.setEnabled(false);
            }
        };

        computationThread.setPriority(Thread.MIN_PRIORITY);
        computationThread.start();

    }//GEN-LAST:event_jButtonRunActionPerformed

    public void runNewComputationThread() {
        
    }
    
    
    private void jButtonFunctionDetails1ActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonFunctionDetails1ActionPerformed

        ParameterSupport _paramSupport = new ParameterSupport(_circuit);
        _paramSupport.setVisible(true);
    }//GEN-LAST:event_jButtonFunctionDetails1ActionPerformed

    private void jButtonAbortActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonAbortActionPerformed
        if (computationThread != null && computationThread.isAlive()) {
            computationThread.stop();
        }
    }//GEN-LAST:event_jButtonAbortActionPerformed

    private void jButtonAdditionalSourcesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAdditionalSourcesActionPerformed
        if (!_extWindowInit) {
            _extSourceWindow.addNewFiles(_circuit._additionalSourceFiles);
            _extWindowInit = true;
        }
        _extSourceWindow.setVisible(true);
    }//GEN-LAST:event_jButtonAdditionalSourcesActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAbort;
    private javax.swing.JButton jButtonAdditionalSources;
    private javax.swing.JButton jButtonCloseWindow;
    private javax.swing.JButton jButtonCompile;
    private javax.swing.JButton jButtonFunctionDetails1;
    private javax.swing.JButton jButtonRun;
    private javax.swing.JComboBox jComboBoxCategory;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JList jListFunctions;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanelCode;
    private javax.swing.JPanel jPanelCodeEditor;
    private javax.swing.JPanel jPanelCompMessages;
    private javax.swing.JPanel jPanelCompilerErrors;
    private javax.swing.JPanel jPanelCompilerMessages;
    private javax.swing.JPanel jPanelDeclarations;
    private javax.swing.JPanel jPanelImports;
    private javax.swing.JPanel jPanelInfo;
    private javax.swing.JPanel jPanelRunOutput;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JSplitPane jSplitPane3;
    private javax.swing.JTabbedPane jTabbedPane;
    private javax.swing.JTextArea jTextArea1;
    javax.swing.JTextArea jTextAreaOutput;
    // End of variables declaration//GEN-END:variables
    private static final String functionDetails = "<font face=\"Tahoma\" size=\"3\">This is a set of functions available within the GeckoSCRIPT which allows the user to write "
            + "scripts for manipulating the simulation and the model within GeckoCIRCUITS. It gives the ability to run the simulation, "
            + "extract results from it, display results and parameter values, and to change the parameter values of model elements. As "
            + "far as the elements of the model (switches, capacitors, integrators, etc.) are concerned, these functions operate based on "
            + "their names (which GeckoCIRCUITS gives by default and which are also editable by the user) and the names of their "
            + "parameters (visible by opening the dialog box for each element by double-clicking on it). </font><br><br><br>";

    private void compileCode() {        
        CompileScript.compile(this);
    }

    public void addSourceLine(String newLine) {
        _sourceCode += newLine + "\n";
    }

    private void runCode() {
        jTextAreaOutput.setText("");
        if (_compileStatus == CompileStatus.COMPILED_SUCCESSFULL) {
            try {
                _scriptObject.runScript();
            } catch (Throwable e) {
                Throwable toPrint = e;
                if (e.getCause() != null) {
                    toPrint = e.getCause();
                }
                System.err.println("\t" + e);
                // this is UGLY: we need a newline at the end, otherwise the output is not written!                
                _outputStream.append("\n\t" + e.getMessage() + "\n");
                for (StackTraceElement ste : e.getStackTrace()) {
                    _outputStream.append(ste + "\n\t");
                }
            }
        }
    }

    public String getSourceCode() {
        return _codeTextArea.getText();
    }

    public String getImportCode() {
        return _importsTextArea.getText();
    }

    public void setImportCode(String importCode) {
        _importsTextArea.setText(importCode);
    }

    public String getDeclarationCode() {
        return _declarationsTextArea.getText();
    }

    public void setDeclarationCode(String decCode) {
        _declarationsTextArea.setText(decCode);
    }

    void setScripterCode(String scripterCode) {
        _codeTextArea.setText(scripterCode);
    }

    String getScripterDeclarations() {
        return _declarationsTextArea.getText();
    }

    private void loadFunctionInfosIntoList() {
        _listModel.clear();
        Class RemoteClass = AbstractGeckoCustom.class;

        for (Method method : RemoteClass.getMethods()) {
            doFilterListMethod(method);
        }

        Class RemoteClass2 = GeckoRemoteInterface.class;
        for (Method method : RemoteClass2.getMethods()) {
            doFilterListMethod(method);
        }
    }

    private void doFilterListMethod(final Method method) {
        final MethodCategory selection = (MethodCategory) jComboBoxCategory.getSelectedItem();
        Documentation docAnnotation = method.getAnnotation(Documentation.class);
        Declaration declaration = method.getAnnotation(Declaration.class);

        if (docAnnotation != null && declaration != null) {
            Category category = method.getAnnotation(Category.class);
            if (category == null) {
                System.err.println("empty category " + method);
                throw new NullPointerException("Empty Category " + method);
            }
            if (selection == MethodCategory.ALL_CATEGORIES || category.value() == selection) {
                _listModel.addElement(new FunctionDescription(declaration.value(), docAnnotation.value().getTranslation()));
            }
        } else {
            if (docAnnotation == null && declaration != null || docAnnotation != null && declaration == null) {
                System.out.println("method not properly documented " + method);
            }
        }
    }

    //when we open a new file, we should clear the GeckoCustom object
    void clearObject() {
        _scriptObject = null;
        _compileStatus = CompileStatus.NOT_COMPILED;
    }            
    

}

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
package gecko.geckoscript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gecko.Category;
import gecko.Declaration;
import gecko.Documentation;
import gecko.GeckoRemoteInterface;
import gecko.MethodCategory;
import gecko.geckocircuits.general.StartupWindow;
import gecko.geckocircuits.control.javablock.CodeWindowModern;
import gecko.geckocircuits.control.javablock.CompileStatus;
import gecko.geckocircuits.control.javablock.ExtraFilesWindow;
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
import java.nio.charset.StandardCharsets;
import javax.swing.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Script window stores simulation access reference for script execution")
public final class ScriptWindow extends JFrame {
    private static final Logger LOGGER = LogManager.getLogger(ScriptWindow.class);

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
    HashMap<String, Object> _advancedObjects;

    String _compileSourceCode = "";
    String _workingDirectory;
    CompileStatus _compileStatus = CompileStatus.NOT_COMPILED;
    AbstractGeckoCustom _scriptObject;
    final RSyntaxTextArea _codeTextArea;
    final RSyntaxTextArea _declarationsTextArea;
    final RSyntaxTextArea _importsTextArea;
    final RSyntaxTextArea _sourceCodeCompilerTextArea;
    final RSyntaxTextArea _compMessagesTextArea;
    String compilerMessages = "";
    final DefaultListModel<FunctionDescription> _listModel;
    final DefaultComboBoxModel<MethodCategory> _categoryModel;
    final PrintStream _outputStream;


    /**
     * Creates new form ScriptWindow
     */
    public ScriptWindow(SimulationAccess circuitSim) {
        _extSourceWindow = new ExtraFilesWindow(circuitSim);
        _circuit = circuitSim;
        _nameGenerator = new Random(new Date().getTime());
        initComponents();
        _listModel = new DefaultListModel<>();
        jListFunctions.setModel(_listModel);
        _outputStream = new PrintStream(new TextAreaOutputStream(jTextAreaOutput), true, StandardCharsets.UTF_8);
        _categoryModel = new DefaultComboBoxModel<>();
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

            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = jListFunctions.locationToIndex(evt.getPoint());
                    FunctionDescription funcDes = (FunctionDescription) _listModel.get(index);
                    optionJDialog(funcDes._detailsString);
                }
            }
        });

        _codeTextArea = CodeWindowModern.createScrollableEditorPane(jPanelCodeEditor);

        _declarationsTextArea = CodeWindowModern.createScrollableEditorPane(jPanelDeclarations);
        _importsTextArea = CodeWindowModern.createScrollableEditorPane(jPanelImports);
        _sourceCodeCompilerTextArea = CodeWindowModern.createScrollableEditorPane(jPanelCompMessages);
        _sourceCodeCompilerTextArea.setEditable(false);

        _compMessagesTextArea = CodeWindowModern.createScrollableEditorPane(jPanelCompilerErrors);
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

            @Override
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
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButtonCloseWindow = new JButton();
        jButtonCompile = new JButton();
        jTabbedPane = new JTabbedPane();
        jPanelCode = new JPanel();
        jSplitPane1 = new JSplitPane();
        jPanel2 = new JPanel();
        jPanelCodeEditor = new JPanel();
        jSplitPane2 = new JSplitPane();
        jPanel3 = new JPanel();
        jScrollPane1 = new JScrollPane();
        jListFunctions =         new JList<FunctionDescription>() {

            @Override
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
        jComboBoxCategory = new JComboBox<>();
        jSplitPane3 = new JSplitPane();
        jPanel4 = new JPanel();
        jPanelImports = new JPanel();
        jPanel5 = new JPanel();
        jPanelDeclarations = new JPanel();
        jPanelCompilerMessages = new JPanel();
        jPanelCompMessages = new JPanel();
        jPanelCompilerErrors = new JPanel();
        jLabel1 = new JLabel();
        jPanelRunOutput = new JPanel();
        jScrollPane4 = new JScrollPane();
        jTextAreaOutput = new JTextArea();
        jPanelInfo = new JPanel();
        jScrollPane2 = new JScrollPane();
        jTextArea1 = new JTextArea();
        jButtonRun = new JButton();
        jButtonAbort = new JButton();
        jButtonFunctionDetails1 = new JButton();
        jButtonAdditionalSources = new JButton();

        setTitle("GeckoSCRIPT");
        setLocationByPlatform(true);
        setMinimumSize(new Dimension(500, 400));

        jButtonCloseWindow.setText("Close window");
        jButtonCloseWindow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                jButtonCloseWindowActionPerformed(evt);
            }
        });

        jButtonCompile.setText("Compile code");
        jButtonCompile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                jButtonCompileActionPerformed(evt);
            }
        });

        jSplitPane1.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setOrientation(JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(0.1);

        jPanel2.setBorder(BorderFactory.createTitledBorder("public void runScript() { ... }"));

        jPanelCodeEditor.setLayout(new java.awt.BorderLayout());

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jPanelCodeEditor, GroupLayout.DEFAULT_SIZE, 1072, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jPanelCodeEditor, GroupLayout.DEFAULT_SIZE, 539, Short.MAX_VALUE)
        );

        jSplitPane1.setBottomComponent(jPanel2);

        jSplitPane2.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jSplitPane2.setDividerLocation(450);
        jSplitPane2.setResizeWeight(0.5);

        jPanel3.setBorder(BorderFactory.createTitledBorder("Available functions"));

        jListFunctions.setFont(jListFunctions.getFont().deriveFont(jListFunctions.getFont().getSize()-1f));
        jListFunctions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jListFunctions);

        jComboBoxCategory.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 611, Short.MAX_VALUE)
            .addComponent(jComboBoxCategory, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addComponent(jComboBoxCategory, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE))
        );

        jSplitPane2.setRightComponent(jPanel3);

        jSplitPane3.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jSplitPane3.setDividerLocation(80);
        jSplitPane3.setOrientation(JSplitPane.VERTICAL_SPLIT);
        jSplitPane3.setResizeWeight(0.5);

        jPanel4.setBorder(BorderFactory.createTitledBorder("Imports"));

        jPanelImports.setLayout(new java.awt.BorderLayout());

        GroupLayout jPanel4Layout = new GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jPanelImports, GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jPanelImports, GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
        );

        jSplitPane3.setTopComponent(jPanel4);

        jPanel5.setBorder(BorderFactory.createTitledBorder("Field declarations"));

        jPanelDeclarations.setLayout(new java.awt.BorderLayout());

        GroupLayout jPanel5Layout = new GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jPanelDeclarations, GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jPanelDeclarations, GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
        );

        jSplitPane3.setRightComponent(jPanel5);

        jSplitPane2.setLeftComponent(jSplitPane3);

        jSplitPane1.setLeftComponent(jSplitPane2);

        GroupLayout jPanelCodeLayout = new GroupLayout(jPanelCode);
        jPanelCode.setLayout(jPanelCodeLayout);
        jPanelCodeLayout.setHorizontalGroup(
            jPanelCodeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, GroupLayout.DEFAULT_SIZE, 1084, Short.MAX_VALUE)
        );
        jPanelCodeLayout.setVerticalGroup(
            jPanelCodeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, GroupLayout.DEFAULT_SIZE, 772, Short.MAX_VALUE)
        );

        jTabbedPane.addTab("Code", jPanelCode);

        jPanelCompMessages.setLayout(new java.awt.BorderLayout());

        jPanelCompilerErrors.setLayout(new java.awt.BorderLayout());

        jLabel1.setText("Compiler messages:");

        GroupLayout jPanelCompilerMessagesLayout = new GroupLayout(jPanelCompilerMessages);
        jPanelCompilerMessages.setLayout(jPanelCompilerMessagesLayout);
        jPanelCompilerMessagesLayout.setHorizontalGroup(
            jPanelCompilerMessagesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, jPanelCompilerMessagesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelCompilerMessagesLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanelCompMessages, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 1060, Short.MAX_VALUE)
                    .addComponent(jLabel1, GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelCompilerErrors, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 1011, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelCompilerMessagesLayout.setVerticalGroup(
            jPanelCompilerMessagesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, jPanelCompilerMessagesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelCompMessages, GroupLayout.DEFAULT_SIZE, 575, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelCompilerErrors, GroupLayout.PREFERRED_SIZE, 140, GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane.addTab("Compiler Output", jPanelCompilerMessages);

        jTextAreaOutput.setEditable(false);
        jTextAreaOutput.setColumns(20);
        jTextAreaOutput.setLineWrap(true);
        jTextAreaOutput.setRows(5);
        jScrollPane4.setViewportView(jTextAreaOutput);

        GroupLayout jPanelRunOutputLayout = new GroupLayout(jPanelRunOutput);
        jPanelRunOutput.setLayout(jPanelRunOutputLayout);
        jPanelRunOutputLayout.setHorizontalGroup(
            jPanelRunOutputLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRunOutputLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, GroupLayout.DEFAULT_SIZE, 1060, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelRunOutputLayout.setVerticalGroup(
            jPanelRunOutputLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRunOutputLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, GroupLayout.DEFAULT_SIZE, 748, Short.MAX_VALUE)
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

        GroupLayout jPanelInfoLayout = new GroupLayout(jPanelInfo);
        jPanelInfo.setLayout(jPanelInfoLayout);
        jPanelInfoLayout.setHorizontalGroup(
            jPanelInfoLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 1060, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelInfoLayout.setVerticalGroup(
            jPanelInfoLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 748, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane.addTab("Help", jPanelInfo);

        jButtonRun.setText("Run");
        jButtonRun.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                jButtonRunActionPerformed(evt);
            }
        });

        jButtonAbort.setText("Abort script");
        jButtonAbort.setEnabled(false);
        jButtonAbort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                jButtonAbortActionPerformed(evt);
            }
        });

        jButtonFunctionDetails1.setText("Available Blocks");
        jButtonFunctionDetails1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                jButtonFunctionDetails1ActionPerformed(evt);
            }
        });

        jButtonAdditionalSources.setText("Additional Sources");
        jButtonAdditionalSources.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                jButtonAdditionalSourcesActionPerformed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButtonCompile)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonRun, GroupLayout.PREFERRED_SIZE, 110, GroupLayout.PREFERRED_SIZE)
                        .addGap(30, 30, 30)
                        .addComponent(jButtonAbort, GroupLayout.PREFERRED_SIZE, 117, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCloseWindow)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonAdditionalSources)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonFunctionDetails1)))
                .addContainerGap())
        );

        layout.linkSize(SwingConstants.HORIZONTAL, new java.awt.Component[] {jButtonAbort, jButtonCloseWindow, jButtonCompile, jButtonRun});

        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jButtonRun)
                        .addComponent(jButtonAbort)
                        .addComponent(jButtonCloseWindow)
                        .addComponent(jButtonFunctionDetails1)
                        .addComponent(jButtonAdditionalSources))
                    .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
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
                try {
                    runCode();
                } catch (Exception e) {
                    if (currentThread().isInterrupted()) {
                        _outputStream.append("\nScript execution was aborted.\n");
                    } else {
                        Throwable toPrint = e;
                        if (e.getCause() != null) {
                            toPrint = e.getCause();
                        }
                        _outputStream.append("\n\t" + toPrint.getMessage() + "\n");
                    }
                } finally {
                    jButtonRun.setEnabled(true);
                    jButtonAbort.setEnabled(false);
                }
            }
        };

        computationThread.setPriority(Thread.MIN_PRIORITY);
        computationThread.start();

    }//GEN-LAST:event_jButtonRunActionPerformed

    public void runNewComputationThread() {

        // no-op
    }


    private void jButtonFunctionDetails1ActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonFunctionDetails1ActionPerformed

        ParameterSupport _paramSupport = new ParameterSupport(_circuit);
        _paramSupport.setVisible(true);
    }//GEN-LAST:event_jButtonFunctionDetails1ActionPerformed

    private void jButtonAbortActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonAbortActionPerformed
        if (computationThread != null && computationThread.isAlive()) {
            computationThread.interrupt();
        }
    }//GEN-LAST:event_jButtonAbortActionPerformed

    private void jButtonAdditionalSourcesActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonAdditionalSourcesActionPerformed
        if (!_extWindowInit) {
            _extSourceWindow.addNewFiles(_circuit._additionalSourceFiles);
            _extWindowInit = true;
        }
        _extSourceWindow.setVisible(true);
    }//GEN-LAST:event_jButtonAdditionalSourcesActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton jButtonAbort;
    private JButton jButtonAdditionalSources;
    private JButton jButtonCloseWindow;
    private JButton jButtonCompile;
    private JButton jButtonFunctionDetails1;
    private JButton jButtonRun;
    private JComboBox<MethodCategory> jComboBoxCategory;
    private JLabel jLabel1;
    private JList<FunctionDescription> jListFunctions;
    private JPanel jPanel2;
    private JPanel jPanel3;
    private JPanel jPanel4;
    private JPanel jPanel5;
    private JPanel jPanelCode;
    private JPanel jPanelCodeEditor;
    private JPanel jPanelCompMessages;
    private JPanel jPanelCompilerErrors;
    private JPanel jPanelCompilerMessages;
    private JPanel jPanelDeclarations;
    private JPanel jPanelImports;
    private JPanel jPanelInfo;
    private JPanel jPanelRunOutput;
    private JScrollPane jScrollPane1;
    private JScrollPane jScrollPane2;
    private JScrollPane jScrollPane4;
    private JSplitPane jSplitPane1;
    private JSplitPane jSplitPane2;
    private JSplitPane jSplitPane3;
    private JTabbedPane jTabbedPane;
    private JTextArea jTextArea1;
    JTextArea jTextAreaOutput;
    // End of variables declaration//GEN-END:variables

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
                LOGGER.error("\t" + e);
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
        Class<?> RemoteClass = AbstractGeckoCustom.class;

        for (Method method : RemoteClass.getMethods()) {
            doFilterListMethod(method);
        }

        Class<?> RemoteClass2 = GeckoRemoteInterface.class;
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
                LOGGER.error("empty category " + method);
                throw new NullPointerException("Empty Category " + method);
            }
            if (selection == MethodCategory.ALL_CATEGORIES || category.value() == selection) {
                _listModel.addElement(new FunctionDescription(declaration.value(), docAnnotation.value().getTranslation()));
            }
        } else {
            if (docAnnotation == null && declaration != null || docAnnotation != null && declaration == null) {
                LOGGER.info("method not properly documented " + method);
            }
        }
    }

    //when we open a new file, we should clear the GeckoCustom object
    void clearObject() {
        _scriptObject = null;
        _compileStatus = CompileStatus.NOT_COMPILED;
    }


}

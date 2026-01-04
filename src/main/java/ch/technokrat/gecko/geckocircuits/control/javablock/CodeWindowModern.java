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
import ch.technokrat.gecko.geckocircuits.circuit.NameAlreadyExistsException;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CodeWindowModern extends JFrame {

    private ReglerJavaFunction _javaFunction;
    private final VariableBusWidth _variableBusWidth;
    private final StringBuffer _outputStringBuffer;

    private final RSyntaxTextArea _codeTextArea;
    private final RSyntaxTextArea _importsTextArea;
    private final RSyntaxTextArea _variablesTextArea;
    private final RSyntaxTextArea _initTextArea;
    private final RSyntaxTextArea _compiledSourceView;
    private final RSyntaxTextArea _compilerMessages;

    private final JTabbedPane _tabbedPane;
    private JCheckBox _checkBoxClear;
    private JCheckBox _checkBoxMatrix;
    private JRadioButton _radioConsole;
    private JRadioButton _radioWindow;
    private JCheckBox _checkBoxShowName;
    private JTextField _textFieldName;

    final ExtraFilesWindow _extSourceWindow;
    private boolean _extWindowInit = false;

    public CodeWindowModern(ReglerJavaFunction regelBlock, StringBuffer outputStringBuffer) {
        super();
        setTitle("Java Custom Code Control Block");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1050, 800);
        setLocationByPlatform(true);
        setMinimumSize(new Dimension(800, 600));

        _javaFunction = regelBlock;
        _variableBusWidth = regelBlock._variableBusWidth;
        _outputStringBuffer = outputStringBuffer;

        _extSourceWindow = new ExtraFilesWindow(_javaFunction);

        _tabbedPane = new JTabbedPane();
        _tabbedPane.setFont(new Font("Dialog", Font.BOLD, 11));

        _codeTextArea = createEditorPanel("Code", "public double[] calculateYOUT(double[] xIN, double time, double dt) {");
        _importsTextArea = createEditorPanel("Imports", "Imports");
        _variablesTextArea = createEditorPanel("Variables", "Variable definitions");
        _initTextArea = createEditorPanel("Init", "Initialization code (executed at simulation start)");

        _compiledSourceView = createEditorPanel("Generated Source", "Generated source code");
        _compiledSourceView.setEditable(false);
        _compiledSourceView.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);

        _compilerMessages = createEditorPanel("Compiler Messages", "Compiler messages");
        _compilerMessages.setEditable(false);
        _compilerMessages.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);

        _tabbedPane.addTab("Code", createCodeTab());
        _tabbedPane.addTab("Compiler Messages", createCompilerTab());
        _tabbedPane.addTab("Info", createInfoTab());
        _tabbedPane.addTab("Output", createOutputTab());
        _tabbedPane.addTab("Advanced", createAdvancedTab());

        add(createMainPanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        loadSourcesText();
        initUIComponents();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                loadCodeIntoRegler();
            }
        });
    }

    private RSyntaxTextArea createEditorPanel(String title, String borderTitle) {
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setMarkOccurrences(true);
        textArea.setTabSize(4);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setEditable(true);
        textArea.setEnabled(true);

        textArea.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                SchematischeEingabe2.Singleton.setDirtyFlag();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                SchematischeEingabe2.Singleton.setDirtyFlag();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                SchematischeEingabe2.Singleton.setDirtyFlag();
            }
        });

        return textArea;
    }

    private JScrollPane createScrollablePane(RSyntaxTextArea textArea) {
        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.setFoldIndicatorEnabled(true);
        return scrollPane;
    }

    private static RSyntaxTextArea createScrollableEditorPaneForPanel(JPanel jPanelToInsert) {
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setMarkOccurrences(true);
        textArea.setTabSize(4);
        textArea.setMinimumSize(new Dimension(30, 30));
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setEditable(true);
        textArea.setEnabled(true);

        textArea.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                SchematischeEingabe2.Singleton.setDirtyFlag();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                SchematischeEingabe2.Singleton.setDirtyFlag();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                SchematischeEingabe2.Singleton.setDirtyFlag();
            }
        });

        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.setFoldIndicatorEnabled(true);
        jPanelToInsert.add(scrollPane, java.awt.BorderLayout.CENTER);
        return textArea;
    }

    public static RSyntaxTextArea createScrollableEditorPane(JPanel jPanelToInsert) {
        return createScrollableEditorPaneForPanel(jPanelToInsert);
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(_tabbedPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        _checkBoxShowName = new JCheckBox("Display name");
        _textFieldName = new JTextField(20);
        topPanel.add(_checkBoxShowName);
        topPanel.add(_textFieldName);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCompile = new JButton("Compile Code");
        JButton btnClose = new JButton("Close Window");
        JButton btnCancel = new JButton("Cancel");

        btnCompile.addActionListener(e -> {
            _tabbedPane.setSelectedIndex(1);
            loadCodeIntoRegler();
        });

        btnClose.addActionListener(e -> {
            loadCodeIntoRegler();
            dispose();
        });

        btnCancel.addActionListener(e -> dispose());

        buttonPanel.add(btnCompile);
        buttonPanel.add(btnClose);
        buttonPanel.add(btnCancel);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createCodeTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JSplitPane topPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        topPane.setDividerLocation(200);
        topPane.setResizeWeight(0.3);

        JPanel importsPanel = new JPanel(new BorderLayout());
        importsPanel.setBorder(BorderFactory.createTitledBorder("Imports"));
        importsPanel.add(createScrollablePane(_importsTextArea), BorderLayout.CENTER);

        JPanel varsPanel = new JPanel(new BorderLayout());
        varsPanel.setBorder(BorderFactory.createTitledBorder("Variable definitions"));
        varsPanel.add(createScrollablePane(_variablesTextArea), BorderLayout.CENTER);

        JSplitPane middlePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        middlePane.setTopComponent(importsPanel);
        middlePane.setBottomComponent(varsPanel);
        middlePane.setDividerLocation(100);
        middlePane.setResizeWeight(0.5);

        JSplitPane leftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftPane.setTopComponent(middlePane);
        leftPane.setBottomComponent(initPane());
        leftPane.setDividerLocation(200);
        leftPane.setResizeWeight(0.5);

        JSplitPane mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainPane.setTopComponent(leftPane);
        mainPane.setBottomComponent(codePane());
        mainPane.setDividerLocation(450);
        mainPane.setResizeWeight(0.7);

        panel.add(mainPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel initPane() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Initialization code (executed at simulation start)"));
        panel.add(createScrollablePane(_initTextArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel codePane() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("public double[] calculateYOUT(double[] xIN, double time, double dt) {"));
        panel.add(createScrollablePane(_codeTextArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCompilerTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.5);

        JPanel sourcePanel = new JPanel(new BorderLayout());
        sourcePanel.setBorder(BorderFactory.createTitledBorder("Generated source code"));
        sourcePanel.add(createScrollablePane(_compiledSourceView), BorderLayout.CENTER);

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Compiler messages"));
        outputPanel.add(createScrollablePane(_compilerMessages), BorderLayout.CENTER);

        splitPane.setTopComponent(sourcePanel);
        splitPane.setBottomComponent(outputPanel);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createInfoTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JTextArea infoText = new JTextArea(
            "The 'Java Code' control block contains a freely programmable interface which allows an easy and\nefficient implementation of complex control structures. Furthermore, the complete Java API is\navailable and makes GeckoCIRCUITS arbitrarily extendable.\n\n-----Technical details -----\n\nThe interface function:\n\npublic double[] calculateYOUT(final double[] xIN, final double time) { \n       // your custom code\n       //return yOUT;\n}\n\nis called at every simulation timestep. Input arguments are the block input ports (as a double\\n \narray xIN) and the current simulation time. The return value is a predefined double array yOUT.\nIt is recommended not to use the 'new' operator within this function, since it would allocate\nmemory at every simulation step. Therefore, variables should be defined in the 'variable definitions'\ntextfield, and initialized immediately, or initialize within the 'Initialization Code' textfield.\n\nA code example is supplied (button 'Load example 1') which shows a simple assignment of\noutput values. Example 2 displays the actual simulation time inside generated window. \n\nAfter a successful code compilation, the Java Code block appears in green color, otherwise an\nerror is indicated by a yellow Java-Block color.\n\nFor further documentation, refer to the various Java Tutorials or books."
        );
        infoText.setEditable(false);
        infoText.setFont(new Font("Dialog", Font.PLAIN, 12));
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(infoText);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(new JLabel("Load example code (warning: your code will be overwritten!):"));

        JButton btnExample1 = new JButton("Example 1");
        JButton btnExample2 = new JButton("Example 2");

        btnExample1.addActionListener(e -> loadExample1());
        btnExample2.addActionListener(e -> loadExample2());

        buttonPanel.add(btnExample1);
        buttonPanel.add(btnExample2);

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createOutputTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JTextArea outputText = new JTextArea();
        outputText.setEditable(false);
        outputText.setFont(new Font("Monospaced", Font.PLAIN, 12));

        _radioConsole = new JRadioButton("Use console output");
        _radioWindow = new JRadioButton("Use Java-block textfield output");
        _radioWindow.setSelected(true);

        ButtonGroup group = new ButtonGroup();
        group.add(_radioConsole);
        group.add(_radioWindow);

        _checkBoxClear = new JCheckBox("Clear Messages on simulation restart");

        _radioConsole.addActionListener(e -> toggleOutput());
        _radioWindow.addActionListener(e -> toggleOutput());
        _checkBoxClear.addActionListener(e -> _javaFunction.setClearOutput(_checkBoxClear.isSelected()));

        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(_radioConsole);
        optionsPanel.add(_radioWindow);
        optionsPanel.add(_checkBoxClear);

        panel.add(optionsPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(outputText), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAdvancedTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        _checkBoxMatrix = new JCheckBox("Use matrix input/outputs for sourcecode");
        _checkBoxMatrix.addActionListener(e -> {
            _variableBusWidth._useMatrix.setUserValue(_checkBoxMatrix.isSelected());
        });

        JButton btnExternalFiles = new JButton("Additional Sources");
        btnExternalFiles.setText("Additional Sources");
        btnExternalFiles.setToolTipText("Specify external .java sourcecode files");
        btnExternalFiles.addActionListener(e -> {
            if (!_extWindowInit) {
                _extSourceWindow.addNewFiles(_javaFunction.getFiles());
                _extWindowInit = true;
            }
            _extSourceWindow.setVisible(true);
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Add additional Java code source file (external):"));
        topPanel.add(btnExternalFiles);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(_checkBoxMatrix, BorderLayout.CENTER);
        return panel;
    }

    public void addNewExtraFiles(java.util.List<GeckoFile> newFiles) {
        _extSourceWindow.addNewFiles(newFiles);
    }

    public void loadSourcesText() {
        JavaBlockSource blockSourceCode = getCurrentJavaBlock().getBlockSourceCode();
        _importsTextArea.setText(blockSourceCode._importsCode);
        _codeTextArea.setText(blockSourceCode._sourceCode);
        _variablesTextArea.setText(blockSourceCode._variablesCode);
        _initTextArea.setText(blockSourceCode._initCode);
        _textFieldName.setText(_javaFunction.getStringID());
        _checkBoxShowName.setSelected(_javaFunction._showName.getValue());
    }

    private void initUIComponents() {
        _checkBoxClear.setSelected(_javaFunction.isClearOutput());
        if (_javaFunction.isConsoleOutput()) {
            _radioConsole.setSelected(true);
            _checkBoxClear.setEnabled(false);
        } else {
            _radioWindow.setSelected(true);
            _checkBoxClear.setEnabled(true);
        }
        toggleOutput();
    }

    private void toggleOutput() {
        boolean isWindow = _radioWindow.isSelected();
        _checkBoxClear.setEnabled(isWindow);
        _javaFunction.setConsoleOutput(!isWindow);
    }

    private void loadExample1() {
        _codeTextArea.setText(
            "\nyOUT[0]= Math.sqrt(Math.abs(xIN[0]*xIN[1])); "
            + "\nyOUT[1]= xIN[0]*xIN[1] +4*xIN[2] -yOUT[0]; "
            + "\nreturn yOUT;"
        );
        _importsTextArea.setText("");
        _initTextArea.setText("");
        _variablesTextArea.setText("");
        _tabbedPane.setSelectedIndex(0);
    }

    private void loadExample2() {
        _codeTextArea.setText(
            "\n//out.println(xIN[0] + \" \" + xIN[1] + \" \" + xIN[2] + \" \" + xIN.length );"
            + "\n\nfor(int i = 0; i < yOUT.length; i++) { "
            + "\n\tyOUT[i] = (i+2.0) * sin(time*1000.0);"
            + "\n}"
        );
        _importsTextArea.setText(
            "import javax.swing.JFrame;"
            + "\nimport javax.swing.JLabel;"
            + "\nimport static java.lang.System.out;"
            + "\nimport static java.lang.Math.*;"
        );
        _initTextArea.setText(
            "// object not yet created -> create and initialize\n"
            + "if(displayWindow == null) {\n"
            + "\tdisplayWindow = new JFrame();\n"
            + "\tdisplayWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);\n"
            + "\tdisplayWindow.getContentPane().add(label);\n"
            + "\tdisplayWindow.pack();\n"
            + "\tdisplayWindow.setSize(400,200);\n"
            + "\tdisplayWindow.setTitle(\"Simulation time output example\");\n"
            + "\tdisplayWindow.setLocationRelativeTo(null);\n"
            + "\n}\n"
        );
        _variablesTextArea.setText(
            "private final JLabel label = new JLabel(\"Show Simulation Time\");\n"
            + "private JFrame displayWindow;"
        );
        _tabbedPane.setSelectedIndex(0);
    }

    private void loadCodeIntoRegler() {
        JavaBlockSource newSource = new JavaBlockSource.Builder()
                .sourceCode(_codeTextArea.getText())
                .importsCode(_importsTextArea.getText())
                .initCode(_initTextArea.getText())
                .variablesCode(_variablesTextArea.getText())
                .build();

        AbstractJavaBlock javaBlock = getCurrentJavaBlock();
        javaBlock.compileNewBlockSource(newSource);

        _compiledSourceView.setText(javaBlock.getCompilerSource());

        String compilerMessages = javaBlock.getCompilerMessage();
        if (javaBlock.getCompileStatus() == CompileStatus.COMPILE_ERROR) {
            compilerMessages = checkForOldCompiler(javaBlock.getCompilerMessage());
        }

        _compilerMessages.setText(compilerMessages);
    }

    private AbstractJavaBlock getCurrentJavaBlock() {
        return _javaFunction.getJavaBlock();
    }

    public static String checkForOldCompiler(String compilerMessage) {
        if (compilerMessage.contains("It is recommended that the compiler be upgraded.")) {
            return compilerMessage + "\n\nImportant information: GeckoCIRCUITS is currently running with Java version "
                    + System.getProperty("java.version") + ". To avoid verbose warning messages,\nthe Java compiler included in "
                    + "lib/tools.jar can be upgraded. It is recommended to replace lib/toos.jar with lib/tools_170.jar,\nwhich you can "
                    + "find in your GeckoCIRCUITS installation folder.";
        } else if (compilerMessage.contains("class file has wrong version 52.0")) {
            return compilerMessage + "\n\nImportant information: GeckoCIRCUITS is currently running with Java version "
                    + System.getProperty("java.version") + ". To avoid verbose warning messages,\nthe Java compiler included in "
                    + "lib/tools.jar can be upgraded. It is recommended to replace lib/toos.jar with lib/tools_180.jar,\nwhich you can "
                    + "find in your GeckoCIRCUITS installation folder.";
        } else {
            return compilerMessage;
        }
    }
}

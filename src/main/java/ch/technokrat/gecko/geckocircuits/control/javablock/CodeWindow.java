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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFile;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.circuit.NameAlreadyExistsException;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.EventObject;
import java.util.List;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import de.sciss.syntaxpane.DefaultSyntaxKit;

/**
 *
 * @author muesinga
 */
// this is a long, gui-generated class, so this is maybe ok to disable warnings. When you do
// large changes, just comment out the following line, temporarily.
@SuppressWarnings({"PMD"})
public final class CodeWindow extends javax.swing.JFrame {

    ReglerJavaFunction _javaFunction;
    private final JEditorPane _codeTextArea;
    private final JEditorPane _importsTextArea;
    private final JEditorPane _variablesTextArea;
    private final JEditorPane _initTextArea;
    private final JEditorPane _compiledSourceView;
    private final JEditorPane _compilerMessages;
    final ExtraFilesWindow _extSourceWindow;
    private boolean _extWindowInit = false;
    private static final int INIT_SIZE_X = 1050;
    private static final int INIT_SIZE_Y = 800;
    private static final int MIN_TXT_SIZE = 30;
    private static final Dimension MIN_DIM = new Dimension(MIN_TXT_SIZE, MIN_TXT_SIZE);
    @SuppressWarnings("PMD")
    private final StringBuffer _outputStringBuffer;
    private int _lastOutputSize = 0;
    private Thread _outputUpdate;
    private static final int THREAD_SLEEP_MILLIS = 200;
    private static final int OUTPUT_TAB_NO = 3;
    private final WindowListener _windowListener = new WindowListener() {
        @Override
        public void windowOpened(final WindowEvent windowEvent) {
            // nothing todo here
        }

        @Override
        public void windowClosing(final WindowEvent windowEvent) {
            loadCodeIntoRegler();
        }

        @Override
        public void windowClosed(final WindowEvent windowEvent) {
            // nothing todo here
        }

        @Override
        public void windowIconified(final WindowEvent windowEvent) {
            // nothing todo here
        }

        @Override
        public void windowDeiconified(final WindowEvent windowEvent) {
            // nothing todo here
        }

        @Override
        public void windowActivated(final WindowEvent windowEvent) {
            // nothing todo here
        }

        @Override
        public void windowDeactivated(final WindowEvent windowEvent) {
            // nothing todo here
        }
    };
    private final VariableBusWidth _variableBusWidth;

    public CodeWindow(final ReglerJavaFunction regelBlock, final StringBuffer outputStringBuffer) {
        super();
        try {
            this.setIconImage(new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif")).getImage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //System.out.println("CodeWindow"); 
        initComponents();
        //-------

        ActionListener tableVisibleListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean value = jRadioButtonFixedBus.isSelected();
                jScrollPane5.setEnabled(!value);
                jSpinnerFixedOutputBus.setEnabled(value);
                jTable1.setVisible(!value);
                _variableBusWidth._fixedOutputBusEnabled.setValueWithoutUndo(value);
            }
        };


        jRadioButtonVariableBus.addActionListener(tableVisibleListener);
        jRadioButtonFixedBus.addActionListener(tableVisibleListener);

        _javaFunction = regelBlock;
        _variableBusWidth = regelBlock._variableBusWidth;
        DefaultSyntaxKit.initKit();

        _codeTextArea = createScrollableEditorPane(jPanelCode);

        _compiledSourceView = createScrollableEditorPane(jPanelCompilerSource);
        _compiledSourceView.setText(getCurrentJavaBlock().getCompilerSource());
        _compiledSourceView.setEditable(false);

        _importsTextArea = createScrollableEditorPane(jPanelImports);
        _initTextArea = createScrollableEditorPane(jPanelStaticInit);
        _variablesTextArea = createScrollableEditorPane(jPanelStatic);
        _compilerMessages = createScrollableEditorPane(jPanelCompilerOutput);

        _extSourceWindow = new ExtraFilesWindow(this._javaFunction);
        loadSourcesText();
        this.setSize(INIT_SIZE_X, INIT_SIZE_Y);

        this.addWindowListener(_windowListener);

        jCheckBoxClear.setSelected(_javaFunction.isClearOutput());
        if (_javaFunction.isConsoleOutput()) {
            jRadioButtonConsole.setSelected(true);
            jCheckBoxClear.setEnabled(false);
        } else {
            jRadioButtonConsole.setSelected(false);
            jCheckBoxClear.setEnabled(true);
        }

        addBusTable();

        _outputStringBuffer = outputStringBuffer;
        toggleOutputDirection();
        if (jTextAreaOutput.isEnabled()) {
            setOutputText();
        }

        jCheckBoxMatrix.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setMatrixButtonsEnabledDisabled();
            }
        });

        jSpinnerFixedOutputBus.setValue(_variableBusWidth._fixedOutputBusWidth.getValue());
        jSpinnerFixedOutputBus.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                _variableBusWidth._fixedOutputBusWidth.setUserValue((Integer) jSpinnerFixedOutputBus.getValue());
            }
        });
        jCheckBoxMatrix.setSelected(_variableBusWidth._useMatrix.getValue());
        setMatrixButtonsEnabledDisabled();
        
        setMatrixCodeWindow(_variableBusWidth._useMatrix.getValue());

        boolean isFixedOutputWidthEnabled = _variableBusWidth._fixedOutputBusEnabled.getValue();
        jRadioButtonFixedBus.setSelected(isFixedOutputWidthEnabled);
        jRadioButtonVariableBus.setSelected(!isFixedOutputWidthEnabled);
        jSpinnerFixedOutputBus.setEnabled(isFixedOutputWidthEnabled);
        jTable1.setVisible(!isFixedOutputWidthEnabled);
//        jCheckBoxDebug.setSelected(_javaFunction._doDebug.getValue());
//        jCheckBoxDebug.addActionListener(new ActionListener() {
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                _javaFunction._doDebug.setUserValue(jCheckBoxDebug.isEnabled());
//            }
//        }); 


    }

    private void setMatrixButtonsEnabledDisabled() {
        jRadioButtonFixedBus.setEnabled(jCheckBoxMatrix.isSelected());
        jRadioButtonVariableBus.setEnabled(jCheckBoxMatrix.isSelected());
        jSpinnerFixedOutputBus.setEnabled(jCheckBoxMatrix.isSelected());
    }

    private void setOutputText() {
        final String outputMessage = _outputStringBuffer.toString();
        _lastOutputSize = outputMessage.length();
        jTextAreaOutput.setText(outputMessage);
    }

    public void loadSourcesText() {
        final JavaBlockSource blockSourceCode = getCurrentJavaBlock().getBlockSourceCode();
        _importsTextArea.setText(blockSourceCode._importsCode);
        _codeTextArea.setText(blockSourceCode._sourceCode);
        _variablesTextArea.setText(blockSourceCode._variablesCode);
        _initTextArea.setText(blockSourceCode._initCode);
        jTextFieldName.setText(_javaFunction.getStringID());
        jCheckBoxShowName.setSelected(_javaFunction._showName.getValue());
    }

    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible);
        this.jTabbedPane.setSelectedIndex(0);

        if (visible) {
            _outputUpdate = new Thread(new OutputUpdate());
            _outputUpdate.setPriority(Thread.MIN_PRIORITY);
            _outputUpdate.start();
        }
    }

    private static final KeyListener keyPressedSetModelDirty = new KeyListener() {

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
        };
    
    public static JEditorPane createScrollableEditorPane(JPanel jPanelToInsert) {
        JEditorPane returnValue = new JEditorPane();
        JScrollPane codeScroll = new JScrollPane(returnValue);
        returnValue.setContentType("text/java");
        returnValue.setMinimumSize(MIN_DIM);
        
        returnValue.addKeyListener(keyPressedSetModelDirty);
        
        jPanelToInsert.add(codeScroll);
        return returnValue;
    }

    public static String checkForOldCompiler(final String compilerMessage) {
        if (compilerMessage.contains("It is recommended that the compiler be upgraded.")) {
            return compilerMessage + "\n\nImportant information: GeckoCIRCUITS is currently running with Java version "
                    + System.getProperty("java.version") + ". To avoid verbose warning messages,\nthe Java compiler included in "
                    + "lib/tools.jar can be upgraded. It is recommended to replace lib/toos.jar with lib/tools_170.jar,\nwhich you can "
                    + "find in your GeckoCIRCUITS installation folder.";

        } else if(compilerMessage.contains("class file has wrong version 52.0")) {
            return compilerMessage + "\n\nImportant information: GeckoCIRCUITS is currently running with Java version "
                    + System.getProperty("java.version") + ". To avoid verbose warning messages,\nthe Java compiler included in "
                    + "lib/tools.jar can be upgraded. It is recommended to replace lib/toos.jar with lib/tools_180.jar,\nwhich you can "
                    + "find in your GeckoCIRCUITS installation folder.";
        } else {
            return compilerMessage;
        }
    }

    private void setMatrixCodeWindow(boolean selected) {
        if (selected) {
            ((TitledBorder) jPanelCode.getBorder()).setTitle("public double[][] calculateYOUT(double[][] xIN, double time, double dt) {");
        } else {
            ((TitledBorder) jPanelCode.getBorder()).setTitle("public double[] calculateYOUT(double[] xIN, double time, double dt) {");
        }
    }

    private AbstractJavaBlock getCurrentJavaBlock() {
        return _javaFunction.getJavaBlock();
    }

    private void addBusTable() {
        AbstractTableModel tableModel = new AbstractTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }

            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public int getRowCount() {
                return _javaFunction.YOUT.size();
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                    case 0:
                        return "Index";
                    case 1:
                        return "Label";
                    case 2:
                        return "Width";
                    case 3:
                        assert false;
                }
                return null;
            }

            @Override
            public Object getValueAt(int row, int column) {
                switch (column) {
                    case 0:
                        return "[" + row + "]";
                    case 1:
                        return _javaFunction.YOUT.get(row).getLabelObject().getLabelString();
                    case 2:
                        return _javaFunction._variableBusWidth.getBusWidth(row);
                    default:
                        assert false;
                }
                return null;
            }
        };

        jTable1.setModel(tableModel);

        class SpinnerEditor extends AbstractCellEditor implements TableCellEditor {

            final JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
            ChangeListener changeListener;

            public SpinnerEditor() {
            }

            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                    final int row, final int column) {
                spinner.removeChangeListener(changeListener);
                spinner.setValue(value);

                changeListener = new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        _javaFunction._variableBusWidth.setBusWidth(row, (Integer) spinner.getValue());
                        ((AbstractTableModel) jTable1.getModel()).fireTableCellUpdated(row, column);
                    }
                };

                spinner.addChangeListener(changeListener);
                return spinner;
            }

            public boolean isCellEditable(EventObject evt) {
                if (evt instanceof MouseEvent) {
                    return ((MouseEvent) evt).getClickCount() >= 2;
                }
                return true;
            }

            public Object getCellEditorValue() {
                return spinner.getValue();
            }
        }

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();

        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        jTable1.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        jTable1.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        jTable1.getColumnModel().getColumn(0).setPreferredWidth(10);
        jTable1.getColumnModel().getColumn(2).setPreferredWidth(15);
        jTable1.getColumnModel().getColumn(2).setCellEditor(new SpinnerEditor());

    }

    private class OutputUpdate implements Runnable {

        @Override
        public void run() {

            do {
                if (jTabbedPane.getSelectedIndex() == OUTPUT_TAB_NO && jTextAreaOutput.isEnabled()
                        && _outputStringBuffer.length() != _lastOutputSize) {
                    setOutputText();
                }

                try {
                    Thread.sleep(THREAD_SLEEP_MILLIS);
                } catch (InterruptedException ex) {
                    Logger.getLogger(CodeWindow.class.getName()).log(Level.SEVERE, null, ex);
                }
            } while (isVisible());

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

        jFrame1 = new javax.swing.JFrame();
        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        jButtonCompile = new javax.swing.JButton();
        jButtonCloseWindow = new javax.swing.JButton();
        jTabbedPane = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        jSplitPane3 = new javax.swing.JSplitPane();
        jSplitPane2 = new javax.swing.JSplitPane();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanelImports = new javax.swing.JPanel();
        jPanelStatic = new javax.swing.JPanel();
        jPanelStaticInit = new javax.swing.JPanel();
        jPanelCode = new javax.swing.JPanel();
        jPanelCompilerMessages = new javax.swing.JPanel();
        jSplitPane4 = new javax.swing.JSplitPane();
        jPanelCompilerSource = new javax.swing.JPanel();
        jPanelCompilerOutput = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButtonExample2 = new javax.swing.JButton();
        jButtonExample1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaOutput = new javax.swing.JTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        jRadioButtonConsole = new javax.swing.JRadioButton();
        jRadioButtonWindow = new javax.swing.JRadioButton();
        jCheckBoxClear = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jButtonExternalFiles = new javax.swing.JButton();
        jCheckBoxMatrix = new javax.swing.JCheckBox();
        jRadioButtonFixedBus = new javax.swing.JRadioButton();
        jRadioButtonVariableBus = new javax.swing.JRadioButton();
        jSpinnerFixedOutputBus = new javax.swing.JSpinner();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jCheckBoxShowName = new javax.swing.JCheckBox();
        jTextFieldName = new javax.swing.JTextField();
        jButtonCancel = new javax.swing.JButton();

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

        setTitle("Java Custom Code Control Block");
        setLocationByPlatform(true);
        setMinimumSize(new java.awt.Dimension(800, 600));

        jButtonCompile.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jButtonCompile.setText("Compile Code");
        jButtonCompile.setToolTipText("Compile changes, keep code window visible");
        jButtonCompile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCompileActionPerformed(evt);
            }
        });

        jButtonCloseWindow.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jButtonCloseWindow.setText("Close Window");
        jButtonCloseWindow.setToolTipText("Compile and close window");
        jButtonCloseWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCloseWindowActionPerformed(evt);
            }
        });

        jTabbedPane.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        jTabbedPane.setMinimumSize(new java.awt.Dimension(15, 21));

        jPanel3.setPreferredSize(new java.awt.Dimension(887, 329));

        jSplitPane3.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jSplitPane3.setDividerLocation(200);
        jSplitPane3.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane3.setResizeWeight(0.3);

        jSplitPane2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jSplitPane2.setDividerLocation(450);
        jSplitPane2.setResizeWeight(0.7);

        jSplitPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jSplitPane1.setDividerLocation(100);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(0.5);

        jPanelImports.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1), "Imports", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 10))); // NOI18N
        jPanelImports.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jPanelImports.setLayout(new java.awt.BorderLayout());
        jSplitPane1.setTopComponent(jPanelImports);

        jPanelStatic.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1), "Variable definitions", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 10))); // NOI18N
        jPanelStatic.setLayout(new java.awt.BorderLayout());
        jSplitPane1.setRightComponent(jPanelStatic);

        jSplitPane2.setLeftComponent(jSplitPane1);

        jPanelStaticInit.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1), "Initialization code (executed at simulation start)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 10))); // NOI18N
        jPanelStaticInit.setLayout(new java.awt.BorderLayout());
        jSplitPane2.setRightComponent(jPanelStaticInit);

        jSplitPane3.setTopComponent(jSplitPane2);

        jPanelCode.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1), "public double[] calculateYOUT(double[] xIN, double time, double dt) {", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 10))); // NOI18N
        jPanelCode.setLayout(new java.awt.BorderLayout());
        jSplitPane3.setRightComponent(jPanelCode);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 546, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
        );

        jTabbedPane.addTab("Code", jPanel3);

        jSplitPane4.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jSplitPane4.setDividerLocation(300);
        jSplitPane4.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jPanelCompilerSource.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1), "Generated source code", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 10))); // NOI18N
        jPanelCompilerSource.setLayout(new java.awt.BorderLayout());
        jSplitPane4.setTopComponent(jPanelCompilerSource);

        jPanelCompilerOutput.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1), "Compiler messages", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 10))); // NOI18N
        jPanelCompilerOutput.setLayout(new java.awt.BorderLayout());
        jSplitPane4.setRightComponent(jPanelCompilerOutput);

        javax.swing.GroupLayout jPanelCompilerMessagesLayout = new javax.swing.GroupLayout(jPanelCompilerMessages);
        jPanelCompilerMessages.setLayout(jPanelCompilerMessagesLayout);
        jPanelCompilerMessagesLayout.setHorizontalGroup(
            jPanelCompilerMessagesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 546, Short.MAX_VALUE)
        );
        jPanelCompilerMessagesLayout.setVerticalGroup(
            jPanelCompilerMessagesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
        );

        jTabbedPane.addTab("Compiler Messages", jPanelCompilerMessages);

        jTextArea1.setEditable(false);
        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jTextArea1.setRows(5);
        jTextArea1.setText("The 'Java Code' control block contains a freely programmable interface which allows an easy and\nefficient implementation of complex control structures. Furthermore, the complete Java API is\navailable and makes GeckoCIRCUITS arbitrarily extendable.\n\n-----Technical details -----\n\nThe interface function:\n\npublic double[] calculateYOUT(final double[] xIN, final double time) { \n       // your custom code\n       //return yOUT;\n}\n\nis called at every simulation timestep. Input arguments are the block input ports (as a double\\n \narray xIN) and the current simulation time. The return value is a predefined double array yOUT.\nIt is recommended not to use the 'new' operator within this function, since it would allocate\nmemory at every simulation step. Therefore, variables should be defined in the 'variable definitions'\ntextfield, and initialized immediately, or initialize within the 'Initialization Code' textfield.\n\nA code example is supplied (button 'Load example 1') which shows a simple assignment of\noutput values. Example 2 displays the actual simulation time inside generated window. \n\nAfter a successful code compilation, the Java Code block appears in green color, otherwise an\nerror is indicated by a yellow Java-Block color.\n\nFor further documentation, refer to the various Java Tutorials or books.");
        jScrollPane2.setViewportView(jTextArea1);

        jButtonExample2.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jButtonExample2.setText("Example 2");
        jButtonExample2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExample2ActionPerformed(evt);
            }
        });

        jButtonExample1.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jButtonExample1.setText("Example 1");
        jButtonExample1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExample1ActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel1.setText("Load example code (warning: your code will be overwritten!):");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonExample1, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButtonExample2)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButtonExample1, jButtonExample2});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 492, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonExample2)
                    .addComponent(jButtonExample1)
                    .addComponent(jLabel1))
                .addContainerGap())
        );

        jTabbedPane.addTab("Info", jPanel4);

        jTextAreaOutput.setColumns(20);
        jTextAreaOutput.setEditable(false);
        jTextAreaOutput.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jTextAreaOutput.setRows(5);
        jScrollPane1.setViewportView(jTextAreaOutput);

        jTextArea3.setColumns(20);
        jTextArea3.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jTextArea3.setRows(5);
        jTextArea3.setText("Display messages from the Java code window: For debugging purposes, it is useful to print messages from the \nprogram execution. The textfield below prints these outputs. Example: The code line \n\t\tSystem.out.println(\"value 1: \" + xIN[0]);\nprints the value of the first input channel of the Java code block. Your Java sourcecode can, furthermore, throw \nexceptions, e.g. when checking your varialbes for \"nan\" (not-a-number) values.\nBy default, the output messages are de-activated, and the output text is written to your Console (standard\noutput).                    \nWarning: when to many outputs lines are written, GeckoCIRCUITS will show a warning message, since the simulation\nspeed will suffer when your custom java code is too verbose.");
        jScrollPane3.setViewportView(jTextArea3);

        buttonGroup1.add(jRadioButtonConsole);
        jRadioButtonConsole.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonConsole.setText("Use console output");
        jRadioButtonConsole.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonConsoleActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRadioButtonWindow);
        jRadioButtonWindow.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonWindow.setSelected(true);
        jRadioButtonWindow.setText("Use Java-block textfield output");
        jRadioButtonWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonWindowActionPerformed(evt);
            }
        });

        jCheckBoxClear.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jCheckBoxClear.setText("Clear Messages on simulation restart");
        jCheckBoxClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxClearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane3)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jRadioButtonConsole)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButtonWindow)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jCheckBoxClear)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButtonConsole)
                    .addComponent(jRadioButtonWindow)
                    .addComponent(jCheckBoxClear))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 358, Short.MAX_VALUE))
        );

        jTabbedPane.addTab("Output messages", jPanel2);

        jLabel2.setText("Add additional Java code source file (external):");

        jButtonExternalFiles.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jButtonExternalFiles.setText("Additional Sources");
        jButtonExternalFiles.setToolTipText("Specify external .java sourcecode files");
        jButtonExternalFiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExternalFilesActionPerformed(evt);
            }
        });

        jCheckBoxMatrix.setText("Use matrix input/outputs for sourcecode");
        jCheckBoxMatrix.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMatrixActionPerformed(evt);
            }
        });

        buttonGroup2.add(jRadioButtonFixedBus);
        jRadioButtonFixedBus.setSelected(true);
        jRadioButtonFixedBus.setText("Fixed output signal bus width");
        jRadioButtonFixedBus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonFixedBusActionPerformed(evt);
            }
        });

        buttonGroup2.add(jRadioButtonVariableBus);
        jRadioButtonVariableBus.setText("Variable output signal bus width");

        jSpinnerFixedOutputBus.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));

        jScrollPane5.setBorder(null);

        jTable1.setBorder(null);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Index", "Label", "Bus width"
            }
        ));
        jScrollPane5.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(43, 43, 43)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jRadioButtonFixedBus)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSpinnerFixedOutputBus, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jRadioButtonVariableBus)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 263, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonExternalFiles))
                    .addComponent(jCheckBoxMatrix))
                .addContainerGap(125, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jButtonExternalFiles))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxMatrix)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButtonFixedBus)
                    .addComponent(jSpinnerFixedOutputBus, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButtonVariableBus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 358, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(75, 75, 75))
        );

        jTabbedPane.addTab("Advanced", jPanel1);

        jCheckBoxShowName.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jCheckBoxShowName.setText("Display name");
        jCheckBoxShowName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxShowNameActionPerformed(evt);
            }
        });

        jTextFieldName.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jTextFieldName.setText("jTextField1");
        jTextFieldName.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jTextFieldNamePropertyChange(evt);
            }
        });

        jButtonCancel.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jButtonCancel.setText("Cancel");
        jButtonCancel.setToolTipText("Close window without applying changes since last compilation");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jCheckBoxShowName, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldName, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButtonCompile)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonCloseWindow)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 562, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonCloseWindow)
                    .addComponent(jButtonCompile)
                    .addComponent(jTextFieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxShowName, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonCancel)))
        );

        jTabbedPane.getAccessibleContext().setAccessibleName("");

        pack();
    }// </editor-fold>//GEN-END:initComponents
    //CHECKSTYLE:ON

private void jButtonCloseWindowActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonCloseWindowActionPerformed
    loadCodeIntoRegler();
    this.dispose();
}//GEN-LAST:event_jButtonCloseWindowActionPerformed

private void jButtonExample2ActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonExample2ActionPerformed

    _codeTextArea.setText(
            "\n//out.println(xIN[0] + \" \" + xIN[1] + \" \" + xIN[2] + \" \" + xIN.length );"
            + "\n\nfor(int i = 0; i < yOUT.length; i++) { "
            + "\n\tyOUT[i] = (i+2.0) * sin(time*1000.0);"
            + "\n}"
            + "\nlabel.setText(\"    Simulation time: \" + time );"
            + "\nreturn yOUT;");
    _importsTextArea.setText(
            "import javax.swing.JFrame;"
            + "\nimport javax.swing.JLabel;"
            + "\nimport static java.lang.System.out;"
            + "\nimport static java.lang.Math.*;");
    _initTextArea.setText("// object not yet created -> create and initialize\n"
            + "if(displayWindow == null) {\n"
            + "\tdisplayWindow = new JFrame();\n"
            + "\tdisplayWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);\n"
            + "\tdisplayWindow.getContentPane().add(label);\n"
            + "\tdisplayWindow.pack();\n"
            + "\tdisplayWindow.setSize(400,200);\n"
            + "\tdisplayWindow.setTitle(\"Simulation time output example\");\n"
            + "\tdisplayWindow.setLocationRelativeTo(null);"
            + "\n}\n"
            + "// if user made window invisible, we enable it for the next simulation run.\n"
            + "if(displayWindow.isVisible() == false) {\n"
            + "\tdisplayWindow.setVisible(true);\n"
            + "}");
    _variablesTextArea.setText(
            // declaration of required GUI elements:
            "private final JLabel label = new JLabel(\"Show Simulation Time\");\n"
            + "private JFrame displayWindow;");

    jTabbedPane.setSelectedIndex(0);

}//GEN-LAST:event_jButtonExample2ActionPerformed

private void jButtonExample1ActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonExample1ActionPerformed

    _codeTextArea.setText(
            "\nyOUT[0]= Math.sqrt(Math.abs(xIN[0]*xIN[1])); "
            + "\nyOUT[1]= xIN[0]*xIN[1] +4*xIN[2] -yOUT[0]; "
            + "\n"
            + "\nreturn yOUT;");
    _importsTextArea.setText("");
    _initTextArea.setText("");
    _variablesTextArea.setText("");
    jTabbedPane.setSelectedIndex(0);
}//GEN-LAST:event_jButtonExample1ActionPerformed

private void jButtonAutoIndentActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonAutoIndentActionPerformed
}//GEN-LAST:event_jButtonAutoIndentActionPerformed

private void jCheckBoxShowNameActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jCheckBoxShowNameActionPerformed
    _javaFunction.setNameVisible(jCheckBoxShowName.isSelected());
}//GEN-LAST:event_jCheckBoxShowNameActionPerformed

private void jTextFieldNamePropertyChange(java.beans.PropertyChangeEvent evt) {//NOPMD//GEN-FIRST:event_jTextFieldNamePropertyChange
    if (_javaFunction != null) {
        try {
            _javaFunction.setNewNameCheckedUndoable(jTextFieldName.getText());
        } catch (NameAlreadyExistsException ex) {
            Logger.getLogger(CodeWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}//GEN-LAST:event_jTextFieldNamePropertyChange

    private void jButtonCompileActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonCompileActionPerformed
        jTabbedPane.setSelectedIndex(1);
        loadCodeIntoRegler();
    }//GEN-LAST:event_jButtonCompileActionPerformed

    private void jRadioButtonWindowActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadioButtonWindowActionPerformed
        toggleOutputDirection();
    }//GEN-LAST:event_jRadioButtonWindowActionPerformed

    private void jRadioButtonConsoleActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadioButtonConsoleActionPerformed
        toggleOutputDirection();
    }//GEN-LAST:event_jRadioButtonConsoleActionPerformed

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        this.dispose();
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jCheckBoxClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxClearActionPerformed
        _javaFunction.setClearOutput(jCheckBoxClear.isSelected());
    }//GEN-LAST:event_jCheckBoxClearActionPerformed

    private void jCheckBoxMatrixActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMatrixActionPerformed
        _variableBusWidth._useMatrix.setUserValue(jCheckBoxMatrix.isSelected());
        setMatrixCodeWindow(jCheckBoxMatrix.isSelected());
    }//GEN-LAST:event_jCheckBoxMatrixActionPerformed

    private void jButtonExternalFilesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExternalFilesActionPerformed
        if (!_extWindowInit) {
            _extSourceWindow.addNewFiles(_javaFunction.getFiles());
            _extWindowInit = true;
        }
        _extSourceWindow.setVisible(true);
    }//GEN-LAST:event_jButtonExternalFilesActionPerformed

    private void jRadioButtonFixedBusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonFixedBusActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButtonFixedBusActionPerformed

    private void loadCodeIntoRegler() {
        final JavaBlockSource newSource = new JavaBlockSource.Builder().sourceCode(_codeTextArea.getText()).
                importsCode(_importsTextArea.getText()).initCode(_initTextArea.getText()).
                variablesCode(_variablesTextArea.getText()).
                build();

        final AbstractJavaBlock javaBlock = getCurrentJavaBlock();
        javaBlock.compileNewBlockSource(newSource);

        _compiledSourceView.setText(javaBlock.getCompilerSource());

        String compilerMessages = javaBlock.getCompilerMessage();

        if (javaBlock.getCompileStatus() == CompileStatus.COMPILE_ERROR) {
            compilerMessages = checkForOldCompiler(javaBlock.getCompilerMessage());
        }

        _compilerMessages.setText(compilerMessages);
    }

    public void addNewExtraFiles(final List<GeckoFile> newFiles) {
        _extSourceWindow.addNewFiles(newFiles);
    }
    //CHECKSTYLE:OFF
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonCloseWindow;
    private javax.swing.JButton jButtonCompile;
    private javax.swing.JButton jButtonExample1;
    private javax.swing.JButton jButtonExample2;
    private javax.swing.JButton jButtonExternalFiles;
    private javax.swing.JCheckBox jCheckBoxClear;
    private javax.swing.JCheckBox jCheckBoxMatrix;
    private javax.swing.JCheckBox jCheckBoxShowName;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanelCode;
    private javax.swing.JPanel jPanelCompilerMessages;
    private javax.swing.JPanel jPanelCompilerOutput;
    private javax.swing.JPanel jPanelCompilerSource;
    private javax.swing.JPanel jPanelImports;
    private javax.swing.JPanel jPanelStatic;
    private javax.swing.JPanel jPanelStaticInit;
    private javax.swing.JRadioButton jRadioButtonConsole;
    private javax.swing.JRadioButton jRadioButtonFixedBus;
    private javax.swing.JRadioButton jRadioButtonVariableBus;
    private javax.swing.JRadioButton jRadioButtonWindow;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSpinner jSpinnerFixedOutputBus;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JSplitPane jSplitPane3;
    private javax.swing.JSplitPane jSplitPane4;
    private javax.swing.JTabbedPane jTabbedPane;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea3;
    javax.swing.JTextArea jTextAreaOutput;
    private javax.swing.JTextField jTextFieldName;
    // End of variables declaration//GEN-END:variables
    //CHECKSTYLE:ON

    private void toggleOutputDirection() {
        final boolean isEnabled = jRadioButtonWindow.isSelected();
        if (isEnabled) {
            jTextAreaOutput.setText("\n\n\t\tActive - Waiting for text output.");
            jCheckBoxClear.setEnabled(true);
        } else {
            jTextAreaOutput.setText("\n\n\t\tDisabled - Output is sent to Console.");
            jCheckBoxClear.setEnabled(false);
        }
        jTextAreaOutput.setEnabled(isEnabled);
        _javaFunction.setConsoleOutput(!isEnabled);
    }
}

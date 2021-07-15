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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFileChooser;
import ch.technokrat.gecko.geckocircuits.allg.GetJarPath;
import ch.technokrat.gecko.geckocircuits.control.ReglerSaveData.OutputType;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataIndexItem;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataTableFrame;
import ch.technokrat.gecko.geckocircuits.datacontainer.HeaderSymbol;
import ch.technokrat.gecko.geckocircuits.datacontainer.TextSeparator;
import java.io.File;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 *
 * @author andreas
 */
public final class DialogDataExport extends javax.swing.JDialog {

    private boolean _txtFormat;
    private final ReportingListTransferHandler _arrayListHandler = new ReportingListTransferHandler();
    private final List<AbstractDataContainer> _containers;
    private DefaultListModel _selectedModel;
    private final boolean _initDone;
    private DataSaver _dataSaver;
    public final ReglerSaveData _reglerDataSave;
    private boolean _inFillLists;
    
    public DialogDataExport(final java.awt.Frame parent, final boolean modal, final ReglerSaveData dataSavable,
            final List<AbstractDataContainer> selectContainers, final DataSaver _parentDataSaver) {
        super(parent, modal);
        _containers = selectContainers;
        _reglerDataSave = dataSavable;
        init();

        jSpinnerDigits.setValue(_reglerDataSave._significDigits.getValue());
        _dataSaver = _parentDataSaver;

        jTextArea1.setText(Fenster.aktuellerDateiName.replace(".ipes", "CISPR.txt"));

        if (_containers.size() == 1) {
            jLabelFilter.setVisible(false);
            jComboBoxFilter.setVisible(false);

            fillLists();

        }

        boolean allNotInit = true;
        for (AbstractDataContainer cont : selectContainers) {
            if (cont.getContainerStatus() != ContainerStatus.NOT_INITIALIZED) {
                allNotInit = false;
            }
        }

        if (allNotInit) {
            jButtonDoSave.setEnabled(false);
            jButtonDoSave.setText("No data available");
        }

        jSpinnerSkip.setValue(_reglerDataSave._skipDataPoints.getValue());
        jCheckPrintHdr.setSelected(_reglerDataSave._printHeader.getValue());
        jCheckBoxTranspose.setSelected(_reglerDataSave._transposeData.getValue());
        disableContSave();
        _initDone = true;
    }

    private void fillLists() {
        _inFillLists = true;
        try { // somewhere, I have a race condition when a signal is deleted... 
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(DialogDataExport.class.getName()).log(Level.SEVERE, null, ex);
        }
        final AbstractDataContainer container = _containers.get(0);
        if (container == null) {
            return;
        }
        
        final List<String> selectedStrings = _reglerDataSave.getSelectedNames();
        final int rowLength = container.getRowLength();
        ((DefaultListModel) jListAvailable.getModel()).clear();
        for (int i = 0; i < rowLength; i++) {
            final DataIndexItem listItem = new DataIndexItem(i, container.getSignalName(i));
            if (!selectedStrings.contains(listItem.toString())) {
                ((DefaultListModel) jListAvailable.getModel()).addElement(listItem);
            }

        }

        // here, it is important that we keep the order of the selected elements!
        ((DefaultListModel) jListSelected.getModel()).clear();
        for (int i = 0; i < selectedStrings.size(); i++) {
            final DataIndexItem listItem = new DataIndexItem(i, selectedStrings.get(i));
            if (selectedStrings.contains(listItem.toString())) {
                ((DefaultListModel) jListSelected.getModel()).addElement(listItem);
            }

        }        
                
        _inFillLists = false;
    }

    private void setSaveModus() {
        if (jRadButtManSave.isSelected()) {
            _reglerDataSave._saveModus = ReglerSaveData.SaveModus.MANUAL;
        }

        if (jRadButtEndSave.isSelected()) {
            _reglerDataSave._saveModus = ReglerSaveData.SaveModus.SIMULATION_END;
        }

        if (jRadButtContinSav.isSelected()) {
            _reglerDataSave._saveModus = ReglerSaveData.SaveModus.DURING_SIMULATION;
        }
    }

    private void init() {
        initComponents();

        if (_reglerDataSave._printHeader.getValue()) {
            jCheckPrintHdr.setSelected(true);
        } else {
            jCheckPrintHdr.setSelected(false);
        }


        for (HeaderSymbol symb : HeaderSymbol.values()) {
            jComboHeaderSym.addItem(symb);
            if (symb == _reglerDataSave._headerSymbol) {
                jComboHeaderSym.setSelectedItem(symb);
            }
        }

        for (TextSeparator sep : TextSeparator.values()) {
            jComboSeparator.addItem(sep);
            if (sep == _reglerDataSave._itemSeparator) {
                jComboSeparator.setSelectedItem(sep);
            }
        }

        switch (_reglerDataSave._saveModus) {
            case MANUAL:
                jRadButtManSave.setSelected(true);
                break;
            case SIMULATION_END:
                jRadButtEndSave.setSelected(true);
                break;
            case DURING_SIMULATION:
                jRadButtContinSav.setSelected(true);
                break;
            default:
                assert false;
        }

        switch (_reglerDataSave._fileOverwrite) {
            case OVERWRITE:
                jRadButtOverwrite.setSelected(true);
                break;
            case DO_NUMBERING:
                jRadButtDoNumber.setSelected(true);
                break;
            default:
                assert false;
        }


        this.getRootPane().setDefaultButton(jButtonDoSave);
        jListAvailable.setModel(new DefaultListModel());
        _selectedModel = new DefaultListModel();
        jListSelected.setModel(_selectedModel);
        jListAvailable.setTransferHandler(_arrayListHandler);
        jListSelected.setTransferHandler(_arrayListHandler);
        jTxtFildFileName.setText(_reglerDataSave._file.getValue());

        _selectedModel.addListDataListener(new ListDataListener() {

            @Override
            public void intervalAdded(final ListDataEvent evt) {
                removedOrAdded();
            }

            @Override
            public void intervalRemoved(final ListDataEvent evt) {
                removedOrAdded();
            }

            @Override
            public void contentsChanged(final ListDataEvent evt) {
                // nothing todo here!
            }

            void removedOrAdded() {
                if (_initDone && !_inFillLists) {
                    DataIndexItem[] listItems = new DataIndexItem[jListSelected.getModel().getSize()];
                    for (int i = 0; i < listItems.length; i++) {
                        listItems[i] = (DataIndexItem) (jListSelected.getModel().getElementAt(i));
                    }
                    _reglerDataSave.setSelectedSignals(listItems);
                }
            }
        });

        switchBinaryTxt();

    }

    private void switchOverwrite() {
        if (jRadButtDoNumber.isSelected()) {
            _reglerDataSave._fileOverwrite = ReglerSaveData.FileOverwrite.DO_NUMBERING;
        } else {
            _reglerDataSave._fileOverwrite = ReglerSaveData.FileOverwrite.OVERWRITE;
        }
    }

    private void switchBinaryTxt() {
        if (jRadioBinary.isSelected()) {
            _txtFormat = false;
            _reglerDataSave._outputType = OutputType.BINARY;
        } else {
            _txtFormat = true;
            _reglerDataSave._outputType = OutputType.TEXT;
        }

        jSpinnerDigits.setEnabled(_txtFormat);
        jComboSeparator.setEnabled(_txtFormat);
        jComboHeaderSym.setEnabled(_txtFormat);

    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        _buttonGroup1 = new javax.swing.ButtonGroup();
        _buttonGroup2 = new javax.swing.ButtonGroup();
        _buttonGroup3 = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jLabelFilter = new javax.swing.JLabel();
        jComboBoxFilter = new javax.swing.JComboBox();
        jScrollPane2 = new javax.swing.JScrollPane();
        jListAvailable = new javax.swing.JList();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jListSelected = new javax.swing.JList();
        jLabel7 = new javax.swing.JLabel();
        jButtonTable = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jRadButtOverwrite = new javax.swing.JRadioButton();
        jRadButtDoNumber = new javax.swing.JRadioButton();
        jRadButtManSave = new javax.swing.JRadioButton();
        jRadButtEndSave = new javax.swing.JRadioButton();
        jRadButtContinSav = new javax.swing.JRadioButton();
        jSpinnerSkip = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        jCheckBoxTranspose = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jComboSeparator = new javax.swing.JComboBox();
        jCheckPrintHdr = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jComboHeaderSym = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jSpinnerDigits = new javax.swing.JSpinner();
        jRadioBinary = new javax.swing.JRadioButton();
        jRadioText = new javax.swing.JRadioButton();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButtonClose = new javax.swing.JButton();
        jButtonDoSave = new javax.swing.JButton();
        jTxtFildFileName = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jButtonAbort = new javax.swing.JButton();
        jButtonFileChooser = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Data export");
        setLocationByPlatform(true);
        setResizable(false);

        jTabbedPane1.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N

        jPanel2.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N

        jLabelFilter.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelFilter.setText("Selection filter:");

        jComboBoxFilter.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jComboBoxFilter.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBoxFilter.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jComboBoxFilterPropertyChange(evt);
            }
        });

        jListAvailable.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jListAvailable.setDragEnabled(true);
        jScrollPane2.setViewportView(jListAvailable);

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Available");

        jListSelected.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jListSelected.setDragEnabled(true);
        jScrollPane3.setViewportView(jListSelected);

        jLabel7.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Selection");

        jButtonTable.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonTable.setText("Show data table");
        jButtonTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonTableActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jButtonTable)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 34, Short.MAX_VALUE)
                        .addComponent(jLabelFilter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabelFilter)
                        .addComponent(jComboBoxFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButtonTable))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 243, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 243, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Data selection", jPanel2);

        _buttonGroup3.add(jRadButtOverwrite);
        jRadButtOverwrite.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadButtOverwrite.setSelected(true);
        jRadButtOverwrite.setText("Overwrite existing data");
        jRadButtOverwrite.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadButtOverwriteActionPerformed(evt);
            }
        });

        _buttonGroup3.add(jRadButtDoNumber);
        jRadButtDoNumber.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadButtDoNumber.setText("Automatic numbering of file names");
        jRadButtDoNumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadButtDoNumberActionPerformed(evt);
            }
        });

        _buttonGroup2.add(jRadButtManSave);
        jRadButtManSave.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadButtManSave.setSelected(true);
        jRadButtManSave.setText("Save manually");
        jRadButtManSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadButtManSaveActionPerformed(evt);
            }
        });

        _buttonGroup2.add(jRadButtEndSave);
        jRadButtEndSave.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadButtEndSave.setText("Save automatically at simulation end");
        jRadButtEndSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadButtEndSaveActionPerformed(evt);
            }
        });

        _buttonGroup2.add(jRadButtContinSav);
        jRadButtContinSav.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadButtContinSav.setText("Save continuously during simulation");
        jRadButtContinSav.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadButtContinSavActionPerformed(evt);
            }
        });

        jSpinnerSkip.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jSpinnerSkip.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));
        jSpinnerSkip.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerSkipStateChanged(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel5.setText("Skip data points:");

        jCheckBoxTranspose.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jCheckBoxTranspose.setText("Transpose data table (rows first)");
        jCheckBoxTranspose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxTransposeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jRadButtManSave)
                    .addComponent(jRadButtEndSave)
                    .addComponent(jRadButtContinSav)
                    .addComponent(jRadButtOverwrite)
                    .addComponent(jRadButtDoNumber)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(34, 34, 34)
                        .addComponent(jSpinnerSkip, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jCheckBoxTranspose))
                .addContainerGap(158, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap(32, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinnerSkip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addGap(18, 18, 18)
                .addComponent(jRadButtManSave)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadButtEndSave)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadButtContinSav)
                .addGap(7, 7, 7)
                .addComponent(jCheckBoxTranspose)
                .addGap(18, 18, 18)
                .addComponent(jRadButtOverwrite)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadButtDoNumber)
                .addGap(59, 59, 59))
        );

        jTabbedPane1.addTab("Options", jPanel3);

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel2.setText("Item separator:");

        jComboSeparator.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jComboSeparator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboSeparatorActionPerformed(evt);
            }
        });

        jCheckPrintHdr.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jCheckPrintHdr.setSelected(true);
        jCheckPrintHdr.setText("Print header line including signal names");
        jCheckPrintHdr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckPrintHdrActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel3.setText("Header symbol:");

        jComboHeaderSym.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jComboHeaderSym.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboHeaderSymActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel4.setText("Significant digits of data:");

        jSpinnerDigits.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jSpinnerDigits.setModel(new javax.swing.SpinnerNumberModel(4, 1, 6, 1));
        jSpinnerDigits.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerDigitsStateChanged(evt);
            }
        });

        _buttonGroup1.add(jRadioBinary);
        jRadioBinary.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioBinary.setText("Binary format");
        jRadioBinary.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioBinaryActionPerformed(evt);
            }
        });

        _buttonGroup1.add(jRadioText);
        jRadioText.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioText.setSelected(true);
        jRadioText.setText("Text format");
        jRadioText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioTextActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jRadioBinary)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioText))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel4)
                            .addGap(18, 18, 18)
                            .addComponent(jSpinnerDigits, javax.swing.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel3)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jComboHeaderSym, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jComboSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(jCheckPrintHdr, javax.swing.GroupLayout.Alignment.LEADING)))
                .addContainerGap(132, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jComboHeaderSym, jComboSeparator, jSpinnerDigits});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioBinary)
                    .addComponent(jRadioText))
                .addGap(31, 31, 31)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jComboHeaderSym, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinnerDigits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckPrintHdr)
                .addContainerGap(127, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jComboHeaderSym, jComboSeparator, jSpinnerDigits});

        jTabbedPane1.addTab("Output format", jPanel1);

        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jTextArea1.setRows(5);
        jTextArea1.setText("Please be aware that saving simulation data can produce large\nfiles (several GB!) on your harddisc. To reduce the amount of \ndata, you can lower the number of significant digits, skip data \npoints and select only the signals that you are interested in.\n\nThe independent variable (time or frequency) will always be\nsaved as double precision number, since a high resolution is\ntypically required, here.\n\nSaving the data as binary file is another possibility to reduce the\namount of storage needed on your harddisc. In binary mode, \nno header is written, but just the raw data. When reading the\nbinary data files, you have to consider the following:\n   - The independent variable is saved as double precision \n      value (8 bytes).\n   - Signal values are saved as single precisioin values (4 bytes)\n    - there is no linebreak-character in binary data, therefore\n      you have to know the number of signals (independent \n      variable + number of selected signals) to re-read the data \n      in separate program, as e.g. Matlab.");
        jScrollPane1.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Info", jPanel4);

        jButtonClose.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonClose.setText("Close");
        jButtonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCloseActionPerformed(evt);
            }
        });

        jButtonDoSave.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonDoSave.setText("Save data to file");
        jButtonDoSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveData(evt);
            }
        });

        jTxtFildFileName.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jTxtFildFileName.setText("/home/andreas/testFile.txt");
        jTxtFildFileName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTxtFildFileNameKeyReleased(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel1.setText("File name:");

        jProgressBar1.setStringPainted(true);

        jButtonAbort.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonAbort.setText("Abort Save");
        jButtonAbort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAbortActionPerformed(evt);
            }
        });

        jButtonFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButtonDoSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonClose, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonAbort, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jTxtFildFileName, javax.swing.GroupLayout.PREFERRED_SIZE, 298, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 351, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonFileChooser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTxtFildFileName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonClose)
                    .addComponent(jButtonDoSave)
                    .addComponent(jButtonAbort))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonSaveData(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonSaveData
        
        _dataSaver = new DataSaver(_containers.get(0), _reglerDataSave);

        _dataSaver.addObserver(new Observer() {

            @Override
            public void update(Observable o, Object arg) {
                jProgressBar1.setValue(_dataSaver.getPercentage());
            }
        });
        _dataSaver.doManualSave();
        fillLists();
    }//GEN-LAST:event_jButtonSaveData

    private void jRadioBinaryActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadioBinaryActionPerformed
        switchBinaryTxt();
    }//GEN-LAST:event_jRadioBinaryActionPerformed

    private void jRadioTextActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadioTextActionPerformed
        switchBinaryTxt();
    }//GEN-LAST:event_jRadioTextActionPerformed

    private void jButtonCloseActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonCloseActionPerformed
        dispose();
    }//GEN-LAST:event_jButtonCloseActionPerformed

    private void jComboBoxFilterPropertyChange(java.beans.PropertyChangeEvent evt) {//NOPMD//GEN-FIRST:event_jComboBoxFilterPropertyChange
    }//GEN-LAST:event_jComboBoxFilterPropertyChange

    private void jButtonAbortActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonAbortActionPerformed
        if (_dataSaver != null) {
            _dataSaver.abortSave();
        }
    }//GEN-LAST:event_jButtonAbortActionPerformed

    private void jTxtFildFileNameKeyReleased(java.awt.event.KeyEvent evt) {//NOPMD//GEN-FIRST:event_jTxtFildFileNameKeyReleased
        _reglerDataSave._file.setUserValue(jTxtFildFileName.getText());
    }//GEN-LAST:event_jTxtFildFileNameKeyReleased

    private void jCheckPrintHdrActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jCheckPrintHdrActionPerformed
        if (_initDone) {
            _reglerDataSave._printHeader.setUserValue(jCheckPrintHdr.isSelected());
        }
    }//GEN-LAST:event_jCheckPrintHdrActionPerformed

    private void jSpinnerDigitsStateChanged(javax.swing.event.ChangeEvent evt) {//NOPMD//GEN-FIRST:event_jSpinnerDigitsStateChanged
        if (_initDone) {
            _reglerDataSave._significDigits.setUserValue((Integer) jSpinnerDigits.getValue());
        }
    }//GEN-LAST:event_jSpinnerDigitsStateChanged

    private void jComboSeparatorActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jComboSeparatorActionPerformed
        if (_initDone) {
            _reglerDataSave._itemSeparator = (TextSeparator) jComboSeparator.getSelectedItem();
        }
    }//GEN-LAST:event_jComboSeparatorActionPerformed

    private void jComboHeaderSymActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jComboHeaderSymActionPerformed
        if (_initDone) {
            _reglerDataSave._headerSymbol = (HeaderSymbol) jComboHeaderSym.getSelectedItem();
        }
    }//GEN-LAST:event_jComboHeaderSymActionPerformed

    private void jRadButtManSaveActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadButtManSaveActionPerformed
        setSaveModus();
    }//GEN-LAST:event_jRadButtManSaveActionPerformed

    private void jRadButtEndSaveActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadButtEndSaveActionPerformed
        setSaveModus();
    }//GEN-LAST:event_jRadButtEndSaveActionPerformed

    private void jRadButtContinSavActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadButtContinSavActionPerformed
        setSaveModus();
    }//GEN-LAST:event_jRadButtContinSavActionPerformed

    private void jRadButtOverwriteActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadButtOverwriteActionPerformed
        switchOverwrite();
    }//GEN-LAST:event_jRadButtOverwriteActionPerformed

    private void jRadButtDoNumberActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadButtDoNumberActionPerformed
        switchOverwrite();
    }//GEN-LAST:event_jRadButtDoNumberActionPerformed

    private void jSpinnerSkipStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerSkipStateChanged
        if (_initDone) {
            _reglerDataSave._skipDataPoints.setUserValue((Integer) jSpinnerSkip.getValue());
        }
    }//GEN-LAST:event_jSpinnerSkipStateChanged

    private void jButtonTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonTableActionPerformed
        DataTableFrame dtf = new DataTableFrame(NetzlisteCONTROL.globalData, this);
        dtf.setVisible(true);
    }//GEN-LAST:event_jButtonTableActionPerformed

    private void jCheckBoxTransposeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxTransposeActionPerformed
        _reglerDataSave._transposeData.setUserValue(jCheckBoxTranspose.isSelected());
        disableContSave();

    }//GEN-LAST:event_jCheckBoxTransposeActionPerformed

    private void jButtonFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonFileChooserActionPerformed
                        
        GeckoFileChooser fileChooser = GeckoFileChooser.createSimpleSaveFileChooser(null, this);
        if (fileChooser.getUserResult() == GeckoFileChooser.FileChooserResult.CANCEL) {
            return;
        }
        String fileName = fileChooser.getFileWithCheckedEnding().getAbsolutePath();
        jTxtFildFileName.setText(fileName);
        _reglerDataSave._file.setUserValue(fileName);


    }//GEN-LAST:event_jButtonFileChooserActionPerformed

    private void disableContSave() {
        if (jCheckBoxTranspose.isSelected()) {
            if (jRadButtContinSav.isSelected()) {
                jRadButtEndSave.setSelected(true);
            }
            jRadButtContinSav.setEnabled(false);
        } else {
            jRadButtContinSav.setEnabled(true);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup _buttonGroup1;
    private javax.swing.ButtonGroup _buttonGroup2;
    private javax.swing.ButtonGroup _buttonGroup3;
    private javax.swing.JButton jButtonAbort;
    private javax.swing.JButton jButtonClose;
    private javax.swing.JButton jButtonDoSave;
    private javax.swing.JButton jButtonFileChooser;
    private javax.swing.JButton jButtonTable;
    private javax.swing.JCheckBox jCheckBoxTranspose;
    private javax.swing.JCheckBox jCheckPrintHdr;
    private javax.swing.JComboBox jComboBoxFilter;
    private javax.swing.JComboBox jComboHeaderSym;
    private javax.swing.JComboBox jComboSeparator;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabelFilter;
    private javax.swing.JList jListAvailable;
    private javax.swing.JList jListSelected;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JRadioButton jRadButtContinSav;
    private javax.swing.JRadioButton jRadButtDoNumber;
    private javax.swing.JRadioButton jRadButtEndSave;
    private javax.swing.JRadioButton jRadButtManSave;
    private javax.swing.JRadioButton jRadButtOverwrite;
    private javax.swing.JRadioButton jRadioBinary;
    private javax.swing.JRadioButton jRadioText;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSpinner jSpinnerDigits;
    private javax.swing.JSpinner jSpinnerSkip;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTxtFildFileName;
    // End of variables declaration//GEN-END:variables
}

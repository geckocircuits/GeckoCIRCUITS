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
 * DialogTransferFunction.java
 *
 * Created on 31.10.2011, 15:56:00
 */
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.circuit.Enabled;
import ch.technokrat.gecko.geckocircuits.circuit.NameAlreadyExistsException;
import ch.technokrat.gecko.geckocircuits.math.NComplex;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

@SuppressWarnings({"PMD.TooManyFields", "PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
public final class DialogTransferFunction extends javax.swing.JFrame {

    private final DefaultListModel _nomModel = new DefaultListModel();
    private final DefaultListModel _deNomModel = new DefaultListModel();
    private final ReglerTransferFunction _reglerTF;
    private boolean _inPolynomialMode = false;
    private final RegelBlock _elementControl;
    private final boolean _initDone;

    public DialogTransferFunction(final ReglerTransferFunction reglerTF, final RegelBlock element) {
        super();
        initComponents();
        this.setLocationRelativeTo(GeckoSim._win);
        jTextFieldName.setText(reglerTF.getStringID());
        _reglerTF = reglerTF;
        _elementControl = element;        
        
        jRadButtPoly.setSelected(_reglerTF._inPolynomMode.getValue());
        _inPolynomialMode = !_reglerTF._inPolynomMode.getValue(); //enforce a toggle at init
        toggleMode();
        _inPolynomialMode = _reglerTF._inPolynomMode.getValue();

        if(element._isEnabled.getValue() == Enabled.ENABLED) {
            jCheckBoxEnabled.setSelected(true);
        } else {
            jCheckBoxEnabled.setSelected(false);
        }
        
        jCheckBoxInitial.setSelected(reglerTF._useInitialState.getValue());
        
        
        jListNom.setModel(_nomModel);
        jListDenom.setModel(_deNomModel);
        
        addListeners();

        jTextFieldConst.setNumberToField(_reglerTF._constantFactor.getValue());


        updateTransferView();
        _initDone = true;
        this.setVisible(true);
    }
        

    private void readPolynomialCofficients() {
        _deNomModel.clear();
        _nomModel.clear();
        
        for (int i = 0; i < _reglerTF.getDenominatorSize(); i++) {
            final double real = _reglerTF.getDenominatorCoefficients(i);
            _deNomModel.addElement(new ComplexPrinter(new NComplex((float) real, 0f)));            
        }

        for (int i = 0; i < _reglerTF.getNumeratorSize(); i++) {
            final double real = _reglerTF.getNumeratorCoefficient(i);
            _nomModel.addElement(new ComplexPrinter(new NComplex((float) real, 0f)));
        }
    }

    private void readPoleZeroCoefficients() {
        _deNomModel.clear();
        _nomModel.clear();
        
        for (int i = 0; i < _reglerTF.getPoles().length; i += 2) {
            final double real = _reglerTF.getPoles()[i];
            final double imag = _reglerTF.getPoles()[i + 1];            
            if (real != 0 || imag != 0) {
                _deNomModel.addElement(new ComplexPrinter(new NComplex((float) real, (float) imag)));            
            }
        }

        for (int i = 0; i < _reglerTF.getZeros().length; i += 2) {
            final double real = _reglerTF.getZeros()[i];
            final double imag = _reglerTF.getZeros()[i + 1];
            if (real != 0 || imag != 0) {
                _nomModel.addElement(new ComplexPrinter(new NComplex((float) real, (float) imag)));
            }
        }
    }

    private void toggleMode() {
                
        if (_inPolynomialMode == jRadButtPoly.isSelected() || !_inPolynomialMode == jRadButtPoleMode.isSelected()) {            
            return; // do nothing, since the mode did not switch!
        }        

        if (jRadButtPoly.isSelected()) {
            _inPolynomialMode = true;
            _reglerTF._inPolynomMode.setUserValue(true);
            jLabelPoleImag.setVisible(false);
            jLabelZeroIm.setVisible(false);
            jTFInsertDeNumIm.setVisible(false);
            jTFInsertNumIm.setVisible(false);
            jTextFieldConst.setVisible(false);
            jLabelConst.setVisible(false);
            jTextFieldPoleZ.setVisible(false);
            ((TitledBorder) jPanelNumerator.getBorder()).setTitle("Numerator polynom Coefficients");
            ((TitledBorder) jPanelDenominator.getBorder()).setTitle("Denominator polynom Coefficients");
            
            readPolynomialCofficients();
            updateTransferView();
        } else {
            _inPolynomialMode = false;
            _reglerTF._inPolynomMode.setUserValue(false);
            jLabelPoleImag.setVisible(true);
            jLabelZeroIm.setVisible(true);
            jTFInsertDeNumIm.setVisible(true);
            jTFInsertNumIm.setVisible(true);
            jTextFieldConst.setVisible(true);
            jLabelConst.setVisible(true);
            jTextFieldPoleZ.setVisible(true);
            ((TitledBorder) jPanelNumerator.getBorder()).setTitle("Zeros");
            ((TitledBorder) jPanelDenominator.getBorder()).setTitle("Poles");
            readPoleZeroCoefficients();
            updateTransferView();
        }
    }

    private void updateReglerPolesZeros() {
        _reglerTF.clearPolesAndZeros();

        for (int i = 0; i < _deNomModel.getSize(); i++) {
            _reglerTF.setPole(((ComplexPrinter) _deNomModel.get(i))._value.getRe(), 2 * i);
            _reglerTF.setPole(((ComplexPrinter) _deNomModel.get(i))._value.getIm(), 2 * i + 1);
        }

        for (int i = 0; i < _nomModel.getSize(); i++) {
            _reglerTF.setZero(((ComplexPrinter) _nomModel.get(i))._value.getRe(), 2 * i);
            _reglerTF.setZero(((ComplexPrinter) _nomModel.get(i))._value.getIm(), 2 * i + 1);
        }
        
        
    }
    
    private void updateReglerPolynom() {
        assert _inPolynomialMode;
        final List<Double> numerator = new ArrayList<Double>();
        final List<Double> denominator = new ArrayList<Double>();

        for (int i = 0; i < _nomModel.size(); i++) {
            final double value = ((ComplexPrinter) _nomModel.get(i))._value.getRe();
            numerator.add(value);
        }

        for (int i = 0; i < _deNomModel.size(); i++) {
            final double value = ((ComplexPrinter) _deNomModel.get(i))._value.getRe();
            denominator.add(value);
        }
                
        _reglerTF.setNumeratorPolynom(numerator);
        _reglerTF.setDeNominatorPolynom(denominator);
    }
    

    private void updateTransferViewPoleZero() {
        jTextFieldPoleZ.hsSetText("H(s) = "
                + PolynomTools.TECH_FORMATTER.formatENG(jTextFieldConst.getNumberFromField(), 2 + 2) + " ");

        final List<NComplex> zeros = PolynomTools.getPolesOrZeros(_nomModel);
        final List<NComplex> poles = PolynomTools.getPolesOrZeros(_deNomModel);


        jTextFieldPoleZ.setNumeratorText(PolynomTools.plotFactorizedPolynoms(zeros));
        jTextFieldPoleZ.setDenominatorText(PolynomTools.plotFactorizedPolynoms(poles));

        final List<Double> nomPolynom = PolynomTools.evaluateFactorizedExpression(zeros, jTextFieldConst.getNumberFromField());
        final List<Double> denomPolynom = PolynomTools.evaluateFactorizedExpression(poles, 1f);

        _reglerTF.setNumeratorPolynom(nomPolynom);
        _reglerTF.setDeNominatorPolynom(denomPolynom);
        
    }

    private void updateTransferViewPolynom() {

        final List<Double> numerator = new ArrayList<Double>();
        final List<Double> denominator = new ArrayList<Double>();

        for (int i = 0; i < _reglerTF.getNumeratorSize(); i++) {
            numerator.add(_reglerTF.getNumeratorCoefficient(i));                        
        }
        
        for (int i = 0; i < _reglerTF.getDenominatorSize(); i++) {
            denominator.add(_reglerTF.getDenominatorCoefficients(i));            
        }        
        
        
        if(denominator.isEmpty()) {
            final List<Double> constValue = new ArrayList<Double>();
            constValue.add(new Double(1));
            jTextFieldPoly.setDenominatorText(PolynomTools.getPolynomString(constValue));
        } else  {
            jTextFieldPoly.setDenominatorText(PolynomTools.getPolynomString(denominator));
        }
        
        if(numerator.isEmpty()) {
            final List<Double> constValue = new ArrayList<Double>();
            constValue.add(new Double(1));
            jTextFieldPoly.setNumeratorText(PolynomTools.getPolynomString(constValue));
        } else {
            jTextFieldPoly.setNumeratorText(PolynomTools.getPolynomString(numerator));        
        }
    }

    private void updateTransferView() {
        if (_inPolynomialMode) {
            updateReglerPolynom();
            updateTransferViewPolynom();
        } else {
            updateReglerPolesZeros();
            updateTransferViewPoleZero();
            updateTransferViewPolynom();
        }
    }

    private void addListeners() {
        jListDenom.addListSelectionListener(
                new ListSelectionListener() {

                    @Override
                    public void valueChanged(final ListSelectionEvent lse) {
                        if (jListDenom.getSelectedIndex() < 0) {
                            jButtAddDenomEdit.setEnabled(false);
                            return;
                        }
                        jButtAddDenomEdit.setEnabled(true);
                        final ComplexPrinter cont = (ComplexPrinter) jListDenom.getSelectedValue();
                        jTFInsertDeNum.setText("" + cont._value.getRe());
                        jTFInsertDeNumIm.setText("" + cont._value.getIm());
                    }
                });

        jListNom.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(final ListSelectionEvent lse) {
                if (jListNom.getSelectedIndex() < 0) {
                    jButtonEditNom.setEnabled(false);
                    return;
                }
                jButtonEditNom.setEnabled(true);
                final ComplexPrinter cont = (ComplexPrinter) jListNom.getSelectedValue();
                jTFInsertNumberRe.setText("" + cont._value.getRe());
                jTFInsertNumIm.setText("" + cont._value.getIm());
            }
        });

    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    //CHECKSTYLE:OFF
    @SuppressWarnings("PMD")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButtGrpPolePoly = new javax.swing.ButtonGroup();
        jLabelConst = new javax.swing.JLabel();
        jButtonOK = new javax.swing.JButton();
        jPanelNumerator = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jListNom = new javax.swing.JList();
        jButtonAddNom = new javax.swing.JButton();
        jTFInsertNumberRe = new javax.swing.JTextField();
        jButtonDelNom = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabelZeroIm = new javax.swing.JLabel();
        jTFInsertNumIm = new javax.swing.JTextField();
        jButtonEditNom = new javax.swing.JButton();
        jPanelDenominator = new javax.swing.JPanel();
        jTFInsertDeNum = new javax.swing.JTextField();
        jButtonDelNom1 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jListDenom = new javax.swing.JList();
        jButtonAddDenom = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabelPoleImag = new javax.swing.JLabel();
        jTFInsertDeNumIm = new javax.swing.JTextField();
        jButtAddDenomEdit = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jTextFieldName = new javax.swing.JTextField();
        jCheckBoxEnabled = new javax.swing.JCheckBox();
        jRadButtPoleMode = new javax.swing.JRadioButton();
        jRadButtPoly = new javax.swing.JRadioButton();
        jButtonSaveState = new javax.swing.JButton();
        jCheckBoxInitial = new javax.swing.JCheckBox();
        jTextFieldPoleZ = new ch.technokrat.gecko.geckocircuits.control.FractionPrinter();
        jTextFieldPoly = new ch.technokrat.gecko.geckocircuits.control.FractionPrinter();
        jTextFieldConst = new ch.technokrat.gecko.geckocircuits.allg.FormatJTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Transfer Function");
        setAlwaysOnTop(true);
        setLocationByPlatform(true);
        setModalExclusionType(java.awt.Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        setResizable(false);

        jLabelConst.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelConst.setText("Constant Factor:");

        jButtonOK.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonOK.setText("Ok");
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOKActionPerformed(evt);
            }
        });

        jPanelNumerator.setBorder(javax.swing.BorderFactory.createTitledBorder("Zeros"));

        jListNom.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jListNom.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jListNom);

        jButtonAddNom.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonAddNom.setText("Add:");
        jButtonAddNom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddNomActionPerformed(evt);
            }
        });

        jTFInsertNumberRe.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jTFInsertNumberRe.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTFInsertNumberRe.setText("1 ");

        jButtonDelNom.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonDelNom.setText("Delete");
        jButtonDelNom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDelNomActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel2.setText("Real:");

        jLabelZeroIm.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelZeroIm.setText("Imag:");

        jTFInsertNumIm.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jTFInsertNumIm.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTFInsertNumIm.setText("0");

        jButtonEditNom.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonEditNom.setText("Edit");
        jButtonEditNom.setEnabled(false);
        jButtonEditNom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonEditNomActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelNumeratorLayout = new javax.swing.GroupLayout(jPanelNumerator);
        jPanelNumerator.setLayout(jPanelNumeratorLayout);
        jPanelNumeratorLayout.setHorizontalGroup(
            jPanelNumeratorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelNumeratorLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelNumeratorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonAddNom, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTFInsertNumberRe)
                    .addComponent(jLabel2)
                    .addComponent(jLabelZeroIm)
                    .addComponent(jTFInsertNumIm, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButtonDelNom, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonEditNom, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanelNumeratorLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButtonAddNom, jTFInsertNumIm, jTFInsertNumberRe});

        jPanelNumeratorLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButtonDelNom, jButtonEditNom});

        jPanelNumeratorLayout.setVerticalGroup(
            jPanelNumeratorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelNumeratorLayout.createSequentialGroup()
                .addGroup(jPanelNumeratorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelNumeratorLayout.createSequentialGroup()
                        .addComponent(jButtonAddNom)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addGap(5, 5, 5)
                        .addComponent(jTFInsertNumberRe, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelZeroIm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTFInsertNumIm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonEditNom)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDelNom))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );

        jPanelDenominator.setBorder(javax.swing.BorderFactory.createTitledBorder("Poles"));

        jTFInsertDeNum.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jTFInsertDeNum.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTFInsertDeNum.setText("-1");

        jButtonDelNom1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonDelNom1.setText("Delete");
        jButtonDelNom1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDelNom1ActionPerformed(evt);
            }
        });

        jListDenom.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jListDenom.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(jListDenom);

        jButtonAddDenom.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonAddDenom.setText("Add:");
        jButtonAddDenom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddDenomActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel4.setText("Real:");

        jLabelPoleImag.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelPoleImag.setText("Imag:");

        jTFInsertDeNumIm.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jTFInsertDeNumIm.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTFInsertDeNumIm.setText("0");

        jButtAddDenomEdit.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtAddDenomEdit.setText("Edit");
        jButtAddDenomEdit.setEnabled(false);
        jButtAddDenomEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtAddDenomEditActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelDenominatorLayout = new javax.swing.GroupLayout(jPanelDenominator);
        jPanelDenominator.setLayout(jPanelDenominatorLayout);
        jPanelDenominatorLayout.setHorizontalGroup(
            jPanelDenominatorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelDenominatorLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 161, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelDenominatorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButtonDelNom1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTFInsertDeNum, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTFInsertDeNumIm, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtAddDenomEdit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelDenominatorLayout.createSequentialGroup()
                        .addGroup(jPanelDenominatorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jButtonAddDenom, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelPoleImag, javax.swing.GroupLayout.Alignment.LEADING))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelDenominatorLayout.setVerticalGroup(
            jPanelDenominatorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelDenominatorLayout.createSequentialGroup()
                .addGroup(jPanelDenominatorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanelDenominatorLayout.createSequentialGroup()
                        .addComponent(jButtonAddDenom)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTFInsertDeNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelPoleImag)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTFInsertDeNumIm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 60, Short.MAX_VALUE)
                        .addComponent(jButtAddDenomEdit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDelNom1))
                    .addComponent(jScrollPane2))
                .addContainerGap())
        );

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel6.setText("Name:");

        jTextFieldName.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jTextFieldName.setText("jTextField1");
        jTextFieldName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextFieldNameKeyReleased(evt);
            }
        });

        jCheckBoxEnabled.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jCheckBoxEnabled.setText("enabled");
        jCheckBoxEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxEnabledActionPerformed(evt);
            }
        });

        jButtGrpPolePoly.add(jRadButtPoleMode);
        jRadButtPoleMode.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadButtPoleMode.setSelected(true);
        jRadButtPoleMode.setText("Poles/Zeros");
        jRadButtPoleMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadButtPoleModeActionPerformed(evt);
            }
        });

        jButtGrpPolePoly.add(jRadButtPoly);
        jRadButtPoly.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadButtPoly.setText("Polynoms");
        jRadButtPoly.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadButtPolyActionPerformed(evt);
            }
        });

        jButtonSaveState.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonSaveState.setText("Save state");
        jButtonSaveState.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveStateActionPerformed(evt);
            }
        });

        jCheckBoxInitial.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jCheckBoxInitial.setText("Use state as initial value");
        jCheckBoxInitial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxInitialActionPerformed(evt);
            }
        });

        jTextFieldPoleZ.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N

        jTextFieldPoly.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N

        jTextFieldConst.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldConst.setText("1");
        jTextFieldConst.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jTextFieldConst.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextFieldConstKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextFieldPoly, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTextFieldPoleZ, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addComponent(jPanelNumerator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jPanelDenominator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                            .addComponent(jLabel6)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jTextFieldName, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jCheckBoxEnabled)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jRadButtPoleMode)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jRadButtPoly))
                                        .addGroup(layout.createSequentialGroup()
                                            .addComponent(jLabelConst)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jTextFieldConst, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jButtonSaveState, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jCheckBoxInitial)))))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(233, 233, 233)
                                .addComponent(jButtonOK, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 15, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jTextFieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxEnabled)
                    .addComponent(jRadButtPoleMode)
                    .addComponent(jRadButtPoly))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelConst)
                    .addComponent(jTextFieldConst, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSaveState)
                    .addComponent(jCheckBoxInitial))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanelDenominator, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelNumerator, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldPoly, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldPoleZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                .addComponent(jButtonOK)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    //CHECKSTYLE:ON
    
    private void jButtonAddNomActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonAddNomActionPerformed

        float imagValue = Math.abs(new Float(jTFInsertNumIm.getText()));
        final float realValue = new Float(jTFInsertNumberRe.getText());
        if (_inPolynomialMode) {
            imagValue = 0;
        }
        final NComplex value = new NComplex(realValue, imagValue);
        if (_nomModel.size() < ReglerTransferFunction.MAX_ARRAY_SIZE / 2) {
            _nomModel.addElement(new ComplexPrinter(value));
        }
        updateTransferView();

    }//GEN-LAST:event_jButtonAddNomActionPerformed

    private void jButtonDelNomActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonDelNomActionPerformed

        final int index = jListNom.getSelectedIndex();
        if (index < 0) {
            return;
        }

        _nomModel.remove(index);
        updateTransferView();
    }//GEN-LAST:event_jButtonDelNomActionPerformed

    private void jButtonAddDenomActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonAddDenomActionPerformed

        final float realValue = new Float(jTFInsertDeNum.getText());
        float imagValue = Math.abs(new Float(jTFInsertDeNumIm.getText()));
        if (_inPolynomialMode) {
            imagValue = 0;
        }

        final NComplex value = new NComplex(realValue, imagValue);
        if (_deNomModel.size() < ReglerTransferFunction.MAX_ARRAY_SIZE / 2) {
            _deNomModel.addElement(new ComplexPrinter(value));
        }
        updateTransferView();
    }//GEN-LAST:event_jButtonAddDenomActionPerformed

    private void jButtonDelNom1ActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonDelNom1ActionPerformed
        final int index = jListDenom.getSelectedIndex();
        if (index < 0) {
            return;
        }

        _deNomModel.remove(index);
        updateTransferView();

    }//GEN-LAST:event_jButtonDelNom1ActionPerformed

    private void jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonOKActionPerformed
        this.dispose();
    }//GEN-LAST:event_jButtonOKActionPerformed

    private void jButtAddDenomEditActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtAddDenomEditActionPerformed

        final int selectionIndex = jListDenom.getSelectedIndex();
        if (selectionIndex < 0) {
            return;
        }

        final NComplex value = new NComplex(new Float(jTFInsertDeNum.getText()),
                Math.abs(new Float(jTFInsertDeNumIm.getText())));
        _deNomModel.remove(selectionIndex);
        _deNomModel.insertElementAt(new ComplexPrinter(value), selectionIndex);
        updateTransferView();

    }//GEN-LAST:event_jButtAddDenomEditActionPerformed

    private void jButtonEditNomActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonEditNomActionPerformed
        final int selectionIndex = jListNom.getSelectedIndex();
        if (selectionIndex < 0) {
            return;
        }

        final NComplex value = new NComplex(new Float(jTFInsertNumberRe.getText()),
                Math.abs(new Float(jTFInsertNumIm.getText())));
        _nomModel.remove(selectionIndex);
        _nomModel.insertElementAt(new ComplexPrinter(value), selectionIndex);
        updateTransferView();
    }//GEN-LAST:event_jButtonEditNomActionPerformed

    private void jRadButtPoleModeActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadButtPoleModeActionPerformed
        toggleMode();
    }//GEN-LAST:event_jRadButtPoleModeActionPerformed

    private void jRadButtPolyActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jRadButtPolyActionPerformed
        toggleMode();
    }//GEN-LAST:event_jRadButtPolyActionPerformed

    private void jTextFieldNameKeyReleased(java.awt.event.KeyEvent evt) {//NOPMD//GEN-FIRST:event_jTextFieldNameKeyReleased
        try {
            _reglerTF.setNewNameCheckedUndoable(jTextFieldName.getText());
        } catch (NameAlreadyExistsException ex) {
            ex.printStackTrace();
        }
        
    }//GEN-LAST:event_jTextFieldNameKeyReleased

    private void jCheckBoxEnabledActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jCheckBoxEnabledActionPerformed
        
        if(_initDone) {
            if(jCheckBoxEnabled.isSelected()) {
                _elementControl._isEnabled.setValue(Enabled.ENABLED);
            } else {
                _elementControl._isEnabled.setValue(Enabled.DISABLED);
            }                
        }
            
    }//GEN-LAST:event_jCheckBoxEnabledActionPerformed

    private void jTextFieldConstKeyReleased(java.awt.event.KeyEvent evt) {//NOPMD//GEN-FIRST:event_jTextFieldConstKeyReleased
        _reglerTF._constantFactor.setUserValue(jTextFieldConst.getNumberFromField());
        updateTransferView();
    }//GEN-LAST:event_jTextFieldConstKeyReleased

    private void jCheckBoxInitialActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jCheckBoxInitialActionPerformed
        if(_initDone) {
            _reglerTF._useInitialState.setUserValue(jCheckBoxInitial.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxInitialActionPerformed

    private void jButtonSaveStateActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonSaveStateActionPerformed
        _reglerTF.saveState();
    }//GEN-LAST:event_jButtonSaveStateActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtAddDenomEdit;
    private javax.swing.ButtonGroup jButtGrpPolePoly;
    private javax.swing.JButton jButtonAddDenom;
    private javax.swing.JButton jButtonAddNom;
    private javax.swing.JButton jButtonDelNom;
    private javax.swing.JButton jButtonDelNom1;
    private javax.swing.JButton jButtonEditNom;
    private javax.swing.JButton jButtonOK;
    private javax.swing.JButton jButtonSaveState;
    private javax.swing.JCheckBox jCheckBoxEnabled;
    private javax.swing.JCheckBox jCheckBoxInitial;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabelConst;
    private javax.swing.JLabel jLabelPoleImag;
    private javax.swing.JLabel jLabelZeroIm;
    private javax.swing.JList jListDenom;
    private javax.swing.JList jListNom;
    private javax.swing.JPanel jPanelDenominator;
    private javax.swing.JPanel jPanelNumerator;
    private javax.swing.JRadioButton jRadButtPoleMode;
    private javax.swing.JRadioButton jRadButtPoly;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField jTFInsertDeNum;
    private javax.swing.JTextField jTFInsertDeNumIm;
    private javax.swing.JTextField jTFInsertNumIm;
    private javax.swing.JTextField jTFInsertNumberRe;
    private ch.technokrat.gecko.geckocircuits.allg.FormatJTextField jTextFieldConst;
    private javax.swing.JTextField jTextFieldName;
    private ch.technokrat.gecko.geckocircuits.control.FractionPrinter jTextFieldPoleZ;
    private ch.technokrat.gecko.geckocircuits.control.FractionPrinter jTextFieldPoly;
    // End of variables declaration//GEN-END:variables
}

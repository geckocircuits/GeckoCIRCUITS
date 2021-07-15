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
package ch.technokrat.gecko.geckocircuits.newscope;

import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public final class DialogConnectSignalsGraphs extends GeckoDialog {

    private final GraferV4 _grafer;
    private final Container _container;
    private FormatJTextField[][] jbM;
    private JButton[] jlGRF;  // draufklicken --> Fenster zum Graph-Editieren
    private JCheckBox[] _jCheckBoxSignals;  // ist Graph 'Digital'?
    private FormatJTextField[] jtfWEIG;  // relative Graphen-Gewichtung (y-Achse) mi SCOPE
    private JButton _jButtonClose;
    private JButton _jButtonAdd;
    private JButton _jButtonDelete;  // Add/Delete Graph
    private JButton _jButtonUp;  // Add/Delete Graph
    private JButton _jButtonDown;  // Add/Delete Graph
    private static final int CELL_WIDTH = 30, CELL_HEIGHT = 28;
    private double[] _yWeightDiagram;
    private JPanel jpMatrix;
    private GridBagConstraints _gbc;
    private JLabelRot[] jlSGN;
    private static final int HUNDRED = 100;    
    private static final int SIGN_NAME_SPACING = 10;
    private static final int ROT_ANGLE = -90;
    private static final int SIGN_NAME_LENTH = 90;
    private Color _origBackColor;
    ;
  private AbstractDiagram _selectedDiagram;
    private final DiagramCurveSignalManager _manager;
    private int modifiedWeightIndex = -1;

    public DialogConnectSignalsGraphs(final GraferV4 grafer) {
        super((JDialog) null, true);

        this.setLocationRelativeTo(grafer);
        _grafer = grafer;
        _manager = _grafer.getManager();
        this.setTitle(I18nKeys.MATRIX_SIGNAL_GRAPH.getTranslation());
        _container = this.getContentPane();
        _container.setLayout(new BorderLayout());
        //--------------------
        this.baueGUI();
        this.pack();
        this.setMinimumSize(new Dimension(this.getWidth(), this.getHeight()));        
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void baueGUI() {
        _jButtonClose = new JButton(I18nKeys.CLOSE_WINDOW.getTranslation());
        _origBackColor = _jButtonClose.getBackground();
        _jButtonAdd = new JButton(I18nKeys.ADD_GRAPH.getTranslation());
        _jButtonDelete = new JButton(I18nKeys.DELETE_GRAPH.getTranslation());  // Add/Delete Graph
        _jButtonUp = new JButton(I18nKeys.UP.getTranslation());  // Add/Delete Graph
        _jButtonDown = new JButton(I18nKeys.DOWN.getTranslation());  // Add/Delete Graph

        _container.removeAll();
        jpMatrix = new JPanel();
        jpMatrix.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                I18nKeys.MATRIX_SIGNAL_GRAPH.getTranslation(), TitledBorder.LEFT, TitledBorder.TOP));

        jpMatrix.setLayout(new GridBagLayout());
        final int noSignalNames = _grafer.getManager().getNumberInputSignals();
        jlSGN = new JLabelRot[noSignalNames + 1];

        final int noDiags = _manager.getNumberDiagrams();

        _yWeightDiagram = new double[noDiags];
        for (int i1 = 0; i1 < _yWeightDiagram.length; i1++) {
            _yWeightDiagram[i1] = _manager.getDiagram(i1)._diagramSettings.getWeightDiagram();
        }
        jlGRF = new JButton[noDiags];

        _jCheckBoxSignals = new JCheckBox[noDiags];
        jtfWEIG = new FormatJTextField[noDiags];
        jbM = new FormatJTextField[noDiags][noSignalNames + 1];
        _gbc = new GridBagConstraints();
        _gbc.fill = GridBagConstraints.BOTH;
        _gbc.gridx = 0;
        _gbc.gridy = 0;
        final JLabel jlX = new JLabel("");
        jpMatrix.add(jlX, _gbc);  // MATRIX: Ecke links oben --> unbeschriftet
        _gbc.gridx = noSignalNames + 1;
        _gbc.gridy = 0;
        final JLabelRot jlrWEIG = new JLabelRot(I18nKeys.Y_WEIGHT_PERCENT.getTranslation(), ROT_ANGLE, CELL_WIDTH + SIGN_NAME_SPACING, 70);

        jpMatrix.add(jlrWEIG, _gbc);
        _gbc.gridx = noSignalNames + 2;
        final JLabelRot jlrVIS = new JLabelRot(I18nKeys.DIGITAL.getTranslation(), ROT_ANGLE, CELL_WIDTH + SIGN_NAME_SPACING, 70);
        jpMatrix.add(jlrVIS, _gbc);  // MATRIX: Beschriftung "Digital" (statt "Analog")

        final List<AbstractScopeSignal> signals = _grafer.getManager().getAllScopeSignals();
        for (int columnIndex = 0; columnIndex < signals.size(); columnIndex++) {
            jlSGN[columnIndex + 1] = new JLabelRot(signals.get(columnIndex).getSignalName(),
                    ROT_ANGLE, CELL_WIDTH, SIGN_NAME_LENTH);
            _gbc.gridx = columnIndex + 1;
            _gbc.gridy = 0;
            jpMatrix.add(jlSGN[columnIndex + 1], _gbc);  // MATRIX: Beschriftung Signal-Namen
        }

        for (int i1 = 0; i1 < noDiags; i1++) {
            addDiagramButtons(i1);
            addWeightField(i1);
            addDigitalCheckBox(i1);

            for (int i2 = 1; i2 < _grafer.getManager().getNumberInputSignals() + 1; i2++) {
                addMatrixField(i1, i2);
            }
        }

        _gbc.gridx = 0;
        _gbc.gridy = noDiags + 2;
        jpMatrix.add(new JLabel(" "), _gbc);  // MATRIX: Vertikaler Abstandshalter nach unten


        final JPanel jpALLG = new JPanel();
        jpALLG.setLayout(new GridBagLayout());
        _gbc.gridx = 0;
        _gbc.gridy = 0;
        _gbc.gridheight = 2;
        _gbc.gridwidth = 1;
        _gbc.gridx = 1;
        _gbc.gridy = 0;
        _gbc.gridheight = 1;
        _gbc.gridwidth = 2;
        final JPanel panelG = new JPanel();
        panelG.setLayout(new BorderLayout());
        panelG.add(jpMatrix, BorderLayout.CENTER);
        panelG.add(jpALLG, BorderLayout.NORTH);
        final JPanel jpOK = new JPanel();

        jpOK.add(_jButtonAdd);
        jpOK.add(_jButtonDelete);
        jpOK.add(_jButtonDown);
        jpOK.add(_jButtonUp);
        jpOK.add(_jButtonClose);
        getRootPane().setDefaultButton(_jButtonClose);
        _container.add(panelG, BorderLayout.CENTER);
        _container.add(jpOK, BorderLayout.SOUTH);
        addActionListeners();
        pack();

        if (_selectedDiagram == null) {
            setSelectedDiagram(_manager.getDiagram(noDiags - 1));
        } else {
            setSelectedDiagram(_selectedDiagram);
        }

    }

    void setSelectedDiagram(final AbstractDiagram diagram) {
        for (int i = 0; i < jlGRF.length; i++) {
            jlGRF[i].setBackground(_origBackColor);
        }
        _selectedDiagram = diagram;
        jlGRF[_manager.getDiagrams().indexOf(diagram)].setBackground(Color.WHITE);
    }

    private void updateXAxisVisibilityBeforeDelete(final AbstractDiagram deletedDiagram) {
        // checking for a similar diagram
        final List<AbstractDiagram> diagrams = _manager.getDiagrams();

        if (deletedDiagram.equals(diagrams.get(diagrams.size() - 1))) {
            // last diagram is deleted, therefore copy its x-Axis Properties to the previous diagram!
            Axis copyFrom = deletedDiagram._xAxis;
            final Axis copyTo = diagrams.get(diagrams.size() - 2)._xAxis;
            copyTo.copyAxisSettings(copyFrom);
        }
    }

    private void updateXAxisVisibilityAfterAdd(final AbstractDiagram addedDiagram) {
        // checking for a similar diagram
        final List<AbstractDiagram> diagrams = _manager.getDiagrams();

        if (addedDiagram.equals(diagrams.get(diagrams.size() - 1))) {
            // last diagram is deleted, therefore copy its x-Axis Properties to the previous diagram!
            final Axis copyFrom = diagrams.get(diagrams.size() - 2)._xAxis;
            final Axis copyTo = addedDiagram._xAxis;
            copyTo.copyAxisSettings(copyFrom);
            copyFrom._axisTickSettings.setShowLabelsMaj(false);
            copyFrom._axisTickSettings.setShowLabelsMin(false);
            copyFrom._axisTickSettings.setTickLengthMaj(0);
            copyFrom._axisTickSettings.setTickLengthMin(0);
        }
    }

    private void addActionListeners() {
        _jButtonAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                AbstractDiagram diag = new DiagramCurve(_grafer);
                _selectedDiagram = diag;
                diag._diagramSettings.setNameDiagram("GRF " + _manager.getNumberDiagrams());
                _grafer.getManager().addDiagram(diag);

                _grafer.refreshComponentPane();
                baueGUI();
                jtfWEIG[jtfWEIG.length - 1].setNumberToField(110 / jtfWEIG.length);
                modifiedWeightIndex = jtfWEIG.length - 1;
                setMinimumSize(new Dimension(getWidth(), getHeight()));
                // die x-Achse wird nur beim untesten Diagramm angezeigt --> Aktualisierung
                updateXAxisVisibilityAfterAdd(diag);
                // Graph-Gewichtung des neuen Graphen muss angepasst werden:
                aktualisiereGrafer();
            }
        });
        _jButtonDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                if (_manager.getNumberDiagrams() == 1) {
                    return;  // mindestens 1 Graph muss angezeigt werden
                }

                int n = JOptionPane.showConfirmDialog(
                        (JFrame) null,
                        "Do you really like to delete\n diagram no: " + _manager.getDiagrams().indexOf(_selectedDiagram)
                        + " " + _selectedDiagram.getName() + "?",
                        "Question",
                        JOptionPane.YES_NO_OPTION);

                if (n == 1) {
                    return;
                }

                final AbstractDiagram deleteDiagram = _selectedDiagram;
                updateXAxisVisibilityBeforeDelete(deleteDiagram);
                final int deleteIndex = _manager.getDiagrams().indexOf(_selectedDiagram);
                _manager.deleteDiagram(_selectedDiagram);
                setSelectedDiagram(_manager.getDiagram(Math.max(0, deleteIndex - 1)));
                //setResizable(true);
                baueGUI();
                setMinimumSize(new Dimension(getWidth(), getHeight()));
                // die x-Achse wird nur beim untesten Diagramm angezeigt --> Aktualisierung

                // Graph-Gewichtung der verringerten Graphen muss angepasst werden:
                aktualisiereGrafer();
            }
        });

        _jButtonClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                aktualisiereGrafer();
                dispose();
            }
        });

        _jButtonUp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int oldIndex = _manager.getDiagrams().indexOf(_selectedDiagram);
                final int noDiags = _manager.getNumberDiagrams();
                if (_selectedDiagram == null || oldIndex == 0) {
                    return;
                }

                if (oldIndex == noDiags - 1 && noDiags > 1) {
                    _manager.getDiagram(oldIndex - 1)._xAxis.copyAxisSettings(_selectedDiagram._xAxis);
                    _selectedDiagram._xAxis.setAxisInvisible();
                }

                _grafer.getManager().swapDiagrams(_selectedDiagram, _manager.getDiagram(oldIndex - 1));
                baueGUI();
            }
        });

        _jButtonDown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                final int oldIndex = _manager.getDiagrams().indexOf(_selectedDiagram);
                if (_selectedDiagram == null || oldIndex == _manager.getNumberDiagrams() - 1) {
                    return;
                }

                _manager.swapDiagrams(_selectedDiagram, _manager.getDiagram(oldIndex + 1));
                final int noDiags = _manager.getNumberDiagrams();
                if (_manager.getDiagrams().indexOf(_selectedDiagram) == noDiags - 1
                        && _manager.getNumberDiagrams() > 1) {
                    final AbstractDiagram oldLastDiagram = _manager.getDiagram(noDiags - 2);
                    _selectedDiagram._xAxis.copyAxisSettings(oldLastDiagram._xAxis);
                    oldLastDiagram._xAxis.setAxisInvisible();
                }

                baueGUI();
            }
        });

    }

    private void aktualisiereGrafer() {
        recalculateWeights();
        _grafer.refreshComponentPane();
        _grafer.setAxisPositions();
        _grafer.updateUI();
        _grafer.repaint();
    }

    private void addDiagramButtons(final int rowIndex) {
        final DiagramSettings diagramSettings = _manager.getDiagram(rowIndex)._diagramSettings;
        jlGRF[rowIndex] = new JButton(diagramSettings.getNameDiagram() + "   ");
        jlGRF[rowIndex].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                final AbstractDiagram diagram = _manager.getDiagram(rowIndex);
                if (diagram instanceof DiagramSignal) {
                    //final Dialog dialog = new DialogDigitalGraphProperties(_grafer, rowIndex);
                    //dialog.setVisible(true);
                } else {
                    final Dialog dialog = new DialogDiagramProps(DialogConnectSignalsGraphs.this, true, diagram, _grafer);
                    dialog.setVisible(true);
                    baueGUI();
                }
                setSelectedDiagram(diagram);
            }
        });
        _gbc.gridx = 0;
        _gbc.gridy = rowIndex + 1;
        jpMatrix.add(jlGRF[rowIndex], _gbc);
    }

    private void addMatrixField(final int rowIndex, final int columnIndex) {
        final AbstractDiagram diagram = _manager.getDiagram(rowIndex);
        final AbstractCurve curve = diagram.getCurve(columnIndex - 1);
        jbM[rowIndex][columnIndex] = new FormatJTextField();
        jbM[rowIndex][columnIndex].setEditable(false);
        jbM[rowIndex][columnIndex].setBackground(Color.white);
        jbM[rowIndex][columnIndex].setHorizontalAlignment(JTextField.CENTER);
        jbM[rowIndex][columnIndex].setToolTipText(I18nKeys.CLICK_LEFT_OR_RIGHT_BUTTON.getTranslation());
        jbM[rowIndex][columnIndex].setText(curve.getAxisConnection().toString());

        _gbc.gridx = columnIndex;
        _gbc.gridy = rowIndex + 1;
        jpMatrix.add(jbM[rowIndex][columnIndex], _gbc);  // MATRIX: x/y/y2/sg - Knoepfe als zentrales Element
        jbM[rowIndex][columnIndex].setLineSettable(curve);
        jbM[rowIndex][columnIndex].addMouseListener(new MouseAdapter() {  // x-Achse (Zeit) nicht anklick- und veraenderbar 
            @Override
            public void mousePressed(final MouseEvent mouseEvent) {
                final List<AbstractDiagram> diagrams = _manager.getDiagrams();
                AbstractDiagram diagram = diagrams.get(rowIndex);
                AbstractCurve curve = diagram.getCurve(columnIndex - 1);
                setSelectedDiagram(diagram);

                // rechte Maus --> 'Flippen' der ZUORDNUNG ohne Dialogfenster-Eingabe
                if (mouseEvent.getModifiers() == MouseEvent.BUTTON3_MASK || mouseEvent.isControlDown()) {
                    final AxisConnection jcbAchsenTyp = curve.getAxisConnection().iterateNext(diagram instanceof DiagramSignal);
                    diagram.getCurve(columnIndex - 1).setAxisConnection(jcbAchsenTyp);
                    jbM[rowIndex][columnIndex].setText(jcbAchsenTyp.toString());

                    _grafer.doZoomAutoFit();
                    _grafer.setAxisPositions();
                    return;
                }
                // linke Maus --> Dialogfenster


                final Dialog dialog = new DialogCurveProperties(DialogConnectSignalsGraphs.this, true, curve, _grafer);
                dialog.setVisible(true);
                repaint();

                _grafer.setAxisPositions();
            }
        });
    }

    private void addWeightField(final int rowIndex) {
        jtfWEIG[rowIndex] = new FormatJTextField();
        jtfWEIG[rowIndex].setPreferredSize(new Dimension(CELL_WIDTH, CELL_HEIGHT));
        jtfWEIG[rowIndex].setHorizontalAlignment(JTextField.CENTER);
        if (_manager.getDiagram(rowIndex) instanceof DiagramSignal) {
            jtfWEIG[rowIndex].setText("-");
            jtfWEIG[rowIndex].setEnabled(false);
        } else {
            jtfWEIG[rowIndex].setEnabled(true);
            jtfWEIG[rowIndex].setNumberToField(Math.round(HUNDRED * _yWeightDiagram[rowIndex]));
        }
        //
        jtfWEIG[rowIndex].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                DialogConnectSignalsGraphs.this.modifiedWeightIndex = rowIndex;
                aktualisiereGrafer();
                setSelectedDiagram(_manager.getDiagram(rowIndex));
            }
        });

        _gbc.gridx = _grafer.getManager().getNumberInputSignals() + 1;
        _gbc.gridy = rowIndex + 1;
        _gbc.fill = GridBagConstraints.NONE;
        jpMatrix.add(jtfWEIG[rowIndex], _gbc);
        _gbc.fill = GridBagConstraints.BOTH;

    }

    private void addDigitalCheckBox(final int rowIndex) {
        _jCheckBoxSignals[rowIndex] = new JCheckBox();
        _jCheckBoxSignals[rowIndex].setSize(new Dimension(CELL_WIDTH / 2, CELL_HEIGHT / 2));
        if (_manager.getDiagrams().get(rowIndex) instanceof DiagramSignal) {
            _jCheckBoxSignals[rowIndex].setSelected(true);
        } else {
            _jCheckBoxSignals[rowIndex].setSelected(false);
        }

        _jCheckBoxSignals[rowIndex].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                final List<AbstractDiagram> diagrams = _manager.getDiagrams();
                final AbstractDiagram oldDiagram = diagrams.get(rowIndex);
                AbstractDiagram newDiagram;

                if (_jCheckBoxSignals[rowIndex].isSelected()) {
                    newDiagram = new DiagramSignal(diagrams.get(rowIndex));
                    _grafer.getManager().replaceDiagram(oldDiagram, newDiagram);
                } else {
                    newDiagram = new DiagramCurve(diagrams.get(rowIndex));
                    jtfWEIG[rowIndex].setText("20");
                    _manager.replaceDiagram(oldDiagram, newDiagram);
                }

                // set the new axisconnection value into textfield
                for (int i3 = 0; i3 < _grafer.getManager().getNumberInputSignals(); i3++) {
                    final AbstractCurve curve = newDiagram.getCurve(i3);
                    jbM[rowIndex][i3 + 1].setText(curve.getAxisConnection().toString());
                }

                _grafer.doZoomAutoFit();
                aktualisiereGrafer();
                setSelectedDiagram(_manager.getDiagram(rowIndex));
            }
        });

        _gbc.gridx = _grafer.getManager().getNumberInputSignals() + 2;
        _gbc.gridy = rowIndex + 1;
        _gbc.fill = GridBagConstraints.NONE;
        jpMatrix.add(_jCheckBoxSignals[rowIndex], _gbc);  // MATRIX: Check-Boxen Digital JA/NEIN ?
        _gbc.fill = GridBagConstraints.BOTH;  // fuer alle anderen Elemente
    }

    private void recalculateWeights() {
        int totalYSpace = 0;  // // fuer Normierung auf 100% insgesamt
        int spaceWOSelection = 100;
        for (int i1 = 0; i1 < jtfWEIG.length; i1++) {

            if (_manager.getDiagram(i1) instanceof DiagramSignal) {
                _yWeightDiagram[i1] = 0;
            } else {
                final int ySp = (int) jtfWEIG[i1].getNumberFromField();
                if (i1 != modifiedWeightIndex) {
                    totalYSpace += ySp;
                    _yWeightDiagram[i1] = ySp;
                } else {
                    spaceWOSelection -= ySp;
                }

            }

        }

        for (int i1 = 0; i1 < jtfWEIG.length; i1++) {
            if (_manager.getDiagram(i1) instanceof DiagramSignal) {
                jtfWEIG[i1].setText("-");
                jtfWEIG[i1].setEnabled(false);
                _yWeightDiagram[i1] = 0;
            } else {
                jtfWEIG[i1].setEnabled(true);
                if (i1 != modifiedWeightIndex) {
                    jtfWEIG[i1].setNumberToField(Math.round(spaceWOSelection * _yWeightDiagram[i1] / totalYSpace));
                }
                _yWeightDiagram[i1] = jtfWEIG[i1].getNumberFromField() / 100.0;
            }
            if (totalYSpace == 0) {
                totalYSpace = 1;  // ist dann relevant, wenn nur DIGITAL-Signale
            }

            _manager.getDiagram(i1)._diagramSettings.setWeightDiagram(_yWeightDiagram[i1]);
        }
        modifiedWeightIndex = -1;
    }
}

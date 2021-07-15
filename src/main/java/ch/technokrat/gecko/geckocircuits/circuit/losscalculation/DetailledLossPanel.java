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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.circuit.DataTablePanel;
import ch.technokrat.gecko.geckocircuits.newscope.LossCurvePlotPanel;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

abstract class DetailledLossPanel<T extends LossCurve> extends JPanel {

    static final int DIVISIONS_TEST_CURVE = 25;
    private LossCurvePlotPanel _grafer;
    private final FormatJTextField _jtfTemperature = new FormatJTextField();
    private JPanel _jPanelCurvesSelection;
    private LossCurveTemperaturePanel _temperatureButtons;
    final JPanel _leftPanelTempAndBlocking = new JPanel();
    DataTablePanel _table;
    boolean _listenerActive = true;
    T _selectedCurve;
    LossCurve _testCurve;
    JPanel _jPanelTemperatureInput = new JPanel();
    public final List<T> _lossCurves = new ArrayList<T>() {
        @Override
        public boolean add(final LossCurve newCurve) {
            int insertionIndex = 0;
            for (LossCurve oldCurve : this) {
                double oldTemperature = oldCurve.tj.getValue();
                double newTemperature = newCurve.tj.getValue();
                if (Math.abs(oldTemperature - newTemperature) < 0.1) {
                    final JPanel parent = DetailledLossPanel.this;
                    JOptionPane.showMessageDialog(parent, "A curve with the selected temperature " + newCurve.tj.getValue()
                            + " is already\n"
                            + "defined. Please select a different curve temperature.", "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                if (newTemperature > oldTemperature) {
                    insertionIndex++;
                }

            }
            super.add(insertionIndex, (T) newCurve);
            return true;
        }
    };

    abstract LossCurve createNewCurve(final double curveTemperatureParameter);

    abstract void addBlockingVoltageButton(final JPanel panelToInsertButton);

    abstract LossCurve calculateNewTestCurve(final double temperature, final double measuredVoltage);

    abstract String[] getTableCaptions();

    private void deleteSelectedCurve() {
        _lossCurves.remove(_selectedCurve);
        _temperatureButtons.setGuiButtonsFromList(_lossCurves);
        _selectedCurve = _lossCurves.get(0);
        updateGuiAndGrafer();
        loadSelectedCurveIntoTable();

    }

    void loadSelectedCurveIntoTable() {
        double[][] dataToSet = _selectedCurve.getCurveData();
        _listenerActive = false;
        _table.clear();
        _jtfTemperature.setNumberToField(_selectedCurve.tj.getValue());

        _table.setValues(dataToSet);
        _listenerActive = true;
    }

    final void baueGUI() {
        
        _jPanelCurvesSelection = new JPanel();
        _jPanelCurvesSelection.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Edit Curves", TitledBorder.LEFT, TitledBorder.TOP));
        _jPanelCurvesSelection.setLayout(new BorderLayout());
        
//jpSWed.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "", TitledBorder.LEFT, TitledBorder.TOP, TxtI.ti_Font_border));
        _leftPanelTempAndBlocking.setLayout(new GridLayout(getTableCaptions().length+1, 1));        

        final JButton jbAdd = GuiFabric.getJButton(I18nKeys.ADD_NEW);
        jbAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                _testCurve = null;
                addNewCurve((T) createNewCurve(_jtfTemperature.getNumberFromField()));
            }
        });
        final JButton jbDel = GuiFabric.getJButton(I18nKeys.DELETE);
        jbDel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                _testCurve = null;
                if (_lossCurves.size() == 1) {
                    return;  // maximal eine Kurve muss immer da sein!
                }
                deleteSelectedCurve();
            }
        });

        addBlockingVoltageButton(_leftPanelTempAndBlocking);

        _leftPanelTempAndBlocking.add(jbAdd);
        _leftPanelTempAndBlocking.add(jbDel);
        _jPanelCurvesSelection.add(_leftPanelTempAndBlocking, BorderLayout.SOUTH);

        // die Messkurven-Liste wird in einer eigenen Methode erzeugt, weil sie aktualisiert werden kann -->        
        _temperatureButtons = createRadioButtonsPanel();
        _jPanelCurvesSelection.add(_temperatureButtons, BorderLayout.NORTH);


        _table = createTable();
        _table.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Data of Selected Curve", TitledBorder.LEFT, TitledBorder.TOP));

        _jPanelTemperatureInput.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Curve temperature Parameter", TitledBorder.LEFT, TitledBorder.TOP));
        _jPanelTemperatureInput.setLayout(new GridLayout(1, 2));
        JLabel jlTJ = new JLabel("Tj [°C] = ");
        _jPanelTemperatureInput.add(jlTJ);
        _jPanelTemperatureInput.add(_jtfTemperature);

        JPanel jPanelData = new JPanel();
        jPanelData.setLayout(new BorderLayout());
        jPanelData.add(_table, BorderLayout.CENTER);
        jPanelData.add(_jPanelTemperatureInput, BorderLayout.NORTH);

        if(!_lossCurves.isEmpty()) {
            _selectedCurve = _lossCurves.get(0);
            loadSelectedCurveIntoTable();
        }        
        

        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "", TitledBorder.LEFT, TitledBorder.TOP));
        setLayout(new BorderLayout());
        updateGuiAndGrafer();
        add(_jPanelCurvesSelection, BorderLayout.WEST);
        add(_grafer, BorderLayout.CENTER);
        add(jPanelData, BorderLayout.EAST);
    }

    private LossCurveTemperaturePanel createRadioButtonsPanel() {
        final LossCurveTemperaturePanel returnValue = new LossCurveTemperaturePanel(_lossCurves);
        returnValue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                _listenerActive = false;
                int index = returnValue.getSelectedIndex();
                _grafer.highlightMessKurve(index);
                _selectedCurve = _lossCurves.get(index);
                loadSelectedCurveIntoTable();
                _listenerActive = true;
            }
        });

        return returnValue;
    }

    private DataTablePanel createTable() {
        final DataTablePanel returnValue = new DataTablePanel(getTableCaptions());

        final Dimension tableSize = new Dimension(200, 200);
        returnValue.setPreferredSize(tableSize);
        returnValue.setMinimumSize(tableSize);
        returnValue.setMinimumSize(tableSize);
        returnValue.setMaximumSize(tableSize);

        returnValue.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent tableModelEvent) {
                if (_listenerActive) {                    
                    _selectedCurve.setCurveData(_table.getCheckedData());
                    updateGuiAndGrafer();
                }

            }
        });
        return returnValue;
    }

    public abstract double calculateMaximumCurrentInAllCurves();

    /**
     * 
     * @param temperature the temperature where the testcurve should be generated
     * @param appliedVoltage still ugly: for conduction losses, the applied voltage 
     * does not have any effect
     */
    final void createTestCurve(final double temperature, final double appliedVoltage) {
        _testCurve = calculateNewTestCurve(temperature, appliedVoltage);        
        updateGuiAndGrafer();
    }

    final void addNewCurve(final T messkurve) {
        messkurve.setCurveData(_table.getCheckedData());
        _lossCurves.add(messkurve);
        _temperatureButtons.setGuiButtonsFromList(_lossCurves);
        _selectedCurve = messkurve;
        updateGuiAndGrafer();
    }

    void updateGuiAndGrafer() {
        if (_grafer != null) {
            remove(_grafer);
        }
        _grafer = LossCurvePlotPanel.fabric(_lossCurves, _testCurve);
        add(_grafer, BorderLayout.CENTER);

        final int selectedIndex = _lossCurves.indexOf(_selectedCurve);
        _temperatureButtons.setSelectedButton(selectedIndex);
        _grafer.highlightMessKurve(selectedIndex);
        updateUI();
        _jPanelCurvesSelection.updateUI();
    }
}

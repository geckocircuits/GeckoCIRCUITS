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
package ch.technokrat.gecko.geckocircuits.circuit;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class DataTablePanelParameters extends JPanel {

    private final DataTablePanelParameters.MyTableModel _tableModel;
    private final JTable _table;
    private final String[] _columnTitles;
    private final Map<String, Integer> _usedParameterNames;

    public DataTablePanelParameters(final String[] columnTitles, final Map<String, Integer> usedParameterNames) {
        super(new GridLayout(1, 1));
        _columnTitles = columnTitles;
                
        _tableModel = new DataTablePanelParameters.MyTableModel(columnTitles.length);
        _table = new JTable(_tableModel) {

            @Override
            public boolean isCellEditable(int row, int column) {
                if(column == 2) {
                    return false;
                } else {
                    return super.isCellEditable(row, column);
                }                
            }
            
        };
        
        _usedParameterNames = usedParameterNames;
        _table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        
        _table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        _table.setFillsViewportHeight(true);
        
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(_table);


        //Add the scroll pane to this panel.
        add(scrollPane);

        TableColumn secondColumn = _table.getColumnModel().getColumn(1);
        secondColumn.setPreferredWidth(30);
        secondColumn.setCellEditor(new MyTableCellEditor());
        _table.getColumnModel().getColumn(1).setCellRenderer(new MyTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return super.getTableCellRendererComponent(table, value, false, hasFocus, row, column); //To change body of generated methods, choose Tools | Templates.
            }
            
        });
                
        TableColumn thirdColumn = _table.getColumnModel().getColumn(2);
        thirdColumn.setPreferredWidth(20);
        thirdColumn.setCellRenderer(new MyTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return super.getTableCellRendererComponent(table, value, false, hasFocus, row, column); //To change body of generated methods, choose Tools | Templates.
            }
            
        });

        TableColumn firstColumn = _table.getColumnModel().getColumn(0);
        firstColumn.setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String valueString = "";
                if (value != null) {
                    valueString = value.toString();
                }
                
                int numberOfUsages = 0;
                if(_usedParameterNames.containsKey(valueString)) {
                    numberOfUsages = _usedParameterNames.get(valueString);
                }
                
                JLabel returnValue = new JLabel(valueString);
                
                if (!valueString.startsWith("$")) {
                    returnValue.setForeground(Color.red);
                } else {
                    if(valueString != null && !valueString.isEmpty() && numberOfUsages == 0) {
                        returnValue.setForeground(Color.blue);
                    }
                }                
                return returnValue;
            }
        });
        
        


    }

    public void setValues(double[][] data) {
        for (int j = 0; j < data[0].length; j++) {
            for (int i = 0; i < data.length; i++) {
                _tableModel.setValueAt(data[i][j], j, i);
            }
        }

    }

    public void addTableModelListener(TableModelListener tableModelListener) {
        _tableModel.addTableModelListener(tableModelListener);
    }

    public double[][] getCheckedData() {
//        List<List<Double>> returnList = new ArrayList<List<Double>>();
//        for (List<Double> row : _tableModel.data) {
//            boolean parsedOK = true;
//            for (Double value : row) {
//                if (value == null) {
//                    parsedOK = false;
//                }
//            }
//            if (parsedOK) {
//                returnList.add(row);
//            }
//        }
//        double[][] returnValue = new double[_table.getColumnCount()][returnList.size()];
//        for (int i = 0; i < returnList.size(); i++) {
//            for (int j = 0; j < returnList.get(i).size(); j++) {
//                returnValue[j][i] = returnList.get(i).get(j);
//            }
//        }
//        return returnValue;
        return null;
    }

    public void insertDataLine(final String parameterName, final double parameterValue) {       
        final int insertRow = _tableModel.getRowCount() - 1;
        _tableModel.setValueAt(parameterName, insertRow, 0);
        _tableModel.setValueAt(parameterValue, insertRow, 1);
    }

    public List<String> getVariableNames() {
        final List<Integer> validIndices = getIndicesWithValidData();
        final List<String> returnValue = new ArrayList<String>();
        for(int index : validIndices) {
            returnValue.add(_tableModel.getValueAt(index, 0).toString());
        }
        return Collections.unmodifiableList(returnValue);
    }

    public List<Double> getVariableValues() {
        final List<Integer> validIndices = getIndicesWithValidData();
        final List<Double> returnValue = new ArrayList<Double>();
        for(int index : validIndices) {
            returnValue.add((Double) _tableModel.getValueAt(index, 1));
        }
        return Collections.unmodifiableList(returnValue);
    }
    
    private List<Integer> getIndicesWithValidData() {
        List<Integer> returnValue = new ArrayList<Integer>();
        for(int row = 0; row < _tableModel.getRowCount(); row++) {
            String firstColValue = (String) _tableModel.getValueAt(row, 0);
            if(firstColValue == null || firstColValue.isEmpty() || !firstColValue.startsWith("$")) {
                continue;
            }            
            Object secondColValue = _tableModel.getValueAt(row, 1);
            if(secondColValue == null) {
                continue;
            }
            returnValue.add(row);
            
        }
        return returnValue;
    }

    class MyTableModel extends AbstractTableModel {

        private final int _numberColumns;
        private List<String> variableNames = new ArrayList<String>();
        private List<Double> variableNumbers = new ArrayList<Double>();

        public MyTableModel(final int numberColumns) {
            _numberColumns = numberColumns;            
        }

        public int getColumnCount() {
            return _numberColumns;
        }

        public int getRowCount() {            
            return variableNumbers.size() + 1;
        }

        public String getColumnName(int col) {
            return _columnTitles[col];
        }

        public Object getValueAt(int row, int col) {
            if (row >= variableNumbers.size()) {
                return null;
            }
            
            switch(col) {
                case 0:
                    return variableNames.get(row);
                case 1:
                    return variableNumbers.get(row);
                case 2:
                    
                    String variableName = variableNames.get(row);
                    if(_usedParameterNames.containsKey(variableName)) {
                        return _usedParameterNames.get(variableName);
                    } else {
                        return 0;
                    }
                default:
                    assert false : col;
                    return null;
            }            
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        public Class getColumnClass(final int column) {
            if (column == 0) {
                return String.class;
            } else {
                return Double.class;
            }
        }

        public boolean isCellEditable(int row, int col) {
            return true;
        }

        public void setValueAt(Object value, int row, int col) {

            boolean isNullValue = false;
            if (value == null) {
                isNullValue = true;
            } else {
                if (value instanceof String && ((String) value).isEmpty()) {
                    isNullValue = true;
                }
            }

            if (row < variableNames.size()) {
                if (col == 0) {
                    variableNames.set(row, (String) value);
                } else {
                    variableNumbers.set(row, (Double) value);
                }

                if (isNullValue) {
                    boolean isNullName = false;
                    if(variableNames.get(row) == null) {
                        isNullName = true;
                    } else  {
                        if(variableNames.get(row).isEmpty()) {
                            isNullName = true;
                        }
                    }
                    if (isNullName && variableNumbers.get(row) == null) {
                        variableNames.remove(row);
                        variableNumbers.remove(row);
                        fireTableRowsDeleted(row, row);
                    }
                }

                fireTableCellUpdated(row, col);
                fireTableCellUpdated(row, 2);
            } else {
                if (value != null && value instanceof String && ((String) value).isEmpty()) {
                    return;
                }
                if (value != null) {
                    createNullRow();
                    if (col == 0) {
                        variableNames.set(row, (String) value);
                    } else {
                        variableNumbers.set(row, (Double) value);
                    }
                    fireTableCellUpdated(row, col);
                    fireTableCellUpdated(row, 2);
                    fireTableRowsInserted(variableNames.size(), variableNames.size());
                }
            }

        }

        private void createNullRow() {
            
            variableNames.add(null);
            variableNumbers.add(null);
        }
    }
}
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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

public class DataTablePanel extends JPanel {

    private final MyTableModel _tableModel;
    private final JTable _table;
    private final String[] _columnTitles;
    private boolean _sortingDisabled;

    public DataTablePanel(final String[] columnTitles) {
        super(new GridLayout(1, 1));
        _columnTitles = columnTitles;
        _tableModel = new MyTableModel(columnTitles.length);
        _table = new JTable(_tableModel);
        _table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        _table.setFillsViewportHeight(true);

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(_table);

        _tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent event) {
                _tableModel.sortWithFirstRow();
            }
        });

        //Add the scroll pane to this panel.
        add(scrollPane);
        for (int i = 0; i < _table.getColumnCount(); i++) {
            TableColumn col = _table.getColumnModel().getColumn(i);
            col.setCellEditor(new MyTableCellEditor());
            _table.getColumnModel().getColumn(i).setCellRenderer(new MyTableCellRenderer());
        }

    }

    public void setValues(double[][] data) {
        _sortingDisabled = true;
        for (int j = 0; j < data[0].length; j++) {
            for (int i = 0; i < data.length; i++) {
                _tableModel.setValueAt(data[i][j], j, i);
            }
        }
        _sortingDisabled = false;
        _tableModel.sortWithFirstRow();        

    }

    public void addTableModelListener(TableModelListener tableModelListener) {
        _tableModel.addTableModelListener(tableModelListener);
    }

    public double[][] getCheckedData() {
        List<List<Double>> returnList = new ArrayList<List<Double>>();
        _tableModel.sortWithFirstRow();
        for (List<Double> row : _tableModel.data) {
            boolean parsedOK = true;
            for (Double value : row) {
                if (value == null) {
                    parsedOK = false;
                }
            }
            if (parsedOK) {
                returnList.add(row);
            }
        }
        double[][] returnValue = new double[_table.getColumnCount()][returnList.size()];
        for (int i = 0; i < returnList.size(); i++) {
            for (int j = 0; j < returnList.get(i).size(); j++) {
                returnValue[j][i] = returnList.get(i).get(j);
            }
        }
        return returnValue;
    }

    public void clear() {  
        _tableModel.data.clear();
        _tableModel.fireTableDataChanged();
    }
    
    static int counter = 0;

    void clearWithoutEvent() {
        _tableModel.data.clear();
    }

    class MyTableModel extends AbstractTableModel {

        private final int _numberColumns;
        private List<List<Double>> data = new ArrayList<List<Double>>() {
            @Override
            public boolean add(List<Double> e) {
                assert e.size() == _numberColumns;
                return super.add(e); //To change body of generated methods, choose Tools | Templates.
            }
        };

        public MyTableModel(final int numberColumns) {
            _numberColumns = numberColumns;
            data.add(createNullRow());
        }

        public int getColumnCount() {
            return _numberColumns;
        }

        public int getRowCount() {
            return data.size() + 1;
        }

        public String getColumnName(int col) {
            return _columnTitles[col];
        }

        public Object getValueAt(int row, int col) {            
            if (row >= data.size()) {
                return null;
            }
            return data.get(row).get(col);           
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        public Class getColumnClass(int c) {
            return Double.class;
        }

        public boolean isCellEditable(int row, int col) {
            return true;
        }

        public void setValueAt(Object value, int row, int col) {
            if (row < data.size()) {
                List<Double> dataRow = data.get(row);
                dataRow.set(col, (Double) value);

                if (value == null) {
                    boolean isZeroRow = true;
                    for (Object test : dataRow) {
                        if (test != null) {
                            isZeroRow = false;
                        }
                    }
                    if (isZeroRow && data.size() > 1) {
                        data.remove(row);
                        fireTableRowsDeleted(row, row);
                    }
                }

                fireTableCellUpdated(row, col);
            } else {
                if (value != null) {
                    List<Double> newRow = createNullRow();
                    newRow.set(col, (Double) value);
                    fireTableCellUpdated(row, col);
                    data.add(newRow);
                    fireTableRowsInserted(data.size(), data.size());
                }
            }

        }

        private void sortWithFirstRow() {
            if(_sortingDisabled) {
                return;
            }
            long oldHash = calculateTableHash();
            if (_tableModel.data.size() < 2) {
                return;
            }            

            Collections.sort(data, new MyTableComparator());

            long newHash = calculateTableHash();
            if (oldHash != newHash) {
                fireTableDataChanged();                
            }
        }
        
        
        private long calculateTableHash() {
            counter++;
            assert counter < 1000;
            long hash = 7;
            for (List<Double> hashList : data) {
                if (hashList == null) {
                    continue;
                }

                long numberHash = 13;
                if (hashList.get(0) != null) {
                    numberHash += hashList.get(0).hashCode();
                }

                hash += (7 + hashList.hashCode() + numberHash) * (13 + (9 + data.indexOf(hashList)));
            }
            return hash;
        }

        private List<Double> createNullRow() {
            List<Double> returnValue = new ArrayList<Double>();
            for (int i = 0; i < _numberColumns; i++) {
                returnValue.add(null);
            }
            return returnValue;
        }
    }
}
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

import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import java.awt.Component;
import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

class MyTableCellEditor extends AbstractCellEditor implements TableCellEditor {

    FormatJTextField component = new FormatJTextField();

  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
      int rowIndex, int vColIndex) {      
      if(value != null) {
          component.setNumberToField((Double) value);
      } else {
          component.setText("");
      }
        
    return component;
  }

  public Object getCellEditorValue() {
      if(component.getText().isEmpty()) {
          return null;
      }
    return component.getNumberFromField();
  }
}
    

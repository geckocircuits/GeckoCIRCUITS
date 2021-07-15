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
package ch.technokrat.gecko.geckocircuits.datacontainer;

import javax.swing.table.AbstractTableModel;

public final class DataContainerTableModel extends AbstractTableModel{
  private final AbstractDataContainer _dataContainer;  
  

  public DataContainerTableModel(final AbstractDataContainer dataContainer){
    super();
    _dataContainer = dataContainer;    
  }

  @Override
  public String getColumnName(final int column){
    if(column == 0){
      return _dataContainer.getXDataName();
    }else{
      return _dataContainer.getSignalName(column - 1);
    }
  }

  @Override
  public int getRowCount(){      
    return _dataContainer.getMaximumTimeIndex(0) + 1;

  }

  @Override
  public int getColumnCount(){    
    return _dataContainer.getRowLength() + 1;
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex){
    Object returnValue = null;
    if(columnIndex == 0){
      returnValue = _dataContainer.getTimeValue(rowIndex, 0);
    }else{
      final float value = _dataContainer.getValue(columnIndex - 1, rowIndex);
      if(_dataContainer.isInvalidNumbers(columnIndex - 1)){                    
        returnValue = "<html><font color=red>" + value + "</font><html>";
      }else{
        if(value == 0 || value == 1){
          returnValue = (byte)value;
        }else{
          returnValue = value;
        }
      }
    }

    return returnValue;
  }
}

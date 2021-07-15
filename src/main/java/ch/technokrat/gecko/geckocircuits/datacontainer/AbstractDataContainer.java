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

import ch.technokrat.gecko.geckocircuits.newscope.AbstractTimeSerie;
import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import java.util.Observable;

/**
 * A powerful data storage object, keeps data information as e.g.
 * minumum-maximum values, ...
 * @author andy
 */
public abstract class AbstractDataContainer extends Observable{
  /**
   * @param row the row where to search inside
   * @param columnMax maximum column value
   * @param columnMin minimum column value      
   * @return the MinMax-Data from columnOld to column
   */
  public abstract HiLoData getHiLoValue(final int row, final int columnMin, final int columnMax);

  public abstract float getValue(final int row, final int column);

  public abstract int getRowLength();

  public abstract double getTimeValue(final int index, final int row);

  public abstract int getMaximumTimeIndex(final int row);

  public abstract Object getDataValueInInterval(final double intervalStart, final double intervalStop, final int columnIndex);

  public abstract HiLoData getAbsoluteMinMaxValue(int row);


  public abstract int findTimeIndex(final double time, final int row);

  public abstract String getSignalName(final int row);

  public abstract String getXDataName();

  public abstract ContainerStatus getContainerStatus();

  public abstract void setContainerStatus(final ContainerStatus containerStatus);

  public abstract boolean isInvalidNumbers(final int row);

  public abstract AbstractTimeSerie getTimeSeries(final int row);

  public abstract float[] getDataArray();
  
  public String getSubcircuitSignalPath(final int row) {      
      return "";
  };

    void setSignalPathName(int containerRowIndex, String subcircuitPath) {        
    }
    
}

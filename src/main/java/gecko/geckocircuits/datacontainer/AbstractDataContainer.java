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
package gecko.geckocircuits.datacontainer;

import gecko.geckocircuits.api.IScopeData;
import gecko.geckocircuits.newscope.AbstractTimeSeries;
import gecko.core.datacontainer.HiLoData;
import java.util.Observable;

/**
 * A powerful data storage object, keeps data information as e.g.
 * minumum-maximum values, ...
 *
 * Implements {@link IScopeData} to provide a standard interface for scope
 * data access that decouples rendering components from concrete implementations.
 *
 * @author andy
 * @see IScopeData
 */
public abstract class AbstractDataContainer extends Observable implements IScopeData {
  /**
   * @param row the row where to search inside
   * @param columnMax maximum column value
   * @param columnMin minimum column value
   * @return the MinMax-Data from columnOld to column
   */
  @Override
  public abstract HiLoData getHiLoValue(final int row, final int columnMin, final int columnMax);

  @Override
  public abstract float getValue(final int row, final int column);

  @Override
  public abstract int getRowLength();

  @Override
  public abstract double getTimeValue(final int index, final int row);

  @Override
  public abstract int getMaximumTimeIndex(final int row);

  @Override
  public abstract Object getDataValueInInterval(final double intervalStart, final double intervalStop, final int columnIndex);

  @Override
  public abstract HiLoData getAbsoluteMinMaxValue(int row);


  @Override
  public abstract int findTimeIndex(final double time, final int row);

  @Override
  public abstract String getSignalName(final int row);

  @Override
  public abstract String getXDataName();

  @Override
  public abstract ContainerStatus getContainerStatus();

  @Override
  public abstract void setContainerStatus(final ContainerStatus containerStatus);

  @Override
  public abstract boolean isInvalidNumbers(final int row);

  @Override
  public abstract AbstractTimeSeries getTimeSeries(final int row);

  @Override
  public abstract float[] getDataArray();

  @Override
  public String getSubcircuitSignalPath(final int row) {
      return "";
  }

    void setSignalPathName(int containerRowIndex, String subcircuitPath) {
        // no-op
    }

}

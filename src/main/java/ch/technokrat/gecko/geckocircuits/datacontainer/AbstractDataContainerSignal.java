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

/**
 *
 * @author andreas
 */
public abstract class AbstractDataContainerSignal{
  protected AbstractDataContainer _dataContainer;
  protected int _dataContainerIndex;

  public AbstractDataContainerSignal(final AbstractDataContainer dataContainer, final int dataContainerIndex){
    _dataContainer = dataContainer;
    _dataContainerIndex = dataContainerIndex;
  }

  public abstract String getSignalName();

  public final AbstractDataContainer getDataContainer(){
    return _dataContainer;
  }

  public final int getContainerSignalIndex(){
    return _dataContainerIndex;
  }

  @Override
  public final String toString(){
    return _dataContainer.getSignalName(_dataContainerIndex) + " " + _dataContainerIndex;
  }
}

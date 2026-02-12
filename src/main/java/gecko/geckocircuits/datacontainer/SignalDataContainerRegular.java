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

/**
 *
 * @author andreas
 */
public class SignalDataContainerRegular extends AbstractDataContainerSignal{
  private String _signalName;

  public SignalDataContainerRegular(final AbstractDataContainer dataContainer, final int dataContainerIndex){
    super(dataContainer, dataContainerIndex);
  }

  @Override
  public final String getSignalName(){
    if(_signalName != null){
      return _signalName;
    }else{
      return _dataContainer.getSignalName(_dataContainerIndex);
    }
  }

  final void setSignalName(final String newName){
    _signalName = newName;
  }
}

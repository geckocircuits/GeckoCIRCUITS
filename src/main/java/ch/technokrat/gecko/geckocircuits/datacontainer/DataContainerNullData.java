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

import ch.technokrat.gecko.geckocircuits.newscope.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author andreas
 */
public final class DataContainerNullData extends AbstractDataContainer implements DataContainerValuesSettable,
                                                                                  DataContainerIntegralCalculatable{
  private List<AbstractScopeSignal> _signalNames;
  private DefinedMeanSignals _definedMeanSignals;  

  public DataContainerNullData(){
  }

  public DataContainerNullData(final List<AbstractScopeSignal> signalNames){
    super();
    _signalNames = signalNames;
  }

  public void setNoDataName(){
    _signalNames = new ArrayList<AbstractScopeSignal>();

    _signalNames.add(new AbstractScopeSignal(){
      @Override
      public String getSignalName(){
        return "no data available";
      }
    });
  }

  @Override
  public HiLoData getHiLoValue(final int row, final int columnMin, final int columnMax){
    return HiLoData.hiLoDataFabric(0, 0);
  }

  @Override
  public float getValue(final int row, final int column){
    return 0;
  }

  @Override
  public int getRowLength(){
    if(_signalNames == null){
      return 0;
    }else{
      return _signalNames.size();
    }
  }

  @Override
  public double getTimeValue(final int index, final int row){
    return 0;
  }

  @Override
  public int getMaximumTimeIndex(final int row){
    return 0;
  }

  @Override
  public Object getDataValueInInterval(final double intervalStart, final double intervalStop, final int columnIndex){
    return null;
  }

  @Override
  public void insertValuesAtEnd(final float[] values, final double timeValue){
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public HiLoData getAbsoluteMinMaxValue(final int row){
    return HiLoData.hiLoDataFabric(0, 1);
  }


  @Override
  public int findTimeIndex(final double time, final int row){
    return 0;
  }

  @Override
  public int getUsedRAMSizeInMB(){
    return 0;
  }

  @Override
  public long getCachedRAMSizeInMB(){
    return 0;
  }

  @Override
  public String getSignalName(final int row){
    if(_signalNames.size() <= row || _signalNames.get(row).getSignalName().isEmpty()){
      return "sg." + row;
    }else{
      return _signalNames.get(row).getSignalName();
    }

  }

  @Override
  public String getXDataName(){
    return "t";
  }

  @Override
  public ContainerStatus getContainerStatus(){
    return ContainerStatus.NOT_INITIALIZED;
  }

  @Override
  public void setContainerStatus(ContainerStatus containerStatus){
    assert containerStatus == ContainerStatus.DELETED : "Null data container should not set status ! " + containerStatus;
  }

  @Override
  public boolean isInvalidNumbers(int row){
    return false;// the container has not data - therefore NaN or Inf is not possible!
  }

  @Override
  public AbstractTimeSerie getTimeSeries(final int row){
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public final float getAVGValueInInterval(double intervalStart, double intervalStop, int columnIndex){
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void defineAvgCalculation(final List<ScopeSignalMean> meansSignals){
  }

  public void setDefinedMeanSignals(final DefinedMeanSignals definedMeanSignals){
    _definedMeanSignals = definedMeanSignals;
  }  

  @Override
  public DefinedMeanSignals getDefinedMeanSignals(){
    return _definedMeanSignals;
  }

    @Override
    public float[] getDataArray() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  
}

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

import gecko.geckocircuits.newscope.*;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * We use a separate class containing the actual datacontainer, for easier
 * memory management. The _data-Field is wrapped by this class, so that no other
 * class can reference to it. Otherwise, garbage-collection would be difficult.
 *
 */
public final class DataContainerGlobal extends AbstractDataContainer implements Observer, DataContainerValuesSettable,
                                                                                DataContainerIntegralCalculatable{
  private AbstractDataContainer _data;
  private DataContainerValuesSettable _settable;
  private int _lastDataIndex = -1;

  public DataContainerGlobal(){
    super();      
    final DataContainerNullData data = new DataContainerNullData();
    _data = data;
    _settable = data;

  }

  public void init(final int rows, final String[] signalNames, final String xDataName){
    
      final DataContainerCompressable data = new DataContainerCompressable(rows, new TimeSeriesConstantDt(),
                                                                         signalNames, xDataName);      
    _settable = data;
    _data = data;
    _data.addObserver(this);
  }

  @Override
  public HiLoData getHiLoValue(final int row, final int columnMin, final int columnMax){
    return _data.getHiLoValue(row, columnMin, columnMax);
  }

  @Override
  public float getValue(final int row, final int column){
    return _data.getValue(row, column);
  }

  @Override
  public int getRowLength(){
    return _data.getRowLength();
  }

  @Override
  public double getTimeValue(final int index, final int row){
    return _data.getTimeValue(index, row);
  }

  @Override
  public int getMaximumTimeIndex(final int row){
    return _data.getMaximumTimeIndex(row);
  }

  @Override
  public void insertValuesAtEnd(final float[] values, final double timeValue){
    _settable.insertValuesAtEnd(values, timeValue);
  }

  @Override
  public HiLoData getAbsoluteMinMaxValue(final int row){
    try{
      return _data.getAbsoluteMinMaxValue(row);
    }catch(ArithmeticException ex){
      return HiLoData.hiLoDataFabric(0, 1); // just return some value, so that the scope is not totally screwed up!
    }

  }

  public void clear(){
    _data.setContainerStatus(ContainerStatus.DELETED);
    _data.deleteObservers();
    _settable = null;
    _data = null;
  }

  @Override
  public int findTimeIndex(final double time, final int row){
    return _data.findTimeIndex(time, row);
  }

  @Override
  public int getUsedRAMSizeInMB(){
    return _settable.getUsedRAMSizeInMB();
  }

  @Override
  public long getCachedRAMSizeInMB(){
    return _settable.getCachedRAMSizeInMB();
  }

  @Override
  public Object getDataValueInInterval(final double intervalStart, final double intervalStop, final int columnIndex){
    return _data.getDataValueInInterval(intervalStart, intervalStop, columnIndex);
  }

  @Override
  public String getSignalName(final int row){      
    return _data.getSignalName(row);
  }

  @Override
  public String getXDataName(){
    return _data.getXDataName();
  }

  @Override
  public ContainerStatus getContainerStatus(){
    return _data.getContainerStatus();
  }

  @Override
  public void setContainerStatus(final ContainerStatus containerStatus){
    _data.setContainerStatus(containerStatus);
  }

  @Override
  public boolean isInvalidNumbers(final int row){
    return _data.isInvalidNumbers(row);
  }

  @Override
  public void update(final Observable observable, final Object arg){

    if(getMaximumTimeIndex(0) != _lastDataIndex){
      this.setChanged();
      this.notifyObservers();
      _lastDataIndex = getMaximumTimeIndex(0);
    }

  }

  @Override
  public float getAVGValueInInterval(final double intervalStart, final double intervalStop, final int columnIndex){
    if(_data instanceof DataContainerIntegralCalculatable){
      return ((DataContainerIntegralCalculatable)_data).getAVGValueInInterval(intervalStart, intervalStop, columnIndex);
    }else{
      return 0f;
    }
  }

  @Override
  public AbstractTimeSerie getTimeSeries(int row){
    return _data.getTimeSeries(row);
  }

  @Override
  public void defineAvgCalculation(final List<ScopeSignalMean> meanSignals){
    if(_data instanceof DataContainerIntegralCalculatable){
      ((DataContainerIntegralCalculatable)_data).defineAvgCalculation(meanSignals);
    }else{
      assert false;
    }
  }

  @Override
  public DefinedMeanSignals getDefinedMeanSignals(){
    assert _data instanceof DataContainerIntegralCalculatable;
    return ((DataContainerIntegralCalculatable) _data).getDefinedMeanSignals();
  }

  @Override
  public int hashCode(){
    return _data.hashCode();
  }

    public float[] getDataArray() {
        return _data.getDataArray();
    }

    @Override
    void setSignalPathName(int containerRowIndex, String subcircuitPath) {
        _data.setSignalPathName(containerRowIndex, subcircuitPath);
    }          
    
    @Override
    public String getSubcircuitSignalPath(final int row) {      
      return _data.getSubcircuitSignalPath(row);
  };
}

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
package ch.technokrat.gecko.geckocircuits.newscope;

import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import java.util.List;

/**
 * this class detects, which data values in the CurvePaintabel have to be
 * loaded. Usually, data is just added to the datacontainer, therefore do not do
 * a full load.
 * @author andy
 */
public final class DataLoader{
  private final AbstractCurvePainter _curvePaintable;
  private int _maxDataIndex;
  private long _oldAxisHash = -1;
  private int _maxLoadedPixel = -1;

  DataLoader(final AbstractCurvePainter curvePaintable){
    _curvePaintable = curvePaintable;
  }

  private long calculateAxisHash(final List<Axis> sensitiveAxis){
    long returnValue = 0;
    for(Axis axis : sensitiveAxis){
      returnValue += axis.getAxisHash();
    }
    return returnValue;
  }

  void loadRequiredData(final AbstractDataContainer newData, boolean forceLoad){

    if(_curvePaintable.getRamData() == null || !newData.equals(_curvePaintable.getRamData())){
      // do a full load when new dataContainer is given.
      _curvePaintable.setRamData(newData);
      final int maxExtendedPixel = getMaximumExtendedPixel(newData.getMaximumTimeIndex(_curvePaintable._curve.getValueDataIndex()));
      _oldAxisHash = calculateAxisHash(_curvePaintable.getSensitiveAxis());
      final int minExtendedPixel = getMinimumExtendedPixel();                            
      _curvePaintable.reLoadData(minExtendedPixel, maxExtendedPixel);        
      _maxLoadedPixel = maxExtendedPixel;
      return;
    }

    final long newAxisHash = calculateAxisHash(_curvePaintable.getSensitiveAxis());
    final int maxExtendedPixel = getMaximumExtendedPixel(newData.getMaximumTimeIndex(_curvePaintable._curve.getValueDataIndex()));
    if(_oldAxisHash != newAxisHash || forceLoad){ // do full load when axis properties changed            
      _oldAxisHash = newAxisHash;
      final int minExtendedPixel = getMinimumExtendedPixel();      
      _curvePaintable.reLoadData(minExtendedPixel, maxExtendedPixel);
      _maxLoadedPixel = maxExtendedPixel;
      return;
    }

    final int newMaximumIndex = _curvePaintable.getRamData().getMaximumTimeIndex(_curvePaintable._curve.getValueDataIndex());      
    final Axis xAxis = _curvePaintable.getXAxis();
    if(_maxDataIndex != newMaximumIndex && _maxDataIndex >= 0
            || _maxLoadedPixel < xAxis._axisOriginPixel.x + xAxis.getAxisLengthPixel()){
      _maxDataIndex = newMaximumIndex;        
      _curvePaintable.loadDataRange(Math.max(_maxLoadedPixel, xAxis._axisOriginPixel.x), maxExtendedPixel);
      _maxLoadedPixel = maxExtendedPixel;
    }
  }

  /**
   * calculate the maximum pixel value (in the scope) for which the data should
   * be loaded. Careful: we are searching for the Next data point pixel, since,
   * when we zoom into the scope, the drawn signal lines should reach to the end
   * of the scope window. Extended means, we search the pixel value for the next
   * bigger data point outside the scope.
   * @param maximumDataContainerIndex
   * @return
   */
  private int getMaximumExtendedPixel(final int maximumDataContainerIndex){      
      
    final Axis xAxis = _curvePaintable.getXAxis();
    if(maximumDataContainerIndex < 0){
      return Integer.MIN_VALUE;
    }
    final double maxTimeValue = _curvePaintable.getRamData().getTimeValue(maximumDataContainerIndex, _curvePaintable._curve.getValueDataIndex());
    final int maxDataPixel = (int)xAxis.getPixelFromValue(maxTimeValue);
    final int maxAxisPixel = (int)xAxis._axisOriginPixel.x + xAxis.getAxisLengthPixel();

    final AbstractTimeSerie timeSerie = _curvePaintable.getRamData().getTimeSeries(_curvePaintable._curve.getValueDataIndex());
    final int maxAxisIndex = timeSerie.findTimeIndex(xAxis.getValueFromPixel(maxAxisPixel));
      
    if(maxAxisIndex + 1 < maximumDataContainerIndex){                                
        return Math.min(maxDataPixel, (int)xAxis.getPixelFromValue(timeSerie.getValue(maxAxisIndex + 2)));
    } else {
        return Math.min(maxDataPixel+1, maxAxisPixel);
    }         

    
  }

  /**
   *
   * @return the x-Axis pixel value of the data point, that is just outside
   * (left) the diagram.
   */
  private int getMinimumExtendedPixel(){
    final Axis xAxis = _curvePaintable.getXAxis();
    final double axisOriginValue = xAxis.getValueFromPixel(xAxis._axisOriginPixel.x);      
    final AbstractTimeSerie timeSerie = _curvePaintable.getRamData().getTimeSeries(0);
    final int originDataIndex = timeSerie.findTimeIndex(axisOriginValue);      
    final int indexToLoad = Math.max(0, originDataIndex - 1);
    final double loadXValue = timeSerie.getValue(indexToLoad);      
    int returnValue = (int) xAxis.getPixelFromValue(loadXValue);
    return returnValue;
  }
}

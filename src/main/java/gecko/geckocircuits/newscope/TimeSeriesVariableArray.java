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
package gecko.geckocircuits.newscope;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author xtibi
 */
public final class TimeSeriesVariableArray extends AbstractTimeSerie{
  private final List<Double> _timeSeries = new ArrayList<Double>();

  @Override
  public double getValue(final int index){
    assert index >= 0;
    assert index < _timeSeries.size();

    return _timeSeries.get(index);
  }

  @Override
  public void setValue(final int index, final double value){
    if(index < _timeSeries.size()){
      _timeSeries.set(index, value);
    }else{
      _timeSeries.add(value);
    }
  }

  @Override
  public int getMaximumIndex(){
    return _timeSeries.size() - 1;
  }

  @Override
  public int findTimeIndex(final double time){
    int minIndex = 0;
    int variableMaxIndex = _timeSeries.size() - 1;
    int returnValue = estimateIndex(time, minIndex, variableMaxIndex);

    while(!isNearToIndex(time, returnValue)){
      if(getValue(returnValue) < time){
        minIndex = returnValue + 1;
        returnValue = estimateIndex(time, minIndex, variableMaxIndex);
      }else{
        variableMaxIndex = returnValue - 1;
        returnValue = estimateIndex(time, minIndex, variableMaxIndex);
      }
    }

    return Math.max(0, returnValue);
  }

  private int estimateIndex(final double time, final int minIndex, final int maxIndex){
    final int dIndex = maxIndex - minIndex;
    final double dTime = getValue(maxIndex) - getValue(minIndex);
    final double variableTime = time - getValue(minIndex);
    int returnValue = (int)(minIndex + dIndex * variableTime / dTime);
    if(returnValue > maxIndex){
      returnValue = maxIndex;
    }
    if(returnValue < minIndex){
      returnValue = minIndex;
    }
    return returnValue;
  }

  private boolean isNearToIndex(final double time, final int index){
    if(index == getMaximumIndex()){
      return time >= getValue(index - 1);
    }else if(index == 0){
      return time <= getValue(index + 1);
    }
    return (getValue(index) <= time && time <= getValue(index + 1))
            || (getValue(index) >= time && time >= getValue(index - 1));
  }

//    public static void main(final String args[]) {
//     
//        TimeSeriesVariableArray ts = new TimeSeriesVariableArray();
//        final int arraySize = 4000000;
//        final Random rnd = new Random();
//        System.out.println("Test started...");
//        
//        System.out.print("Setting values...");
//        ts.setValue(0, 0.0);
//        for (int i = 1; i < arraySize; i++) {
//            ts.setValue(i, ts.getValue(i-1) + i/3);
//        }
//        System.out.println(" done");
//        
////        int getIndex = rnd.nextInt(arraySize);
////        System.out.print("Trying to get value at index: " + getIndex);
////        System.out.println(", got: " + ts.getValue(getIndex));
//        
//        double time = ts.getValue(ts.getMaximumIndex())*rnd.nextDouble();
//        System.out.println("Trying to find index of " + time);
//        long startTime = System.currentTimeMillis();
//        int indexFound = ts.findTimeIndex(time, ts.getMaximumIndex());
//        long endTime = System.currentTimeMillis();
//        
//        System.out.println(" -> found index: " + indexFound +
//                " in " + (endTime-startTime) + " milliseconds.");
//        System.out.println("Previous value: " + ts.getValue(indexFound - 1));
//        System.out.println("VALUE FOUND:    " + ts.getValue(indexFound));
//        System.out.println("Next value:     " + ts.getValue(indexFound + 1));
//        
//    }
  @Override
  public double getLastTimeInterval(){
    throw new UnsupportedOperationException("Not supported yet.");
  }
}

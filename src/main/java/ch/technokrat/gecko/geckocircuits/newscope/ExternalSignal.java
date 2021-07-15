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

import java.awt.geom.Point2D;

/**
 * Provides basic functions to facilitate access to external signals.
 * @author JF
 */
public class ExternalSignal extends AbstractScopeSignal{
  private String _signalName = ""; // The name of the external signal.
  private double[] _signalTimes = {}; // The array holding the time values for the external signal.
  private double[] _signalValues = {}; // the array holding the data values for the external signal.
  private double _min = Double.POSITIVE_INFINITY;
  private double _max = Double.NEGATIVE_INFINITY;
  private double _offsetX = 0;
  private double _offsetY = 0;
    private String _subCircuitPath;

  /**
   * Empty standard constructor
   */
  public ExternalSignal(){
  }

  /**
   * Constructor to initialize a signal with its time values and its data
   * values.
   * @param times An array holding the time values matching the data values.
   * @param values An array holding the data values of the external signal.
   */
  public ExternalSignal(final double[] times, final double[] values){
    this.setData(times, values);
    _subCircuitPath = "";
  }

  /**
   * Constructor to initialize a signal with its name, its time values and its
   * data values.
   * @param name The name for the external signal.
   * @param times An array holding the time values matching the data values.
   * @param values An array holding the data values of the external signal.
   */
  public ExternalSignal(final String name, final double[] times, final double[] values, String subCircuitPath){
    this.setName(name);
    this.setData(times, values);
    _subCircuitPath = subCircuitPath;
  }

  /**
   * Sets the offset of the signal to [x, y]
   * @param x The x-axis offset.
   * @param y The y-axis offset.
   */
  public final void moveTo(final double x, final double y){
    this._offsetX = x;
    this._offsetY = y;
  }

  /**
   * Moves the offset of the signal by [x, y]
   * @param x Amount the x-axis offset is moved by.
   * @param y Amount the y-axis offset is moved by.
   */
  public final void moveBy(final double x, final double y){
    this._offsetX += x;
    this._offsetY += y;
  }

  // <editor-fold defaultstate="collapsed" desc="get, set, state">
  /**
   * Getter for the name of the external signal.
   * @return Returns the name of the external signal.
   */
  public final String getName(){
    return this._signalName;
  }

  /**
   * Getter for the offset of the signal as a Point2D object.
   * @return Returns the offset of the signal.
   */
  public final Point2D getPos(){
    return new Point2D.Double(this._offsetX, this._offsetY);
  }

  /**
   * Setter for the name of the external signal.
   * @param name The name for the external signal.
   */
  public final void setName(final String name){
    if(name != null){
      this._signalName = name;
    }
  }

  /**
   * Returns the number of data values of the external signal.
   * @return The number of data values of the external signal.
   */
  public final int size(){
    return this._signalTimes.length;
  }

  /**
   * Sets the time values and the data values for an external signal. Sets the
   * values _min and _max of this signal.
   * @param times
   * @param values
   */
  public final void setData(final double[] times, final double[] values){
    if(times == null || values == null || times.length != values.length){
      System.err.println("Error: Invalid params provided to setData in " + this.toString() + "!");
    }else{
      this._signalTimes = times.clone();
      this._signalValues = values.clone();
      double min = Double.POSITIVE_INFINITY;
      double max = Double.NEGATIVE_INFINITY;
      for(int i = 0; i < values.length; i++){
        if(values[i] < min){
          min = values[i];
        }
        if(values[i] > max){
          max = values[i];
        }
      }
      this._min = min;
      this._max = max;
    }
  }

  /**
   * Returns the data value of the external signal at index.
   * @param index The index of the data value to be returned.
   * @return The data value of the external signal at index.
   */
  public final double getValue(final int index){
    assert (index < this.size() && index >= 0) : "Error: Index out of bounds!";
    return this._signalValues[index] + this._offsetY;
  }

  /**
   * Returns the value of the external signal within the interval
   * [startTime,stopTime]
   * @param startTime The start time of the interval.
   * @param stopTime The stop time of the interval.
   * @return The value of the externalSignal within the interval as a float or
   * HiLoData Object
   */
  public final Object getValueInInterval(final double startTime, final double stopTime){
    Object returnValue = null;
    int nrOfValues = 0, firstIndex;
    double start, stop, step;
    float ret1;
    HiLoData ret2;
    if(this.size() > 0){
      if(startTime > stopTime){
        start = stopTime;
        stop = startTime;
      }else{
        start = startTime;
        stop = stopTime;
      }
      step = ((double)(this.getTime(this.size() - 1) - this.getTime(0))) / this.size();
      firstIndex = (int)((start - this.getTime(0)) / step);
      if(firstIndex < 0){
        firstIndex = 0;
      }
      if(firstIndex >= this.size()){
        firstIndex = this.size() - 1;
      }
      while(firstIndex < (this.size() - 1) && this.getTime(firstIndex) < start){
        firstIndex++;
      }
      while(firstIndex > 0 && this.getTime(firstIndex - 1) > start){
        firstIndex--;
      }
      for(int i = firstIndex; start <= this.getTime(i) && this.getTime(i) <= stop && i < (this.size() - 1); i++){
        nrOfValues++;
      }
      if(nrOfValues == 0){
        returnValue = null;
      }else if(nrOfValues == 1){
        ret1 = (float)this.getValue(firstIndex);
        returnValue = ret1;
      }else{
        ret2 = HiLoData.hiLoDataFabric((float)this.getValue(firstIndex), (float)this.getValue(firstIndex));
        for(int i = firstIndex + 1; (i - firstIndex) < nrOfValues; i++){
          ret2 = HiLoData.mergeFromValue(ret2, (float)this.getValue(i));
        }
        returnValue = ret2;
      }
    }
    return returnValue;
  }

  /**
   * Returns the minimum value and the maximum value of the external signal.
   * @return The maximum value of the external signal as a HiLoData object
   */
  public final HiLoData getMinMax(){
    return HiLoData.hiLoDataFabric((float)(this._min + this._offsetY), (float)(_max + this._offsetY));
  }

  /**
   * Returns the time value of the external signal at index.
   * @param index The index of the time value to be returned.
   * @return The time value of the external signal at index.
   */
  public final double getTime(final int index){
    assert (index < this.size() && index >= 0) : "Error: Index out of bounds!";
    return this._signalTimes[index] + this._offsetX;
  }
  // </editor-fold>

  /**
   * Implementation of getSignalName in AbstractScopeSignal returning the name
   * of the signal.
   * @return Returns the name of the external signal.
   */
  @Override
  public final String getSignalName(){
    return this.getName();
  }

    @Override
    public String getSubcircuitPath() {
        return _subCircuitPath;
    }
  
  
}
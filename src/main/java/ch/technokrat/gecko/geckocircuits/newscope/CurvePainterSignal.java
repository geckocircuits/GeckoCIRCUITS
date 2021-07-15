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

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 *
 * @author andy
 */
final class CurvePainterSignal extends AbstractCurvePainter{
  public static final double SGN_THRESHOLD = 0.5;
  private final Stack<SignalStateDrawer> _allDrawables = new Stack<SignalStateDrawer>();

  public CurvePainterSignal(final AbstractCurve curve){
    super(curve);
  }

  /*
   * get all data from the dataContainer (expensive operation!);
   */
  @Override
  public void reLoadData(final int minimumPixel, final int maximumPixel){
    synchronized(this){ // if we don't do this synchronized, we will get concurrent modifications of the curve painters.
      _allDrawables.clear();
      _allPoints.clear();
      loadDataRange(minimumPixel, maximumPixel);
    }
  }

  @Override
  public void loadDataRange(final int startPixel, final int stopPixel){      
    for(int i = startPixel; i < stopPixel; i++){
      final Object value = _ramData.getDataValueInInterval(_xAxis.getValueFromPixel(i),
                                                           _xAxis.getValueFromPixel(i + 1), _curve.getValueDataIndex());
      if(value == null){
        continue;
      }

      if(value instanceof HiLoData){
        loadHiLoDataValue((HiLoData)value, i);

      }else{
        loadFloatValue((Float)value, i);
      }
    }
  }

  @Override
  public void paintComponent(final Graphics2D g2d){
    synchronized(this){
      for(int i = 0; i < _allDrawables.size(); i++){
        _allDrawables.get(i).paintComponent(g2d, _curve);
      }
    }

    if(_curve.isSymbolEnabled()){
      final GeckoSymbol symbol = _curve.getSymbol();
      for(Point2D.Float pt : _allPoints){
        symbol.drawSymbol(g2d, pt.x, pt.y);
      }
    }

  }

  private void appendDataPixel(final boolean lowPixelValue, final boolean hiPixelValue, final int xPixelValue){
    synchronized(this){
      if(_allDrawables.empty()){
        _allDrawables.push(SignalStateDrawer.fabric(null, lowPixelValue, hiPixelValue, xPixelValue));
      }else{
        final SignalStateDrawer lastValue = _allDrawables.pop();
        final SignalStateDrawer newValue = SignalStateDrawer.fabric(lastValue, lowPixelValue, hiPixelValue, xPixelValue);
        if(!newValue.equals(lastValue)){
          _allDrawables.push(lastValue);
        }
        _allDrawables.push(newValue);
      }
    }
  }

  @Override
  public List<Axis> getSensitiveAxis(){
    return Arrays.asList(_xAxis);
  }

  private void loadHiLoDataValue(final HiLoData value, final int index){
    final HiLoData hiLow = value;
    final boolean lowPixelValue = hiLow._yHi > SGN_THRESHOLD;
    final boolean hiPixelValue = hiLow._yLo > SGN_THRESHOLD;
    appendDataPixel(lowPixelValue, hiPixelValue, index);

    if(_curve.isSymbolEnabled()){
      if(lowPixelValue){
        addValueToAllPoints(0f, index);
      }
      if(!hiPixelValue){
        addValueToAllPoints((float)CurveSignal.SGN_HEIGHT, index);
      }
    }

  }

  private void loadFloatValue(final Float value, final int index){
    if(value instanceof Float){
      final boolean signalValue = ((Float)value) > SGN_THRESHOLD;
      appendDataPixel(signalValue, signalValue, index);

      if(_curve.isSymbolEnabled()){
        if(signalValue){
          addValueToAllPoints((float)CurveSignal.SGN_HEIGHT, index);
        }else{
          addValueToAllPoints(0f, index);
        }
      }


    }
  }
}

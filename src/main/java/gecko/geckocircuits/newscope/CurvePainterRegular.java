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

import gecko.geckocircuits.datacontainer.DataContainerCompressable;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 *
 * @author andy
 */
class CurvePainterRegular extends AbstractCurvePainter{
  private final Stack<AbstractCurvePixelPainter> _allPainters = new Stack<AbstractCurvePixelPainter>();
  private static final float ZERO_TRANS_REPLACE = 0.15f;
  private static final int MAX_PIXEL_DRAW = 1000000;

  public CurvePainterRegular(final AbstractCurve curve){
    super(curve);
  }

  @Override
  public void paintComponent(final Graphics2D g2d){

    float transparency = (float)_curve._curveSettings._crvTransparency;
    if(transparency == 0){
      transparency = ZERO_TRANS_REPLACE;
    }
    final AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency);
    g2d.setComposite(alphaComposite);
    g2d.setColor(_curve._curveSettings._curveColor.getJavaColor());
    //_curvePoints.drawPath(g2d, _startIndex);        

    final Stroke oldStroke = g2d.getStroke();
    g2d.setStroke(_curve._curveSettings._curveLineStyle.stroke());


    synchronized(this){
      for(AbstractCurvePixelPainter painter : _allPainters){
        painter.paintComponent(g2d);
      }
    }

    g2d.setStroke(oldStroke);
    if(_curve.isSymbolEnabled()){
      final GeckoSymbol symbol = _curve.getSymbol();
      for(Point2D.Float pt : _allPoints){
        symbol.drawSymbol(g2d, pt.x, (float)_curve._yAxis.getPixelFromValue(pt.y));
      }
    }
  }

  /*
   * get all data from the dataContainer (expensive operation!);
   */
  @Override
  public void reLoadData(final int minimumPixel, final int maximumPixel){      
    synchronized(this){ // do this synchronized, otherwise we will get concurrent modificationss of _allPainters.
      _allPainters.clear();
      _allPoints.clear();        
      loadDataRange(minimumPixel, maximumPixel);        
    }
  }

  @Override
  public void loadDataRange(final int startPixel, final int stopPixel){                        
      
    final int pixelInterval = stopPixel - startPixel;
    if(pixelInterval > MAX_PIXEL_DRAW || pixelInterval < 0){
      return;
    }      
            
    for(int xPixel = startPixel; xPixel <= stopPixel; xPixel++){
        if(xPixel > MAX_PIXEL_DRAW) {
            return;
        }        
      final double x1Value = _xAxis.getValueFromPixel(xPixel - 1);        
      final double x2Value = _xAxis.getValueFromPixel(xPixel); 
              
      if(x1Value == 0 && x2Value == 0){
        continue;
      }

      if(_ramData == null) {
        return;
      }
                  
      final Object value = _ramData.getDataValueInInterval(x1Value, x2Value, _curve.getValueDataIndex());            
      
      if(value == null){
        continue;
      }
      
      AbstractCurvePixelPainter previousValue = null;
      if(_allPainters.size() > 0){
        previousValue = _allPainters.get(_allPainters.size() - 1);
      }

      AbstractCurvePixelPainter pixelPainter = null;
      switch(_curve.getLineType()) {
          case CONNECT_NEIGHBOURS:
               pixelPainter = AbstractCurvePixelPainter.curvePixelPainterFabric(xPixel, value, _curve._yAxis, previousValue);
              break;
          case BAR:
              pixelPainter = AbstractCurvePixelPainter.barPixelPainter(xPixel, value, _curve._yAxis);
              break;
          default:
              assert false;
      }
      
                                                                                                       

      addValueToAllPoints(value, xPixel);
      synchronized(this){
        _allPainters.add(pixelPainter);
      }      
    }
      
  }

  @Override
  public List<Axis> getSensitiveAxis(){
    return Arrays.asList(_xAxis, _curve._yAxis);
  }
}

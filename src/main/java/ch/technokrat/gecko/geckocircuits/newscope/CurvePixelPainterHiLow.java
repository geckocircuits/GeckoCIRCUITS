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
import java.awt.Stroke;

/**
 *
 * @author andy
 */
final class CurvePixelPainterHiLow extends AbstractCurvePixelPainter{
  private final float _lowPixelValue;
  private final float _hiPixelValue;

  public CurvePixelPainterHiLow(final float xPixel, final HiLoData value,
                                final Axis yAxis, final AbstractCurvePixelPainter previousValue){
    super(xPixel, yAxis, previousValue);
    final HiLoData hiLow = (HiLoData)value;
    final float lowPixelValue = (float)_yAxis.getPixelFromValue(hiLow._yLo);
    final float hiPixelValue = (float)_yAxis.getPixelFromValue(hiLow._yHi);

    _lowPixelValue = Math.min(lowPixelValue, hiPixelValue);
    _hiPixelValue = Math.max(lowPixelValue, hiPixelValue);

    _lastValue = new Float((lowPixelValue + hiPixelValue) / 2);
    findPath();
  }

  @Override
  void paintComponent(final Graphics2D g2d){
    super.paintComponent(g2d);
    if(_drawShape){
      final Stroke oldStroke = g2d.getStroke();
      g2d.setStroke(GeckoLineStyle.SOLID_PLAIN.stroke());
      g2d.drawLine((int)_xPixel, (int)_lowPixelValue, (int)_xPixel, (int)_hiPixelValue);
      g2d.setStroke(oldStroke);
    }
  }

  @Override
  void findPath(){
    if(_previousPixelPainter == null){ // fist element!
      return;
    }

    if(_previousPixelPainter instanceof CurvePixelPainterHiLow){
      // if two HiLo-Painters sit next to each other, 
      // and have a big overlap, dont continue path simplificationi
      final double overlap = calculateOverlap(this, (CurvePixelPainterHiLow)_previousPixelPainter);
      if(overlap > 1){
        return;
      }
    }

    _drawShape = false;

    // start new generalPath!
    if(_previousPixelPainter._generalPath == null){
      _previousPixelPainter._generalPath = new GeneralPathWrapper();
      _previousPixelPainter._generalPath.moveTo(_xPixel, _previousPixelPainter._lastValue);
    }


    _generalPath = _previousPixelPainter._generalPath;
    _previousPixelPainter._generalPath = null;
    assert _generalPath != null;
    _generalPath.lineTo(_xPixel, _lastValue);
    if(_previousPixelPainter._lastValue < _lastValue){
      _generalPath.lineTo(_xPixel, _hiPixelValue);
    }else{
      _generalPath.lineTo(_xPixel, _lowPixelValue);
    }


  }

  private static float calculateOverlap(final CurvePixelPainterHiLow dat1, final CurvePixelPainterHiLow dat2){
    float returnValue;
    returnValue = Math.min(dat1._hiPixelValue, dat2._hiPixelValue) - Math.max(dat1._lowPixelValue, dat2._lowPixelValue);
    if(returnValue < 0){
      returnValue = 0;
    }
    return returnValue;
  }

  @Override
  public String toString(){
    return super.toString() + " " + _xPixel + " " + _lowPixelValue + " " + _hiPixelValue;
  }
}

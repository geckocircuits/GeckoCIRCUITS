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
import java.awt.geom.Line2D;

/**
 *
 * @author andy
 */
final class CurvePixelPainterPointsLine extends AbstractCurvePixelPainter{
  CurvePixelPainterPointsLine(final float xPixel, final float value, final Axis yAxis,
                              final AbstractCurvePixelPainter previousValue){
    super(xPixel, yAxis, previousValue);
    if(_previousPixelPainter != null && _previousPixelPainter._lastValue != null){
      _paintShape = new Line2D.Float(_xPixel - 1, _previousPixelPainter._lastValue, _xPixel, value);
    }
    _lastValue = new Float(value);
    findPath();
  }

  @Override
  public void paintComponent(final Graphics2D g2d){
    super.paintComponent(g2d);
    if(_paintShape != null && _drawShape){
      g2d.draw(_paintShape);
    }
  }

  @Override
  void findPath(){
    _drawShape = false;

    if(_previousPixelPainter == null){
      _generalPath = new GeneralPathWrapper();
      _generalPath.moveTo(_xPixel, _lastValue);
      return;
    }

    // start new generalPath!
    if(_previousPixelPainter._generalPath == null){
      _previousPixelPainter._generalPath = new GeneralPathWrapper();
      _previousPixelPainter._generalPath.moveTo(_xPixel, _previousPixelPainter._lastValue);
    }


    _generalPath = _previousPixelPainter._generalPath;
    _previousPixelPainter._generalPath = null;
    assert _generalPath != null;
    _generalPath.lineTo(_xPixel, _lastValue);
  }
}

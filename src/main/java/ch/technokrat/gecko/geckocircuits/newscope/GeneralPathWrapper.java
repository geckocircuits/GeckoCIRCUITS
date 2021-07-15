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
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * This class "wraps" a GeneralPath for drawing. This is done since GeneralPath
 * is quite inefficient when drawing. the "lineTo"-Method first checks, if the
 * inserted point is "far away" from the previous line. If not, the last drawn
 * point is just moved to the newly inserted point.
 * @author andy
 */
final class GeneralPathWrapper{
  private final GeneralPath _generalPath = new GeneralPath();
  private Point2D _lastInsertPoint;
  private Point2D _lastPathpoint;

  void moveTo(final float xPixel, final float yPixel){
    _generalPath.moveTo(xPixel, yPixel);
    _lastInsertPoint = new Point2D.Float(xPixel, yPixel);
  }

  void paintSymbols(final Graphics2D g2d, final GeckoSymbol symbol){
    if(symbol == null){
      return;
    }

  }

  void lineTo(final float xPixel, final float yPixel){
    if(detectSimplification(xPixel, yPixel)){
      _lastInsertPoint = new Point2D.Float(xPixel, yPixel);
      //_generalPath.moveTo(xPixel, yPixel);
    }else{
      _lastPathpoint = _lastInsertPoint;
      _lastInsertPoint = new Point2D.Float(xPixel, yPixel);

      if(_lastPathpoint != null){
        _generalPath.lineTo(_lastPathpoint.getX(), _lastPathpoint.getY());
      }
    }

  }

  void draw(final Graphics2D g2d){
    g2d.draw(_generalPath);
    if(_lastInsertPoint != null && _lastPathpoint != null){
      final Line2D.Float lastLine = new Line2D.Float(_lastInsertPoint, _lastPathpoint);
      g2d.draw(lastLine);
    }

  }

  /*
   * if points are nearly on a line, and pixel distance < 10
   */
  private boolean detectSimplification(final float xPixel, final float yPixel){

    if(_lastPathpoint == null || _lastInsertPoint == null){
      return false;
    }

    final double lastSlope = Math.tan((_lastPathpoint.getY() - _lastInsertPoint.getY())
            / (_lastPathpoint.getX() - _lastInsertPoint.getX()));
    final double slope = Math.tan((_lastInsertPoint.getY() - yPixel) / (_lastInsertPoint.getX() - xPixel));

    // if slope direction is changed,(maximum/minimum), do a detailed plot
    if(Math.signum(slope) != Math.signum(lastSlope)){
      return false;
    }

    if(xPixel - _lastPathpoint.getX() > 2){
      final Point2D toInsert = new Point2D.Float(xPixel, yPixel);
      final double kruemmung = calculateInvCircleRadius(_lastInsertPoint, _lastPathpoint, toInsert);
      if(kruemmung > 0.1){
        return false;
      }
    }

    return true;
  }

  /*
   * Given three points, calculate the curvature of the circle through the three points
   */
  private double calculateInvCircleRadius(final Point2D point1, final Point2D point2, final Point2D point3){

    final double sideA = point1.distance(point2);
    final double sideB = point2.distance(point3);
    final double sideC = point3.distance(point1);
    final double sValue = 0.5 * (sideA + sideB + sideC);
    return sValue * (sValue - sideA) * (sValue - sideB) * (sValue - sideC);
  }
}

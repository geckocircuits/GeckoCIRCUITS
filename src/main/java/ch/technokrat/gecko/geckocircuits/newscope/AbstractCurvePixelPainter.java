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

import ch.technokrat.gecko.geckocircuits.control.PreviewDialogRectangular;
import java.awt.Graphics2D;
import java.awt.Shape;

/**
 *
 * @author andy
 */
abstract class AbstractCurvePixelPainter {

    private static final float HI_LOW_THRESHOLD = 1.5f;

    
    final float _xPixel;
    protected final Axis _yAxis;
    protected final AbstractCurvePixelPainter _previousPixelPainter;
    protected Float _lastValue;
    protected Shape _paintShape;
    protected boolean _drawShape = true;
    protected GeneralPathWrapper _generalPath = null;

    protected AbstractCurvePixelPainter(final float xPixel, final Axis yAxis,
            final AbstractCurvePixelPainter previousValue) {
        _xPixel = xPixel;
        _yAxis = yAxis;
        _previousPixelPainter = previousValue;
    }
    
    static AbstractCurvePixelPainter barPixelPainter(final int xPixel, Object value, Axis yAxis) {
        if (value instanceof HiLoData) {
            final HiLoData hiLow = (HiLoData) value;            
            if(hiLow._yLo > 0 && hiLow._yHi > 0) {
                HiLoData barPixelRepresentation = HiLoData.hiLoDataFabric(hiLow._yHi * 1e-3f, hiLow._yHi);
                return new CurvePixelPainterHiLow(xPixel, barPixelRepresentation, yAxis, (AbstractCurvePixelPainter) null);
            } else if(hiLow._yLo < 0 && hiLow._yHi < 0) {
                HiLoData barPixelRepresentation = HiLoData.hiLoDataFabric(hiLow._yLo, hiLow._yLo * 1e-3f);
                return new CurvePixelPainterHiLow(xPixel, barPixelRepresentation, yAxis, (AbstractCurvePixelPainter) null);
            } else {
                return new CurvePixelPainterHiLow(xPixel, hiLow, yAxis, (AbstractCurvePixelPainter) null);
            }            
        } else {
            if (value instanceof Float) {
                HiLoData barPixelRepresentation = null;
                float floatValue = (Float) value;                
                barPixelRepresentation = HiLoData.hiLoDataFabric(floatValue, floatValue * 1e-3f);                                 
                return new CurvePixelPainterHiLow(xPixel, barPixelRepresentation, yAxis, (AbstractCurvePixelPainter) null);                        
            }
        }

        assert false : value.getClass();
        return null;
    }

    public static AbstractCurvePixelPainter curvePixelPainterFabric(final float xPixel, final Object value,
            final Axis yAxis, final AbstractCurvePixelPainter previousValue) {

        if (value instanceof HiLoData) {
            final HiLoData hiLow = (HiLoData) value;
            final float lowPixelValue = (float) yAxis.getPixelFromValue(hiLow._yLo);
            final float hiPixelValue = (float) yAxis.getPixelFromValue(hiLow._yHi);
            if (Math.abs(lowPixelValue - hiPixelValue) > HI_LOW_THRESHOLD) { // real range is given
                return new CurvePixelPainterHiLow(xPixel, hiLow, yAxis, previousValue);
            } else { // otherwise high and low pixel value are identical, return point representation
                if (previousValue == null || previousValue._lastValue == null) {
                    return new CurvePixelPainterHiLow(xPixel, hiLow, yAxis, previousValue);
                } else {
                    return new CurvePixelPainterPointsLine(xPixel, (hiPixelValue + lowPixelValue) / 2, yAxis,
                            previousValue);
                }
            }

        } else {
            if (value instanceof Float) {
                return new CurvePixelPainterPointsLine(xPixel, (float) yAxis.getPixelFromValue((Float) value),
                        yAxis, previousValue);
            }
        }

        assert false : value.getClass();
        return null;
    }

    void paintComponent(final Graphics2D g2d) {
        if (_generalPath != null) {
            _generalPath.draw(g2d);
        }
    }

    abstract void findPath();
}

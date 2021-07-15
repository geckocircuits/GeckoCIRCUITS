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

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * this is a helper class, to draw the signal curve efficiently. The constructor is private, since the values should be obtained
 * over the fabric. If subsequent pixel states are the same, the last element ist returned with extended interval range.
 *
 * @author andy
 */
final class SignalStateDrawer {

    final DrawState _drawState;
    final int _xPixelStart;
    int _xPixelEnd;
    private boolean _paintTransitionLine;

    private enum DrawState {

        OFF,
        ON,
        ON_OFF;
    }

    public static SignalStateDrawer fabric(final SignalStateDrawer lastValue, final boolean lowPixelValue,
            final boolean hiPixelValue, final int xPixelValue) {

        DrawState newState = null;

        if (lowPixelValue != hiPixelValue) {
            newState = DrawState.ON_OFF;
        }

        if (lowPixelValue && hiPixelValue) {
            newState = DrawState.ON;
        }

        if (!lowPixelValue && !hiPixelValue) {
            newState = DrawState.OFF;
        }

        assert newState != null;

        SignalStateDrawer returnValue;
        if (lastValue == null) {
            returnValue = new SignalStateDrawer(newState, xPixelValue, xPixelValue);
        } else {
            returnValue = findNewPixelDrawer(lastValue, newState, xPixelValue);
        }

        return returnValue;
    }

    private static SignalStateDrawer findNewPixelDrawer(final SignalStateDrawer lastValue, final DrawState newState,
            final int xPixelValue) {
        SignalStateDrawer returnValue;
        if (lastValue._drawState == newState) {
            lastValue.setEndPixel(xPixelValue);
            returnValue = lastValue;
        } else {
            // signal type changed, create new object
            assert newState != lastValue._drawState;
            returnValue = new SignalStateDrawer(newState, lastValue._xPixelEnd + 1, xPixelValue);
        }
        if (detectTransitionLine(returnValue._drawState, lastValue._drawState)) {
            returnValue._paintTransitionLine = true;
        }
        return returnValue;
    }

    @Override
    public String toString() {
        return _drawState + " " + _xPixelStart + "  " + _xPixelEnd;
    }

    private void setEndPixel(final int pixelValue) {
        _xPixelEnd = pixelValue;
    }

    private SignalStateDrawer(final DrawState state, final int startPixel, final int stopPixel) {
        _drawState = state;
        _xPixelStart = startPixel;
        _xPixelEnd = stopPixel;
    }

    private static boolean detectTransitionLine(final DrawState myDrawState, final DrawState oldDrawState) {
        if (oldDrawState == DrawState.OFF && myDrawState == DrawState.ON) {
            return true;
        }

        if (oldDrawState == DrawState.ON && myDrawState == DrawState.OFF) {
            return true;
        }
        return false;
    }

    void paintComponent(final Graphics2D g2d, final AbstractCurve signalCurve) {
        final int signalHeight = CurveSignal.SGN_HEIGHT;

        switch (_drawState) {
            case OFF:
                g2d.setColor(signalCurve._curveSettings._curveColor.getJavaColor());
                g2d.drawLine(_xPixelStart, 0, _xPixelEnd, 0);
                if (_paintTransitionLine) {
                    g2d.drawLine(_xPixelStart, 0, _xPixelStart, signalHeight);
                }
                break;
            case ON_OFF:
                g2d.setColor(signalCurve._curveSettings._curveColor.getJavaColor());
                g2d.fillRect(_xPixelStart, -1, _xPixelEnd - _xPixelStart + 1, signalHeight + 1);

                // paint shading
                final Color oldColor = g2d.getColor();
                final Color shadedColor = new Color(findModifiedColor(oldColor.getRed()), findModifiedColor(oldColor.getGreen()),
                        findModifiedColor(oldColor.getBlue()));
                g2d.setColor(shadedColor);
                for (int xPix = _xPixelStart + 1; xPix < _xPixelEnd; xPix += 2) {
                    g2d.drawLine(xPix, 1, xPix, signalHeight - 1);
                }
                break;
            case ON:
                g2d.setColor(Color.lightGray);
                g2d.fillRect(_xPixelStart, -1, _xPixelEnd - _xPixelStart + 1, signalHeight);
                g2d.setColor(signalCurve._curveSettings._curveColor.getJavaColor());
                signalCurve._curveSettings._curveLineStyle.setStrokeStyle(g2d);
                if (_paintTransitionLine) {
                    g2d.drawLine(_xPixelStart, 0, _xPixelStart, signalHeight);
                }
                g2d.drawLine(_xPixelStart, signalHeight, _xPixelEnd, signalHeight);
                break;
            default:
                assert false;
        }
    }
    
    private int findModifiedColor(int colorBelow255) {
         int returnValue = colorBelow255 * 6 / 5;
         if(returnValue > 255) {
             returnValue = colorBelow255 * 5 / 6;
         }
         return returnValue;
    }
}

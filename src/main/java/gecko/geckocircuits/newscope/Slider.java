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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

/**
 *
 * @author andy
 */
class Slider {

    private final Color _sliderColor;
    private double _xSliderValue;
    private int _xSliderPix;
    private int _lastPaintedX;
    private static final int MAX_HEIGHT = 2000;
    AbstractDiagram _parentDiagram;    
    
    public Slider(final Color color) {
        super();
        _sliderColor = color;        
    }

    Slider(final Color color, final AbstractDiagram parentDiagram) {
        this(color);
        _parentDiagram = parentDiagram;        
    }

    void paintComponent(final Graphics graphics, final int yMin, final int yMax, final Axis xAxis) {
        graphics.setColor(_sliderColor);
        final int xSPix = (int) xAxis.getPixelFromValue(_xSliderValue);
        graphics.drawLine(xSPix, yMin, xSPix, yMax);
        if(_lastPaintedX != xSPix) {
            _lastPaintedX = xSPix;
        }                
    }

    public void setXSliderValue(final double value) {
        _xSliderValue = value;        
    }

    public double getXSliderValue() {
        return _xSliderValue;
    }

    public int getXSliderPix() {
        return _xSliderPix;
    }
       
    public Color getColor() {
        return _sliderColor;
    }

    public void setXSliderPix(final int pixelValue, final Axis xAxis, final GraferV4 repaintComponent) {
        int checkedPixelValue = pixelValue;
        // do a range check:
        checkedPixelValue = Math.max(checkedPixelValue, xAxis._axisOriginPixel.x);
        checkedPixelValue = Math.min(checkedPixelValue, xAxis._axisOriginPixel.x + xAxis.getAxisLengthPixel());
        _xSliderPix = checkedPixelValue;
        setXSliderValue(xAxis.getValueFromPixel(_xSliderPix));

        // this is a graphics optimization, since drawing the whole plot during sliding is too time consuming!
        final int startXPaint = Math.min(_xSliderPix, _lastPaintedX)-1;
        final int xWidth = Math.max(_xSliderPix, _lastPaintedX) - startXPaint + 2;
        for(Component comp : repaintComponent.getComponents()) {
            if(comp instanceof AbstractDiagram) {
                repaintComponent.repaint(startXPaint, 0, xWidth, MAX_HEIGHT);                                                     
            } else {
                comp.repaint();
            }            
        }
        
    }
    
    
}

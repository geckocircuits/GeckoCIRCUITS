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
import java.awt.Component;
import java.awt.Graphics;

/**
 *
 * @author Zimmi
 */
public final class HorizontalLevel {

    private final Color _levelColor;
    private double _yLevelValue;
    private int _lastPaintedY;
    AbstractDiagram _parentDiagram;    
    
    public HorizontalLevel(final Color color) {
        super();
        _levelColor = color;        
    }

    HorizontalLevel(final Color color, final AbstractDiagram parentDiagram) {
        this(color);
        _parentDiagram = parentDiagram;        
    }
    
    public void setYLevel(final double ylevel) {
        _yLevelValue = ylevel;
    }

    void paintComponent(final Graphics graphics, final Axis xAxis, final Axis yAxis) {
        graphics.setColor(_levelColor);
        final int ySPix = (int) yAxis.getPixelFromValue(_yLevelValue);
        
        final HiLoData limits = xAxis._axisMinMax.getLimits();
        final int xmin = (int) xAxis.getPixelFromValue((double) limits._yLo);
        final int xmax = (int) xAxis.getPixelFromValue((double) limits._yHi);
        
        graphics.drawLine(xmin, ySPix, xmax, ySPix);

        if(_lastPaintedY != ySPix) {
            _lastPaintedY = ySPix;
        }    
        
    }      
}


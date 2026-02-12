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

import gecko.geckocircuits.newscope.AbstractDiagram;
import gecko.geckocircuits.newscope.Axis;
import gecko.geckocircuits.newscope.HiLoData;
import java.awt.Color;
import java.awt.Graphics;

/**
 *
 * @author Zimmi
 */
public class TriggerPosition {
    
    private final Color _levelColor;
    private double _timeXValue;
    AbstractDiagram _parentDiagram;    
    
    public TriggerPosition(final Color color) {
        super();
        _levelColor = color;        
    }

    TriggerPosition(final Color color, final AbstractDiagram parentDiagram) {
        this(color);
        _parentDiagram = parentDiagram;        
    }
    
    public void setXPos(final double time) {
        _timeXValue = time;
    }

    void paintComponent(final Graphics graphics, final Axis xAxis, final Axis yAxis) {
        graphics.setColor(_levelColor);
        final int xpos = (int) xAxis.getPixelFromValue(_timeXValue);
        
        final HiLoData limits = yAxis._axisMinMax.getLimits();
        final int ymin = (int) yAxis.getPixelFromValue((double) limits._yLo);
        final int ymax = (int) yAxis.getPixelFromValue((double) limits._yHi);
        
        graphics.drawLine(xpos, ymin, xpos, ymax);
        
    }        
       
}



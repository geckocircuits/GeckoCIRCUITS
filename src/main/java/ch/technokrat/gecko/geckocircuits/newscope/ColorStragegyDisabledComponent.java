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

public class ColorStragegyDisabledComponent implements ColorSettable {
    private static final int ADDITIONAL_GREY = 15;
    private static final int COLOR_DIVISOR = 5;
    private static final int MAX_VAL = 255;
    
    @Override
    public void setColor(final Color color, final Graphics2D g2d) {
        int redInt = calclateWeakColorComponent(color.getRed());
        int greenInt = calclateWeakColorComponent(color.getGreen());
        int blueInt = calclateWeakColorComponent(color.getBlue());
        
        blueInt = limitToMinMaxRange(blueInt);
        greenInt = limitToMinMaxRange(greenInt);
        redInt = limitToMinMaxRange(redInt);
        
        final Color disabledColor = new Color(redInt, greenInt, blueInt);
        g2d.setColor(disabledColor);
    }    

    private int calclateWeakColorComponent(final int originalColor) {        
        return MAX_VAL - (MAX_VAL - originalColor) / COLOR_DIVISOR - ADDITIONAL_GREY;
    }

    private int limitToMinMaxRange(final int colorIntValue) {
        int limitedValue = Math.min(MAX_VAL, colorIntValue);
        return Math.max(0, limitedValue);        
    }
}

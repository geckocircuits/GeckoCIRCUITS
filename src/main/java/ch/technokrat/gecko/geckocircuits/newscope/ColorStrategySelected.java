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
 *
 * @author andreas
 */
public class ColorStrategySelected implements ColorSettable {
    private static final int MAX_COL_VALUE = 255;
    private static final int DIVIDER = 2;
    @Override
    public void setColor(final Color color, final Graphics2D g2d) {
        int sub = 0;

        int totalSum = color.getRed() + color.getGreen() + color.getBlue();
        totalSum /= DIVIDER; 

        // make all colors grey
        int red = totalSum;
        int green = totalSum;
        int blue = totalSum;

        blue = Math.min(MAX_COL_VALUE, blue);
        blue = Math.max(0, blue);

        red = Math.min(MAX_COL_VALUE, red);
        red = Math.max(0, red);

        green = Math.min(MAX_COL_VALUE, green);
        green = Math.max(0, green);
        
        
        final Color disabledColor = new Color(red, green, blue);
        g2d.setColor(disabledColor);
    }
}

/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class TerminalRelativePositionReluctance extends TerminalRelativePosition {

    public TerminalRelativePositionReluctance(AbstractBlockInterface parent, int poxX, int posY) {
        super(parent, poxX, posY);
    }

    @Override
    public void paintComponent(Graphics graphics) {
        Color origColor = graphics.getColor();                
        graphics.setColor(GlobalColors.farbeElementRELFOREGROUND);
        super.paintComponent(graphics);
        graphics.setColor(origColor);
    }
    
    public void paintLabelString(final Graphics2D graphics, final int dpix) {
        Color origColor = graphics.getColor();                
        graphics.setColor(GlobalColors.farbeElementRELFOREGROUND);
        if (!_label.getLabelString().isEmpty()) {
            graphics.drawString(_label.getLabelString(), (int) (dpix * getPosition().x) + DX_IN, (int) (dpix * getPosition().y) + DY_TEXT);
        }
        graphics.setColor(origColor);
    }
    
    
    
}

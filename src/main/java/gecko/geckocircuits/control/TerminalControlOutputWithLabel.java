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
package gecko.geckocircuits.control;

import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.TerminalControlOutput;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

class TerminalControlOutputWithLabel extends TerminalControlOutput {
    private final String _magentaLabel;

    public TerminalControlOutputWithLabel(AbstractBlockInterface parentComponent, int posX, int posY, String label) {
        super(parentComponent, posX, posY);
        _magentaLabel = label;
    }    

    @Override
    public void paintLabelString(final Graphics2D graphics) {
        final int dpix = AbstractBlockInterface.dpix;
        super.paintLabelString(graphics); //To change body of generated methods, choose Tools | Templates.
                
        final Font orig = graphics.getFont();
        graphics.setFont(TerminalControlInputWithLabel.getReducedSizeFont(orig));
        
        int rightAlignShift = graphics.getFontMetrics().stringWidth(_magentaLabel);
        final int xPos = (int) (dpix * getPosition().x + dpix * 0.75) - 5 - rightAlignShift + graphics.getFont().getSize() / 3;
        final int yPos = TerminalControlInputWithLabel.getYFontPosition(this, graphics);
                
        final Color origColor = graphics.getColor();
        graphics.setColor(Color.magenta);
        
        
        graphics.drawString(_magentaLabel, xPos - (int) (1.2 * dpix) , yPos);
        graphics.setColor(origColor);                
        graphics.setFont(orig);
    }                        
}

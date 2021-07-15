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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalControlInput;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

class TerminalControlInputWithLabel extends TerminalControlInput {
    private final String _magentaLabel;

        
    
    public TerminalControlInputWithLabel(AbstractBlockInterface parentComponent, int posX, int posY, String label) {
        super(parentComponent, posX, posY);
        _magentaLabel = label;
    }    

    @Override
    public void paintLabelString(final Graphics2D graphics) {        
        final int dpix = AbstractBlockInterface.dpix;
        super.paintLabelString(graphics); //To change body of generated methods, choose Tools | Templates.                        
        Font origFont = graphics.getFont();
        graphics.setFont(getReducedSizeFont(origFont));
        final int xPos = (int) (_parentElement.dpix * getPosition().x + _parentElement.dpix * 1.75)+2;
        final int yPos = getYFontPosition(this, graphics);
        
        final Color origColor = graphics.getColor();
        graphics.setColor(Color.magenta);
        
        graphics.drawString(_magentaLabel, xPos - (int) (dpix * 1.2) , yPos);
        graphics.setColor(origColor);                
        graphics.setFont(origFont);
    }                        
    
    public static int getYFontPosition(final AbstractTerminal terminal, final Graphics2D graphics) {
        final int fontHeightThird = graphics.getFontMetrics().getHeight() / 3;         
        return (int) (terminal._parentElement.dpix * terminal.getPosition().y) + fontHeightThird-1;        
    }
    
    
    public static Font getReducedSizeFont(final Font orig) {
        int originalSize = orig.getSize();
        int newSize = Math.max(6, originalSize * 3 / 4);
        return new Font(orig.getFontName(), orig.getStyle(),  newSize);        
    }
    
}

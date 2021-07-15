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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;

public class TerminalControlOutput extends TerminalControl {

    public TerminalControlOutput(AbstractBlockInterface relatedComponent, int posX, int posY) {
        super(relatedComponent, posX, posY);
    }

    @Override
    public void paintComponent(final Graphics graphics) {
        final int dpix = AbstractCircuitSheetComponent.dpix;
        Graphics2D g2d = (Graphics2D) graphics;
        
        final int[] xFl = new int[3];
        final int[] yFl = new int[3];

        final int pFa = (int) (0.7 * dpix);
        final int pFb = (int) (0.3 * dpix);
        final int x = getPosition().x * dpix;
        final int y = getPosition().y * dpix;
        
        xFl[0] = x + 0;
        xFl[1] = xFl[0] - pFa;
        xFl[2] = xFl[1];
        yFl[0] = y;
        yFl[1] = yFl[0] - pFb;
        yFl[2] = yFl[0] + pFb;

        g2d.drawPolygon(xFl, yFl, 3);                
    }                
    
    @Override
    public AbstractTerminal createCopy(AbstractBlockInterface relatedComponent) {
        AbstractTerminal returnValue =  new TerminalControlOutput(relatedComponent, _posX, _posY);
        returnValue.getLabelObject().setLabel(_label.getLabelString());
        return returnValue;
    }
    
    @Override
    public void paintLabelString(final Graphics2D graphics2D) {        
        final int dpix = _parentElement.dpix;
        FontRenderContext frc = graphics2D.getFontRenderContext();
        
        final int stringLength = (int) graphics2D.getFont().getStringBounds(_label.getLabelString(), frc).getWidth();
        
        if (!_label.getLabelString().isEmpty()) {
            graphics2D.drawString(_label.getLabelString(), (int) (dpix * getPosition().x), 
                    (int) (dpix * getPosition().y) + DY_TEXT);
        }
    }           
    
}

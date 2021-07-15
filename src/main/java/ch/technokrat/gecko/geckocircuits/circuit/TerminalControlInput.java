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

/**
 *
 * @author andreas
 */
public class TerminalControlInput extends TerminalControl {

    public TerminalControlInput(AbstractBlockInterface relatedComponent, int posX, int posY) {
        super(relatedComponent, posX, posY);
    }    
    
    
    @Override
    public void paintComponent(Graphics graphics) {
        final int dpix = AbstractCircuitSheetComponent.dpix;
        graphics.drawLine( dpix * getPosition().x, dpix * getPosition().y, 
                _parentElement.getSheetPosition().x * dpix, dpix * getPosition().y);
        super.paintComponent(graphics);
    }
    
    @Override
    public AbstractTerminal createCopy(AbstractBlockInterface relatedComponent) {
        AbstractTerminal returnValue =  new TerminalControlInput(relatedComponent, _posX, _posY);
        returnValue.getLabelObject().setLabel(_label.getLabelString());
        return returnValue;
    }   
    
    @Override
    public void paintLabelString(final Graphics2D graphics2D) {                
        FontRenderContext frc = graphics2D.getFontRenderContext();        
        final int stringLength = (int) graphics2D.getFont().getStringBounds(_label.getLabelString(), frc).getWidth();        
        int xShift = 0;
        if(!_hasDoubleLabel) {
            xShift = - stringLength;
        }
        if (!_label.getLabelString().isEmpty()) {
            graphics2D.drawString(_label.getLabelString(), (int) (_parentElement.dpix * getPosition().x) + xShift, 
                    (int) (_parentElement.dpix * getPosition().y) + DY_TEXT);
        }
    }
    
    
}

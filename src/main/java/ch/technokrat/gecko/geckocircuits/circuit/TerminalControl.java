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

import ch.technokrat.gecko.geckocircuits.control.RegelBlock;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class TerminalControl extends TerminalRelativePosition implements ControlTerminable {
    /**
     * _nodeNumber is a unique index for every control potential. this means, two terminals have the
     * same node-number when they have some direct connection within the netlist.
     */
    private int _nodeNumber = -1;    
    
    public TerminalControl(AbstractBlockInterface relatedComponent, int posX, int posY) {
        super(relatedComponent, posX, posY);        
    }        
    
    @Override
    public int getNodeNumber() {
        return _nodeNumber;
    }
    
    @Override
    public void setNodeNumber(final int newValue) {
        _nodeNumber = newValue;
    }
    
    @Override
    public void clearNodeNumber() {
        _nodeNumber = -1;
    }                                   
    
    public void paintControlState(Graphics2D graphics) {
        Color oldColor = graphics.getColor();
        final int CIRCLE_DIAMETER = 6;
        RegelBlock _parentRegler = (RegelBlock) _parentElement;
        
        if(_parentRegler._calculator == null) {
            return;
        }
        int index = _parentRegler.YOUT.indexOf(this);
        double value = 0;
        if(index < 0) {
            index = _parentRegler.XIN.indexOf(this);            
            value = _parentRegler._calculator._inputSignal[index][0];
        } else {
            value = _parentRegler._calculator._outputSignal[index][0];
        }
        if(index < 0) {
            return;
        }
        
        
        
        final int dpix = _parentElement.dpix;
        if(value == 0) {
            graphics.setColor(Color.LIGHT_GRAY);
            graphics.fillOval(_parentElement.dpix * getPosition().x - CIRCLE_DIAMETER/2, 
                dpix * getPosition().y-CIRCLE_DIAMETER/2, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
            graphics.setColor(Color.GRAY);
            graphics.drawOval(_parentElement.dpix * getPosition().x - CIRCLE_DIAMETER/2, 
                dpix * getPosition().y-CIRCLE_DIAMETER/2, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
        } else if(value == 1) {
            graphics.setColor(Color.BLUE);
            graphics.fillOval(_parentElement.dpix * getPosition().x - CIRCLE_DIAMETER/2, 
                dpix * getPosition().y-CIRCLE_DIAMETER/2, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
        } else {
            
            String valueString = "" + value;
            if(value < 0) {
                valueString = valueString.substring(0, 2);
            } else {
                valueString = valueString.substring(0, 1);
            }
            //graphics.setColor(Color.GREEN);
            graphics.fillOval(_parentElement.dpix * getPosition().x - CIRCLE_DIAMETER/2, 
                _parentElement.dpix * getPosition().y-2 * CIRCLE_DIAMETER/2, 2 * CIRCLE_DIAMETER, 2 * CIRCLE_DIAMETER);
            graphics.setColor(Color.WHITE);
            Font oldFont = graphics.getFont();
            graphics.setFont(new Font("Arial", Font.PLAIN, 10));
            graphics.drawString(valueString, dpix * getPosition().x-1, dpix * getPosition().y+3);
            graphics.setFont(oldFont);
        }
        
        
        graphics.setColor(oldColor);
    }

}

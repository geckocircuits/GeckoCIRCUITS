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

import ch.technokrat.gecko.geckocircuits.control.Point;

/**
 *
 * @author andreas
 */
public class TerminalRelativePosition extends AbstractTerminal {
    protected int _posX;
    protected int _posY;
    
    public TerminalRelativePosition(AbstractBlockInterface relatedComponent, int posX, int posY) {
        super(relatedComponent);
        _posX = posX;
        _posY = posY;
    } 
        

    public int getRelativeX() {
        return _posX;
    }
    
    public int getRelativeY() {
        return _posY;
    }
    
    @Override
    public Point getPosition() {
        return getPointFromDirection(_parentElement.getComponentDirection(), _parentElement.getSheetPosition(), _posX, _posY);
    }           
    
    public static Point getPointFromDirection(ComponentDirection direction, Point center, int posX, int posY) {
        
        
        int _termDist = 5;
        int x1 = center.x, y1 = center.y;
        switch (direction) {
            case NORTH_SOUTH:
                y1 -= posY;
                x1 += posX;
                break;
            case EAST_WEST:
                y1 += posX;
                x1 += posY;
                break;            
            case SOUTH_NORTH:
                y1 += posY;
                x1 -= posX;
                break;
            case WEST_EAST:
                y1 -= posX;
                x1 -= posY;                
                break;
             default:
                assert false;
        }

        return new Point(x1, y1);
    }

    @Override
    public AbstractTerminal createCopy(AbstractBlockInterface relatedComponent) {
        AbstractTerminal returnValue =  new TerminalRelativePosition(relatedComponent, _posX, _posY);
        returnValue.getLabelObject().setLabel(_label.getLabelString());
        return returnValue;
    }
    
}
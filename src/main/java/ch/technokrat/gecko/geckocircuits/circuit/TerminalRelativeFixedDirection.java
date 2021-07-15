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
public final class TerminalRelativeFixedDirection extends AbstractTerminal {
    private final int _posX;
    private final int _posY;
    private final ComponentDirection _fixedDirection;
    
    public TerminalRelativeFixedDirection(final AbstractBlockInterface relatedComponent, final int posX, final int posY) {
        super(relatedComponent);
        _posX = posX;
        _posY = posY;
        _fixedDirection = _parentElement.getComponentDirection();
    }

    @Override
    public Point getPosition() {
        return getPointFromDirection(_fixedDirection, _parentElement.getSheetPosition(), _posX, _posY);
    }           
    
    public static Point getPointFromDirection(final ComponentDirection direction, final Point center, 
            final int posX, final int posY) {                
        final int _termDist = 5;
        int x1 = center.x, y1 = center.y;
        switch (direction) {
            case NORTH_SOUTH:
                y1 -= posY;
                x1 += posX;
                break;
            case EAST_WEST:
                y1 -= posX;
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
    public AbstractTerminal createCopy(final AbstractBlockInterface relatedComponent) {
        final AbstractTerminal returnValue = new TerminalRelativeFixedDirection(relatedComponent, _posX, _posY);
        returnValue.getLabelObject().setLabel(_label.getLabelString());
        return returnValue;
    }                
}

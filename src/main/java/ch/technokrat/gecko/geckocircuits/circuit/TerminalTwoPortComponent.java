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
import java.awt.Color;
import java.awt.Graphics;

public final class TerminalTwoPortComponent extends AbstractTerminal {

    private final int _termDist;
    
    /**
     * a two-Terminal component should only show 1 flow symbol.
     */
    boolean _isFlowSymbolTerminal = false;    
    
    
    
    public TerminalTwoPortComponent(AbstractBlockInterface relatedComponent, int termDist) {
        super(relatedComponent);
        _termDist = termDist;
    }

    public static Point getPointFromDirection(ComponentDirection direction, Point center, int _termDist) {
        int x1 = center.x, y1 = center.y;
        switch (direction) {
            case NORTH_SOUTH:
                y1 += _termDist;
                break;
            case SOUTH_NORTH:
                y1 -= _termDist;
                break;
            case WEST_EAST:
                x1 += _termDist;
                break;
            case EAST_WEST:
                x1 -= _termDist;
                break;
            default:
                assert false;
        }

        return new Point(x1, y1);
    }

    @Override
    public Point getPosition() {
        return getPointFromDirection(_parentElement.getComponentDirection(), _parentElement.getSheetPosition(), _termDist);
    }

    @Override
    public AbstractTerminal createCopy(AbstractBlockInterface relatedComponent) {
        final TerminalTwoPortComponent returnValue = new TerminalTwoPortComponent(relatedComponent, _termDist);
        returnValue.getLabelObject().setLabel(_label.getLabelString());
        returnValue._isFlowSymbolTerminal = _isFlowSymbolTerminal;
        return returnValue;
    }
    

    @Override
    public void paintComponent(Graphics graphics) {
        final int dpix = AbstractCircuitSheetComponent.dpix;
        if (_isFlowSymbolTerminal && _parentElement.getDisplayProperties().showFlowSymbol) {
            paintFlowSymbol(dpix, graphics);
        }


        super.paintComponent(graphics);
    }        
    
    public final void setIsFlowSymbolTerminal(final boolean value) {
        _isFlowSymbolTerminal = value;
    }
}

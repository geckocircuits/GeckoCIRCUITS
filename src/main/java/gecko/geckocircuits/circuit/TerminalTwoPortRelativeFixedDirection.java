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
package gecko.geckocircuits.circuit;

import gecko.geckocircuits.control.Point;

/**
 *
 * @author andreas
 */
public class TerminalTwoPortRelativeFixedDirection extends AbstractTerminal {
    private final int _termDist;
    private final ComponentDirection _fixedDirection;
    
    public TerminalTwoPortRelativeFixedDirection(AbstractBlockInterface relatedComponent, int termDist, ComponentDirection fixedDirection) {
        super(relatedComponent);
        _termDist = termDist;                        
        _fixedDirection = fixedDirection; 
        
    }

    public Point getPosition() {
        return TerminalTwoPortComponent.getPointFromDirection(_fixedDirection, _parentElement.getSheetPosition(), _termDist);
    }

    @Override
    public AbstractTerminal createCopy(AbstractBlockInterface relatedComponent) {
        AbstractTerminal returnValue =  new TerminalTwoPortRelativeFixedDirection(relatedComponent, _termDist, _fixedDirection);
        returnValue.getLabelObject().setLabel(_label.getLabelString());
        return returnValue;
    }
}

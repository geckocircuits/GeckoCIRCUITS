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
import java.awt.Graphics;
import java.awt.Graphics2D;

public class TerminalFixedPositionInvisible extends TerminalFixedPosition {

    public TerminalFixedPositionInvisible(final AbstractBlockInterface parent, final Point position) {
        super(parent, position);
    }
    
    
    // do not paint this terminal! It is used from ther ThermPvChip-Object
        // or other thermal components to reference to the zero temperatre.
        // exporting the view to svg-images makes a regular terminal still
        // visible, therefore this workaround-class is used here!
    
    @Override
    public void paintComponent(final Graphics graphics) {
        // nothing to paint!
    }
    
    public void paintLabelString(final Graphics2D graphics, final int dpix) {
        // nothing to paint!!!
    }
    
    
}

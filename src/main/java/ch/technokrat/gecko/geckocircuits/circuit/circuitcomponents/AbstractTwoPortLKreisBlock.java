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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import static ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalTwoPortComponent;
import java.awt.Graphics2D;

public abstract class AbstractTwoPortLKreisBlock extends AbstractCircuitBlockInterface {
    private static final int TWO_PORT_DIST = 2;            

    public AbstractTwoPortLKreisBlock() {
        createTwoPortTerminals();
    } 
    
    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        graphics.drawLine(0, dpix * 2, 0, -dpix * 2);        
    }
        
    private void createTwoPortTerminals() {
        XIN.add(new TerminalTwoPortComponent(this, -TWO_PORT_DIST));
        final TerminalTwoPortComponent outTerminal = new TerminalTwoPortComponent(this, TWO_PORT_DIST);
        outTerminal.setIsFlowSymbolTerminal(true);
        YOUT.add(outTerminal);

    }
    
}

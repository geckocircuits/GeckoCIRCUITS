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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.circuit.DialogGlobalTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.GlobalTerminable;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalHiddenSubcircuit;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalInterface;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalTwoPortComponent;
import ch.technokrat.gecko.geckocircuits.control.Point;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractCircuitGlobalTerminal extends AbstractCircuitBlockInterface implements GlobalTerminable {    
    public static final Map<AbstractComponentTyp, HashSet<AbstractCircuitGlobalTerminal>> ALL_GLOBALS = 
            new HashMap<AbstractComponentTyp, HashSet<AbstractCircuitGlobalTerminal>>();

    public AbstractCircuitGlobalTerminal() {
        super();
        XIN.add(new TerminalTwoPortComponent(this, 0));     
        
        // this is only a dummy terminal, so that we get the correct label name 
        // when loading from file. It will be replaced when the references are
        // set correctly.
        YOUT.add(new TerminalHiddenSubcircuit(this));
        if(!ALL_GLOBALS.containsKey(getCircuitTyp())) {
            ALL_GLOBALS.put(getCircuitTyp(), new HashSet<AbstractCircuitGlobalTerminal>());
        } 
        
        ALL_GLOBALS.get(getCircuitTyp()).add(this);        
    }
        
    
    /**
     * return null if no terminal was clicked!
     *
     * @param px screen coordinates in (dpix-scaled!) pixel
     * @param py
     * @return
     */
    @Override
    public TerminalInterface clickedTerminal(final Point clickPoint) {
        // the label dialog should never apear!
        return null;
    }

    @Override
    public void deleteComponent() {
        super.deleteComponent();
        final Set<AbstractCircuitGlobalTerminal> possibleRemoval = ALL_GLOBALS.get(getTypeEnum());
        if(possibleRemoval != null && possibleRemoval.contains(this)) {
            possibleRemoval.remove(this);
        }
    }

    @Override
    public void doDoubleClickAction(final Point clickedPoint) {
        final DialogGlobalTerminal dialog = new DialogGlobalTerminal(GeckoSim._win, this);
        dialog.setVisible(true);
    }        
    
    @Override
    protected void drawForeground(final Graphics2D graphics) {
        final int diameter = dpix / 2;
        graphics.fillOval(-diameter / 2 - 1, -diameter / 2 - 1, diameter + 1, diameter + 1);        
    }

    @Override
    protected void drawBackground(final Graphics2D graphics) {
        final int diameter = dpix;
        graphics.fillRect(-diameter / 2-1, -diameter / 2-1, diameter+1, diameter+1);        
    }    
    
    
    
    @Override
    public Set<? extends GlobalTerminable> getAllGlobalTerminals() {
        return ALL_GLOBALS.get(getTypeEnum());
    }

    @Override
    protected Window openDialogWindow() {
        // nothing todo, the dialog is opened in "doDoubleClickAction"
        assert false;
        return null;
    }                

    @Override
    void drawConnectorLines(final Graphics2D graphics) {        
        // this component does not have connector lines!
    }
    
    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

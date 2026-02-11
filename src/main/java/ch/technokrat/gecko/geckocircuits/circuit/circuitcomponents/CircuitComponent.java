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

import ch.technokrat.gecko.geckocircuits.allg.MainWindow;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitComponentCore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * GUI-enabled circuit component for desktop application.
 * Extends CircuitComponentCore (pure simulation logic).
 * 
 * <p>This class adds GUI-specific functionality like terminal references
 * that connect to the visual circuit representation. The core simulation
 * logic is inherited from CircuitComponentCore.
 * 
 * @author andy
 * @since 2.0 (refactored to extend CircuitComponentCore)
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Circuit component must share references to parent and terminal objects for proper circuit connectivity")
public abstract class CircuitComponent<T extends AbstractTwoPortLKreisBlock>
    extends CircuitComponentCore {

    /** First terminal (GUI connection) */
    protected final AbstractTerminal _term1;
    
    /** Second terminal (GUI connection) */
    protected AbstractTerminal _term2;
    
    /** Parent block (GUI component) */
    protected final T _parent;

    /**
     * Constructor for CircuitComponent.
     * @param parent the parent GUI block
     */
    public CircuitComponent(final T parent) {
        super(MainWindow._solverSettings.SOLVER_TYPE.getValue());
        _parent = parent;
        _term1 = parent.XIN.get(0);
        _term2 = parent.YOUT.get(0);        
    }

    /**
     * Assign terminal indices from the GUI terminals.
     * Overrides core implementation to use GUI-specific terminals.
     */
    @Override
    public void assignTerminalIndices() {
        matrixIndices[0] = _term1.getIndex();
        if (_term2 != null) {
            matrixIndices[1] = _term2.getIndex();
        } else {
            matrixIndices[1] = 0;
        }
    }

    /**
     * Get the parent GUI block.
     * @return the parent block
     */
    public T getParent() {
        return _parent;
    }
    
    /**
     * Get the first terminal.
     * @return the first terminal
     */
    public AbstractTerminal getTerm1() {
        return _term1;
    }
    
    /**
     * Get the second terminal.
     * @return the second terminal
     */
    public AbstractTerminal getTerm2() {
        return _term2;
    }

    @Override
    public String toString() {
        return _parent.getStringID() + " calculator ";
    }
}

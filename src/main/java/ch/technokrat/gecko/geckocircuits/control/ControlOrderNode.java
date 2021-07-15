/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.circuit.AbstractTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.ControlTerminable;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalControl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * this wrapper class is used for determining the control execution order. It
 * hides all unnecessary RegelBlock properties from the programmer.
 *
 * @author andy
 */
final class ControlOrderNode {

    private final Set<ControlOrderNode> _directInputs = new LinkedHashSet<ControlOrderNode>();
    private final Set<ControlOrderNode> _directOutputs = new LinkedHashSet<ControlOrderNode>();
    private final RegelBlock _elementControl;
    private int _priority;
    private boolean _loopCrack;

    public ControlOrderNode(final RegelBlock elementControl) {
        _elementControl = elementControl;        
    }
    
    public void calculateNeighbours(final Collection<ControlOrderNode> allBlocks) {
        if (_elementControl.getType() != ControlType.SOURCE) {
            calculateDirectInputs(allBlocks);
        }

        if (_elementControl.getType() != ControlType.SINK) {
            calculateDirectOutputs(allBlocks);
        }       
    }

    public ControlType getControlType() {
        return _elementControl.getType();
    }

    public RegelBlock getElementControl() {
        return _elementControl;
    }
    

    private void calculateDirectInputs(final Collection<ControlOrderNode> allBlocks) {
        assert _directInputs.isEmpty() : "do this calculation only once at the beginning!";

        final List<Integer> nodeIndexIn = new ArrayList<Integer>();
        for (AbstractTerminal term : _elementControl.XIN) {
            nodeIndexIn.add(((ControlTerminable) term).getNodeNumber());
        }

        for (ControlOrderNode element2 : allBlocks) {
            if (element2 == this) {
                continue;
            }

            for (AbstractTerminal term : element2.getElementControl().YOUT) {
                final int nodeIndex = ((ControlTerminable) term).getNodeNumber();
                for (int i4 = 0; i4 < nodeIndexIn.size(); i4++) {
                    if (nodeIndex == nodeIndexIn.get(i4)) {
                        _directInputs.add(element2);
                    }
                }
            }
        }
    }

    private void calculateDirectOutputs(final Collection<ControlOrderNode> allBlocks) {
        assert _directOutputs.isEmpty() : "do this calculation only once at the beginning!";

        final List<Integer> nodeIndexOut = new ArrayList<Integer>();
        for (AbstractTerminal term : _elementControl.YOUT) {
            nodeIndexOut.add(((ControlTerminable) term).getNodeNumber());
        }

        for(ControlOrderNode element2 : allBlocks) {
            if (element2 == this) {
                continue;
            }

            for (AbstractTerminal term : element2.getElementControl().XIN) {
                final int nodeIndex = ((ControlTerminable) term).getNodeNumber();
                for (int i4 = 0; i4 < nodeIndexOut.size(); i4++) {
                    if (nodeIndex == nodeIndexOut.get(i4)) {                        
                        _directOutputs.add(element2);
                    }
                }
            }
        }

    }

    public boolean isDirectInputOfElement(final ControlOrderNode node) {
        return node._directInputs.contains(this);        
    }

    public boolean isDirectOutputOfElement(final ControlOrderNode node) {
        return node._directOutputs.contains(this);        
    }

    Set<ControlOrderNode> getAllDirectOutputs() {
        return Collections.unmodifiableSet(_directOutputs);
    }
    
    /**
     * direct inputs are direct predecessor blocks of the control block node
     *
     * @return
     */
    Set<ControlOrderNode> getAllDirectInputs() {
        return Collections.unmodifiableSet(_directInputs);
    }

    void setInitialPriority(final int initialPriority) {
        _priority = initialPriority;
    }
    
    void testSetMaximumPriority(final int newMaxPriority) {
        _priority = Math.max(_priority, newMaxPriority);        
    }
    
    void testSetMinimumPriority(final int newMinPriority) {        
        _priority = Math.min(_priority, newMinPriority);        
    }

    int getPriority() {
        return _priority;
    }

    void setLoopCrackTrue() {
        _loopCrack = true;
    }
    
    boolean getLoopCrack() {
        return _loopCrack;
    }
    
}
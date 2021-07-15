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

import java.util.Collection;
import java.util.Set;

abstract class AbstractSinkControlOrderer extends AbstractControlOrderer {

    public AbstractSinkControlOrderer(Collection<ControlOrderNode> allNodesToSort) {
        super(allNodesToSort);
    }
    
    @Override
    final Set<ControlOrderNode> getNextNeighbourNodes(final ControlOrderNode node) {
        return node.getAllDirectInputs();
    }
    
    /**
     * reverse ordering (from end to start), therefore the list end
     * is where we insert the components first.
     * @param node moved to corresponding list
     */
    @Override    
    final void moveNodeToStartDirectionInList(final ControlOrderNode node) {
        moveNodeToListEnd(node);
    }
    
    /**
     * reverse ordering (from end to start), therefore the list start
     * is where we insert the component last.
     * @param node 
     */
    @Override
    final void moveNodeToEndDirectionInList(final ControlOrderNode node) {
        moveNodeToListStart(node);        
    }
}

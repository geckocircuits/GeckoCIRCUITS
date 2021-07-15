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
import java.util.LinkedHashSet;
import java.util.Set;

class StartFromBlocksWithoutPredecessorOrderer extends AbstractSourceControlOrderer {

    public StartFromBlocksWithoutPredecessorOrderer(final Collection<ControlOrderNode> allNodesToSort) {
        super(allNodesToSort);        
        assert _sourceList.isEmpty() : "these components should already be filtered/removed!";
        assert _sinkList.isEmpty()  : "these components should already be filtered/removed!";                                
    }    
    
    @Override
    Set<ControlOrderNode> getStartSet() {
        return findBlocksWithoutPredecessors();
    }            

    private Set<ControlOrderNode> findBlocksWithoutPredecessors() {
        final Set<ControlOrderNode> returnValue = new LinkedHashSet<ControlOrderNode>();
        for(ControlOrderNode node : _transferList) {
            if(node.getAllDirectInputs().isEmpty()) {
                returnValue.add(node);
            }
        }
        return returnValue;
    }
    
    @Override
    final String getRemainingWarningString() {
        return "The following control blocks don't have any"
                + " path to any control block: ";
    }
    
}

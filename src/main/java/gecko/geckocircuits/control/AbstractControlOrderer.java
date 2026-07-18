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
package gecko.geckocircuits.control;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
@SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
        justification = "Abstract class - subclasses are designed to handle constructor properly")
 abstract class AbstractControlOrderer {
    private static final Logger LOGGER = LogManager.getLogger(AbstractControlOrderer.class);

    static final int MAX_ITERATION_COUNT = 10000;

    @SuppressWarnings("PMD")
    private final LinkedList<ControlOrderNode> _orderedList = new LinkedList<ControlOrderNode>();
    private final Set<ControlOrderNode> _availableBlocks = new LinkedHashSet<ControlOrderNode>();
    protected final Set<ControlOrderNode> _sourceList = new LinkedHashSet<ControlOrderNode>();
    protected final Set<ControlOrderNode> _transferList = new LinkedHashSet<ControlOrderNode>();
    protected final Set<ControlOrderNode> _sinkList = new LinkedHashSet<ControlOrderNode>();
    protected final Set<ControlOrderNode> _nextInsertBlocks = new LinkedHashSet<ControlOrderNode>();

    AbstractControlOrderer(final Collection<ControlOrderNode> allNodesToSort) {
        sortNodesIntoSourceTransferSinks(allNodesToSort);
        _availableBlocks.addAll(allNodesToSort);
    }

    public List<ControlOrderNode> getOptimizedList() {
        return Collections.unmodifiableList(_orderedList);
    }

    public Set<ControlOrderNode> getNotConsideredNodes() {
        return Collections.unmodifiableSet(_availableBlocks);
    }

    private void sortMoveInitial() {
        for (ControlOrderNode node : getStartSet()) {
            moveNodeToStartDirectionInList(node);
            addNodesToNextList(node);
        }
    }


    final void moveNodeToListEnd(final ControlOrderNode node) {
        if(_orderedList.contains(node)) {
            _orderedList.remove(node);
        }
        _orderedList.addLast(node);
        _availableBlocks.remove(node);
    }

    final void moveNodeToListStart(final ControlOrderNode node) {

        if(_orderedList.contains(node)) {
            _orderedList.remove(node);
        }
        _orderedList.addFirst(node);
        _availableBlocks.remove(node);
    }

    private void sortNodesIntoSourceTransferSinks(final Collection<ControlOrderNode> allNodes) {
        for(ControlOrderNode node : allNodes) {
            switch(node.getControlType()) {
                case SOURCE:
                    _sourceList.add(node);
                    break;
                case TRANSFER:
                    _transferList.add(node);
                    break;
                case SINK:
                    _sinkList.add(node);
                    break;
                default:
                    assert false;
            }
        }
        assert _sourceList.size() + _sinkList.size() + _transferList.size() == allNodes.size() :
                "Duplicate component in list!";
    }

    final void doOrdering() {
        sortMoveInitial();
        iterateSortRemainingBlocks();
        printWarningsForNodesWithNoEffect();
    }

    void printWarningsForNodesWithNoEffect() {
        if(_availableBlocks.isEmpty()) {
            return;
        }
        final StringBuffer warningsString = new StringBuffer(getRemainingWarningString());

        for (ControlOrderNode notSinkConnected : _availableBlocks) {
            warningsString.append(' ');
            warningsString.append(notSinkConnected.getElementControl().getStringID());
            warningsString.append(' ');
        }
        warningsString.append('\n');LogManager.getLogger(StartFromSinkOrderer.class).warn(warningsString.toString());
    }

    void addNodesToNextList(final ControlOrderNode node) {
        for (ControlOrderNode directNeighbour : getNextNeighbourNodes(node)) {
            if(_availableBlocks.contains(directNeighbour)) {
                _nextInsertBlocks.add(directNeighbour);
            }
        }
    }

    void iterateSortRemainingBlocks() {
        int lastSize = -1;
        for (int iterCounter = 0; _nextInsertBlocks.size() != lastSize; iterCounter++) {
            if (iterCounter > MAX_ITERATION_COUNT) {
                throw new StackOverflowError("Cannot determine correct control execution order.");
            }
            lastSize = _nextInsertBlocks.size();
            for (ControlOrderNode node : _nextInsertBlocks.toArray(new ControlOrderNode[0])) {
                moveNodeToEndDirectionInList(node);
                addNodesToNextList(node);

                if(node.getElementControl().getStringID().equals("OR.1")) {
                    LOGGER.info("next: " + _nextInsertBlocks.size());
                }

            }
        }
    }

    abstract Set<ControlOrderNode> getStartSet();
    abstract void moveNodeToStartDirectionInList(ControlOrderNode node);
    abstract void moveNodeToEndDirectionInList(ControlOrderNode node);
    abstract Set<ControlOrderNode> getNextNeighbourNodes(ControlOrderNode node);
    abstract String getRemainingWarningString();
}

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

import java.util.LinkedList;
import java.util.Set;


import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BlockOrderOptimizer3 {

    private final List<ControlOrderNode> _allNodes = new ArrayList<ControlOrderNode>();
    /**
     * the non-optimized output list as given as input to this optimizer
     *
     */
    private List<RegelBlock> _allControlsInput;
    private LinkedList<ControlOrderNode> _optimizedList;
    private ArrayList<RegelBlock> _outList;
    private Set<ControlOrderNode> _transferStops;
    private Set<ControlOrderNode> _transferStarts;
    private int _stopIteration;

    public BlockOrderOptimizer3(final List<RegelBlock> allControlBlocks) {
        this._allControlsInput = Collections.unmodifiableList(allControlBlocks);        
        for (RegelBlock regler : _allControlsInput) {            
            _allNodes.add(new ControlOrderNode(regler));
        }        

        for (ControlOrderNode node : _allNodes) {
            node.calculateNeighbours(_allNodes);
        }

        doOrdering();
    }

    public List<RegelBlock> getOptimierteAbarbeitungsListe() {
        return Collections.unmodifiableList(_outList);
    }

    private void doOrdering() {
        doInitialPriorities();
        _transferStarts = findPossibleStarts();

        _transferStops = findPossibleStops();
        _stopIteration = _allControlsInput.size();
                                
        for (int i = 0; i < 1; i++) {
            for (ControlOrderNode node : _transferStops) {
                try {
                    iterateIntoNegativeDirection(node.getAllDirectInputs(), node.getPriority() - 1);
                } catch (LoopDetectionException ex) {
                    ex.printLoopMessage();
                    //Logger.getLogger(BlockOrderOptimizer3.class.getName()).log(Level.SEVERE, null, ex);
                }
            }                                    
        }
        
        for (ControlOrderNode node : _transferStarts) {
                try {
                    iterateIntoPositiveDirection(node.getAllDirectOutputs(), node.getPriority() + 1);
                } catch (LoopDetectionException ex) {
                    ex.printLoopMessage();
                }
        }
        
        for (int i = 0; i < 1; i++) {
            for (ControlOrderNode node : _transferStops) {
                try {
                    iterateIntoNegativeDirection(node.getAllDirectInputs(), node.getPriority() - 1);
                } catch (LoopDetectionException ex) {
                    ex.printLoopMessage();
                    //Logger.getLogger(BlockOrderOptimizer3.class.getName()).log(Level.SEVERE, null, ex);
                }
            }                                    
        }
        
               
        _optimizedList = sortNodesWithPriorities();
        
        _outList = new ArrayList<RegelBlock>();
        for (int i = 0; i < _allControlsInput.size(); i++) {
            _outList.add(_optimizedList.get(i).getElementControl());         
        }        

        doConsistencyChecks(_outList, _allControlsInput);                        
        //System.out.println("number of loops " + countNumberOfOutOfOrderLoops(_optimizedList) + " of " + _optimizedList.size());
//        System.out.println("---------- control block order: ");
//        for(RegelBlock block : _outList) {
//            System.out.println(block.getIDStringDialog());
//        }
//        System.out.println("---------");

    }

    Set<ControlOrderNode> findPossibleStarts() {
        Set<ControlOrderNode> returnValue = new LinkedHashSet<ControlOrderNode>();
        for (ControlOrderNode node : _allNodes) {
            if (node.getAllDirectInputs().isEmpty()) {
                returnValue.add(node);
            }
        }
        return returnValue;
    }

    Set<ControlOrderNode> findPossibleStops() {
        Set<ControlOrderNode> returnValue = new LinkedHashSet<ControlOrderNode>();
        for (ControlOrderNode node : _allNodes) {
            if (node.getAllDirectOutputs().isEmpty()) {
                returnValue.add(node);
            }

        }
        return returnValue;
    }

    private void doInitialPriorities() {
        for (ControlOrderNode node : _allNodes) {
            switch (node.getControlType()) {
                case SINK:
                    node.setInitialPriority(Integer.MAX_VALUE);
                    break;
                case SOURCE:
                    node.setInitialPriority(Integer.MIN_VALUE);
                    break;
                case TRANSFER:
                    node.setInitialPriority(0);
                    break;
            }
        }
    }

    /**
     * the number of "out-of-order" control blocks is the optimization criteria.
     * The algorithm searches for each control block (node) all successors. If
     * any of the successors is an input of the node, the loop counter is
     * increased. Please note that a loop is not meant here as a real control
     * loop, but a loop means that a block is executed (in the ordered list) out
     * of order, which infact is a cyclic dependency.
     *
     */
    static int countNumberOfOutOfOrderLoops(final List<ControlOrderNode> elements) {
        int loop = 0;
        for (int i = 0; i < elements.size() - 1; i++) {
            for (int j = i; j < elements.size(); j++) {
                if (elements.get(j).isDirectInputOfElement(elements.get(i))) {
                    System.out.println("out of order: " + j + " " + elements.get(j).getElementControl().getStringID() + " "
                            + elements.get(j).getPriority()
                            + " " + i + " " + elements.get(i).getElementControl().getStringID() + " " + elements.get(i).getPriority());
                    loop++;
                }
            }
        }
        return loop;
    }

    private static void doConsistencyChecks(final List<RegelBlock> outList, final List<RegelBlock> allControlsInput) {
        if (outList.size() != allControlsInput.size()) {
            System.err.println("unequal length of lists: input " + allControlsInput.size() + " output: " + outList.size());
            assert false;
        }

        final Set<RegelBlock> set1 = new HashSet<RegelBlock>(outList);
        if (outList.size() != set1.size()) {
            System.err.println("duplicate components in sorted list!");
            assert false;
        }

        final Set<RegelBlock> set2 = new HashSet<RegelBlock>(allControlsInput);
        if (allControlsInput.size() != set2.size()) {
            System.err.println("duplicate components in input of sorted list!");
            assert false;
        }

        for (RegelBlock block : set1) {
            assert set2.contains(block);
        }

        for (RegelBlock block : set2) {
            assert set1.contains(block);
        }
    }

    private void iterateIntoPositiveDirection(Set<ControlOrderNode> transferStarts, final int level)
            throws LoopDetectionException {


        for (ControlOrderNode nextNode : transferStarts) {
            if (nextNode.getLoopCrack()) {
                continue;
            }
            assert level < Integer.MAX_VALUE;
            if (level > _stopIteration) {
                List<ControlOrderNode> loopList = new ArrayList<ControlOrderNode>();
                loopList.add(nextNode);
                throw new LoopDetectionException(nextNode, loopList);
            }

            nextNode.testSetMaximumPriority(level);

            try {
                iterateIntoPositiveDirection(nextNode.getAllDirectOutputs(), nextNode.getPriority() + 1);
            } catch (LoopDetectionException ex) {
                boolean loopFinished = ex.insertTestLoopFinished(nextNode);
                if (loopFinished) {
                    ex.printLoopMessage();
                } else {
                    throw ex;
                }

            }


        }
    }

    private void iterateIntoNegativeDirection(Set<ControlOrderNode> nextLevelComponents, final int level)
            throws LoopDetectionException {

        for (ControlOrderNode nextNode : nextLevelComponents) {
            assert level > Integer.MIN_VALUE;
            if (nextNode.getLoopCrack()) {
                continue;
            }
            if (level < - _stopIteration) {
                List<ControlOrderNode> loopList = new ArrayList<ControlOrderNode>();
                loopList.add(nextNode);
                throw new LoopDetectionException(nextNode, loopList);
            }

            nextNode.testSetMinimumPriority(level);
            try {
                iterateIntoNegativeDirection(nextNode.getAllDirectInputs(), nextNode.getPriority() - 1);
            } catch (LoopDetectionException ex) {
                boolean loopFinished = ex.insertTestLoopFinished(nextNode);
                if (loopFinished) {
                    ex.printLoopMessage();
                } else {
                    throw ex;
                }

            }
        }
    }

    private LinkedList<ControlOrderNode> sortNodesWithPriorities() {
        LinkedList<ControlOrderNode> returnValue = new LinkedList<ControlOrderNode>();

        for (ControlOrderNode node : _allNodes) {
            int insertIndex = 0;
            for (; insertIndex < returnValue.size(); insertIndex++) {
                if (returnValue.get(insertIndex).getPriority() < node.getPriority()) {
                    continue;
                } else {
                    break;
                }
            }
            returnValue.add(insertIndex, node);
        }
        return returnValue;
    }
}

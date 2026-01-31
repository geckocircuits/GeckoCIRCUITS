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
package ch.technokrat.gecko.geckocircuits.circuit.netlist;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import java.util.*;

/**
 * Handles node numbering and indexing for circuit netlists.
 * Extracted from NetListLK for better testability and separation of concerns.
 * 
 * <p>Responsibilities:
 * <ul>
 *   <li>Assign unique node numbers to circuit connection points</li>
 *   <li>Track the maximum node index (knotenMAX)</li>
 *   <li>Map element terminals to node indices</li>
 *   <li>Handle ground reference (node 0)</li>
 * </ul>
 * 
 * <p>Node numbering rules:
 * <ul>
 *   <li>Nodes are numbered consecutively starting from 0</li>
 *   <li>Ground is typically node 0</li>
 *   <li>Each unique potential gets a unique node number</li>
 * </ul>
 * 
 * @author Extracted from NetListLK
 * @since Sprint 2 - Circuit Refactoring
 */
public final class NodeIndexer {
    
    /** Node indices for element X terminals (positive/input) */
    private int[] nodeX;
    
    /** Node indices for element Y terminals (negative/output) */
    private int[] nodeY;
    
    /** Maximum node index (total nodes = knotenMAX + 1, including ground) */
    private int knotenMAX;
    
    /** Number of elements in the netlist */
    private final int elementCount;
    
    /**
     * Creates a new NodeIndexer with the specified number of elements.
     * 
     * @param elementCount number of circuit elements
     */
    public NodeIndexer(int elementCount) {
        this.elementCount = elementCount;
        this.nodeX = new int[elementCount];
        this.nodeY = new int[elementCount];
        this.knotenMAX = 0;
    }
    
    /**
     * Creates a NodeIndexer from pre-existing node arrays.
     * Used when node assignments are already known.
     * 
     * @param nodeX array of X node indices
     * @param nodeY array of Y node indices
     */
    public NodeIndexer(int[] nodeX, int[] nodeY) {
        if (nodeX == null || nodeY == null) {
            throw new IllegalArgumentException("Node arrays cannot be null");
        }
        if (nodeX.length != nodeY.length) {
            throw new IllegalArgumentException("Node arrays must have same length");
        }
        
        this.elementCount = nodeX.length;
        this.nodeX = nodeX.clone();
        this.nodeY = nodeY.clone();
        
        // Calculate knotenMAX
        this.knotenMAX = calculateMaxNode();
    }
    
    /**
     * Sets the node indices for an element.
     * 
     * @param elementIndex element index (0-based)
     * @param xNode node number for X terminal
     * @param yNode node number for Y terminal
     * @throws IndexOutOfBoundsException if elementIndex is invalid
     */
    public void setNodes(int elementIndex, int xNode, int yNode) {
        if (elementIndex < 0 || elementIndex >= elementCount) {
            throw new IndexOutOfBoundsException("Element index out of range: " + elementIndex);
        }
        if (xNode < 0 || yNode < 0) {
            throw new IllegalArgumentException("Node indices must be non-negative");
        }
        
        nodeX[elementIndex] = xNode;
        nodeY[elementIndex] = yNode;
        
        // Update knotenMAX
        knotenMAX = Math.max(knotenMAX, Math.max(xNode, yNode));
    }
    
    /**
     * Gets the X node index for an element.
     * 
     * @param elementIndex element index (0-based)
     * @return node number for X terminal
     */
    public int getNodeX(int elementIndex) {
        return nodeX[elementIndex];
    }
    
    /**
     * Gets the Y node index for an element.
     * 
     * @param elementIndex element index (0-based)
     * @return node number for Y terminal
     */
    public int getNodeY(int elementIndex) {
        return nodeY[elementIndex];
    }
    
    /**
     * Gets the maximum node index.
     * The total number of nodes (excluding ground) is knotenMAX.
     * 
     * @return maximum node index
     */
    public int getKnotenMAX() {
        return knotenMAX;
    }
    
    /**
     * Gets the total number of nodes including ground.
     * 
     * @return total node count
     */
    public int getTotalNodeCount() {
        return knotenMAX + 1;
    }
    
    /**
     * Gets the number of elements.
     * 
     * @return element count
     */
    public int getElementCount() {
        return elementCount;
    }
    
    /**
     * Recalculates knotenMAX from current node assignments.
     * Call this after bulk node assignments.
     */
    public void recalculateMaxNode() {
        this.knotenMAX = calculateMaxNode();
    }
    
    /**
     * Calculates the maximum node index from the node arrays.
     * 
     * @return maximum node index found
     */
    private int calculateMaxNode() {
        int max = 0;
        for (int i = 0; i < elementCount; i++) {
            max = Math.max(max, Math.max(nodeX[i], nodeY[i]));
        }
        return max;
    }
    
    /**
     * Validates that all nodes are assigned (non-negative).
     * 
     * @return true if all nodes are valid
     */
    public boolean isValid() {
        for (int i = 0; i < elementCount; i++) {
            if (nodeX[i] < 0 || nodeY[i] < 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gets a copy of all X node indices.
     * 
     * @return copy of nodeX array
     */
    public int[] getAllNodeX() {
        return nodeX.clone();
    }
    
    /**
     * Gets a copy of all Y node indices.
     * 
     * @return copy of nodeY array
     */
    public int[] getAllNodeY() {
        return nodeY.clone();
    }
    
    /**
     * Finds all elements connected to a specific node.
     * 
     * @param nodeIndex the node to search for
     * @return list of element indices connected to this node
     */
    public List<Integer> findElementsAtNode(int nodeIndex) {
        List<Integer> elements = new ArrayList<>();
        for (int i = 0; i < elementCount; i++) {
            if (nodeX[i] == nodeIndex || nodeY[i] == nodeIndex) {
                elements.add(i);
            }
        }
        return elements;
    }
    
    /**
     * Checks if two elements share a common node.
     * 
     * @param element1 first element index
     * @param element2 second element index
     * @return true if they share at least one node
     */
    public boolean shareCommonNode(int element1, int element2) {
        return nodeX[element1] == nodeX[element2] ||
               nodeX[element1] == nodeY[element2] ||
               nodeY[element1] == nodeX[element2] ||
               nodeY[element1] == nodeY[element2];
    }
    
    /**
     * Gets the shared node between two elements, or -1 if none.
     * 
     * @param element1 first element index
     * @param element2 second element index
     * @return shared node index, or -1 if no shared node
     */
    public int getSharedNode(int element1, int element2) {
        if (nodeX[element1] == nodeX[element2] || nodeX[element1] == nodeY[element2]) {
            return nodeX[element1];
        }
        if (nodeY[element1] == nodeX[element2] || nodeY[element1] == nodeY[element2]) {
            return nodeY[element1];
        }
        return -1;
    }
    
    /**
     * Creates a node-to-elements mapping.
     * 
     * @return map from node index to list of connected element indices
     */
    public Map<Integer, List<Integer>> buildNodeToElementsMap() {
        Map<Integer, List<Integer>> map = new HashMap<>();
        for (int i = 0; i < elementCount; i++) {
            map.computeIfAbsent(nodeX[i], k -> new ArrayList<>()).add(i);
            if (nodeX[i] != nodeY[i]) {
                map.computeIfAbsent(nodeY[i], k -> new ArrayList<>()).add(i);
            }
        }
        return map;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NodeIndexer[elements=").append(elementCount);
        sb.append(", knotenMAX=").append(knotenMAX).append("]\n");
        for (int i = 0; i < elementCount; i++) {
            sb.append("  [").append(i).append("]: X=").append(nodeX[i]);
            sb.append(", Y=").append(nodeY[i]).append("\n");
        }
        return sb.toString();
    }
}

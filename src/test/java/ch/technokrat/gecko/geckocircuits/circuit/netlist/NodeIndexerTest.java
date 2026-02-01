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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

/**
 * Unit tests for NodeIndexer.
 * Tests node numbering, indexing, and element-to-node mapping functionality.
 */
public class NodeIndexerTest {
    
    private NodeIndexer indexer;
    
    @Before
    public void setUp() {
        indexer = new NodeIndexer(5);
    }
    
    // ===== Construction Tests =====
    
    @Test
    public void testConstructorWithSize() {
        NodeIndexer ni = new NodeIndexer(10);
        assertEquals(10, ni.getElementCount());
        assertEquals(0, ni.getKnotenMAX());
    }
    
    @Test
    public void testConstructorWithArrays() {
        int[] nodeX = {0, 1, 2, 1};
        int[] nodeY = {1, 2, 0, 0};
        NodeIndexer ni = new NodeIndexer(nodeX, nodeY);
        
        assertEquals(4, ni.getElementCount());
        assertEquals(2, ni.getKnotenMAX());
        assertEquals(0, ni.getNodeX(0));
        assertEquals(1, ni.getNodeY(0));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullArrayX() {
        new NodeIndexer(null, new int[]{1, 2});
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullArrayY() {
        new NodeIndexer(new int[]{1, 2}, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithMismatchedArrays() {
        new NodeIndexer(new int[]{1, 2, 3}, new int[]{1, 2});
    }
    
    // ===== Node Assignment Tests =====
    
    @Test
    public void testSetNodes() {
        indexer.setNodes(0, 1, 2);
        indexer.setNodes(1, 2, 3);
        
        assertEquals(1, indexer.getNodeX(0));
        assertEquals(2, indexer.getNodeY(0));
        assertEquals(2, indexer.getNodeX(1));
        assertEquals(3, indexer.getNodeY(1));
    }
    
    @Test
    public void testSetNodesUpdatesKnotenMAX() {
        assertEquals(0, indexer.getKnotenMAX());
        
        indexer.setNodes(0, 5, 3);
        assertEquals(5, indexer.getKnotenMAX());
        
        indexer.setNodes(1, 2, 10);
        assertEquals(10, indexer.getKnotenMAX());
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetNodesInvalidIndex() {
        indexer.setNodes(10, 1, 2);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetNodesNegativeX() {
        indexer.setNodes(0, -1, 2);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetNodesNegativeY() {
        indexer.setNodes(0, 1, -1);
    }
    
    // ===== Node Count Tests =====
    
    @Test
    public void testGetTotalNodeCount() {
        indexer.setNodes(0, 0, 1);
        indexer.setNodes(1, 1, 2);
        indexer.setNodes(2, 2, 3);
        
        assertEquals(3, indexer.getKnotenMAX());
        assertEquals(4, indexer.getTotalNodeCount()); // 0, 1, 2, 3
    }
    
    @Test
    public void testGetTotalNodeCountIncludesGround() {
        // Even if no element uses node 0, knotenMAX is the highest used node
        indexer.setNodes(0, 5, 5);
        assertEquals(5, indexer.getKnotenMAX());
        assertEquals(6, indexer.getTotalNodeCount());
    }
    
    // ===== Validation Tests =====
    
    @Test
    public void testIsValidWithAllAssigned() {
        indexer.setNodes(0, 0, 1);
        indexer.setNodes(1, 1, 2);
        indexer.setNodes(2, 2, 0);
        indexer.setNodes(3, 0, 3);
        indexer.setNodes(4, 3, 1);
        
        assertTrue(indexer.isValid());
    }
    
    @Test
    public void testIsValidWithDefaultValues() {
        // Default values are 0, which is valid (ground)
        indexer.setNodes(0, 0, 1);
        // Element 1 still has default nodes (0, 0)
        assertTrue(indexer.isValid());
    }
    
    // ===== Array Getter Tests =====
    
    @Test
    public void testGetAllNodeX() {
        indexer.setNodes(0, 1, 2);
        indexer.setNodes(1, 3, 4);
        
        int[] allX = indexer.getAllNodeX();
        assertEquals(5, allX.length);
        assertEquals(1, allX[0]);
        assertEquals(3, allX[1]);
    }
    
    @Test
    public void testGetAllNodeXReturnsCopy() {
        indexer.setNodes(0, 1, 2);
        int[] allX = indexer.getAllNodeX();
        allX[0] = 99;
        
        assertEquals(1, indexer.getNodeX(0)); // Original unchanged
    }
    
    @Test
    public void testGetAllNodeY() {
        indexer.setNodes(0, 1, 2);
        indexer.setNodes(1, 3, 4);
        
        int[] allY = indexer.getAllNodeY();
        assertEquals(5, allY.length);
        assertEquals(2, allY[0]);
        assertEquals(4, allY[1]);
    }
    
    // ===== Element Finding Tests =====
    
    @Test
    public void testFindElementsAtNode() {
        indexer.setNodes(0, 1, 2);
        indexer.setNodes(1, 2, 3);
        indexer.setNodes(2, 2, 4);
        indexer.setNodes(3, 5, 6);
        indexer.setNodes(4, 3, 2);
        
        List<Integer> atNode2 = indexer.findElementsAtNode(2);
        assertEquals(4, atNode2.size());
        assertTrue(atNode2.contains(0)); // nodeY = 2
        assertTrue(atNode2.contains(1)); // nodeX = 2
        assertTrue(atNode2.contains(2)); // nodeX = 2
        assertTrue(atNode2.contains(4)); // nodeY = 2
    }
    
    @Test
    public void testFindElementsAtNodeEmpty() {
        indexer.setNodes(0, 1, 2);
        indexer.setNodes(1, 3, 4);
        
        List<Integer> atNode99 = indexer.findElementsAtNode(99);
        assertTrue(atNode99.isEmpty());
    }
    
    // ===== Shared Node Tests =====
    
    @Test
    public void testShareCommonNode() {
        indexer.setNodes(0, 1, 2);
        indexer.setNodes(1, 2, 3);
        indexer.setNodes(2, 4, 5);
        
        assertTrue(indexer.shareCommonNode(0, 1)); // Share node 2
        assertFalse(indexer.shareCommonNode(0, 2)); // No common node
    }
    
    @Test
    public void testShareCommonNodeSameElement() {
        indexer.setNodes(0, 1, 2);
        assertTrue(indexer.shareCommonNode(0, 0)); // Same element shares itself
    }
    
    @Test
    public void testGetSharedNode() {
        indexer.setNodes(0, 1, 2);
        indexer.setNodes(1, 2, 3);
        indexer.setNodes(2, 4, 5);
        
        assertEquals(2, indexer.getSharedNode(0, 1));
        assertEquals(-1, indexer.getSharedNode(0, 2));
    }
    
    @Test
    public void testGetSharedNodeMultipleShared() {
        indexer.setNodes(0, 1, 2);
        indexer.setNodes(1, 1, 2); // Same as element 0
        
        // Returns first match (nodeX[0])
        int shared = indexer.getSharedNode(0, 1);
        assertTrue(shared == 1 || shared == 2);
    }
    
    // ===== Node to Elements Map Tests =====
    
    @Test
    public void testBuildNodeToElementsMap() {
        // Use a fresh indexer with exactly 3 elements
        NodeIndexer ni = new NodeIndexer(3);
        ni.setNodes(0, 0, 1);
        ni.setNodes(1, 1, 2);
        ni.setNodes(2, 0, 2);
        
        Map<Integer, List<Integer>> map = ni.buildNodeToElementsMap();
        
        assertEquals(3, map.size()); // nodes 0, 1, 2
        
        assertEquals(2, map.get(0).size()); // elements 0, 2
        assertTrue(map.get(0).contains(0));
        assertTrue(map.get(0).contains(2));
        
        assertEquals(2, map.get(1).size()); // elements 0, 1
        assertTrue(map.get(1).contains(0));
        assertTrue(map.get(1).contains(1));
        
        assertEquals(2, map.get(2).size()); // elements 1, 2
        assertTrue(map.get(2).contains(1));
        assertTrue(map.get(2).contains(2));
    }
    
    @Test
    public void testBuildNodeToElementsMapWithSelfLoop() {
        // Element with same X and Y node (like a short circuit)
        indexer.setNodes(0, 1, 1);
        indexer.setNodes(1, 1, 2);
        
        Map<Integer, List<Integer>> map = indexer.buildNodeToElementsMap();
        
        // Element 0 should only appear once for node 1
        assertEquals(2, map.get(1).size()); // elements 0 and 1
    }
    
    // ===== Recalculate Tests =====
    
    @Test
    public void testRecalculateMaxNode() {
        int[] nodeX = {0, 1, 2};
        int[] nodeY = {1, 2, 3};
        NodeIndexer ni = new NodeIndexer(nodeX, nodeY);
        
        // Manually modify internal state by setting a lower node
        ni.setNodes(2, 1, 1);
        
        // knotenMAX was updated to max of all, but let's verify recalculate
        ni.recalculateMaxNode();
        assertEquals(2, ni.getKnotenMAX()); // max is now 2 (from element 1)
    }
    
    // ===== ToString Tests =====
    
    @Test
    public void testToString() {
        indexer.setNodes(0, 1, 2);
        indexer.setNodes(1, 3, 4);
        
        String str = indexer.toString();
        assertTrue(str.contains("NodeIndexer"));
        assertTrue(str.contains("elements=5"));
        assertTrue(str.contains("X=1"));
        assertTrue(str.contains("Y=2"));
    }
    
    // ===== Integration Tests =====
    
    @Test
    public void testTypicalCircuitSetup() {
        // Simulate a simple voltage divider: V-R1-node1-R2-GND
        // Element 0: Voltage source (node 2 to node 0/ground)
        // Element 1: R1 (node 2 to node 1)
        // Element 2: R2 (node 1 to node 0/ground)
        
        NodeIndexer circuit = new NodeIndexer(3);
        circuit.setNodes(0, 2, 0); // V source
        circuit.setNodes(1, 2, 1); // R1
        circuit.setNodes(2, 1, 0); // R2
        
        assertEquals(2, circuit.getKnotenMAX());
        assertEquals(3, circuit.getTotalNodeCount());
        
        // Check connections at node 2 (V source + R1)
        List<Integer> atNode2 = circuit.findElementsAtNode(2);
        assertEquals(2, atNode2.size());
        assertTrue(atNode2.contains(0));
        assertTrue(atNode2.contains(1));
        
        // Check R1 and R2 share node 1
        assertTrue(circuit.shareCommonNode(1, 2));
        assertEquals(1, circuit.getSharedNode(1, 2));
    }
    
    @Test
    public void testBridgeCircuit() {
        // H-bridge topology: 4 switches, 1 load
        // Nodes: 0=GND, 1=VDC+, 2=left mid, 3=right mid
        
        NodeIndexer bridge = new NodeIndexer(5);
        bridge.setNodes(0, 1, 2); // S1 (high-left)
        bridge.setNodes(1, 2, 0); // S2 (low-left)
        bridge.setNodes(2, 1, 3); // S3 (high-right)
        bridge.setNodes(3, 3, 0); // S4 (low-right)
        bridge.setNodes(4, 2, 3); // Load (across bridge)
        
        assertEquals(3, bridge.getKnotenMAX());
        
        // Check that load connects the two mid-points
        assertTrue(bridge.shareCommonNode(0, 4)); // S1 and Load share node 2
        assertTrue(bridge.shareCommonNode(2, 4)); // S3 and Load share node 3
    }
}

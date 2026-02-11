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
 * Unit tests for LabelResolver.
 * Tests label to index mapping, lookups, and validation.
 */
public class LabelResolverTest {
    
    private LabelResolver resolver;
    
    @Before
    public void setUp() {
        resolver = new LabelResolver();
    }
    
    // ===== Construction Tests =====
    
    @Test
    public void testDefaultConstructor() {
        LabelResolver lr = new LabelResolver();
        assertEquals(0, lr.getLabelCount());
        assertEquals(-1, lr.getIndex("any"));
    }
    
    @Test
    public void testConstructorWithArray() {
        String[] labels = {"GND", "VCC", "OUT", null, "IN"};
        LabelResolver lr = new LabelResolver(labels);
        
        assertEquals(4, lr.getLabelCount()); // null is skipped
        assertEquals(0, lr.getIndex("GND"));
        assertEquals(1, lr.getIndex("VCC"));
        assertEquals(2, lr.getIndex("OUT"));
        assertEquals(4, lr.getIndex("IN"));
    }
    
    @Test
    public void testConstructorWithNullArray() {
        LabelResolver lr = new LabelResolver((String[]) null);
        assertEquals(0, lr.getLabelCount());
    }
    
    @Test
    public void testConstructorWithEmptyArray() {
        LabelResolver lr = new LabelResolver(new String[0]);
        assertEquals(0, lr.getLabelCount());
    }
    
    @Test
    public void testConstructorWithMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("A", 5);
        map.put("B", 10);
        map.put("C", 3);
        
        LabelResolver lr = new LabelResolver(map);
        
        assertEquals(3, lr.getLabelCount());
        assertEquals(5, lr.getIndex("A"));
        assertEquals(10, lr.getIndex("B"));
        assertEquals(3, lr.getIndex("C"));
    }
    
    // ===== Add Label Tests =====
    
    @Test
    public void testAddLabel() {
        resolver.addLabel("node1", 5);
        
        assertEquals(1, resolver.getLabelCount());
        assertEquals(5, resolver.getIndex("node1"));
        assertEquals("node1", resolver.getLabel(5));
    }
    
    @Test
    public void testAddLabelUpdatesExisting() {
        resolver.addLabel("node1", 5);
        resolver.addLabel("node1", 10);
        
        assertEquals(1, resolver.getLabelCount());
        assertEquals(10, resolver.getIndex("node1"));
        assertNull(resolver.getLabel(5)); // Old index cleared
    }
    
    @Test
    public void testAddLabelReplacesAtIndex() {
        resolver.addLabel("old", 5);
        resolver.addLabel("new", 5);
        
        assertEquals(1, resolver.getLabelCount());
        assertEquals(5, resolver.getIndex("new"));
        assertEquals(-1, resolver.getIndex("old"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testAddNullLabel() {
        resolver.addLabel(null, 5);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testAddEmptyLabel() {
        resolver.addLabel("", 5);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testAddNegativeIndex() {
        resolver.addLabel("test", -1);
    }
    
    // ===== Lookup Tests =====
    
    @Test
    public void testGetIndex() {
        resolver.addLabel("VCC", 1);
        resolver.addLabel("GND", 0);
        
        assertEquals(1, resolver.getIndex("VCC"));
        assertEquals(0, resolver.getIndex("GND"));
        assertEquals(-1, resolver.getIndex("MISSING"));
    }
    
    @Test
    public void testGetIndexNull() {
        assertEquals(-1, resolver.getIndex(null));
    }
    
    @Test
    public void testGetLabel() {
        resolver.addLabel("VCC", 1);
        
        assertEquals("VCC", resolver.getLabel(1));
        assertNull(resolver.getLabel(99));
    }
    
    @Test
    public void testHasLabel() {
        resolver.addLabel("test", 5);
        
        assertTrue(resolver.hasLabel("test"));
        assertFalse(resolver.hasLabel("missing"));
        assertFalse(resolver.hasLabel(null));
        assertFalse(resolver.hasLabel(""));
    }
    
    @Test
    public void testHasLabelAtIndex() {
        resolver.addLabel("test", 5);
        
        assertTrue(resolver.hasLabelAtIndex(5));
        assertFalse(resolver.hasLabelAtIndex(0));
        assertFalse(resolver.hasLabelAtIndex(99));
    }
    
    // ===== Collection Access Tests =====
    
    @Test
    public void testGetAllLabels() {
        resolver.addLabel("A", 1);
        resolver.addLabel("B", 2);
        resolver.addLabel("C", 3);
        
        Set<String> labels = resolver.getAllLabels();
        assertEquals(3, labels.size());
        assertTrue(labels.contains("A"));
        assertTrue(labels.contains("B"));
        assertTrue(labels.contains("C"));
    }
    
    @Test
    public void testGetAllLabelsUnmodifiable() {
        resolver.addLabel("A", 1);
        Set<String> labels = resolver.getAllLabels();
        
        try {
            labels.add("X");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }
    
    @Test
    public void testGetAllIndices() {
        resolver.addLabel("A", 1);
        resolver.addLabel("B", 5);
        resolver.addLabel("C", 10);
        
        Set<Integer> indices = resolver.getAllIndices();
        assertEquals(3, indices.size());
        assertTrue(indices.contains(1));
        assertTrue(indices.contains(5));
        assertTrue(indices.contains(10));
    }
    
    @Test
    public void testGetLabelList() {
        String[] original = {"GND", "VCC", "OUT"};
        LabelResolver lr = new LabelResolver(original);
        
        String[] list = lr.getLabelList();
        assertArrayEquals(original, list);
        
        // Verify it's a copy
        list[0] = "MODIFIED";
        assertEquals("GND", lr.getLabel(0));
    }
    
    // ===== Remove Tests =====
    
    @Test
    public void testRemoveLabel() {
        resolver.addLabel("test", 5);
        assertTrue(resolver.removeLabel("test"));
        
        assertFalse(resolver.hasLabel("test"));
        assertFalse(resolver.hasLabelAtIndex(5));
        assertEquals(0, resolver.getLabelCount());
    }
    
    @Test
    public void testRemoveLabelMissing() {
        assertFalse(resolver.removeLabel("missing"));
    }
    
    @Test
    public void testRemoveLabelAtIndex() {
        resolver.addLabel("test", 5);
        assertTrue(resolver.removeLabelAtIndex(5));
        
        assertFalse(resolver.hasLabel("test"));
        assertFalse(resolver.hasLabelAtIndex(5));
    }
    
    @Test
    public void testRemoveLabelAtIndexMissing() {
        assertFalse(resolver.removeLabelAtIndex(99));
    }
    
    @Test
    public void testClear() {
        resolver.addLabel("A", 1);
        resolver.addLabel("B", 2);
        resolver.clear();
        
        assertEquals(0, resolver.getLabelCount());
        assertFalse(resolver.hasLabel("A"));
        assertFalse(resolver.hasLabel("B"));
    }
    
    // ===== Search Tests =====
    
    @Test
    public void testFindLabelsMatching() {
        resolver.addLabel("R1", 1);
        resolver.addLabel("R2", 2);
        resolver.addLabel("C1", 3);
        resolver.addLabel("L1", 4);
        
        List<String> resistors = resolver.findLabelsMatching("R\\d+");
        assertEquals(2, resistors.size());
        assertTrue(resistors.contains("R1"));
        assertTrue(resistors.contains("R2"));
    }
    
    @Test
    public void testFindLabelsWithPrefix() {
        resolver.addLabel("node_in", 1);
        resolver.addLabel("node_out", 2);
        resolver.addLabel("signal_a", 3);
        
        List<String> nodeLabels = resolver.findLabelsWithPrefix("node_");
        assertEquals(2, nodeLabels.size());
        assertTrue(nodeLabels.contains("node_in"));
        assertTrue(nodeLabels.contains("node_out"));
    }
    
    @Test
    public void testFindLabelsWithPrefixNoMatch() {
        resolver.addLabel("A", 1);
        List<String> result = resolver.findLabelsWithPrefix("X");
        assertTrue(result.isEmpty());
    }
    
    // ===== Validation Tests =====
    
    @Test
    public void testValidateLabels() {
        resolver.addLabel("VCC", 1);
        resolver.addLabel("GND", 0);
        
        List<String> required = Arrays.asList("VCC", "GND", "OUT");
        List<String> missing = resolver.validateLabels(required);
        
        assertEquals(1, missing.size());
        assertTrue(missing.contains("OUT"));
    }
    
    @Test
    public void testValidateLabelsAllPresent() {
        resolver.addLabel("A", 1);
        resolver.addLabel("B", 2);
        
        List<String> missing = resolver.validateLabels(Arrays.asList("A", "B"));
        assertTrue(missing.isEmpty());
    }
    
    // ===== Merge Tests =====
    
    @Test
    public void testMerge() {
        resolver.addLabel("A", 1);
        resolver.addLabel("B", 2);
        
        LabelResolver other = new LabelResolver();
        other.addLabel("C", 3);
        other.addLabel("D", 4);
        
        LabelResolver merged = resolver.merge(other);
        
        assertEquals(4, merged.getLabelCount());
        assertEquals(1, merged.getIndex("A"));
        assertEquals(3, merged.getIndex("C"));
    }
    
    @Test
    public void testMergeOverwrites() {
        resolver.addLabel("X", 1);
        
        LabelResolver other = new LabelResolver();
        other.addLabel("X", 99);
        
        LabelResolver merged = resolver.merge(other);
        assertEquals(99, merged.getIndex("X")); // Other wins
    }
    
    @Test
    public void testMergeDoesNotModifyOriginals() {
        resolver.addLabel("A", 1);
        
        LabelResolver other = new LabelResolver();
        other.addLabel("B", 2);
        
        LabelResolver merged = resolver.merge(other);
        
        // Originals unchanged
        assertEquals(1, resolver.getLabelCount());
        assertEquals(1, other.getLabelCount());
        assertEquals(2, merged.getLabelCount());
    }
    
    // ===== GetIndexOrThrow Tests =====
    
    @Test
    public void testGetIndexOrThrowFound() {
        resolver.addLabel("VCC", 5);
        assertEquals(5, resolver.getIndexOrThrow("VCC", "TestComponent"));
    }
    
    @Test
    public void testGetIndexOrThrowNotFound() {
        try {
            resolver.getIndexOrThrow("MISSING", "VM1");
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("VM1"));
            assertTrue(e.getMessage().contains("MISSING"));
        }
    }
    
    // ===== ToString, Equals, HashCode Tests =====
    
    @Test
    public void testToString() {
        resolver.addLabel("VCC", 1);
        resolver.addLabel("GND", 0);
        
        String str = resolver.toString();
        assertTrue(str.contains("LabelResolver"));
        assertTrue(str.contains("VCC"));
        assertTrue(str.contains("GND"));
    }
    
    @Test
    public void testEquals() {
        resolver.addLabel("A", 1);
        resolver.addLabel("B", 2);
        
        LabelResolver same = new LabelResolver();
        same.addLabel("A", 1);
        same.addLabel("B", 2);
        
        LabelResolver different = new LabelResolver();
        different.addLabel("A", 1);
        different.addLabel("C", 3);
        
        assertEquals(resolver, same);
        assertNotEquals(resolver, different);
        assertNotEquals(resolver, null);
        assertNotEquals(resolver, "string");
    }
    
    @Test
    public void testHashCode() {
        resolver.addLabel("A", 1);
        
        LabelResolver same = new LabelResolver();
        same.addLabel("A", 1);
        
        assertEquals(resolver.hashCode(), same.hashCode());
    }
    
    // ===== Integration Tests =====
    
    @Test
    public void testTypicalCircuitLabels() {
        // Simulate typical circuit labels
        resolver.addLabel("GND", 0);
        resolver.addLabel("V+", 1);
        resolver.addLabel("V-", 2);
        resolver.addLabel("OUT", 3);
        resolver.addLabel("FB", 4);  // Feedback node
        
        assertEquals(5, resolver.getLabelCount());
        
        // Find power supply labels
        List<String> powerLabels = resolver.findLabelsWithPrefix("V");
        assertEquals(2, powerLabels.size());
        
        // Validate required labels for measurement
        List<String> missing = resolver.validateLabels(
            Arrays.asList("GND", "OUT", "MISSING_LABEL")
        );
        assertEquals(1, missing.size());
        assertEquals("MISSING_LABEL", missing.get(0));
    }
    
    @Test
    public void testSubcircuitLabels() {
        // Simulate subcircuit with hierarchical labels
        resolver.addLabel("TOP.VCC", 10);
        resolver.addLabel("TOP.GND", 11);
        resolver.addLabel("TOP.SUB1.IN", 20);
        resolver.addLabel("TOP.SUB1.OUT", 21);
        resolver.addLabel("TOP.SUB2.IN", 30);
        
        // Find all labels in SUB1
        List<String> sub1Labels = resolver.findLabelsWithPrefix("TOP.SUB1.");
        assertEquals(2, sub1Labels.size());
        
        // Find all labels with pattern
        List<String> inLabels = resolver.findLabelsMatching(".*\\.IN$");
        assertEquals(2, inLabels.size());
    }
}

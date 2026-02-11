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
package ch.technokrat.gecko.geckocircuits.circuit.component;

import ch.technokrat.gecko.geckocircuits.circuit.component.TerminalRegistry.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

/**
 * Unit tests for TerminalRegistry.
 * Tests terminal management for circuit components.
 */
public class TerminalRegistryTest {
    
    private TerminalRegistry<SimpleTerminal> registry;
    private SimpleTerminalAdapter adapter;
    
    @Before
    public void setUp() {
        adapter = new SimpleTerminalAdapter();
        registry = new TerminalRegistry<>(adapter);
    }
    
    // ===== Constructor Tests =====
    
    @Test
    public void testDefaultConstructor() {
        TerminalRegistry<SimpleTerminal> r = new TerminalRegistry<>();
        assertEquals(0, r.getInputCount());
        assertEquals(0, r.getOutputCount());
        assertTrue(r.isEmpty());
    }
    
    @Test
    public void testConstructorWithAdapter() {
        assertSame(adapter, registry.getAdapter());
    }
    
    // ===== Input Terminal Tests =====
    
    @Test
    public void testAddInput() {
        SimpleTerminal t = new SimpleTerminal("IN1");
        registry.addInput(t);
        
        assertEquals(1, registry.getInputCount());
        assertSame(t, registry.getInput(0));
    }
    
    @Test
    public void testAddInputChaining() {
        SimpleTerminal t1 = new SimpleTerminal("IN1");
        SimpleTerminal t2 = new SimpleTerminal("IN2");
        
        registry.addInput(t1).addInput(t2);
        
        assertEquals(2, registry.getInputCount());
    }
    
    @Test
    public void testAddInputAtIndex() {
        SimpleTerminal t1 = new SimpleTerminal("IN1");
        SimpleTerminal t2 = new SimpleTerminal("IN2");
        SimpleTerminal t3 = new SimpleTerminal("IN3");
        
        registry.addInput(t1);
        registry.addInput(t3);
        registry.addInput(1, t2); // Insert between
        
        assertSame(t1, registry.getInput(0));
        assertSame(t2, registry.getInput(1));
        assertSame(t3, registry.getInput(2));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testAddInputNull() {
        registry.addInput(null);
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddInputAtInvalidIndex() {
        registry.addInput(5, new SimpleTerminal("T"));
    }
    
    @Test
    public void testRemoveInput() {
        SimpleTerminal t = new SimpleTerminal("IN1");
        registry.addInput(t);
        
        assertTrue(registry.removeInput(t));
        assertEquals(0, registry.getInputCount());
        assertFalse(registry.removeInput(t)); // Already removed
    }
    
    @Test
    public void testRemoveInputAtIndex() {
        SimpleTerminal t1 = new SimpleTerminal("IN1");
        SimpleTerminal t2 = new SimpleTerminal("IN2");
        registry.addInput(t1).addInput(t2);
        
        SimpleTerminal removed = registry.removeInput(0);
        
        assertSame(t1, removed);
        assertEquals(1, registry.getInputCount());
        assertSame(t2, registry.getInput(0));
    }
    
    @Test
    public void testGetInputs() {
        SimpleTerminal t1 = new SimpleTerminal("IN1");
        SimpleTerminal t2 = new SimpleTerminal("IN2");
        registry.addInput(t1).addInput(t2);
        
        List<SimpleTerminal> inputs = registry.getInputs();
        assertEquals(2, inputs.size());
        assertSame(t1, inputs.get(0));
        assertSame(t2, inputs.get(1));
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testGetInputsUnmodifiable() {
        registry.addInput(new SimpleTerminal("IN1"));
        registry.getInputs().add(new SimpleTerminal("X"));
    }
    
    @Test
    public void testHasInputs() {
        assertFalse(registry.hasInputs());
        registry.addInput(new SimpleTerminal("IN1"));
        assertTrue(registry.hasInputs());
    }
    
    // ===== Output Terminal Tests =====
    
    @Test
    public void testAddOutput() {
        SimpleTerminal t = new SimpleTerminal("OUT1");
        registry.addOutput(t);
        
        assertEquals(1, registry.getOutputCount());
        assertSame(t, registry.getOutput(0));
    }
    
    @Test
    public void testAddOutputChaining() {
        SimpleTerminal t1 = new SimpleTerminal("OUT1");
        SimpleTerminal t2 = new SimpleTerminal("OUT2");
        
        registry.addOutput(t1).addOutput(t2);
        
        assertEquals(2, registry.getOutputCount());
    }
    
    @Test
    public void testAddOutputAtIndex() {
        SimpleTerminal t1 = new SimpleTerminal("OUT1");
        SimpleTerminal t2 = new SimpleTerminal("OUT2");
        
        registry.addOutput(t1);
        registry.addOutput(0, t2); // Insert at beginning
        
        assertSame(t2, registry.getOutput(0));
        assertSame(t1, registry.getOutput(1));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testAddOutputNull() {
        registry.addOutput(null);
    }
    
    @Test
    public void testRemoveOutput() {
        SimpleTerminal t = new SimpleTerminal("OUT1");
        registry.addOutput(t);
        
        assertTrue(registry.removeOutput(t));
        assertEquals(0, registry.getOutputCount());
    }
    
    @Test
    public void testRemoveOutputAtIndex() {
        SimpleTerminal t1 = new SimpleTerminal("OUT1");
        SimpleTerminal t2 = new SimpleTerminal("OUT2");
        registry.addOutput(t1).addOutput(t2);
        
        SimpleTerminal removed = registry.removeOutput(1);
        
        assertSame(t2, removed);
        assertEquals(1, registry.getOutputCount());
    }
    
    @Test
    public void testGetOutputs() {
        SimpleTerminal t1 = new SimpleTerminal("OUT1");
        registry.addOutput(t1);
        
        List<SimpleTerminal> outputs = registry.getOutputs();
        assertEquals(1, outputs.size());
    }
    
    @Test
    public void testHasOutputs() {
        assertFalse(registry.hasOutputs());
        registry.addOutput(new SimpleTerminal("OUT1"));
        assertTrue(registry.hasOutputs());
    }
    
    // ===== Generic Terminal Access Tests =====
    
    @Test
    public void testGetTerminalByType() {
        SimpleTerminal in = new SimpleTerminal("IN1");
        SimpleTerminal out = new SimpleTerminal("OUT1");
        registry.addInput(in).addOutput(out);
        
        assertSame(in, registry.getTerminal(TerminalType.INPUT, 0));
        assertSame(out, registry.getTerminal(TerminalType.OUTPUT, 0));
    }
    
    @Test
    public void testGetTerminalCount() {
        registry.addInput(new SimpleTerminal("IN1"));
        registry.addInput(new SimpleTerminal("IN2"));
        registry.addOutput(new SimpleTerminal("OUT1"));
        
        assertEquals(2, registry.getTerminalCount(TerminalType.INPUT));
        assertEquals(1, registry.getTerminalCount(TerminalType.OUTPUT));
    }
    
    @Test
    public void testGetTotalTerminalCount() {
        registry.addInput(new SimpleTerminal("IN1"));
        registry.addInput(new SimpleTerminal("IN2"));
        registry.addOutput(new SimpleTerminal("OUT1"));
        
        assertEquals(3, registry.getTotalTerminalCount());
    }
    
    @Test
    public void testGetAllTerminals() {
        SimpleTerminal in1 = new SimpleTerminal("IN1");
        SimpleTerminal in2 = new SimpleTerminal("IN2");
        SimpleTerminal out1 = new SimpleTerminal("OUT1");
        
        registry.addInput(in1).addInput(in2).addOutput(out1);
        
        List<SimpleTerminal> all = registry.getAllTerminals();
        assertEquals(3, all.size());
        assertSame(in1, all.get(0));
        assertSame(in2, all.get(1));
        assertSame(out1, all.get(2));
    }
    
    @Test
    public void testIsEmpty() {
        assertTrue(registry.isEmpty());
        registry.addInput(new SimpleTerminal("IN1"));
        assertFalse(registry.isEmpty());
    }
    
    // ===== Lookup Tests =====
    
    @Test
    public void testFindByName() {
        SimpleTerminal t = new SimpleTerminal("MyTerminal");
        registry.addInput(t);
        
        assertSame(t, registry.findByName("MyTerminal"));
        assertSame(t, registry.findByName("MYTERMINAL")); // Case insensitive
        assertNull(registry.findByName("Unknown"));
    }
    
    @Test
    public void testFindByNameInOutputs() {
        SimpleTerminal t = new SimpleTerminal("OutTerm");
        registry.addOutput(t);
        
        assertSame(t, registry.findByName("OutTerm"));
    }
    
    @Test
    public void testFindByNameNull() {
        registry.addInput(new SimpleTerminal("T"));
        assertNull(registry.findByName(null));
    }
    
    @Test
    public void testFindByNameNoAdapter() {
        TerminalRegistry<SimpleTerminal> noAdapter = new TerminalRegistry<>();
        noAdapter.addInput(new SimpleTerminal("T"));
        assertNull(noAdapter.findByName("T"));
    }
    
    @Test
    public void testIndexOf() {
        SimpleTerminal in1 = new SimpleTerminal("IN1");
        SimpleTerminal in2 = new SimpleTerminal("IN2");
        SimpleTerminal out1 = new SimpleTerminal("OUT1");
        
        registry.addInput(in1).addInput(in2).addOutput(out1);
        
        assertEquals(0, registry.indexOf(in1));
        assertEquals(1, registry.indexOf(in2));
        assertEquals(0, registry.indexOf(out1)); // Index in output list
        assertEquals(-1, registry.indexOf(new SimpleTerminal("Unknown")));
    }
    
    @Test
    public void testGetTypeOf() {
        SimpleTerminal in = new SimpleTerminal("IN");
        SimpleTerminal out = new SimpleTerminal("OUT");
        
        registry.addInput(in).addOutput(out);
        
        assertEquals(TerminalType.INPUT, registry.getTypeOf(in));
        assertEquals(TerminalType.OUTPUT, registry.getTypeOf(out));
        assertNull(registry.getTypeOf(new SimpleTerminal("Unknown")));
    }
    
    @Test
    public void testContains() {
        SimpleTerminal t = new SimpleTerminal("T");
        registry.addInput(t);
        
        assertTrue(registry.contains(t));
        assertFalse(registry.contains(new SimpleTerminal("Other")));
    }
    
    // ===== Bulk Operations Tests =====
    
    @Test
    public void testClear() {
        registry.addInput(new SimpleTerminal("IN1"));
        registry.addOutput(new SimpleTerminal("OUT1"));
        
        registry.clear();
        
        assertTrue(registry.isEmpty());
        assertEquals(0, registry.getInputCount());
        assertEquals(0, registry.getOutputCount());
    }
    
    @Test
    public void testClearByType() {
        registry.addInput(new SimpleTerminal("IN1"));
        registry.addOutput(new SimpleTerminal("OUT1"));
        
        registry.clear(TerminalType.INPUT);
        
        assertEquals(0, registry.getInputCount());
        assertEquals(1, registry.getOutputCount());
    }
    
    @Test
    public void testSetInputs() {
        registry.addInput(new SimpleTerminal("OLD"));
        
        SimpleTerminal t1 = new SimpleTerminal("NEW1");
        SimpleTerminal t2 = new SimpleTerminal("NEW2");
        registry.setInputs(Arrays.asList(t1, t2));
        
        assertEquals(2, registry.getInputCount());
        assertSame(t1, registry.getInput(0));
        assertSame(t2, registry.getInput(1));
    }
    
    @Test
    public void testSetInputsWithNulls() {
        registry.setInputs(Arrays.asList(
            new SimpleTerminal("T1"),
            null,
            new SimpleTerminal("T2")
        ));
        
        assertEquals(2, registry.getInputCount()); // Nulls filtered
    }
    
    @Test
    public void testSetOutputs() {
        SimpleTerminal t = new SimpleTerminal("NEW");
        registry.setOutputs(Arrays.asList(t));
        
        assertEquals(1, registry.getOutputCount());
        assertSame(t, registry.getOutput(0));
    }
    
    @Test
    public void testSetOutputsNull() {
        registry.addOutput(new SimpleTerminal("T"));
        registry.setOutputs(null);
        assertEquals(0, registry.getOutputCount());
    }
    
    // ===== Terminal Names Tests =====
    
    @Test
    public void testGetInputNames() {
        registry.addInput(new SimpleTerminal("IN1"));
        registry.addInput(new SimpleTerminal("IN2"));
        
        List<String> names = registry.getInputNames();
        assertEquals(2, names.size());
        assertEquals("IN1", names.get(0));
        assertEquals("IN2", names.get(1));
    }
    
    @Test
    public void testGetOutputNames() {
        registry.addOutput(new SimpleTerminal("OUT1"));
        
        List<String> names = registry.getOutputNames();
        assertEquals(1, names.size());
        assertEquals("OUT1", names.get(0));
    }
    
    // ===== Connection Status Tests =====
    
    @Test
    public void testAreAllInputsConnected() {
        SimpleTerminal t1 = new SimpleTerminal("IN1");
        SimpleTerminal t2 = new SimpleTerminal("IN2");
        t1.setConnected(true);
        t2.setConnected(true);
        
        registry.addInput(t1).addInput(t2);
        
        assertTrue(registry.areAllInputsConnected());
        
        t2.setConnected(false);
        assertFalse(registry.areAllInputsConnected());
    }
    
    @Test
    public void testAreAllInputsConnectedEmpty() {
        assertTrue(registry.areAllInputsConnected()); // Empty = all connected
    }
    
    @Test
    public void testAreAllOutputsConnected() {
        SimpleTerminal t = new SimpleTerminal("OUT1");
        t.setConnected(true);
        registry.addOutput(t);
        
        assertTrue(registry.areAllOutputsConnected());
    }
    
    @Test
    public void testGetUnconnectedInputs() {
        SimpleTerminal t1 = new SimpleTerminal("IN1");
        SimpleTerminal t2 = new SimpleTerminal("IN2");
        t1.setConnected(true);
        t2.setConnected(false);
        
        registry.addInput(t1).addInput(t2);
        
        List<SimpleTerminal> unconnected = registry.getUnconnectedInputs();
        assertEquals(1, unconnected.size());
        assertSame(t2, unconnected.get(0));
    }
    
    @Test
    public void testGetUnconnectedOutputs() {
        SimpleTerminal t = new SimpleTerminal("OUT1");
        t.setConnected(false);
        registry.addOutput(t);
        
        List<SimpleTerminal> unconnected = registry.getUnconnectedOutputs();
        assertEquals(1, unconnected.size());
    }
    
    @Test
    public void testGetConnectedCount() {
        SimpleTerminal t1 = new SimpleTerminal("IN1");
        SimpleTerminal t2 = new SimpleTerminal("OUT1");
        t1.setConnected(true);
        t2.setConnected(true);
        
        registry.addInput(t1).addOutput(t2);
        
        assertEquals(2, registry.getConnectedCount());
    }
    
    @Test
    public void testConnectionStatusNoAdapter() {
        TerminalRegistry<SimpleTerminal> noAdapter = new TerminalRegistry<>();
        noAdapter.addInput(new SimpleTerminal("T"));
        
        assertTrue(noAdapter.areAllInputsConnected()); // Without adapter, assume connected
        assertEquals(0, noAdapter.getConnectedCount());
    }
    
    // ===== Copy Tests =====
    
    @Test
    public void testCopy() {
        registry.addInput(new SimpleTerminal("IN1"));
        registry.addOutput(new SimpleTerminal("OUT1"));
        
        TerminalRegistry<SimpleTerminal> copy = registry.copy();
        
        assertEquals(registry.getInputCount(), copy.getInputCount());
        assertEquals(registry.getOutputCount(), copy.getOutputCount());
        assertSame(registry.getAdapter(), copy.getAdapter());
        
        // Ensure independence
        copy.addInput(new SimpleTerminal("IN2"));
        assertEquals(1, registry.getInputCount());
        assertEquals(2, copy.getInputCount());
    }
    
    // ===== TerminalType Tests =====
    
    @Test
    public void testTerminalTypeLegacyNames() {
        assertEquals("XIN", TerminalType.INPUT.getLegacyName());
        assertEquals("YOUT", TerminalType.OUTPUT.getLegacyName());
    }
    
    // ===== SimpleTerminal Tests =====
    
    @Test
    public void testSimpleTerminal() {
        SimpleTerminal t = new SimpleTerminal("Test");
        
        assertEquals("Test", t.getName());
        assertFalse(t.isConnected());
        assertEquals(-1, t.getNodeIndex());
        
        t.setConnected(true);
        t.setNodeIndex(5);
        
        assertTrue(t.isConnected());
        assertEquals(5, t.getNodeIndex());
    }
    
    @Test
    public void testSimpleTerminalToString() {
        SimpleTerminal t = new SimpleTerminal("T1");
        assertTrue(t.toString().contains("T1"));
        
        t.setConnected(true);
        assertTrue(t.toString().contains("connected"));
    }
    
    // ===== Adapter Tests =====
    
    @Test
    public void testSimpleTerminalAdapter() {
        SimpleTerminal t = new SimpleTerminal("Test");
        t.setConnected(true);
        t.setNodeIndex(10);
        
        assertEquals("Test", adapter.getName(t));
        assertTrue(adapter.isConnected(t));
        assertEquals(10, adapter.getNodeIndex(t));
    }
    
    @Test
    public void testSetAdapter() {
        TerminalRegistry<SimpleTerminal> r = new TerminalRegistry<>();
        assertNull(r.getAdapter());
        
        r.setAdapter(adapter);
        assertSame(adapter, r.getAdapter());
    }
    
    // ===== ToString Tests =====
    
    @Test
    public void testToString() {
        registry.addInput(new SimpleTerminal("IN1"));
        registry.addOutput(new SimpleTerminal("OUT1"));
        registry.addOutput(new SimpleTerminal("OUT2"));
        
        String str = registry.toString();
        assertTrue(str.contains("inputs=1"));
        assertTrue(str.contains("outputs=2"));
    }
    
    // ===== Integration Tests =====
    
    @Test
    public void testTypicalTwoPortComponent() {
        // Simulate a resistor with 2 terminals
        SimpleTerminal p = new SimpleTerminal("p");
        SimpleTerminal n = new SimpleTerminal("n");
        
        registry.addInput(p).addOutput(n);
        
        assertEquals(2, registry.getTotalTerminalCount());
        assertSame(p, registry.findByName("p"));
        assertSame(n, registry.findByName("n"));
    }
    
    @Test
    public void testTypicalSemiconductor() {
        // Simulate a transistor with 3 terminals
        SimpleTerminal drain = new SimpleTerminal("drain");
        SimpleTerminal gate = new SimpleTerminal("gate");
        SimpleTerminal source = new SimpleTerminal("source");
        
        registry.addInput(drain).addInput(gate).addOutput(source);
        
        assertEquals(3, registry.getTotalTerminalCount());
        assertEquals(2, registry.getInputCount());
        assertEquals(1, registry.getOutputCount());
    }
    
    @Test
    public void testControlBlockWithMultipleIO() {
        // Simulate a control block with multiple inputs/outputs
        for (int i = 0; i < 5; i++) {
            registry.addInput(new SimpleTerminal("in" + i));
            registry.addOutput(new SimpleTerminal("out" + i));
        }

        assertEquals(5, registry.getInputCount());
        assertEquals(5, registry.getOutputCount());
        assertEquals(10, registry.getTotalTerminalCount());

        List<String> inputNames = registry.getInputNames();
        assertEquals("in0", inputNames.get(0));
        assertEquals("in4", inputNames.get(4));
    }

    // ===== Edge Cases and Boundary Tests =====

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetInputInvalidIndex() {
        registry.addInput(new SimpleTerminal("IN1"));
        registry.getInput(5); // Out of bounds
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetOutputInvalidIndex() {
        registry.addOutput(new SimpleTerminal("OUT1"));
        registry.getOutput(5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveInputInvalidIndex() {
        registry.addInput(new SimpleTerminal("IN1"));
        registry.removeInput(5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveOutputInvalidIndex() {
        registry.addOutput(new SimpleTerminal("OUT1"));
        registry.removeOutput(5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddInputAtNegativeIndex() {
        registry.addInput(-1, new SimpleTerminal("T"));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddOutputAtNegativeIndex() {
        registry.addOutput(-1, new SimpleTerminal("T"));
    }

    @Test
    public void testAddInputAtBoundaryIndex() {
        SimpleTerminal t1 = new SimpleTerminal("IN1");
        SimpleTerminal t2 = new SimpleTerminal("IN2");
        registry.addInput(t1);
        registry.addInput(1, t2); // Add at end

        assertEquals(2, registry.getInputCount());
        assertSame(t2, registry.getInput(1));
    }

    @Test
    public void testAddOutputAtBoundaryIndex() {
        SimpleTerminal t1 = new SimpleTerminal("OUT1");
        SimpleTerminal t2 = new SimpleTerminal("OUT2");
        registry.addOutput(t1);
        registry.addOutput(1, t2);

        assertEquals(2, registry.getOutputCount());
        assertSame(t2, registry.getOutput(1));
    }

    @Test
    public void testRemoveInputNotFound() {
        registry.addInput(new SimpleTerminal("IN1"));
        assertFalse(registry.removeInput(new SimpleTerminal("IN2")));
    }

    @Test
    public void testRemoveOutputNotFound() {
        registry.addOutput(new SimpleTerminal("OUT1"));
        assertFalse(registry.removeOutput(new SimpleTerminal("OUT2")));
    }

    @Test
    public void testGetTerminalsInputType() {
        SimpleTerminal t1 = new SimpleTerminal("IN1");
        SimpleTerminal t2 = new SimpleTerminal("IN2");
        registry.addInput(t1).addInput(t2);

        List<SimpleTerminal> terminals = registry.getTerminals(TerminalType.INPUT);
        assertEquals(2, terminals.size());
        assertSame(t1, terminals.get(0));
        assertSame(t2, terminals.get(1));
    }

    @Test
    public void testGetTerminalsOutputType() {
        SimpleTerminal t = new SimpleTerminal("OUT1");
        registry.addOutput(t);

        List<SimpleTerminal> terminals = registry.getTerminals(TerminalType.OUTPUT);
        assertEquals(1, terminals.size());
        assertSame(t, terminals.get(0));
    }

    @Test
    public void testFindByNameInBothInputsAndOutputs() {
        SimpleTerminal in = new SimpleTerminal("T");
        SimpleTerminal out = new SimpleTerminal("T");

        registry.addInput(in);
        registry.addOutput(out);

        // Should find input first
        assertSame(in, registry.findByName("T"));
    }

    @Test
    public void testFindByNameCaseSensitive() {
        SimpleTerminal t = new SimpleTerminal("Test");
        registry.addInput(t);

        assertSame(t, registry.findByName("test")); // Case insensitive
        assertSame(t, registry.findByName("TEST"));
        assertSame(t, registry.findByName("Test"));
    }

    @Test
    public void testIndexOfInputNotFound() {
        SimpleTerminal t1 = new SimpleTerminal("IN1");
        SimpleTerminal t2 = new SimpleTerminal("IN2");
        registry.addInput(t1);

        assertEquals(-1, registry.indexOf(t2));
    }

    @Test
    public void testClearSpecificTypeInputEmpty() {
        registry.addOutput(new SimpleTerminal("OUT1"));
        registry.clear(TerminalType.INPUT);

        assertEquals(0, registry.getInputCount());
        assertEquals(1, registry.getOutputCount());
    }

    @Test
    public void testClearSpecificTypeOutputEmpty() {
        registry.addInput(new SimpleTerminal("IN1"));
        registry.clear(TerminalType.OUTPUT);

        assertEquals(1, registry.getInputCount());
        assertEquals(0, registry.getOutputCount());
    }

    @Test
    public void testSetInputsEmpty() {
        registry.addInput(new SimpleTerminal("OLD"));
        registry.setInputs(new ArrayList<>());

        assertEquals(0, registry.getInputCount());
    }

    @Test
    public void testSetOutputsEmpty() {
        registry.addOutput(new SimpleTerminal("OLD"));
        registry.setOutputs(new ArrayList<>());

        assertEquals(0, registry.getOutputCount());
    }

    @Test
    public void testSetInputsAllNulls() {
        registry.setInputs(Arrays.asList(null, null, null));
        assertEquals(0, registry.getInputCount());
    }

    @Test
    public void testGetInputNamesEmpty() {
        List<String> names = registry.getInputNames();
        assertTrue(names.isEmpty());
    }

    @Test
    public void testGetOutputNamesEmpty() {
        List<String> names = registry.getOutputNames();
        assertTrue(names.isEmpty());
    }

    @Test
    public void testGetInputNamesWithoutAdapter() {
        TerminalRegistry<SimpleTerminal> noAdapter = new TerminalRegistry<>();
        noAdapter.addInput(new SimpleTerminal("T"));

        List<String> names = noAdapter.getInputNames();
        assertEquals(1, names.size());
        assertTrue(names.get(0).contains("Terminal")); // toString fallback
    }

    @Test
    public void testGetUnconnectedInputsEmpty() {
        List<SimpleTerminal> unconnected = registry.getUnconnectedInputs();
        assertTrue(unconnected.isEmpty());
    }

    @Test
    public void testGetUnconnectedOutputsEmpty() {
        List<SimpleTerminal> unconnected = registry.getUnconnectedOutputs();
        assertTrue(unconnected.isEmpty());
    }

    @Test
    public void testGetUnconnectedInputsAllConnected() {
        SimpleTerminal t1 = new SimpleTerminal("IN1");
        SimpleTerminal t2 = new SimpleTerminal("IN2");
        t1.setConnected(true);
        t2.setConnected(true);

        registry.addInput(t1).addInput(t2);

        List<SimpleTerminal> unconnected = registry.getUnconnectedInputs();
        assertTrue(unconnected.isEmpty());
    }

    @Test
    public void testGetConnectedCountEmpty() {
        assertEquals(0, registry.getConnectedCount());
    }

    @Test
    public void testGetConnectedCountAllConnected() {
        SimpleTerminal t1 = new SimpleTerminal("IN1");
        SimpleTerminal t2 = new SimpleTerminal("OUT1");
        t1.setConnected(true);
        t2.setConnected(true);

        registry.addInput(t1).addOutput(t2);

        assertEquals(2, registry.getConnectedCount());
    }

    @Test
    public void testGetConnectedCountMixed() {
        SimpleTerminal t1 = new SimpleTerminal("IN1");
        SimpleTerminal t2 = new SimpleTerminal("OUT1");
        t1.setConnected(true);
        t2.setConnected(false);

        registry.addInput(t1).addOutput(t2);

        assertEquals(1, registry.getConnectedCount());
    }

    @Test
    public void testGetAllTerminalsEmpty() {
        List<SimpleTerminal> all = registry.getAllTerminals();
        assertTrue(all.isEmpty());
    }

    @Test
    public void testGetAllTerminalsMixed() {
        SimpleTerminal in1 = new SimpleTerminal("IN1");
        SimpleTerminal in2 = new SimpleTerminal("IN2");
        SimpleTerminal out1 = new SimpleTerminal("OUT1");

        registry.addInput(in1).addInput(in2).addOutput(out1);

        List<SimpleTerminal> all = registry.getAllTerminals();
        assertEquals(3, all.size());
        // Inputs come first
        assertSame(in1, all.get(0));
        assertSame(in2, all.get(1));
        assertSame(out1, all.get(2));
    }

    @Test
    public void testContainsEmpty() {
        assertFalse(registry.contains(new SimpleTerminal("T")));
    }

    @Test
    public void testCopyEmpty() {
        TerminalRegistry<SimpleTerminal> copy = registry.copy();
        assertEquals(0, copy.getInputCount());
        assertEquals(0, copy.getOutputCount());
    }

    @Test
    public void testSimpleTerminalNodeIndexBoundary() {
        SimpleTerminal t = new SimpleTerminal("T");
        t.setNodeIndex(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, t.getNodeIndex());

        t.setNodeIndex(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, t.getNodeIndex());
    }

    @Test
    public void testSimpleTerminalAdapterNodeIndex() {
        SimpleTerminal t = new SimpleTerminal("Test");
        t.setNodeIndex(42);

        assertEquals(42, adapter.getNodeIndex(t));
    }

    @Test
    public void testToStringEmpty() {
        String str = registry.toString();
        assertTrue(str.contains("inputs=0"));
        assertTrue(str.contains("outputs=0"));
    }
}

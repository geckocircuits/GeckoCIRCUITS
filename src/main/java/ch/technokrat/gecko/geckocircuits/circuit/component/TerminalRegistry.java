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

import java.util.*;

/**
 * Manages component terminal connections (input/output nodes).
 * Extracted from AbstractBlockInterface to separate terminal management.
 * 
 * <p>Terminal Types:
 * <ul>
 *   <li>Input terminals (XIN) - typically on left side of component</li>
 *   <li>Output terminals (YOUT) - typically on right side of component</li>
 * </ul>
 * 
 * <p>Thread-safety: Not thread-safe. External synchronization required.
 * 
 * @param <T> Terminal type
 * @author Sprint 3 refactoring
 */
public class TerminalRegistry<T> {
    
    /** Terminal type enumeration. */
    public enum TerminalType {
        INPUT("XIN"),
        OUTPUT("YOUT");
        
        private final String legacyName;
        
        TerminalType(String legacyName) {
            this.legacyName = legacyName;
        }
        
        public String getLegacyName() {
            return legacyName;
        }
    }
    
    // Terminal storage using lists for order preservation
    private final List<T> inputTerminals;
    private final List<T> outputTerminals;
    
    // Optional terminal adapter for flexible operations
    private TerminalAdapter<T> adapter;
    
    /**
     * Creates an empty terminal registry.
     */
    public TerminalRegistry() {
        this.inputTerminals = new ArrayList<>();
        this.outputTerminals = new ArrayList<>();
        this.adapter = null;
    }
    
    /**
     * Creates a terminal registry with adapter.
     * 
     * @param adapter Terminal adapter for operations
     */
    public TerminalRegistry(TerminalAdapter<T> adapter) {
        this();
        this.adapter = adapter;
    }
    
    // ===== Input Terminal Management =====
    
    /**
     * Adds an input terminal.
     * 
     * @param terminal Terminal to add
     * @return this for chaining
     */
    public TerminalRegistry<T> addInput(T terminal) {
        if (terminal == null) {
            throw new IllegalArgumentException("Terminal cannot be null");
        }
        inputTerminals.add(terminal);
        return this;
    }
    
    /**
     * Adds an input terminal at specific index.
     * 
     * @param index Index to insert at
     * @param terminal Terminal to add
     */
    public void addInput(int index, T terminal) {
        if (terminal == null) {
            throw new IllegalArgumentException("Terminal cannot be null");
        }
        if (index < 0 || index > inputTerminals.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + inputTerminals.size());
        }
        inputTerminals.add(index, terminal);
    }
    
    /**
     * Gets input terminal at index.
     * 
     * @param index Terminal index
     * @return Terminal at index
     */
    public T getInput(int index) {
        return inputTerminals.get(index);
    }
    
    /**
     * Removes input terminal.
     * 
     * @param terminal Terminal to remove
     * @return true if removed
     */
    public boolean removeInput(T terminal) {
        return inputTerminals.remove(terminal);
    }
    
    /**
     * Removes input terminal at index.
     * 
     * @param index Index to remove
     * @return Removed terminal
     */
    public T removeInput(int index) {
        return inputTerminals.remove(index);
    }
    
    /**
     * Gets number of input terminals.
     */
    public int getInputCount() {
        return inputTerminals.size();
    }
    
    /**
     * Gets all input terminals as unmodifiable list.
     */
    public List<T> getInputs() {
        return Collections.unmodifiableList(inputTerminals);
    }
    
    /**
     * Checks if has any input terminals.
     */
    public boolean hasInputs() {
        return !inputTerminals.isEmpty();
    }
    
    // ===== Output Terminal Management =====
    
    /**
     * Adds an output terminal.
     * 
     * @param terminal Terminal to add
     * @return this for chaining
     */
    public TerminalRegistry<T> addOutput(T terminal) {
        if (terminal == null) {
            throw new IllegalArgumentException("Terminal cannot be null");
        }
        outputTerminals.add(terminal);
        return this;
    }
    
    /**
     * Adds an output terminal at specific index.
     * 
     * @param index Index to insert at
     * @param terminal Terminal to add
     */
    public void addOutput(int index, T terminal) {
        if (terminal == null) {
            throw new IllegalArgumentException("Terminal cannot be null");
        }
        if (index < 0 || index > outputTerminals.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + outputTerminals.size());
        }
        outputTerminals.add(index, terminal);
    }
    
    /**
     * Gets output terminal at index.
     * 
     * @param index Terminal index
     * @return Terminal at index
     */
    public T getOutput(int index) {
        return outputTerminals.get(index);
    }
    
    /**
     * Removes output terminal.
     * 
     * @param terminal Terminal to remove
     * @return true if removed
     */
    public boolean removeOutput(T terminal) {
        return outputTerminals.remove(terminal);
    }
    
    /**
     * Removes output terminal at index.
     * 
     * @param index Index to remove
     * @return Removed terminal
     */
    public T removeOutput(int index) {
        return outputTerminals.remove(index);
    }
    
    /**
     * Gets number of output terminals.
     */
    public int getOutputCount() {
        return outputTerminals.size();
    }
    
    /**
     * Gets all output terminals as unmodifiable list.
     */
    public List<T> getOutputs() {
        return Collections.unmodifiableList(outputTerminals);
    }
    
    /**
     * Checks if has any output terminals.
     */
    public boolean hasOutputs() {
        return !outputTerminals.isEmpty();
    }
    
    // ===== Generic Terminal Access =====
    
    /**
     * Gets terminal by type and index.
     * 
     * @param type Terminal type (INPUT or OUTPUT)
     * @param index Terminal index
     * @return Terminal at position
     */
    public T getTerminal(TerminalType type, int index) {
        return type == TerminalType.INPUT ? getInput(index) : getOutput(index);
    }
    
    /**
     * Gets terminal count by type.
     */
    public int getTerminalCount(TerminalType type) {
        return type == TerminalType.INPUT ? getInputCount() : getOutputCount();
    }
    
    /**
     * Gets all terminals of given type.
     */
    public List<T> getTerminals(TerminalType type) {
        return type == TerminalType.INPUT ? getInputs() : getOutputs();
    }
    
    /**
     * Gets total terminal count (input + output).
     */
    public int getTotalTerminalCount() {
        return inputTerminals.size() + outputTerminals.size();
    }
    
    /**
     * Checks if registry is empty (no terminals).
     */
    public boolean isEmpty() {
        return inputTerminals.isEmpty() && outputTerminals.isEmpty();
    }
    
    /**
     * Gets all terminals as a combined list (inputs first, then outputs).
     */
    public List<T> getAllTerminals() {
        List<T> all = new ArrayList<>(inputTerminals.size() + outputTerminals.size());
        all.addAll(inputTerminals);
        all.addAll(outputTerminals);
        return Collections.unmodifiableList(all);
    }
    
    // ===== Lookup Operations =====
    
    /**
     * Finds terminal by name using adapter.
     * 
     * @param name Terminal name
     * @return Found terminal or null
     */
    public T findByName(String name) {
        if (name == null || adapter == null) {
            return null;
        }
        
        // Search inputs
        for (T terminal : inputTerminals) {
            if (name.equalsIgnoreCase(adapter.getName(terminal))) {
                return terminal;
            }
        }
        
        // Search outputs
        for (T terminal : outputTerminals) {
            if (name.equalsIgnoreCase(adapter.getName(terminal))) {
                return terminal;
            }
        }
        
        return null;
    }
    
    /**
     * Gets index of terminal in its type's list.
     * 
     * @param terminal Terminal to find
     * @return Index or -1 if not found
     */
    public int indexOf(T terminal) {
        int idx = inputTerminals.indexOf(terminal);
        if (idx >= 0) return idx;
        return outputTerminals.indexOf(terminal);
    }
    
    /**
     * Gets type of terminal.
     * 
     * @param terminal Terminal to check
     * @return Terminal type or null if not found
     */
    public TerminalType getTypeOf(T terminal) {
        if (inputTerminals.contains(terminal)) {
            return TerminalType.INPUT;
        }
        if (outputTerminals.contains(terminal)) {
            return TerminalType.OUTPUT;
        }
        return null;
    }
    
    /**
     * Checks if terminal exists in registry.
     */
    public boolean contains(T terminal) {
        return inputTerminals.contains(terminal) || outputTerminals.contains(terminal);
    }
    
    // ===== Bulk Operations =====
    
    /**
     * Clears all terminals.
     */
    public void clear() {
        inputTerminals.clear();
        outputTerminals.clear();
    }
    
    /**
     * Clears terminals of specific type.
     */
    public void clear(TerminalType type) {
        if (type == TerminalType.INPUT) {
            inputTerminals.clear();
        } else {
            outputTerminals.clear();
        }
    }
    
    /**
     * Sets all input terminals (replaces existing).
     */
    public void setInputs(List<T> terminals) {
        inputTerminals.clear();
        if (terminals != null) {
            for (T t : terminals) {
                if (t != null) {
                    inputTerminals.add(t);
                }
            }
        }
    }
    
    /**
     * Sets all output terminals (replaces existing).
     */
    public void setOutputs(List<T> terminals) {
        outputTerminals.clear();
        if (terminals != null) {
            for (T t : terminals) {
                if (t != null) {
                    outputTerminals.add(t);
                }
            }
        }
    }
    
    // ===== Adapter Operations =====
    
    /**
     * Sets the terminal adapter.
     */
    public void setAdapter(TerminalAdapter<T> adapter) {
        this.adapter = adapter;
    }
    
    /**
     * Gets the terminal adapter.
     */
    public TerminalAdapter<T> getAdapter() {
        return adapter;
    }
    
    /**
     * Gets all terminal names (requires adapter).
     */
    public List<String> getInputNames() {
        List<String> names = new ArrayList<>(inputTerminals.size());
        for (T terminal : inputTerminals) {
            names.add(adapter != null ? adapter.getName(terminal) : String.valueOf(terminal));
        }
        return names;
    }
    
    /**
     * Gets all output terminal names (requires adapter).
     */
    public List<String> getOutputNames() {
        List<String> names = new ArrayList<>(outputTerminals.size());
        for (T terminal : outputTerminals) {
            names.add(adapter != null ? adapter.getName(terminal) : String.valueOf(terminal));
        }
        return names;
    }
    
    // ===== Connection Status (requires adapter) =====
    
    /**
     * Checks if all input terminals are connected.
     */
    public boolean areAllInputsConnected() {
        if (adapter == null) {
            return true; // Cannot check without adapter
        }
        for (T terminal : inputTerminals) {
            if (!adapter.isConnected(terminal)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if all output terminals are connected.
     */
    public boolean areAllOutputsConnected() {
        if (adapter == null) {
            return true;
        }
        for (T terminal : outputTerminals) {
            if (!adapter.isConnected(terminal)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gets list of unconnected input terminals.
     */
    public List<T> getUnconnectedInputs() {
        List<T> unconnected = new ArrayList<>();
        if (adapter != null) {
            for (T terminal : inputTerminals) {
                if (!adapter.isConnected(terminal)) {
                    unconnected.add(terminal);
                }
            }
        }
        return unconnected;
    }
    
    /**
     * Gets list of unconnected output terminals.
     */
    public List<T> getUnconnectedOutputs() {
        List<T> unconnected = new ArrayList<>();
        if (adapter != null) {
            for (T terminal : outputTerminals) {
                if (!adapter.isConnected(terminal)) {
                    unconnected.add(terminal);
                }
            }
        }
        return unconnected;
    }
    
    /**
     * Gets count of connected terminals.
     */
    public int getConnectedCount() {
        if (adapter == null) {
            return 0;
        }
        int count = 0;
        for (T terminal : inputTerminals) {
            if (adapter.isConnected(terminal)) count++;
        }
        for (T terminal : outputTerminals) {
            if (adapter.isConnected(terminal)) count++;
        }
        return count;
    }
    
    // ===== Copy/Clone =====
    
    /**
     * Creates a shallow copy of this registry.
     */
    public TerminalRegistry<T> copy() {
        TerminalRegistry<T> copy = new TerminalRegistry<>(adapter);
        copy.inputTerminals.addAll(this.inputTerminals);
        copy.outputTerminals.addAll(this.outputTerminals);
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("TerminalRegistry[inputs=%d, outputs=%d]",
            inputTerminals.size(), outputTerminals.size());
    }
    
    // ===== Adapter Interface =====
    
    /**
     * Adapter interface for terminal operations.
     * 
     * @param <T> Terminal type
     */
    public interface TerminalAdapter<T> {
        /**
         * Gets terminal name.
         */
        String getName(T terminal);
        
        /**
         * Checks if terminal is connected.
         */
        boolean isConnected(T terminal);
        
        /**
         * Gets terminal node index (for circuit solving).
         */
        default int getNodeIndex(T terminal) {
            return -1;
        }
    }
    
    /**
     * Simple terminal implementation for testing.
     */
    public static class SimpleTerminal {
        private final String name;
        private boolean connected;
        private int nodeIndex;
        
        public SimpleTerminal(String name) {
            this.name = name;
            this.connected = false;
            this.nodeIndex = -1;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean isConnected() {
            return connected;
        }
        
        public void setConnected(boolean connected) {
            this.connected = connected;
        }
        
        public int getNodeIndex() {
            return nodeIndex;
        }
        
        public void setNodeIndex(int nodeIndex) {
            this.nodeIndex = nodeIndex;
        }
        
        @Override
        public String toString() {
            return "Terminal[" + name + (connected ? ", connected" : "") + "]";
        }
    }
    
    /**
     * Adapter for SimpleTerminal.
     */
    public static class SimpleTerminalAdapter implements TerminalAdapter<SimpleTerminal> {
        @Override
        public String getName(SimpleTerminal terminal) {
            return terminal.getName();
        }
        
        @Override
        public boolean isConnected(SimpleTerminal terminal) {
            return terminal.isConnected();
        }
        
        @Override
        public int getNodeIndex(SimpleTerminal terminal) {
            return terminal.getNodeIndex();
        }
    }
}

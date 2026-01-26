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
 * Registry for managing component parameters (UserParameter instances).
 * Extracted from AbstractBlockInterface for better testability and separation of concerns.
 * 
 * <p>Responsibilities:
 * <ul>
 *   <li>Register and unregister parameters</li>
 *   <li>Lookup parameters by name (short name, alternative name)</li>
 *   <li>Provide parameter value access (get/set)</li>
 *   <li>Manage parameter-to-array synchronization</li>
 *   <li>Support optimizer parameter naming (nameOpt)</li>
 * </ul>
 * 
 * <p>Parameter concepts in GeckoCIRCUITS:
 * <ul>
 *   <li><b>Short name</b>: Brief identifier like "R", "L", "C"</li>
 *   <li><b>Long name</b>: Descriptive name like "Resistance", "Inductance"</li>
 *   <li><b>Alternative name</b>: Backward-compatible alias</li>
 *   <li><b>nameOpt</b>: Optimizer variable name reference</li>
 *   <li><b>Index</b>: Position in parameter[] array</li>
 * </ul>
 * 
 * @author Extracted from AbstractBlockInterface
 * @since Sprint 3 - Circuit Refactoring
 * @param <P> The parameter type (typically UserParameter or a compatible interface)
 */
public class ParameterRegistry<P> {
    
    /**
     * Interface for parameter access - allows working with different parameter types.
     */
    public interface Parameter {
        String getShortName();
        String getAlternativeShortName();
        String getLongName();
        String getUnit();
        double getDoubleValue();
        void setFromDoubleValue(double value);
        String getNameOpt();
        void setNameOpt(String name);
        int getIndex();
        void writeToParamterArray(double[] array);
        void readFromParameterArray(double[] array);
        void readFromParamterArrayWithoutUndo(double[] array);
        void writeNameOptArray(String[] nameOpt);
        void readFromNameOptArray(String[] nameOpt);
    }
    
    /** Registered parameters in order of registration */
    private final List<P> parameters;
    
    /** Index by short name (case-insensitive) */
    private final Map<String, P> byShortName;
    
    /** Index by alternative name (case-insensitive) */
    private final Map<String, P> byAlternativeName;
    
    /** Adapter for extracting names from parameter objects */
    private final ParameterAdapter<P> adapter;
    
    /**
     * Adapter interface to extract information from parameter objects.
     * Allows this registry to work with different parameter types.
     */
    public interface ParameterAdapter<P> {
        String getShortName(P param);
        String getAlternativeShortName(P param);
        String getLongName(P param);
        String getUnit(P param);
        double getDoubleValue(P param);
        void setFromDoubleValue(P param, double value);
    }
    
    /**
     * Creates an empty parameter registry with a custom adapter.
     * 
     * @param adapter adapter for parameter access
     */
    public ParameterRegistry(ParameterAdapter<P> adapter) {
        this.parameters = new ArrayList<>();
        this.byShortName = new HashMap<>();
        this.byAlternativeName = new HashMap<>();
        this.adapter = adapter;
    }
    
    /**
     * Creates an empty parameter registry with a default string-based adapter.
     * Used for simple testing scenarios.
     */
    @SuppressWarnings("unchecked")
    public ParameterRegistry() {
        this((ParameterAdapter<P>) new SimpleParameterAdapter());
    }
    
    /**
     * Registers a parameter.
     * 
     * @param param the parameter to register
     * @throws IllegalArgumentException if parameter is null
     */
    public void register(P param) {
        if (param == null) {
            throw new IllegalArgumentException("Parameter cannot be null");
        }
        
        parameters.add(param);
        
        String shortName = adapter.getShortName(param);
        if (shortName != null && !shortName.isEmpty()) {
            byShortName.put(shortName.toLowerCase(), param);
        }
        
        String altName = adapter.getAlternativeShortName(param);
        if (altName != null && !altName.isEmpty()) {
            byAlternativeName.put(altName.toLowerCase(), param);
        }
    }
    
    /**
     * Unregisters a parameter.
     * 
     * @param param the parameter to unregister
     * @return true if the parameter was removed
     */
    public boolean unregister(P param) {
        boolean removed = parameters.remove(param);
        if (removed) {
            String shortName = adapter.getShortName(param);
            if (shortName != null) {
                byShortName.remove(shortName.toLowerCase());
            }
            String altName = adapter.getAlternativeShortName(param);
            if (altName != null) {
                byAlternativeName.remove(altName.toLowerCase());
            }
        }
        return removed;
    }
    
    /**
     * Finds a parameter by its short name (case-insensitive).
     * 
     * @param name the short name to search for
     * @return the parameter, or null if not found
     */
    public P findByShortName(String name) {
        if (name == null) return null;
        return byShortName.get(name.toLowerCase());
    }
    
    /**
     * Finds a parameter by its alternative name (case-insensitive).
     * 
     * @param name the alternative name to search for
     * @return the parameter, or null if not found
     */
    public P findByAlternativeName(String name) {
        if (name == null) return null;
        return byAlternativeName.get(name.toLowerCase());
    }
    
    /**
     * Finds a parameter by any known name (short or alternative).
     * Short name takes precedence.
     * 
     * @param name the name to search for
     * @return the parameter, or null if not found
     */
    public P findByAnyName(String name) {
        P result = findByShortName(name);
        if (result == null) {
            result = findByAlternativeName(name);
        }
        return result;
    }
    
    /**
     * Gets a parameter value by name.
     * 
     * @param name parameter name
     * @return the double value
     * @throws IllegalArgumentException if parameter not found
     */
    public double getValue(String name) {
        P param = findByAnyName(name);
        if (param == null) {
            throw new IllegalArgumentException("Parameter not found: " + name);
        }
        return adapter.getDoubleValue(param);
    }
    
    /**
     * Sets a parameter value by name.
     * 
     * @param name parameter name
     * @param value the value to set
     * @throws IllegalArgumentException if parameter not found
     */
    public void setValue(String name, double value) {
        P param = findByAnyName(name);
        if (param == null) {
            throw new IllegalArgumentException("Parameter not found: " + name);
        }
        adapter.setFromDoubleValue(param, value);
    }
    
    /**
     * Checks if a parameter with the given name exists.
     * 
     * @param name the name to check
     * @return true if a parameter with this name exists
     */
    public boolean hasParameter(String name) {
        return findByAnyName(name) != null;
    }
    
    /**
     * Gets all registered parameters.
     * 
     * @return unmodifiable list of parameters
     */
    public List<P> getAll() {
        return Collections.unmodifiableList(parameters);
    }
    
    /**
     * Gets the number of registered parameters.
     * 
     * @return parameter count
     */
    public int size() {
        return parameters.size();
    }
    
    /**
     * Checks if the registry is empty.
     * 
     * @return true if no parameters are registered
     */
    public boolean isEmpty() {
        return parameters.isEmpty();
    }
    
    /**
     * Clears all registered parameters.
     */
    public void clear() {
        parameters.clear();
        byShortName.clear();
        byAlternativeName.clear();
    }
    
    /**
     * Gets all short names of registered parameters.
     * 
     * @return list of short names
     */
    public List<String> getAllShortNames() {
        List<String> names = new ArrayList<>();
        for (P param : parameters) {
            String name = adapter.getShortName(param);
            if (name != null && !name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }
    
    /**
     * Gets all long/verbose names of registered parameters.
     * 
     * @return list of long names
     */
    public List<String> getAllLongNames() {
        List<String> names = new ArrayList<>();
        for (P param : parameters) {
            String name = adapter.getLongName(param);
            if (name != null && !name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }
    
    /**
     * Gets all units of registered parameters.
     * 
     * @return list of units
     */
    public List<String> getAllUnits() {
        List<String> units = new ArrayList<>();
        for (P param : parameters) {
            String unit = adapter.getUnit(param);
            units.add(unit != null ? unit : "");
        }
        return units;
    }
    
    /**
     * Gets all parameter values as an array.
     * 
     * @return array of parameter values
     */
    public double[] getAllValues() {
        double[] values = new double[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            values[i] = adapter.getDoubleValue(parameters.get(i));
        }
        return values;
    }
    
    /**
     * Sets all parameter values from an array.
     * 
     * @param values array of values (must match parameter count)
     * @throws IllegalArgumentException if array size doesn't match
     */
    public void setAllValues(double[] values) {
        if (values.length != parameters.size()) {
            throw new IllegalArgumentException(
                "Array size " + values.length + " doesn't match parameter count " + parameters.size());
        }
        for (int i = 0; i < parameters.size(); i++) {
            adapter.setFromDoubleValue(parameters.get(i), values[i]);
        }
    }
    
    /**
     * Creates a map of parameter names to values.
     * 
     * @return map of short name to value
     */
    public Map<String, Double> toMap() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (P param : parameters) {
            String name = adapter.getShortName(param);
            if (name != null && !name.isEmpty()) {
                map.put(name, adapter.getDoubleValue(param));
            }
        }
        return map;
    }
    
    /**
     * Validates that all required parameter names exist.
     * 
     * @param requiredNames names that must exist
     * @return list of missing names (empty if all present)
     */
    public List<String> validateRequired(Collection<String> requiredNames) {
        List<String> missing = new ArrayList<>();
        for (String name : requiredNames) {
            if (!hasParameter(name)) {
                missing.add(name);
            }
        }
        return missing;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ParameterRegistry[count=").append(parameters.size()).append("]\n");
        for (P param : parameters) {
            sb.append("  ").append(adapter.getShortName(param));
            sb.append(" = ").append(adapter.getDoubleValue(param));
            String unit = adapter.getUnit(param);
            if (unit != null && !unit.isEmpty()) {
                sb.append(" ").append(unit);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Simple adapter for testing with SimpleParameter objects.
     */
    public static class SimpleParameterAdapter implements ParameterAdapter<SimpleParameter> {
        @Override
        public String getShortName(SimpleParameter param) { return param.shortName; }
        @Override
        public String getAlternativeShortName(SimpleParameter param) { return param.alternativeName; }
        @Override
        public String getLongName(SimpleParameter param) { return param.longName; }
        @Override
        public String getUnit(SimpleParameter param) { return param.unit; }
        @Override
        public double getDoubleValue(SimpleParameter param) { return param.value; }
        @Override
        public void setFromDoubleValue(SimpleParameter param, double value) { param.value = value; }
    }
    
    /**
     * Simple parameter class for testing purposes.
     */
    public static class SimpleParameter {
        public String shortName;
        public String alternativeName;
        public String longName;
        public String unit;
        public double value;
        
        public SimpleParameter(String shortName, double value) {
            this.shortName = shortName;
            this.value = value;
        }
        
        public SimpleParameter(String shortName, String longName, String unit, double value) {
            this.shortName = shortName;
            this.longName = longName;
            this.unit = unit;
            this.value = value;
        }
        
        public SimpleParameter withAlternativeName(String altName) {
            this.alternativeName = altName;
            return this;
        }
    }
}

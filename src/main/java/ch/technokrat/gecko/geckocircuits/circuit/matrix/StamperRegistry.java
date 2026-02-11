/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit.matrix;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry that maps circuit component types to their matrix stamper implementations.
 *
 * This class implements the Strategy pattern for MNA (Modified Nodal Analysis) matrix
 * stamping. Each circuit component type (resistor, capacitor, etc.) has a corresponding
 * stamper that knows how to contribute to the system matrices.
 *
 * Usage:
 * <pre>
 *     StamperRegistry registry = StamperRegistry.createDefault();
 *     IMatrixStamper stamper = registry.getStamper(CircuitTyp.LK_R);
 *     stamper.stampMatrixA(a, nodeX, nodeY, nodeZ, params, dt);
 * </pre>
 *
 * @author GeckoCIRCUITS Team
 */
public class StamperRegistry {

    private final Map<CircuitTyp, IMatrixStamper> stampers;

    /**
     * Creates an empty registry.
     */
    public StamperRegistry() {
        this.stampers = new EnumMap<>(CircuitTyp.class);
    }

    /**
     * Creates a registry initialized with all standard stampers.
     *
     * @return a new registry with default stampers registered
     */
    public static StamperRegistry createDefault() {
        StamperRegistry registry = new StamperRegistry();
        registry.registerDefaults();
        return registry;
    }

    /**
     * Registers all default stamper implementations.
     */
    private void registerDefaults() {
        register(CircuitTyp.LK_R, new ResistorStamper());
        register(CircuitTyp.LK_C, new CapacitorStamper());
        register(CircuitTyp.LK_L, new InductorStamper());
        register(CircuitTyp.LK_U, new VoltageSourceStamper());
        register(CircuitTyp.LK_I, new CurrentSourceStamper());
        register(CircuitTyp.LK_D, new DiodeStamper());
    }

    /**
     * Registers a stamper for a specific component type.
     *
     * @param type the circuit component type
     * @param stamper the stamper implementation
     * @throws IllegalArgumentException if type or stamper is null
     */
    public void register(CircuitTyp type, IMatrixStamper stamper) {
        if (type == null) {
            throw new IllegalArgumentException("CircuitTyp cannot be null");
        }
        if (stamper == null) {
            throw new IllegalArgumentException("Stamper cannot be null");
        }
        stampers.put(type, stamper);
    }

    /**
     * Gets the stamper for a specific component type.
     *
     * @param type the circuit component type
     * @return the stamper for that type, or null if not registered
     */
    public IMatrixStamper getStamper(CircuitTyp type) {
        return stampers.get(type);
    }

    /**
     * Gets the stamper for a specific component type, throwing if not found.
     *
     * @param type the circuit component type
     * @return the stamper for that type
     * @throws IllegalArgumentException if no stamper is registered for the type
     */
    public IMatrixStamper getStamperRequired(CircuitTyp type) {
        IMatrixStamper stamper = stampers.get(type);
        if (stamper == null) {
            throw new IllegalArgumentException("No stamper registered for type: " + type);
        }
        return stamper;
    }

    /**
     * Checks if a stamper is registered for the given type.
     *
     * @param type the circuit component type
     * @return true if a stamper is registered
     */
    public boolean hasStamper(CircuitTyp type) {
        return stampers.containsKey(type);
    }

    /**
     * Removes the stamper for a specific component type.
     *
     * @param type the circuit component type
     * @return the removed stamper, or null if none was registered
     */
    public IMatrixStamper unregister(CircuitTyp type) {
        return stampers.remove(type);
    }

    /**
     * Gets all registered component types.
     *
     * @return unmodifiable set of registered types
     */
    public Set<CircuitTyp> getRegisteredTypes() {
        return Collections.unmodifiableSet(stampers.keySet());
    }

    /**
     * Gets the number of registered stampers.
     *
     * @return count of registered stampers
     */
    public int size() {
        return stampers.size();
    }

    /**
     * Clears all registered stampers.
     */
    public void clear() {
        stampers.clear();
    }

    @Override
    public String toString() {
        return "StamperRegistry[" + stampers.size() + " stampers: " + stampers.keySet() + "]";
    }
}

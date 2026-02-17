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
package gecko.core.circuit.netlist;

import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NetlistBuilder factory class.
 *
 * <p>Validates that the factory correctly creates empty netlists and handles
 * CircuitModel conversion without errors.</p>
 */
public class NetlistBuilderTest {

    @Test
    public void testBuildEmptyCreatesNetlistWithCorrectDimensions() {
        // Arrange
        int nodeCount = 5;
        int voltageSourceCount = 2;
        int elementCount = 8;

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildEmpty(nodeCount, voltageSourceCount, elementCount);

        // Assert
        assertNotNull(netlist, "Netlist should not be null");
        assertEquals(nodeCount - 1, netlist.getNodeMax(), "Node count should match");
        assertEquals(voltageSourceCount, netlist.getVoltageSourceMax(), "Voltage source count should match");
        assertEquals(elementCount, netlist.getElementCount(), "Element count should match");
    }

    @Test
    public void testBuildEmptyWithZeroDimensions() {
        // Arrange - Edge case: empty circuit
        int nodeCount = 0;
        int voltageSourceCount = 0;
        int elementCount = 0;

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildEmpty(nodeCount, voltageSourceCount, elementCount);

        // Assert
        assertNotNull(netlist, "Empty netlist should not be null");
        assertEquals(0, netlist.getNodeMax(), "Zero node count");
        assertEquals(0, netlist.getVoltageSourceMax(), "Zero voltage source count");
        assertEquals(0, netlist.getElementCount(), "Zero element count");
    }

    @Test
    public void testBuildEmptyWithNegativeNodeCountThrowsException() {
        // Act & Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> NetlistBuilder.buildEmpty(-1, 0, 0),
            "Negative nodeCount should throw IllegalArgumentException"
        );
    }

    @Test
    public void testBuildEmptyWithNegativeVoltageSourceCountThrowsException() {
        // Act & Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> NetlistBuilder.buildEmpty(1, -1, 0),
            "Negative voltageSourceCount should throw IllegalArgumentException"
        );
    }

    @Test
    public void testBuildEmptyWithNegativeElementCountThrowsException() {
        // Act & Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> NetlistBuilder.buildEmpty(1, 0, -1),
            "Negative elementCount should throw IllegalArgumentException"
        );
    }

    @Test
    public void testBuildEmptyCreatesValidNetlist() {
        // Arrange
        int nodeCount = 3;
        int voltageSourceCount = 1;
        int elementCount = 4;

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildEmpty(nodeCount, voltageSourceCount, elementCount);

        // Assert - Verify netlist is valid
        assertTrue(netlist.isValid(), "Built netlist should be valid");
        assertTrue(netlist.toString().length() > 0, "Netlist toString should not be null");
    }

    @Test
    public void testBuildFromCircuitModelWithNullReturnsEmptyNetlist() {
        // Act
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(null);

        // Assert
        assertNotNull(netlist, "Netlist should not be null for null model");
        assertEquals(0, netlist.getNodeMax(), "Should have zero nodes");
        assertEquals(0, netlist.getVoltageSourceMax(), "Should have zero voltage sources");
        assertEquals(0, netlist.getElementCount(), "Should have zero elements");
    }

    @Test
    public void testBuildFromCircuitModelEstimatesDimensions() {
        // Arrange
        CircuitModel model = new CircuitModel();
        for (int i = 0; i < 10; i++) {
            model.addCircuitComponent(new CircuitModel.ComponentData(1, "R" + i));
        }

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Assert
        assertNotNull(netlist, "Netlist should not be null");
        assertEquals(10, netlist.getElementCount(), "Element count should match component count");
        // Estimated nodes: (10 / 2) + 1 = 6, so nodeMax = 5
        assertEquals(5, netlist.getNodeMax(), "Nodes should be estimated");
        // Estimated voltage sources: 10 / 5 = 2
        assertEquals(2, netlist.getVoltageSourceMax(), "Voltage sources should be estimated");
    }

    @Test
    public void testBuildFromCircuitModelWithEmptyModel() {
        // Arrange
        CircuitModel model = new CircuitModel();
        // Model has no components

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Assert
        assertNotNull(netlist, "Netlist should not be null");
        assertEquals(0, netlist.getElementCount(), "Element count should be zero");
    }

    @Test
    public void testBuildFromCircuitModelMixedComponents() {
        // Arrange
        CircuitModel model = new CircuitModel();
        model.addCircuitComponent(new CircuitModel.ComponentData(1, "R1"));
        model.addCircuitComponent(new CircuitModel.ComponentData(2, "L1"));
        model.addControlComponent(new CircuitModel.ComponentData(3, "PI1"));
        model.addThermalComponent(new CircuitModel.ComponentData(4, "Th1"));
        // Total: 4 components

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Assert
        assertNotNull(netlist, "Netlist should not be null");
        assertEquals(4, netlist.getElementCount(), "Element count should be 4");
    }

    @Test
    public void testBuildEmptyNetlistAllComponentsAreResistors() {
        // Arrange
        int elementCount = 3;

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildEmpty(2, 0, elementCount);

        // Assert
        for (int i = 0; i < elementCount; i++) {
            assertNotNull(netlist.getType(i), "Component type should not be null");
            // All components should be resistors
            assertEquals(
                gecko.core.circuit.circuitcomponents.CircuitTypCore.LK_R,
                netlist.getType(i),
                "All components should be resistors"
            );
        }
    }
}

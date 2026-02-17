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

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CircuitNetlist class.
 *
 * Validates netlist creation, parameter access, result storage,
 * nonlinear updates, and circuit validity checks.
 */
public class CircuitNetlistTest {

    private CircuitNetlist netlist;

    @BeforeEach
    public void setUp() {
        netlist = new CircuitNetlist();
    }

    // ========== 1. Creation and Initialization Tests ==========

    @Test
    public void testEmptyNetlistHasZeroElements() {
        // Arrange - netlist created in setUp()

        // Act & Assert
        assertEquals(0, netlist.getElementCount(), "Empty netlist should have 0 elements");
        assertEquals(0, netlist.getNodeMax(), "Empty netlist should have 0 nodes");
        assertEquals(0, netlist.getVoltageSourceMax(), "Empty netlist should have 0 voltage sources");
    }

    @Test
    public void testInitNetlistWithFiveElements() {
        // Arrange
        int elementCount = 5;
        CircuitTypCore[] types = new CircuitTypCore[elementCount];
        int[] nodeX = new int[elementCount];
        int[] nodeY = new int[elementCount];
        int[] voltageSourceNr = new int[elementCount];
        double[][] params = new double[elementCount][];

        for (int i = 0; i < elementCount; i++) {
            types[i] = CircuitTypCore.LK_R;
            nodeX[i] = 0;
            nodeY[i] = 0;
            voltageSourceNr[i] = -1;
            params[i] = new double[]{100.0};
        }

        // Act
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 2, 0, elementCount);

        // Assert
        assertEquals(elementCount, netlist.getElementCount(), "Element count should be 5");
        assertEquals(2, netlist.getNodeMax(), "Node max should be 2");
        assertEquals(0, netlist.getVoltageSourceMax(), "Voltage source max should be 0");
    }

    @Test
    public void testInitNetlistWithNullArrayThrowsException() {
        // Arrange
        int elementCount = 3;
        int[] nodeX = new int[elementCount];
        int[] nodeY = new int[elementCount];
        int[] voltageSourceNr = new int[elementCount];
        double[][] params = new double[elementCount][];

        // Act & Assert - types array is null
        assertThrows(
            IllegalArgumentException.class,
            () -> netlist.initNetlist(null, nodeX, nodeY, voltageSourceNr, params, 2, 0, elementCount)
        );
    }

    // ========== 2. Parameter Access Tests ==========

    @Test
    public void testGetElementParamReturnsCorrectValue() {
        // Arrange
        int elementCount = 3;
        CircuitTypCore[] types = new CircuitTypCore[elementCount];
        int[] nodeX = new int[elementCount];
        int[] nodeY = new int[elementCount];
        int[] voltageSourceNr = new int[elementCount];
        double[][] params = new double[elementCount][];

        for (int i = 0; i < elementCount; i++) {
            types[i] = CircuitTypCore.LK_R;
            nodeX[i] = 0;
            nodeY[i] = 1;
            voltageSourceNr[i] = -1;
            params[i] = new double[]{(i + 1) * 100.0};  // 100.0, 200.0, 300.0
        }

        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        // Act & Assert
        assertEquals(100.0, netlist.getElementParam(0, 0), 0.0001);
        assertEquals(200.0, netlist.getElementParam(1, 0), 0.0001);
        assertEquals(300.0, netlist.getElementParam(2, 0), 0.0001);
    }

    @Test
    public void testSetElementParamUpdatesValueCorrectly() {
        // Arrange
        int elementCount = 2;
        CircuitTypCore[] types = {CircuitTypCore.LK_R, CircuitTypCore.LK_R};
        int[] nodeX = {0, 0};
        int[] nodeY = {1, 1};
        int[] voltageSourceNr = {-1, -1};
        double[][] params = {new double[]{50.0}, new double[]{75.0}};

        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        // Act
        netlist.setElementParam(0, 0, 150.0);
        double newValue = netlist.getElementParam(0, 0);

        // Assert
        assertEquals(150.0, newValue, 0.0001, "Parameter should be updated to 150.0");
    }

    @Test
    public void testGetElementParamOutOfBoundsThrowsException() {
        // Arrange
        int elementCount = 2;
        CircuitTypCore[] types = {CircuitTypCore.LK_R, CircuitTypCore.LK_R};
        int[] nodeX = {0, 0};
        int[] nodeY = {1, 1};
        int[] voltageSourceNr = {-1, -1};
        double[][] params = {new double[]{50.0}, new double[]{75.0}};

        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        // Act & Assert
        assertThrows(
            IndexOutOfBoundsException.class,
            () -> netlist.getElementParam(5, 0),
            "Out-of-bounds element index should throw exception"
        );
    }

    @Test
    public void testSetElementParamOutOfBoundsThrowsException() {
        // Arrange
        int elementCount = 2;
        CircuitTypCore[] types = {CircuitTypCore.LK_R, CircuitTypCore.LK_R};
        int[] nodeX = {0, 0};
        int[] nodeY = {1, 1};
        int[] voltageSourceNr = {-1, -1};
        double[][] params = {new double[]{50.0}, new double[]{75.0}};

        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        // Act & Assert
        assertThrows(
            IndexOutOfBoundsException.class,
            () -> netlist.setElementParam(10, 0, 200.0),
            "Out-of-bounds element index should throw exception"
        );
    }

    // ========== 3. Result Storage Tests ==========

    @Test
    public void testStoreResultsStoresNodeVoltagesCorrectly() {
        // Arrange
        int elementCount = 2;
        CircuitTypCore[] types = {CircuitTypCore.LK_R, CircuitTypCore.LK_R};
        int[] nodeX = {0, 0};
        int[] nodeY = {1, 1};
        int[] voltageSourceNr = {-1, -1};
        double[][] params = {new double[]{50.0}, new double[]{75.0}};

        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        double[] nodeVoltages = {0.0, 5.0, 10.0};
        double[] componentCurrents = {0.1, 0.2};

        // Act
        netlist.storeResults(nodeVoltages, componentCurrents);
        double[] retrieved = netlist.getLastNodeVoltages();

        // Assert
        assertNotNull(retrieved, "Retrieved node voltages should not be null");
        assertEquals(3, retrieved.length, "Should have 3 node voltages");
        assertEquals(0.0, retrieved[0], 0.0001);
        assertEquals(5.0, retrieved[1], 0.0001);
        assertEquals(10.0, retrieved[2], 0.0001);
    }

    @Test
    public void testStoreResultsStoresComponentCurrentsCorrectly() {
        // Arrange
        int elementCount = 3;
        CircuitTypCore[] types = {CircuitTypCore.LK_R, CircuitTypCore.LK_R, CircuitTypCore.LK_R};
        int[] nodeX = {0, 0, 0};
        int[] nodeY = {1, 1, 1};
        int[] voltageSourceNr = {-1, -1, -1};
        double[][] params = {new double[]{50.0}, new double[]{75.0}, new double[]{100.0}};

        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        double[] nodeVoltages = {0.0, 5.0};
        double[] componentCurrents = {0.1, 0.2, 0.3};

        // Act
        netlist.storeResults(nodeVoltages, componentCurrents);
        double[] retrieved = netlist.getLastComponentCurrents();

        // Assert
        assertNotNull(retrieved, "Retrieved component currents should not be null");
        assertEquals(3, retrieved.length, "Should have 3 component currents");
        assertEquals(0.1, retrieved[0], 0.0001);
        assertEquals(0.2, retrieved[1], 0.0001);
        assertEquals(0.3, retrieved[2], 0.0001);
    }

    @Test
    public void testGetLastNodeVoltagesReturnsCopy() {
        // Arrange
        int elementCount = 1;
        CircuitTypCore[] types = {CircuitTypCore.LK_R};
        int[] nodeX = {0};
        int[] nodeY = {1};
        int[] voltageSourceNr = {-1};
        double[][] params = {new double[]{50.0}};

        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        double[] nodeVoltages = {0.0, 5.0};
        double[] componentCurrents = {0.1};

        netlist.storeResults(nodeVoltages, componentCurrents);

        // Act
        double[] retrieved1 = netlist.getLastNodeVoltages();
        double[] retrieved2 = netlist.getLastNodeVoltages();

        // Assert - Arrays should be different objects (copies)
        assertNotSame(retrieved1, retrieved2, "getLastNodeVoltages() should return copies");
        assertArrayEquals(retrieved1, retrieved2, 0.0001, "But values should be the same");
    }

    @Test
    public void testStoredResultsSurvivNewStoreResultsCall() {
        // Arrange
        int elementCount = 1;
        CircuitTypCore[] types = {CircuitTypCore.LK_R};
        int[] nodeX = {0};
        int[] nodeY = {1};
        int[] voltageSourceNr = {-1};
        double[][] params = {new double[]{50.0}};

        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        double[] firstVoltages = {0.0, 5.0};
        double[] firstCurrents = {0.1};

        // Act - Store first result set
        netlist.storeResults(firstVoltages, firstCurrents);

        // Store second result set with different values
        double[] secondVoltages = {0.0, 10.0};
        double[] secondCurrents = {0.2};
        netlist.storeResults(secondVoltages, secondCurrents);

        // Get results after second store
        double[] retrievedVoltages = netlist.getLastNodeVoltages();
        double[] retrievedCurrents = netlist.getLastComponentCurrents();

        // Assert - Should have the second set
        assertEquals(10.0, retrievedVoltages[1], 0.0001, "Should have second voltage value");
        assertEquals(0.2, retrievedCurrents[0], 0.0001, "Should have second current value");
    }

    // ========== 4. Nonlinear Update Tests ==========

    @Test
    public void testUpdateNonlinearComponentsReturnsFalse() {
        // Arrange
        int elementCount = 2;
        CircuitTypCore[] types = {CircuitTypCore.LK_R, CircuitTypCore.LK_R};
        int[] nodeX = {0, 0};
        int[] nodeY = {1, 1};
        int[] voltageSourceNr = {-1, -1};
        double[][] params = {new double[]{50.0}, new double[]{75.0}};

        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        // Act
        boolean result = netlist.updateNonlinearComponents();

        // Assert - Placeholder implementation returns false
        assertFalse(result, "updateNonlinearComponents() should return false (placeholder)");
    }

    // ========== 5. Circuit Validity Tests ==========

    @Test
    public void testEmptyNetlistIsNotValid() {
        // Arrange - Empty netlist created in setUp()

        // Act & Assert
        assertFalse(netlist.isValid(), "Empty netlist should be invalid (has 0 elements)");
    }

    @Test
    public void testNetlistWithElementsIsValid() {
        // Arrange
        int elementCount = 5;
        CircuitTypCore[] types = new CircuitTypCore[elementCount];
        int[] nodeX = new int[elementCount];
        int[] nodeY = new int[elementCount];
        int[] voltageSourceNr = new int[elementCount];
        double[][] params = new double[elementCount][];

        for (int i = 0; i < elementCount; i++) {
            types[i] = CircuitTypCore.LK_R;
            nodeX[i] = 0;
            nodeY[i] = 1;
            voltageSourceNr[i] = -1;
            params[i] = new double[]{100.0};
        }

        // Act
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        // Assert
        assertTrue(netlist.isValid(), "Netlist with valid elements should be valid");
    }

    @Test
    public void testNetlistToStringIsNonEmpty() {
        // Arrange
        int elementCount = 2;
        CircuitTypCore[] types = {CircuitTypCore.LK_R, CircuitTypCore.LK_R};
        int[] nodeX = {0, 0};
        int[] nodeY = {1, 1};
        int[] voltageSourceNr = {-1, -1};
        double[][] params = {new double[]{50.0}, new double[]{75.0}};

        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        // Act
        String result = netlist.toString();

        // Assert
        assertNotNull(result, "toString() should not be null");
        assertTrue(result.length() > 0, "toString() should be non-empty");
        assertTrue(result.contains("elements=2"), "toString() should mention element count");
    }

    // ========== 6. Additional Comprehensive Tests ==========

    @Test
    public void testGetTotalNodeCount() {
        // Arrange
        int elementCount = 3;
        CircuitTypCore[] types = {CircuitTypCore.LK_R, CircuitTypCore.LK_R, CircuitTypCore.LK_R};
        int[] nodeX = {0, 1, 2};
        int[] nodeY = {1, 2, 3};
        int[] voltageSourceNr = {-1, -1, -1};
        double[][] params = {new double[]{50.0}, new double[]{75.0}, new double[]{100.0}};

        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 3, 0, elementCount);

        // Act
        int totalNodes = netlist.getTotalNodeCount();

        // Assert - nodeMax = 3, so total = 3 + 1 = 4
        assertEquals(4, totalNodes, "Total node count should be nodeMax + 1");
    }

    @Test
    public void testGetSimulationTime() {
        // Arrange
        netlist.setSimulationTime(0.005);

        // Act
        double time = netlist.getSimulationTime();

        // Assert
        assertEquals(0.005, time, 0.00001, "Simulation time should match set value");
    }

    @Test
    public void testNetlistWithMixedComponentTypes() {
        // Arrange
        int elementCount = 4;
        CircuitTypCore[] types = {
            CircuitTypCore.LK_R,
            CircuitTypCore.LK_L,
            CircuitTypCore.LK_C,
            CircuitTypCore.LK_U
        };
        int[] nodeX = {0, 0, 0, 0};
        int[] nodeY = {1, 1, 1, 1};
        int[] voltageSourceNr = {-1, -1, -1, 1};
        double[][] params = {
            new double[]{50.0},
            new double[]{1e-3},
            new double[]{1e-6},
            new double[]{1, 5.0}
        };

        // Act
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 1, elementCount);

        // Assert
        assertEquals(4, netlist.getElementCount());
        assertEquals(CircuitTypCore.LK_R, netlist.getType(0));
        assertEquals(CircuitTypCore.LK_L, netlist.getType(1));
        assertEquals(CircuitTypCore.LK_C, netlist.getType(2));
        assertEquals(CircuitTypCore.LK_U, netlist.getType(3));
    }

    @Test
    public void testSetAllParametersEquivalentToInitNetlist() {
        // Arrange
        int elementCount = 2;
        CircuitTypCore[] types = {CircuitTypCore.LK_R, CircuitTypCore.LK_R};
        int[] nodeX = {0, 0};
        int[] nodeY = {1, 1};
        int[] voltageSourceNr = {-1, -1};
        double[][] params = {new double[]{50.0}, new double[]{75.0}};

        // Act
        netlist.setAllParameters(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        // Assert
        assertEquals(elementCount, netlist.getElementCount());
        assertEquals(50.0, netlist.getElementParam(0, 0), 0.0001);
        assertEquals(75.0, netlist.getElementParam(1, 0), 0.0001);
    }

    @Test
    public void testClearResetsNetlist() {
        // Arrange
        int elementCount = 3;
        CircuitTypCore[] types = {CircuitTypCore.LK_R, CircuitTypCore.LK_R, CircuitTypCore.LK_R};
        int[] nodeX = {0, 0, 0};
        int[] nodeY = {1, 1, 1};
        int[] voltageSourceNr = {-1, -1, -1};
        double[][] params = {new double[]{50.0}, new double[]{75.0}, new double[]{100.0}};

        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 2, 1, elementCount);

        // Act
        netlist.clear();

        // Assert
        assertEquals(0, netlist.getElementCount(), "Element count should be reset to 0");
        assertEquals(0, netlist.getNodeMax(), "Node max should be reset to 0");
        assertEquals(0, netlist.getVoltageSourceMax(), "Voltage source max should be reset to 0");
    }

    @Test
    public void testStoreResultsWithNullArrays() {
        // Arrange
        // Act
        netlist.storeResults(null, null);

        // Assert - Should not throw, should store empty arrays
        double[] voltages = netlist.getLastNodeVoltages();
        double[] currents = netlist.getLastComponentCurrents();

        assertEquals(0, voltages.length, "Null node voltages should result in empty array");
        assertEquals(0, currents.length, "Null component currents should result in empty array");
    }

    @Test
    public void testGetLastNodeVoltagesAndComponentCurrentsRef() {
        // Arrange
        int elementCount = 1;
        CircuitTypCore[] types = {CircuitTypCore.LK_R};
        int[] nodeX = {0};
        int[] nodeY = {1};
        int[] voltageSourceNr = {-1};
        double[][] params = {new double[]{50.0}};

        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        double[] nodeVoltages = {0.0, 5.0};
        double[] componentCurrents = {0.1};

        netlist.storeResults(nodeVoltages, componentCurrents);

        // Act
        double[] refVoltages = netlist.getLastNodeVoltagesRef();
        double[] refCurrents = netlist.getLastComponentCurrentsRef();

        // Assert - These should return references (not copies)
        assertSame(refVoltages, netlist.getLastNodeVoltagesRef(),
                "getLastNodeVoltagesRef() should return same reference on multiple calls");
        assertEquals(5.0, refVoltages[1], 0.0001);
        assertEquals(0.1, refCurrents[0], 0.0001);
    }
}

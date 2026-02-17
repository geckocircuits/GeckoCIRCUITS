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

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.simulation.ControlNetlist;
import gecko.core.simulation.solver.MatrixSolver;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for netlist building and execution with solvers.
 *
 * Validates that CircuitNetlist, MatrixSolver, NetlistBuilder, and ControlNetlist
 * work together correctly in a complete simulation flow.
 */
public class NetlistIntegrationTest {

    // ========== Test 1: MatrixSolver + CircuitNetlist Integration ==========

    @Test
    public void testMatrixSolverWithCircuitNetlistWiring() {
        // Arrange - Create a simple 3-node circuit with only resistors
        int nodeCount = 3;
        int voltageSourceCount = 0;
        int elementCount = 3;

        CircuitTypCore[] types = {CircuitTypCore.LK_R, CircuitTypCore.LK_R, CircuitTypCore.LK_R};
        int[] nodeX = {0, 0, 0};
        int[] nodeY = {1, 1, 2};
        int[] voltageSourceNr = {-1, -1, -1};
        double[][] params = {
            new double[]{100.0},   // 100 ohm resistor
            new double[]{50.0},    // 50 ohm resistor
            new double[]{200.0}    // 200 ohm resistor
        };

        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, nodeCount - 1, voltageSourceCount, elementCount);

        // Create and initialize MatrixSolver
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(nodeCount, voltageSourceCount, elementCount);

        // Act - Build matrix A and vector b
        double dt = 1e-6;
        double time = 0.0;
        solver.buildMatrixA(netlist, dt, time, false);
        solver.buildVectorB(netlist, dt, time, false);

        // Verify matrices were built
        double[][] matrixA = solver.getSystemMatrix();
        double[] vectorB = solver.getb();

        // Assert - Verify that matrices exist and solution vector p has correct size
        assertNotNull(matrixA, "System matrix A should not be null");
        assertNotNull(vectorB, "Vector b should not be null");
        double[] p = solver.getP();
        assertNotNull(p, "Solution vector p should not be null");
        assertEquals(nodeCount + voltageSourceCount + 1, p.length,
                "Solution vector size should be nodeCount + voltageSourceCount + 1");
    }

    // ========== Test 2: NetlistBuilder with Null Model ==========

    @Test
    public void testNetlistBuilderBuildFromNullCircuitModel() {
        // Act - Call NetlistBuilder with null model
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(null);

        // Assert - Should return non-null empty netlist without throwing
        assertNotNull(netlist, "NetlistBuilder.buildFromCircuitModel(null) should return non-null");
        assertEquals(0, netlist.getElementCount(), "Should have zero elements");
        assertEquals(0, netlist.getNodeMax(), "Should have zero nodes");
        assertEquals(0, netlist.getVoltageSourceMax(), "Should have zero voltage sources");
        assertFalse(netlist.isValid(), "Empty netlist should be invalid (0 elements)");
    }

    // ========== Test 3: ControlNetlist Execution ==========

    @Test
    public void testControlNetlistEmptyCreationAndTimeStepExecution() {
        // Act - Create empty control netlist
        ControlNetlist controlNetlist = ControlNetlist.createEmpty();

        // Assert - Initial state
        assertNotNull(controlNetlist, "ControlNetlist should not be null");
        assertEquals(0, controlNetlist.getCalculatorCount(), "Should have zero calculators");
        assertFalse(controlNetlist.hasCalculators(), "Should not have calculators");

        // Act - Execute a time step without throwing exception
        assertDoesNotThrow(() -> {
            controlNetlist.executeTimeStep(1e-6, 0.0);
        }, "executeTimeStep should not throw with empty calculator list");

        // Assert - Time was updated
        assertEquals(0.0, controlNetlist.getSimulationTime(), 0.00001,
                "Simulation time should be updated to 0.0");
    }

    // ========== Additional Integration Test: Complete Netlist Building Pipeline ==========

    @Test
    public void testCompleteNetlistBuildingPipeline() {
        // Arrange - Build from empty factory method
        int nodeCount = 4;
        int voltageSourceCount = 1;
        int elementCount = 5;

        // Act - Create netlist using NetlistBuilder.buildEmpty()
        CircuitNetlist netlist = NetlistBuilder.buildEmpty(nodeCount, voltageSourceCount, elementCount);

        // Assert - Verify netlist dimensions
        assertNotNull(netlist, "Built netlist should not be null");
        assertEquals(elementCount, netlist.getElementCount(), "Element count should match");

        // Store some result data
        double[] nodeVoltages = new double[nodeCount + 1];
        double[] componentCurrents = new double[elementCount];
        for (int i = 0; i < nodeVoltages.length; i++) {
            nodeVoltages[i] = i * 1.5;
        }
        for (int i = 0; i < componentCurrents.length; i++) {
            componentCurrents[i] = i * 0.1;
        }

        // Act - Store results
        netlist.storeResults(nodeVoltages, componentCurrents);

        // Assert - Results are stored correctly
        double[] retrievedVoltages = netlist.getLastNodeVoltages();
        double[] retrievedCurrents = netlist.getLastComponentCurrents();

        assertEquals(nodeVoltages.length, retrievedVoltages.length,
                "Stored voltages should have correct length");
        assertEquals(componentCurrents.length, retrievedCurrents.length,
                "Stored currents should have correct length");

        for (int i = 0; i < nodeVoltages.length; i++) {
            assertEquals(nodeVoltages[i], retrievedVoltages[i], 0.0001,
                    "Voltage[" + i + "] should be preserved");
        }
    }

    // ========== Advanced Integration Test: CircuitModel Conversion Pipeline ==========

    @Test
    public void testCircuitModelToNetlistConversion() {
        // Arrange - Create a CircuitModel with some components
        CircuitModel model = new CircuitModel();
        model.addCircuitComponent(new CircuitModel.ComponentData(1, "R1"));
        model.addCircuitComponent(new CircuitModel.ComponentData(2, "L1"));
        model.addCircuitComponent(new CircuitModel.ComponentData(3, "C1"));
        // 3 circuit components total

        // Act - Convert to netlist
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Assert - Netlist should be created with estimated dimensions
        assertNotNull(netlist, "Netlist from CircuitModel should not be null");
        assertEquals(3, netlist.getElementCount(), "Element count should match component count");
        assertTrue(netlist.getNodeMax() >= 0, "Node count should be non-negative");
        assertTrue(netlist.getVoltageSourceMax() >= 0, "Voltage source count should be non-negative");
    }

    // ========== Test: MatrixSolver History Shifting with Netlist ==========

    @Test
    public void testMatrixSolverHistoryShiftingWithNetlist() {
        // Arrange - Create a simple 2-node circuit
        int nodeCount = 2;
        int voltageSourceCount = 0;
        int elementCount = 1;

        CircuitTypCore[] types = {CircuitTypCore.LK_R};
        int[] nodeX = {0};
        int[] nodeY = {1};
        int[] voltageSourceNr = {-1};
        double[][] params = {new double[]{100.0}};

        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, nodeCount - 1, voltageSourceCount, elementCount);

        // Create solver and initialize
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_TRZ);
        solver.initializeMatrices(nodeCount, voltageSourceCount, elementCount);

        // Act - Initialize some potentials
        double[] p = solver.getP();
        double[] pALT = solver.getPALT();
        p[0] = 1.0;
        p[1] = 2.0;
        pALT[0] = 0.5;
        pALT[1] = 1.0;

        // Update history
        solver.updateNodePotentials(1e-6, 0.0);

        // Assert - History should be shifted correctly
        double[] newPALT = solver.getPALT();
        assertEquals(1.0, newPALT[0], 0.0001, "pALT[0] should have old p[0] value");
        assertEquals(2.0, newPALT[1], 0.0001, "pALT[1] should have old p[1] value");
    }

    // ========== Test: Mixed Multi-Domain Components ==========

    @Test
    public void testNetlistWithMultiDomainComponents() {
        // Arrange - Create netlist with mixed electrical, thermal, and reluctance components
        int elementCount = 6;
        CircuitTypCore[] types = {
            CircuitTypCore.LK_R,           // Electrical resistor
            CircuitTypCore.LK_L,           // Electrical inductor
            CircuitTypCore.TH_RTH,         // Thermal resistor
            CircuitTypCore.REL_RELUCTANCE, // Reluctance (magnetic)
            CircuitTypCore.TH_CTH,         // Thermal capacitance
            CircuitTypCore.LK_C            // Electrical capacitance
        };

        int[] nodeX = {0, 0, 0, 0, 0, 0};
        int[] nodeY = {1, 1, 1, 1, 1, 1};
        int[] voltageSourceNr = {-1, -1, -1, -1, -1, -1};
        double[][] params = {
            new double[]{100.0},     // Resistance
            new double[]{1e-3},      // Inductance
            new double[]{10.0},      // Thermal resistance
            new double[]{50.0},      // Reluctance
            new double[]{1e-4},      // Thermal capacitance
            new double[]{1e-6}       // Capacitance
        };

        // Act
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        // Assert
        assertEquals(elementCount, netlist.getElementCount());
        assertTrue(netlist.isValid(), "Multi-domain netlist should be valid");

        // Verify each component type is correctly stored
        for (int i = 0; i < elementCount; i++) {
            assertEquals(types[i], netlist.getType(i),
                    "Component type [" + i + "] should match");
        }
    }

    // ========== Test: Large Netlist Construction ==========

    @Test
    public void testLargeNetlistConstruction() {
        // Arrange - Create a large netlist with many elements
        int elementCount = 100;
        int nodeCount = 50;
        int voltageSourceCount = 5;

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildEmpty(nodeCount, voltageSourceCount, elementCount);

        // Assert
        assertEquals(elementCount, netlist.getElementCount());
        assertEquals(nodeCount - 1, netlist.getNodeMax());
        assertEquals(voltageSourceCount, netlist.getVoltageSourceMax());
        assertTrue(netlist.isValid(), "Large netlist should be valid");

        // Create solver for large netlist
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        assertDoesNotThrow(() -> {
            solver.initializeMatrices(nodeCount, voltageSourceCount, elementCount);
        }, "Solver should handle large netlist initialization without error");

        assertEquals(nodeCount + voltageSourceCount + 1, solver.getMatrixSize(),
                "Matrix size should account for all nodes and voltage sources");
    }

    // ========== Test: Parameter Modification and Persistence ==========

    @Test
    public void testParameterModificationPersistsAcrossNetlistOperations() {
        // Arrange
        int elementCount = 3;
        CircuitTypCore[] types = {CircuitTypCore.LK_R, CircuitTypCore.LK_R, CircuitTypCore.LK_R};
        int[] nodeX = {0, 0, 0};
        int[] nodeY = {1, 1, 1};
        int[] voltageSourceNr = {-1, -1, -1};
        double[][] params = {new double[]{100.0}, new double[]{200.0}, new double[]{300.0}};

        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params, 1, 0, elementCount);

        // Act - Modify a parameter
        netlist.setElementParam(1, 0, 250.0);

        // Store results (which shouldn't affect parameters)
        netlist.storeResults(new double[]{0.0, 5.0}, new double[]{0.1, 0.2, 0.3});

        // Assert - Modified parameter should persist
        assertEquals(250.0, netlist.getElementParam(1, 0), 0.0001,
                "Parameter modification should persist across storeResults");

        // Act - Retrieve and verify all parameters
        assertEquals(100.0, netlist.getElementParam(0, 0), 0.0001);
        assertEquals(250.0, netlist.getElementParam(1, 0), 0.0001);
        assertEquals(300.0, netlist.getElementParam(2, 0), 0.0001);
    }
}

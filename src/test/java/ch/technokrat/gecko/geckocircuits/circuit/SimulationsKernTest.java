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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.circuit.SimulationsKern.SimulationStatus;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for SimulationsKern - the main simulation engine.
 *
 * These tests focus on the simulation kernel's lifecycle, timing, and status management.
 * More comprehensive integration tests require circuit setup which is tested separately.
 */
public class SimulationsKernTest {

    private SimulationsKern simulationsKern;

    @Before
    public void setUp() {
        simulationsKern = new SimulationsKern();
    }

    @Test
    public void testConstructor_InitializesWithNotInitStatus() {
        assertEquals("New kernel should have NOT_INIT status",
                SimulationStatus.NOT_INIT, simulationsKern._simulationStatus);
    }

    @Test
    public void testSetZeiten_SetsTimeParameters() {
        double tStart = 0.0;
        double tEnd = 1.0;
        double dt = 1e-6;

        simulationsKern.setZeiten(tStart, tEnd, dt);

        assertEquals("dt should be set", dt, simulationsKern.getdt(), 1e-15);
        assertEquals("tEND should be set", tEnd, simulationsKern.getTEND(), 1e-15);
        assertEquals("tSTART should be set", tStart, simulationsKern.getTSTART(), 1e-15);
    }

    @Test
    public void testGetdt_ReturnsTimeStep() {
        simulationsKern.setZeiten(0.0, 1.0, 5e-6);
        assertEquals(5e-6, simulationsKern.getdt(), 1e-15);
    }

    @Test
    public void testGetTEND_ReturnsEndTime() {
        simulationsKern.setZeiten(0.0, 2.5, 1e-6);
        assertEquals(2.5, simulationsKern.getTEND(), 1e-15);
    }

    @Test
    public void testGetTSTART_ReturnsStartTime() {
        simulationsKern.setZeiten(0.5, 1.5, 1e-6);
        assertEquals(0.5, simulationsKern.getTSTART(), 1e-15);
    }

    @Test
    public void testGetZeitAktuell_ReturnsZeroInitially() {
        // Current time should be 0 before simulation starts
        assertEquals("Initial time should be 0", 0.0, simulationsKern.getZeitAktuell(), 1e-15);
    }

    @Test
    public void testSimulationStatus_EnumValues() {
        // Verify all expected status values exist
        assertNotNull(SimulationStatus.NOT_INIT);
        assertNotNull(SimulationStatus.RUNNING);
        assertNotNull(SimulationStatus.PAUSED);
        assertNotNull(SimulationStatus.FINISHED);
    }

    @Test
    public void testSetZeiten_WithDifferentTimeSteps() {
        // Test with various time step sizes
        double[] testDts = {1e-9, 1e-6, 1e-3, 0.01};

        for (double dt : testDts) {
            simulationsKern.setZeiten(0.0, 1.0, dt);
            assertEquals("dt should match for " + dt, dt, simulationsKern.getdt(), 1e-15);
        }
    }

    @Test
    public void testSetZeiten_WithNegativeStartTime() {
        // System should accept negative start time (for some use cases)
        simulationsKern.setZeiten(-1.0, 1.0, 1e-6);
        assertEquals(-1.0, simulationsKern.getTSTART(), 1e-15);
    }

    @Test
    public void testSetZeiten_PreservesStaticNature() {
        // tSTART and tEND are static fields - verify behavior
        SimulationsKern kern1 = new SimulationsKern();
        SimulationsKern kern2 = new SimulationsKern();

        kern1.setZeiten(0.0, 5.0, 1e-6);

        // Since tSTART and tEND are static, kern2 should see the same values
        assertEquals(5.0, kern2.getTEND(), 1e-15);
        assertEquals(0.0, kern2.getTSTART(), 1e-15);
    }

    @Test
    public void testPauseSimulation_DoesNotThrow() {
        // pauseSimulation() is currently empty but should not throw
        simulationsKern.pauseSimulation();
        // No assertion needed - test passes if no exception is thrown
    }

    @Test
    public void testMultipleKernelInstances_HaveIndependentStatus() {
        SimulationsKern kern1 = new SimulationsKern();
        SimulationsKern kern2 = new SimulationsKern();

        // Modify kern1's status
        kern1._simulationStatus = SimulationStatus.RUNNING;

        // kern2 should still be NOT_INIT
        assertEquals(SimulationStatus.NOT_INIT, kern2._simulationStatus);
    }

    // === Additional tests for getSimulationStatus() ===

    @Test
    public void testGetSimulationStatus_NotInit() {
        simulationsKern._simulationStatus = SimulationStatus.NOT_INIT;
        assertEquals("Should return NOT_INIT",
                ch.technokrat.gecko.geckocircuits.api.ISimulationEngine.SimulationStatus.NOT_INIT,
                simulationsKern.getSimulationStatus());
    }

    @Test
    public void testGetSimulationStatus_Running() {
        simulationsKern._simulationStatus = SimulationStatus.RUNNING;
        assertEquals("Should return RUNNING",
                ch.technokrat.gecko.geckocircuits.api.ISimulationEngine.SimulationStatus.RUNNING,
                simulationsKern.getSimulationStatus());
    }

    @Test
    public void testGetSimulationStatus_Paused() {
        simulationsKern._simulationStatus = SimulationStatus.PAUSED;
        assertEquals("Should return PAUSED",
                ch.technokrat.gecko.geckocircuits.api.ISimulationEngine.SimulationStatus.PAUSED,
                simulationsKern.getSimulationStatus());
    }

    @Test
    public void testGetSimulationStatus_Finished() {
        simulationsKern._simulationStatus = SimulationStatus.FINISHED;
        assertEquals("Should return FINISHED",
                ch.technokrat.gecko.geckocircuits.api.ISimulationEngine.SimulationStatus.FINISHED,
                simulationsKern.getSimulationStatus());
    }

    // === Tests for time step precision ===

    @Test
    public void testSetZeiten_VerySmallTimeStep() {
        double tStart = 0.0;
        double tEnd = 1e-3;
        double dt = 1e-12; // Picosecond time step

        simulationsKern.setZeiten(tStart, tEnd, dt);

        assertEquals("Very small dt should be preserved", dt, simulationsKern.getdt(), 1e-20);
    }

    @Test
    public void testSetZeiten_LargeTimeValues() {
        double tStart = 1000.0;
        double tEnd = 2000.0;
        double dt = 1e-3;

        simulationsKern.setZeiten(tStart, tEnd, dt);

        assertEquals(tStart, simulationsKern.getTSTART(), 1e-10);
        assertEquals(tEnd, simulationsKern.getTEND(), 1e-10);
        assertEquals(dt, simulationsKern.getdt(), 1e-15);
    }

    // === Tests for simulation status transitions ===

    @Test
    public void testStatusTransition_NotInitToRunning() {
        assertEquals(SimulationStatus.NOT_INIT, simulationsKern._simulationStatus);
        simulationsKern._simulationStatus = SimulationStatus.RUNNING;
        assertEquals(SimulationStatus.RUNNING, simulationsKern._simulationStatus);
    }

    @Test
    public void testStatusTransition_RunningToPaused() {
        simulationsKern._simulationStatus = SimulationStatus.RUNNING;
        simulationsKern._simulationStatus = SimulationStatus.PAUSED;
        assertEquals(SimulationStatus.PAUSED, simulationsKern._simulationStatus);
    }

    @Test
    public void testStatusTransition_RunningToFinished() {
        simulationsKern._simulationStatus = SimulationStatus.RUNNING;
        simulationsKern._simulationStatus = SimulationStatus.FINISHED;
        assertEquals(SimulationStatus.FINISHED, simulationsKern._simulationStatus);
    }

    // === Tests for getTimeStep (external interface) ===

    @Test
    public void testGetTimeStep_ReturnsCorrectValue() {
        simulationsKern.setZeiten(0.0, 1.0, 2.5e-6);
        assertEquals("getTimeStep should return dt", 2.5e-6, simulationsKern.getTimeStep(), 1e-15);
    }

    // === Test implements interface ===

    @Test
    public void testImplementsISimulationEngine() {
        assertTrue("SimulationsKern should implement ISimulationEngine",
                simulationsKern instanceof ch.technokrat.gecko.geckocircuits.api.ISimulationEngine);
    }
}

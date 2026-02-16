/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SimulationProgress.
 */
class SimulationProgressTest {

    @Test
    void constructor_setsAllFields() {
        SimulationProgress progress = new SimulationProgress(
            0.5,           // overallProgress
            0.0,           // preCalcProgress
            0.5,           // mainSimProgress
            5000,          // currentStep
            10000,         // totalSteps
            0.005,         // currentTime
            0.01,          // endTime
            5000L,         // estimatedRemainingMs
            HeadlessSimulationEngine.EngineState.RUNNING
        );

        assertEquals(0.5, progress.getOverallProgress(), 0.001);
        assertEquals(0.0, progress.getPreCalcProgress(), 0.001);
        assertEquals(0.5, progress.getMainSimProgress(), 0.001);
        assertEquals(5000, progress.getCurrentStep());
        assertEquals(10000, progress.getTotalSteps());
        assertEquals(0.005, progress.getCurrentTime(), 0.0001);
        assertEquals(0.01, progress.getEndTime(), 0.0001);
        assertEquals(5000L, progress.getEstimatedRemainingMs());
        assertEquals(HeadlessSimulationEngine.EngineState.RUNNING, progress.getState());
    }

    @Test
    void getProgressPercentage_returnsFormattedString() {
        SimulationProgress progress = new SimulationProgress(
            0.452,         // 45.2%
            0.0,
            0.452,
            4520,
            10000,
            0.00452,
            0.01,
            5480L,
            HeadlessSimulationEngine.EngineState.RUNNING
        );

        String percentage = progress.getProgressPercentage();

        assertEquals("45.2%", percentage);
    }

    @Test
    void getProgressPercentage_zeroProgress() {
        SimulationProgress progress = new SimulationProgress(
            0.0,
            0.0,
            0.0,
            0,
            10000,
            0.0,
            0.01,
            null,
            HeadlessSimulationEngine.EngineState.RUNNING
        );

        String percentage = progress.getProgressPercentage();

        assertEquals("0.0%", percentage);
    }

    @Test
    void getProgressPercentage_completeProgress() {
        SimulationProgress progress = new SimulationProgress(
            1.0,
            0.0,
            1.0,
            10000,
            10000,
            0.01,
            0.01,
            0L,
            HeadlessSimulationEngine.EngineState.RUNNING
        );

        String percentage = progress.getProgressPercentage();

        assertEquals("100.0%", percentage);
    }

    @Test
    void toString_containsAllFields() {
        SimulationProgress progress = new SimulationProgress(
            0.5,
            0.0,
            0.5,
            5000,
            10000,
            0.005,
            0.01,
            5000L,
            HeadlessSimulationEngine.EngineState.RUNNING
        );

        String str = progress.toString();

        assertTrue(str.contains("50.0%"));
        assertTrue(str.contains("5000/10000"));
        assertTrue(str.contains("0.005"));
        assertTrue(str.contains("0.01"));
        assertTrue(str.contains("RUNNING"));
        assertTrue(str.contains("5000ms"));
    }

    @Test
    void toString_withNullETA() {
        SimulationProgress progress = new SimulationProgress(
            0.05,
            0.0,
            0.05,
            50,
            10000,
            0.00005,
            0.01,
            null,          // No ETA yet
            HeadlessSimulationEngine.EngineState.RUNNING
        );

        String str = progress.toString();

        assertTrue(str.contains("5.0%"));
        assertTrue(str.contains("-1ms"));  // Null ETA shows as -1
    }

    @Test
    void getEstimatedRemainingMs_canBeNull() {
        SimulationProgress progress = new SimulationProgress(
            0.01,
            0.0,
            0.01,
            10,
            1000,
            0.00001,
            0.001,
            null,
            HeadlessSimulationEngine.EngineState.RUNNING
        );

        Long eta = progress.getEstimatedRemainingMs();

        assertNull(eta);
    }

    @Test
    void state_pausedSimulation() {
        SimulationProgress progress = new SimulationProgress(
            0.3,
            0.0,
            0.3,
            3000,
            10000,
            0.003,
            0.01,
            7000L,
            HeadlessSimulationEngine.EngineState.PAUSED
        );

        assertEquals(HeadlessSimulationEngine.EngineState.PAUSED, progress.getState());
    }

    @Test
    void state_cancelledSimulation() {
        SimulationProgress progress = new SimulationProgress(
            0.2,
            0.0,
            0.2,
            2000,
            10000,
            0.002,
            0.01,
            null,
            HeadlessSimulationEngine.EngineState.CANCELLED
        );

        assertEquals(HeadlessSimulationEngine.EngineState.CANCELLED, progress.getState());
    }

    @Test
    void preCalcProgress_separateFromMainSim() {
        SimulationProgress progress = new SimulationProgress(
            0.4,           // 40% overall
            0.8,           // 80% through pre-calc
            0.2,           // 20% through main sim
            2000,
            10000,
            0.002,
            0.01,
            6000L,
            HeadlessSimulationEngine.EngineState.RUNNING
        );

        assertEquals(0.4, progress.getOverallProgress(), 0.001);
        assertEquals(0.8, progress.getPreCalcProgress(), 0.001);
        assertEquals(0.2, progress.getMainSimProgress(), 0.001);
    }

    @Test
    void stepTracking_currentAndTotal() {
        SimulationProgress progress = new SimulationProgress(
            0.75,
            0.0,
            0.75,
            7500,          // Current step
            10000,         // Total steps
            0.0075,
            0.01,
            2500L,
            HeadlessSimulationEngine.EngineState.RUNNING
        );

        assertEquals(7500, progress.getCurrentStep());
        assertEquals(10000, progress.getTotalSteps());
    }

    @Test
    void timeTracking_currentAndEnd() {
        SimulationProgress progress = new SimulationProgress(
            0.25,
            0.0,
            0.25,
            2500,
            10000,
            0.0025,        // Current time: 2.5ms
            0.01,          // End time: 10ms
            7500L,
            HeadlessSimulationEngine.EngineState.RUNNING
        );

        assertEquals(0.0025, progress.getCurrentTime(), 0.00001);
        assertEquals(0.01, progress.getEndTime(), 0.00001);
    }
}

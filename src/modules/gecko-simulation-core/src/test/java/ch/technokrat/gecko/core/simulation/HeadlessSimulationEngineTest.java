/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package ch.technokrat.gecko.core.simulation;

import ch.technokrat.gecko.core.allg.SolverType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeadlessSimulationEngineTest {

    @Test
    void runSimulation_nullConfig_returnsFailedResult() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        SimulationResult result = engine.runSimulation(null);

        assertFalse(result.isSuccess());
        assertEquals(SimulationResult.Status.FAILED, result.getStatus());
    }

    @Test
    void runSimulation_invalidStepWidth_returnsFailedResult() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
                .stepWidth(0.0)
                .simulationDuration(1e-3)
                .solverType(SolverType.SOLVER_BE)
                .build();

        SimulationResult result = engine.runSimulation(config);

        assertFalse(result.isSuccess());
        assertEquals(SimulationResult.Status.FAILED, result.getStatus());
    }

    @Test
    void runSimulation_missingCircuitFile_returnsFailedResult() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("/definitely/missing/file.ipes")
                .stepWidth(1e-6)
                .simulationDuration(1e-3)
                .solverType(SolverType.SOLVER_BE)
                .build();

        SimulationResult result = engine.runSimulation(config);

        assertFalse(result.isSuccess());
        assertEquals(SimulationResult.Status.FAILED, result.getStatus());
    }

    @Test
    void cancelWhileIdle_doesNotBlockNextSimulation() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        engine.cancel();

        SimulationConfig config = SimulationConfig.builder()
                .stepWidth(1e-6)
                .simulationDuration(1e-3)
                .solverType(SolverType.SOLVER_BE)
                .build();

        SimulationResult result = engine.runSimulation(config);

        assertTrue(result.isSuccess());
        assertEquals(SimulationResult.Status.SUCCESS, result.getStatus());
    }
}

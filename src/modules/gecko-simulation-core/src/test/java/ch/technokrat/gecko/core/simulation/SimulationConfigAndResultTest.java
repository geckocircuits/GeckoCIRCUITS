/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package ch.technokrat.gecko.core.simulation;

import ch.technokrat.gecko.core.allg.SolverSettingsCore;
import ch.technokrat.gecko.core.datacontainer.DataContainerGlobal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationConfigAndResultTest {

    @Test
    void simulationConfig_keepsInternalSolverSettingsImmutable() {
        SolverSettingsCore settings = new SolverSettingsCore();
        settings.setStepWidth(1e-6);

        SimulationConfig config = SimulationConfig.builder()
                .solverSettings(settings)
                .build();

        settings.setStepWidth(5e-6);
        assertEquals(1e-6, config.getSolverSettings().getStepWidth(), 1e-12);

        SolverSettingsCore settingsFromConfig = config.getSolverSettings();
        settingsFromConfig.setStepWidth(9e-6);
        assertEquals(1e-6, config.getSolverSettings().getStepWidth(), 1e-12);
    }

    @Test
    void simulationResult_getTimeArrayWithNoRows_returnsEmptyArray() {
        DataContainerGlobal dataContainer = new DataContainerGlobal();
        dataContainer.init(0, 10, new String[0], "time [s]");

        SimulationResult result = SimulationResult.builder()
                .status(SimulationResult.Status.SUCCESS)
                .dataContainer(dataContainer)
                .build();

        assertArrayEquals(new double[0], result.getTimeArray());
    }
}

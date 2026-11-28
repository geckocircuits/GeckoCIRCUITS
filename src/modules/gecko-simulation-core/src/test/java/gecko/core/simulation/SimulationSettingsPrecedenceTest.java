package gecko.core.simulation;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SimulationSettingsPrecedenceTest {
    private String circuit() throws Exception {
        return Path.of(Objects.requireNonNull(getClass().getResource("/ipes/buck_simple.ipes")).toURI()).toString();
    }

    @Test
    void explicitSettingsOverrideTheCircuitFile() throws Exception {
        SimulationResult result = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder()
                .circuitFile(circuit()).stepWidth(0.00025).simulationDuration(0.002)
                .solverType(SolverType.SOLVER_GS).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(0.00025, (double) result.getMetadata().get("dt"));
        assertEquals(SolverType.SOLVER_GS.toString(), result.getMetadata().get("solver"));
        assertEquals(9, result.getTotalTimeSteps());
    }

    @Test
    void unspecifiedSettingsStillComeFromTheCircuitFile() throws Exception {
        SimulationResult result = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder().circuitFile(circuit()).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(1e-6, (double) result.getMetadata().get("dt"));
        assertEquals(SolverType.SOLVER_BE.toString(), result.getMetadata().get("solver"));
        assertEquals(1000, result.getTotalTimeSteps());
    }

    @Test
    void oneExplicitSettingDoesNotReplaceOtherFileDefaults() throws Exception {
        SimulationResult result = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder().circuitFile(circuit()).simulationDuration(3e-6).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(1e-6, (double) result.getMetadata().get("dt"));
        assertEquals(4, result.getTotalTimeSteps());
    }

    @Test
    void solverSettingsObjectIsAnExplicitOverride() throws Exception {
        SolverSettingsCore settings = new SolverSettingsCore(SolverType.SOLVER_TRZ, 0.00025, 0.001);
        SimulationConfig config = SimulationConfig.builder().circuitFile(circuit()).solverSettings(settings).build();
        settings.setStepWidth(0.1);
        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(0.00025, (double) result.getMetadata().get("dt"));
        assertEquals(SolverType.SOLVER_TRZ.toString(), result.getMetadata().get("solver"));
        assertEquals(5, result.getTotalTimeSteps());
    }

    @Test
    void omittedSolverAndDurationUseDifferentFileDefaults() {
        CircuitModel file = new CircuitModel();
        file.setSolverType(SolverType.SOLVER_GS);
        file.setTimeStep(1e-6);
        file.setSimulationDuration(0.003);
        SimulationConfig config = SimulationConfig.builder().stepWidth(0.0001).build();
        SolverSettingsCore resolved = config.resolveSolverSettings(file);
        assertEquals(SolverType.SOLVER_GS, resolved.getSolverType());
        assertEquals(0.003, resolved.getSimulationDuration());
        assertEquals(0.0001, resolved.getStepWidth());
        assertEquals(SolverType.SOLVER_BE, config.getSolverSettings().getSolverType());
    }

    @Test
    void invalidExplicitSettingsAreNotHiddenByValidFileDefaults() throws Exception {
        SimulationResult result = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder().circuitFile(circuit()).stepWidth(0).build());
        assertEquals(SimulationResult.Status.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("Step width"));
    }
}

package gecko.core.simulation;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class LegacyPreparationSettingsContractTest {
    private static Path saved(String settings) throws Exception {
        Path file = Files.createTempFile("legacy-preparation-settings-", ".ipes");
        var values = new LinkedHashMap<String, String>();
        values.put("dt", "0.125");
        values.put("tDURATION", "0.25");
        values.put("solverType", "0");
        for (String line : settings.split("\n")) {
            String[] parts = line.split(" ", 2);
            values.put(parts[0], parts[1]);
        }
        String header = values.entrySet().stream().map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining("\n", "", "\n"));
        Files.writeString(file, header + """
                e
                <ElementLK>
                typ 1
                idStringDialog R
                parameter[] 10
                labelAnfangsKnoten[] /n
                labelEndKnoten[] /0
                <\\ElementLK>
                """);
        return file;
    }

    private static void supply(SimulationConfig.Builder builder, String field) {
        switch (field) {
            case "timeStep" -> builder.stepWidth(0.125);
            case "duration" -> builder.simulationDuration(0.25);
            case "solver" -> builder.solverType(SolverType.SOLVER_TRZ);
            default -> throw new IllegalArgumentException(field);
        }
    }

    private static void assertRejectedBeforeSteps(SimulationConfig config) {
        var count = new AtomicInteger();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, amps, netlist) -> count.incrementAndGet());
        var result = engine.runSimulation(config);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("INVALID_SETTING"), result.getErrorMessage());
        assertEquals(0, count.get());
        assertEquals(0, result.getTotalTimeSteps());
    }

    @ParameterizedTest
    @CsvSource({"dt,broken,timeStep", "tDURATION,broken,duration", "solverType,broken,solver", "solverType,9,solver"})
    void omittedCallerSettingCannotRepairMalformedSavedEvidence(String token, String value, String field) throws Exception {
        var config = SimulationConfig.builder().circuitFile(saved(token + " " + value).toString()).build();
        assertRejectedBeforeSteps(config);
    }

    @ParameterizedTest
    @CsvSource({"dt,broken,timeStep", "tDURATION,broken,duration", "solverType,broken,solver", "solverType,9,solver"})
    void actualCallerSettingRepairsOnlyTheSelectedSavedField(String token, String value, String field) throws Exception {
        var builder = SimulationConfig.builder().circuitFile(saved(token + " " + value).toString());
        supply(builder, field);
        var result = new HeadlessSimulationEngine().runSimulation(builder.build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(3, result.getTotalTimeSteps());
        assertEquals(0.25, result.getSimulatedTime());
    }

    @ParameterizedTest
    @CsvSource({"timeStep", "duration", "solver"})
    void oneCallerSettingDoesNotRepairOtherMalformedSavedFields(String repairedField) throws Exception {
        var builder = SimulationConfig.builder().circuitFile(saved("dt broken\ntDURATION broken\nsolverType 9").toString());
        supply(builder, repairedField);
        assertRejectedBeforeSteps(builder.build());
    }

    @Test
    void anExplicitSettingsObjectRepairsAllThreeSavedFields() throws Exception {
        var config = SimulationConfig.builder().circuitFile(saved("dt broken\ntDURATION broken\nsolverType 9").toString())
                .solverSettings(new SolverSettingsCore(SolverType.SOLVER_TRZ, 0.125, 0.25)).build();
        var result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(3, result.getTotalTimeSteps());
        assertEquals(SolverType.SOLVER_TRZ.toString(), result.getMetadata().get("solver"));
    }
    @Test
    void blankLegacySourceRetainsTheExplicitNoFileBehavior() {
        var baseline = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder()
                .stepWidth(0.125).simulationDuration(0.25).build());
        var blank = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder()
                .circuitFile(" \t").stepWidth(0.125).simulationDuration(0.25).build());
        assertTrue(baseline.isSuccess(), baseline.getErrorMessage());
        assertTrue(blank.isSuccess(), blank.getErrorMessage());
        assertEquals(3, blank.getTotalTimeSteps());
        assertEquals(0.25, blank.getSimulatedTime());
        assertArrayEquals(baseline.getTimeArray(), blank.getTimeArray());
        assertArrayEquals(baseline.getSignalData(0), blank.getSignalData(0));
    }

    @ParameterizedTest
    @CsvSource({"1.0E-12,1.0", "4.9E-324,1.0"})
    void excessiveStepCountsFailBeforeAnyNumericalStep(double step, double duration) throws Exception {
        var count = new AtomicInteger();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, amps, netlist) -> count.incrementAndGet());
        var result = engine.runSimulation(SimulationConfig.builder().circuitFile(saved("dt 0.125").toString())
                .stepWidth(step).simulationDuration(duration).build());
        assertEquals(SimulationResult.Status.FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());
        assertEquals(0, result.getTotalTimeSteps());
        assertEquals(0, count.get());
        assertEquals(HeadlessSimulationEngine.EngineState.IDLE, engine.getState());
    }
}

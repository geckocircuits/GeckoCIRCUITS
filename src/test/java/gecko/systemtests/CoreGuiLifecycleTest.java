package gecko.systemtests;

import gecko.GeckoExternal;
import gecko.GeckoSim;
import gecko.core.io.CircuitFileParser;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.simulation.SessionHost;
import gecko.core.simulation.SimulationSession;
import gecko.geckocircuits.allg.SimulationRunner;
import gecko.geckocircuits.control.NetzlisteCONTROL;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import gecko.systemtests.GoldenReferenceHelper.GoldenEntry;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

@Tag("regression")
class CoreGuiLifecycleTest {
    private static final Path BUCK_CIRCUIT = Path.of("src/test/resources/ipes/education/buck_simple.ipes");
    private static final Path BUCK_GOLDEN = Path.of("src/test/resources/golden/buck_simple.golden.csv");
    private static final long DEADLINE_SECONDS = 60;

    @Test
    @Timeout(value = 4, unit = java.util.concurrent.TimeUnit.MINUTES)
    void asynchronousCoreRunCanPauseContinueAndExtendWithoutLosingSamples() throws Exception {
        String previousBackend = System.getProperty("gecko.backend");
        boolean previousTestingMode = GeckoSim._isTestingMode;
        System.setProperty("gecko.backend", "core");
        try {
            GeckoSim._isTestingMode = true;
            GeckoSim.main(new String[]{});
            await(() -> GeckoSim._win != null && GeckoSim._win._simRunner != null,
                    "GeckoCIRCUITS did not finish initialization");

            SimulationRunner runner = GeckoSim._win._simRunner;
            boolean started = false;
            try {
            List<GoldenEntry> goldenEntries = GoldenReferenceHelper.readGoldenCsv(BUCK_GOLDEN);
            assertFalse(goldenEntries.isEmpty(), "buck_simple golden has no scope ports");
            double[] simParams = GoldenReferenceHelper.readSimulationParams(BUCK_GOLDEN);
            assertNotNull(simParams, "buck_simple golden has no simulation metadata");
            double endTime = simParams[0] * 20;

            openBuckCircuit();
            GeckoSim._win._solverSettings._tDURATION.setValue(endTime);
            runner.startCalculation(true, GeckoSim._win._solverSettings);
            started = true;
            assertNull(runner.simKern, "CORE GUI starts must use SessionHost, not SimulationsKern");
            await(() -> {
                var status = runner.getCoreStatus();
                return status != null && status.state() == SimulationSession.State.RUNNING
                        && status.time() >= status.endTime() * 0.30;
            }, "Core GUI run did not reach 30% while RUNNING");

            assertEquals(SimulationSession.State.RUNNING, runner.getCoreStatus().state(),
                    "Pause must be requested while the host is running");
            runner.pauseSimulation();
            await(() -> NetzlisteCONTROL.globalData.getContainerStatus() == ContainerStatus.PAUSED,
                    "Core GUI run did not publish its paused state");
            double pausedTime = GeckoExternal.getSimulationTime();
            assertTrue(pausedTime >= endTime * 0.30, "Pause occurred before the requested progress point");

            runner.continueCalculation(true, GeckoSim._win._solverSettings);
            await(() -> GeckoExternal.getSimulationTime() >= endTime * 0.99
                            && NetzlisteCONTROL.globalData.getContainerStatus() == ContainerStatus.PAUSED,
                    "Core GUI run did not complete after continue");
            Map<String, double[]> asyncValues = characteristics(goldenEntries, endTime);

            runner.continueCalculation(true, GeckoSim._win._solverSettings);
            await(() -> GeckoExternal.getSimulationTime() >= endTime * 1.99
                            && NetzlisteCONTROL.globalData.getContainerStatus() == ContainerStatus.PAUSED,
                    () -> {
                        var status = runner.getCoreStatus();
                        return "Core GUI run did not finish its continuation past the original end time; host="
                                + (status == null ? "none" : status.state() + ", time=" + status.time()
                                        + ", endTime=" + status.endTime())
                                + ", settingsEnd=" + GeckoSim._win._solverSettings._tDURATION.getValue()
                                + ", globalData=" + NetzlisteCONTROL.globalData.getContainerStatus();
                    });
            runner.endSimulation();
            assertNull(runner.getCoreStatus(), "End must close the asynchronous SessionHost");
            assertEquals(ContainerStatus.PAUSED, NetzlisteCONTROL.globalData.getContainerStatus(),
                    "End must leave the captured presentation paused");

            openBuckCircuit();
            GeckoSim._win._solverSettings._tDURATION.setValue(endTime);
            GeckoExternal.runSimulation(); // B1's synchronous CORE path is the reference execution.
            Map<String, double[]> synchronousValues = characteristics(goldenEntries, endTime);
            assertEquals(asyncValues.keySet(), synchronousValues.keySet(), "Scope-port set changed between runs");
            for (String key : asyncValues.keySet()) {
                double[] async = asyncValues.get(key);
                double[] synchronous = synchronousValues.get(key);
                assertEquals(synchronous.length, async.length, key + " characteristic count changed");
                for (int index = 0; index < async.length; index++) {
                    double expected = synchronous[index];
                    double actual = async[index];
                    if (Double.isNaN(expected) || Double.isNaN(actual)) {
                        assertTrue(Double.isNaN(expected) && Double.isNaN(actual),
                                key + " characteristic " + index + " NaN mismatch");
                    } else {
                        double tolerance = Math.max(1e-12, Math.abs(expected) * 1e-9);
                        assertEquals(expected, actual, tolerance,
                                key + " characteristic " + index + " differs from synchronous core");
                    }
                }
            }

            openBuckCircuit();
            GeckoSim._win._solverSettings._tDURATION.setValue(endTime);
            runner.startCalculation(true, GeckoSim._win._solverSettings);
            await(() -> {
                var status = runner.getCoreStatus();
                return status != null && status.state() == SimulationSession.State.RUNNING
                        && status.time() >= status.endTime() * 0.30;
            }, "Second Core GUI run did not reach the active-end checkpoint while RUNNING");
            var statusBeforeEnd = runner.getCoreStatus();
            assertNotNull(statusBeforeEnd, "Active Core GUI run lost its host before end");
            int committedBeforeEnd = statusBeforeEnd.committedSteps();
            runner.endSimulation();
            await(() -> runner.getCoreStatus() == null
                            && NetzlisteCONTROL.globalData.getContainerStatus() == ContainerStatus.PAUSED,
                    "End did not finish after the active run closed");
            int capturedSamples = NetzlisteCONTROL.globalData.getMaximumTimeIndex(0) + 1;
            assertTrue(capturedSamples >= committedBeforeEnd,
                    "End retired the host before draining already committed samples");
            double lastCapturedTime = NetzlisteCONTROL.globalData.getTimeValue(capturedSamples - 1, 0);
            assertEquals(runner.getCurrentTime(), lastCapturedTime, 1e-12,
                    "End must drain the final committed sample before clearing the host");
        } finally {
                if (started) {
                    runner.endSimulation();
                    await(() -> NetzlisteCONTROL.globalData.getContainerStatus() == ContainerStatus.PAUSED
                                    || runner.getCurrentTime() == 0,
                            "Simulation did not stop during test cleanup");
                }
            }
        } finally {
            GeckoSim._isTestingMode = previousTestingMode;
            if (previousBackend == null) System.clearProperty("gecko.backend");
            else System.setProperty("gecko.backend", previousBackend);
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void fractionalGridCompletionReturnsWithoutAnotherSample() throws Exception {
        var parser = new CircuitFileParser();
        var model = parser.parse(BUCK_CIRCUIT.toAbsolutePath().toString());
        double dt = model.getTimeStep();
        assertTrue(dt > 0, "buck_simple must provide a positive time step");
        double endTime = dt * 999.5;
        double pauseAt = dt * 999;
        model.setSimulationDuration(endTime);
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));

        try (SessionHost host = SessionHost.open(ready.execution(), ExecutionOptions.defaults())) {
            SimulationSession.Status completed = host.runUntil(pauseAt).get(10, TimeUnit.SECONDS);
            assertEquals(SimulationSession.State.COMPLETED, completed.state());
            assertTrue(completed.time() < completed.endTime());
            assertTrue(completed.nextTime() > completed.endTime(),
                    "Fixture must complete between two time-grid points");
            var samples = new java.util.ArrayList<SimulationSession.Sample>();
            host.drainSamples(samples::add);
            assertEquals(completed.committedSteps(), samples.size());

            var resumed = host.run();
            SimulationSession.Status unchanged = resumed.get(10, TimeUnit.SECONDS);
            assertTrue(resumed.isDone(), "A completed session must release a continuation Future immediately");
            assertEquals(SimulationSession.State.COMPLETED, unchanged.state());
            assertEquals(completed.committedSteps(), unchanged.committedSteps(),
                    "A run after fractional-grid completion cannot commit another sample");
            assertEquals(completed.time(), unchanged.time(), 0,
                    "A run after fractional-grid completion must retain the last committed time");
            assertEquals(0, host.drainSamples(sample -> fail("Completed continuation emitted an extra sample")));
        }
    }

    private static void openBuckCircuit() throws Exception {
        Path circuit = BUCK_CIRCUIT.toAbsolutePath();
        assertTrue(circuit.toFile().isFile(), "Missing circuit: " + circuit);
        GeckoExternal.openFile(circuit.toString());
    }

    private static Map<String, double[]> characteristics(List<GoldenEntry> entries, double endTime) {
        Map<String, double[]> result = new LinkedHashMap<>();
        for (GoldenEntry entry : entries) {
            String key = entry.scopeName + " port " + entry.port;
            result.put(key, GeckoExternal.getSignalCharacteristics(entry.scopeName, entry.port,
                    endTime * 0.8, endTime));
        }
        return result;
    }

    private static void await(BooleanSupplier condition, String failureMessage) {
        await(condition, () -> failureMessage);
    }

    private static void await(BooleanSupplier condition, Supplier<String> failureMessage) {
        long deadline = System.nanoTime() + Duration.ofSeconds(DEADLINE_SECONDS).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
            if (Thread.currentThread().isInterrupted()) {
                throw new AssertionError("Interrupted while waiting: " + failureMessage.get());
            }
        }
        assertTrue(condition.getAsBoolean(), failureMessage.get());
    }
}

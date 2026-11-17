package gecko.core.preparation;

import gecko.core.io.CircuitFileParser;
import gecko.core.simulation.SimulationSession;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class EducationCircuitReadinessTest {
    private static final List<String> CIRCUITS = List.of(
            "buck_simple",
            "boost_simple",
            "buckBoost_simple",
            "cuk_simple",
            "sepic_simple",
            "boostPFC",
            "boostPFC_currentControl",
            "singlePhase_PWM_converter",
            "diode_RL_singlePH_trafo",
            "2phaseDiodeBridge_AC-Inductor",
            "2phaseDiodeBridge_DC-Inductor",
            "thyristor_RL_single",
            "thyristor_RL_singlePh_trafo",
            "thyristor_RL_2phBridge",
            "thyristor_RL_3phBridge",
            "thyristor_RL_3ph_trafo",
            "thyristor_freeWheelingDiode",
            "thyristor_interface_trafo",
            "thyristor_commutation_3ph_trafo",
            "thyristor_lossOfCommutation",
            "thyristor_Jakopovic");

    static Stream<String> circuitFiles() {
        return CIRCUITS.stream();
    }

    @Test
    void corpusContainsExactlyTwentyOneDistinctCircuits() {
        assertEquals(21, CIRCUITS.size());
        assertEquals(21, CIRCUITS.stream().distinct().count());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("circuitFiles")
    void circuitPreparesAndCompletesWithFiniteSamples(String circuitName) throws Exception {
        File file = findCircuit(circuitName);
        var model = new CircuitFileParser().parse(file.getAbsolutePath());
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        String diagnostics = result.report().diagnostics().stream()
                .map(item -> item.code() + "[" + item.component() + "]: " + item.message())
                .collect(Collectors.joining("; "));
        if (!(result instanceof PreparationResult.Ready ready)) {
            System.out.printf("EDUCATION | %s | %s | %s | NOT_RUN%n",
                    circuitName, result.report().outcome(), diagnostics);
            fail(circuitName + " is not Ready: " + diagnostics);
            return;
        }

        long started = System.nanoTime();
        try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            var status = session.run();
            assertEquals(SimulationSession.State.COMPLETED, status.state(), circuitName);
            assertTrue(session.signalNames().length > 0, circuitName + " has no signals");
            int samples = 0;
            for (;;) {
                var observation = session.observe(samples, 10000);
                if (observation.times().length == 0) break;
                for (int row = 0; row < observation.values().length; row++) {
                    float[] values = observation.values()[row];
                    for (int index = 0; index < values.length; index++) {
                        assertTrue(Float.isFinite(values[index]), circuitName + " has nonfinite value at row "
                                + row + ", sample " + (samples + index) + ": " + values[index]);
                    }
                }
                samples += observation.times().length;
            }
            assertTrue(samples > 0, circuitName + " has no recorded samples");
            assertEquals(status.committedSteps(), (long) samples, circuitName + " must observe every committed step");
            double seconds = (System.nanoTime() - started) / 1e9;
            System.out.printf(Locale.ROOT,
                    "EDUCATION | %s | READY | %s | COMPLETED, %d samples, %.3f s%s%n",
                    circuitName, diagnostics, samples, seconds, seconds > 60 ? " (OVER_60_SECONDS)" : "");
        } catch (RuntimeException | AssertionError failure) {
            System.out.printf(Locale.ROOT, "EDUCATION | %s | READY | %s | FAILED: %s, %.3f s%n",
                    circuitName, diagnostics, failure, (System.nanoTime() - started) / 1e9);
            throw failure;
        }
    }

    private static File findCircuit(String name) {
        File file = new File("../../test/resources/ipes/education/" + name + ".ipes");
        if (file.isFile()) return file;
        file = new File("src/test/resources/ipes/education/" + name + ".ipes");
        assertTrue(file.isFile(), "Missing education circuit: " + file.getAbsolutePath());
        return file;
    }
}

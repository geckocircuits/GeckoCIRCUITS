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
import gecko.geckocircuits.allg.MainWindow;
import gecko.geckocircuits.circuit.EditorCircuitCapture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Reports exact parser/editor sample differences without replacing the golden acceptance tests. */
@Tag("regression")
class CoreInputParityProbeTest {
    private record Recording(String[] names, List<SimulationSession.Sample> samples) { }

    @BeforeAll
    static void openDesktop() {
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[0]);
    }

    @AfterAll
    static void closeDesktop() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (var window : java.awt.Window.getWindows()) window.dispose();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"cuk_simple", "sepic_simple", "thyristor_RL_2phBridge"})
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void compareEveryCommittedSampleFromBothPreparedInputs(String circuit) throws Exception {
        Path path = Path.of("src/test/resources/ipes/education", circuit + ".ipes").toAbsolutePath();
        assertTrue(Files.isRegularFile(path), path::toString);
        var preparation = new CircuitPreparation();
        var parsed = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(new CircuitFileParser().parse(path.toString())),
                        PreparationRequest.defaults()));

        GeckoExternal.openFile(path.toString());
        var scripts = MainWindow._scripter;
        var captured = new EditorCircuitCapture().capture(MainWindow._se._circuitSheet.getAllElements(),
                MainWindow._solverSettings, MainWindow.optimizerParameterData, MainWindow._fileManager,
                new EditorCircuitCapture.ScriptSources(scripts.getScriptCode(), scripts.getImportCode(),
                        scripts.getDeclarationCode(), scripts.getExtraFilesHashes()), path.toString());
        var editor = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(captured, PreparationRequest.defaults()));

        Recording direct = run(parsed);
        Recording desktop = run(editor);
        assertEquals(direct.names().length, Arrays.stream(direct.names()).distinct().count(), "Duplicate parser signal names");
        assertEquals(desktop.names().length, Arrays.stream(desktop.names()).distinct().count(), "Duplicate editor signal names");
        assertEquals(new java.util.HashSet<>(Arrays.asList(direct.names())),
                new java.util.HashSet<>(Arrays.asList(desktop.names())), "The signal sets differ");
        int common = Math.min(direct.samples().size(), desktop.samples().size());
        int timeDifferences = 0;
        for (int step = 0; step < common; step++) {
            if (Double.doubleToLongBits(direct.samples().get(step).time())
                    != Double.doubleToLongBits(desktop.samples().get(step).time())) timeDifferences++;
        }
        System.out.println("INPUT_PARITY: " + circuit + " | parserSteps=" + direct.samples().size()
                + " | editorSteps=" + desktop.samples().size() + " | timeDifferences=" + timeDifferences);
        long totalDifferences = 0;
        for (int signal = 0; signal < direct.names().length; signal++) {
            String name = direct.names()[signal];
            int other = Arrays.asList(desktop.names()).indexOf(name);
            int differences = 0;
            String first = "none";
            double maximum = 0;
            for (int step = 0; step < common; step++) {
                var left = direct.samples().get(step);
                var right = desktop.samples().get(step);
                float expected = left.values()[signal];
                float actual = right.values()[other];
                if (Float.floatToIntBits(expected) != Float.floatToIntBits(actual)) {
                    differences++;
                    maximum = Math.max(maximum, Math.abs((double) expected - actual));
                    if (differences == 1) first = "step=" + step + ",time=" + left.time()
                            + ",parser=" + expected + ",editor=" + actual;
                }
            }
            totalDifferences += differences;
            System.out.println("INPUT_SIGNAL: " + circuit + " | " + name + " | differences=" + differences
                    + "/" + common + " | first=" + first + " | maxAbs=" + maximum);
        }
        System.out.println("INPUT_SUMMARY: " + circuit + " | valueDifferences=" + totalDifferences);
    }

    private static Recording run(PreparationResult.Ready circuit) throws Exception {
        assertFalse(SwingUtilities.isEventDispatchThread());
        try (var host = SessionHost.open(circuit.execution(), ExecutionOptions.defaults())) {
            String[] names = host.signalNames();
            var status = host.run().get(60, TimeUnit.SECONDS);
            assertEquals(SimulationSession.State.COMPLETED, status.state());
            var samples = new ArrayList<SimulationSession.Sample>();
            host.drainSamples(samples::add);
            assertEquals(status.committedSteps(), samples.size());
            assertFalse(samples.isEmpty());
            return new Recording(names, List.copyOf(samples));
        }
    }
}

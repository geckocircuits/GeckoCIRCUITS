package gecko.systemtests;

import gecko.GeckoExternal;
import gecko.GeckoSim;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.simulation.SessionHost;
import gecko.geckocircuits.allg.MainWindow;
import gecko.geckocircuits.circuit.EditorCircuitCapture;
import gecko.geckocircuits.circuit.SolverSettings;
import gecko.geckocircuits.control.NetzlisteCONTROL;
import gecko.geckocircuits.datacontainer.DataContainerGlobal;
import gecko.core.io.CircuitFileParser;
import gecko.core.simulation.SimulationSession;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

@Tag("regression")
class ThyristorBridgeCaptureParityTest {
    private record SimulationSeries(String[] names, List<SimulationSession.Sample> samples) { }
    private record LegacySeries(String[] names, double[][] times, float[][] values) { }
    private static Path circuitPath;
    private static Path goldenPath;

    @BeforeAll
    static void startGecko() throws Exception {
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[0]);
        circuitPath = resource("/ipes/education/thyristor_RL_2phBridge.ipes");
        goldenPath = resource("/golden/thyristor_RL_2phBridge.golden.csv");
        SwingUtilities.invokeAndWait(() -> { });
    }

    @AfterAll
    static void closeGecko() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (var window : java.awt.Window.getWindows()) window.dispose();
        });
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void editorCapturedBridgeMatchesLegacyGoldenWhileDirectParserIsMeasured() throws Exception {
        GeckoExternal.openFile(circuitPath.toAbsolutePath().toString());

        var parser = new CircuitFileParser();
        var directModel = parser.parse(circuitPath.toString());
        var directSnapshot = new CircuitPreparation().capture(directModel);
        var directResult = new CircuitPreparation().prepare(directSnapshot, PreparationRequest.defaults());
        assertInstanceOf(PreparationResult.Ready.class, directResult);

        var scripts = MainWindow._scripter;
        var editorSnapshot = new EditorCircuitCapture().capture(MainWindow._se._circuitSheet.getAllElements(),
                MainWindow._solverSettings, MainWindow.optimizerParameterData, MainWindow._fileManager,
                new EditorCircuitCapture.ScriptSources(scripts.getScriptCode(), scripts.getImportCode(),
                        scripts.getDeclarationCode(), scripts.getExtraFilesHashes()), circuitPath.toString());
        var editorResult = new CircuitPreparation().prepare(editorSnapshot, PreparationRequest.defaults());
        assertInstanceOf(PreparationResult.Ready.class, editorResult);

        String previousBackend = System.getProperty("gecko.backend");
        LegacySeries legacy;
        try {
            // This assertion compares editor-captured core output with the old desktop reference.
            System.setProperty("gecko.backend", "legacy");
            GeckoExternal.runSimulation();
            legacy = captureLegacy(NetzlisteCONTROL.globalData);
        } finally {
            if (previousBackend == null) System.clearProperty("gecko.backend");
            else System.setProperty("gecko.backend", previousBackend);
        }

        double[] window = GoldenReferenceHelper.readSimulationParams(goldenPath);
        assertNotNull(window, "Golden file must identify the simulation duration");
        double startTime = window[0] * 0.8;
        SimulationSeries directSamples = run(((PreparationResult.Ready) directResult).execution());
        SimulationSeries editorSamples = run(((PreparationResult.Ready) editorResult).execution());
        double directAverage = average(directSamples, "u2", startTime, window[0]);
        double editorAverage = average(editorSamples, "u2", startTime, window[0]);
        double legacyAverage = average(legacy, "u2", startTime, window[0]);
        var expected = GoldenReferenceHelper.readGoldenCsv(goldenPath).stream()
                .filter(entry -> entry.scopeName.equals("SCOPE.1") && entry.port == 2)
                .findFirst().orElseThrow().characteristics[0];

        assertTrue(GoldenReferenceHelper.withinTolerance(expected, legacyAverage, 0.01, 1e-10),
                () -> String.format("Legacy desktop u2 average %.9g differs from golden %.9g", legacyAverage, expected));
        assertTrue(GoldenReferenceHelper.withinTolerance(legacyAverage, editorAverage, 0.01, 1e-10),
                () -> String.format("Editor-captured core u2 average %.9g differs from legacy desktop %.9g; "
                        + "direct-parser core average %.9g. First control samples: %s", editorAverage, legacyAverage,
                        directAverage, trace(legacy, editorSamples, directSamples)));
    }

    private static SimulationSeries run(gecko.core.preparation.PreparedCircuit circuit) throws Exception {
        try (var host = SessionHost.open(circuit, ExecutionOptions.defaults())) {
            String[] names = host.signalNames();
            host.run().get();
            List<SimulationSession.Sample> samples = new ArrayList<>();
            host.drainSamples(samples::add);
            return new SimulationSeries(names, samples);
        }
    }

    private static double average(SimulationSeries series, String signal, double start, double end) {
        assertFalse(series.samples().isEmpty(), "Core run must produce samples");
        int row = java.util.Arrays.asList(series.names()).indexOf(signal);
        assertTrue(row >= 0, () -> "Core signal not found: " + signal + " in " + java.util.Arrays.toString(series.names()));
        double sum = 0;
        int count = 0;
        for (var sample : series.samples()) {
            if (sample.time() >= start && sample.time() <= end) {
                sum += sample.values()[row];
                count++;
            }
        }
        assertTrue(count > 0, "Steady-state window must include core samples");
        return sum / count;
    }

    private static LegacySeries captureLegacy(DataContainerGlobal data) {
        String[] names = new String[data.getRowLength()];
        double[][] times = new double[names.length][];
        float[][] values = new float[names.length][];
        for (int row = 0; row < names.length; row++) {
            names[row] = data.getSignalName(row);
            int count = data.getMaximumTimeIndex(row) + 1;
            times[row] = new double[count];
            values[row] = new float[count];
            for (int index = 0; index < count; index++) {
                times[row][index] = data.getTimeValue(index, row);
                values[row][index] = data.getValue(row, index);
            }
        }
        return new LegacySeries(names, times, values);
    }

    private static double average(LegacySeries series, String signal, double start, double end) {
        int row = java.util.Arrays.asList(series.names()).indexOf(signal);
        assertTrue(row >= 0, () -> "Legacy signal not found: " + signal + " in " + java.util.Arrays.toString(series.names()));
        double sum = 0;
        int count = 0;
        for (int index = 0; index < series.times()[row].length; index++) {
            double time = series.times()[row][index];
            if (time >= start && time <= end) {
                sum += series.values()[row][index];
                count++;
            }
        }
        assertTrue(count > 0, "Legacy steady-state window must include samples");
        return sum / count;
    }

    private static String trace(LegacySeries legacy, SimulationSeries editor, SimulationSeries direct) {
        StringBuilder result = new StringBuilder();
        double[] times = {0, 0.000001, 0.001521, 0.001522};
        for (String signal : List.of("p", "sg", "gt1", "uTH", "u2", "i")) {
            result.append(signal).append('[');
            for (double time : times) {
                result.append(String.format("%.6g:L%.6g/E%.6g/D%.6g;", time,
                        valueAt(legacy, signal, time), valueAt(editor, signal, time), valueAt(direct, signal, time)));
            }
            result.append(']');
        }
        return result.toString();
    }

    private static float valueAt(LegacySeries series, String signal, double target) {
        int row = java.util.Arrays.asList(series.names()).indexOf(signal);
        if (row < 0) return Float.NaN;
        int index = closest(series.times()[row], target);
        return series.values()[row][index];
    }

    private static float valueAt(SimulationSeries series, String signal, double target) {
        int row = java.util.Arrays.asList(series.names()).indexOf(signal);
        if (row < 0) return Float.NaN;
        return (float) series.samples().get(closestSample(series.samples(), target)).values()[row];
    }

    private static int closest(double[] times, double target) {
        int index = java.util.Arrays.binarySearch(times, target);
        if (index >= 0) return index;
        index = -index - 1;
        if (index == 0) return 0;
        if (index == times.length) return times.length - 1;
        return target - times[index - 1] <= times[index] - target ? index - 1 : index;
    }

    private static int closestSample(List<SimulationSession.Sample> samples, double target) {
        for (int index = 0; index < samples.size(); index++) {
            if (samples.get(index).time() >= target) {
                if (index == 0) return 0;
                return target - samples.get(index - 1).time() <= samples.get(index).time() - target ? index - 1 : index;
            }
        }
        return samples.size() - 1;
    }

    private static Path resource(String name) throws URISyntaxException {
        return Path.of(ThyristorBridgeCaptureParityTest.class.getResource(name).toURI());
    }
}

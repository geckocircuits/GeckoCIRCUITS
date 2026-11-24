package gecko.systemtests;

import gecko.GeckoExternal;
import gecko.GeckoSim;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.simulation.SessionHost;
import gecko.core.simulation.SimulationSession;
import gecko.geckocircuits.allg.MainWindow;
import gecko.geckocircuits.circuit.EditorCircuitCapture;
import gecko.geckocircuits.control.NetzlisteCONTROL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the saved BuckBoost thermal circuit (IGBT and diode with SIMPLE loss models) on the legacy kernel and in the
 * core. The junction temperature, its rise and the mean losses must agree within 1 %.
 */
@Tag("regression")
class ThermalJunctionParityTest {
    private static final double TOLERANCE = 0.01;

    private static double[] legacy(String name) {
        var data = NetzlisteCONTROL.globalData;
        int row = -1;
        for (int index = 0; index < data.getRowLength(); index++) if (data.getSignalName(index).equals(name)) row = index;
        assertTrue(row >= 0, "Legacy recording must contain " + name);
        double[] values = new double[data.getMaximumTimeIndex(row) + 1];
        for (int index = 0; index < values.length; index++) values[index] = data.getValue(row, index);
        return values;
    }

    private static double mean(double[] values, int from, int to) {
        double sum = 0;
        for (int index = from; index < to; index++) sum += values[index];
        return sum / (to - from);
    }

    @Test
    @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void junctionTemperatureRiseAndMeanLossesMatchTheLegacyKernel() throws Exception {
        String backend = System.getProperty("gecko.backend");
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[0]);
        double duration = MainWindow._solverSettings._tDURATION.getValue();
        try {
            String path = Path.of("src/test/resources/ipes/thermal/BuckBoost_thermal.ipes").toAbsolutePath().toString();
            GeckoExternal.openFile(path);
            MainWindow._solverSettings._tDURATION.setValueWithoutUndo(0.02);
            var scripts = MainWindow._scripter;
            var snapshot = new EditorCircuitCapture().capture(MainWindow._se._circuitSheet.getAllElements(),
                    MainWindow._solverSettings, MainWindow.optimizerParameterData, MainWindow._fileManager,
                    new EditorCircuitCapture.ScriptSources(scripts.getScriptCode(), scripts.getImportCode(),
                            scripts.getDeclarationCode(), scripts.getExtraFilesHashes()), path);
            var ready = assertInstanceOf(PreparationResult.Ready.class,
                    new CircuitPreparation().prepare(snapshot, PreparationRequest.defaults()),
                    "The saved thermal circuit must be ready for the core.");
            System.setProperty("gecko.backend", "legacy");
            GeckoExternal.runSimulation();
            double[] legacyTemperature = legacy("TjSw"), legacySwitch = legacy("PSw"), legacyDiode = legacy("PD");
            try (var host = SessionHost.open(ready.execution(), ExecutionOptions.defaults())) {
                var names = Arrays.asList(host.signalNames());
                int temperatureRow = names.indexOf("TjSw"), switchRow = names.indexOf("PSw"), diodeRow = names.indexOf("PD");
                assertTrue(temperatureRow >= 0 && switchRow >= 0 && diodeRow >= 0, names.toString());
                assertEquals(SimulationSession.State.COMPLETED, host.run().get(300, TimeUnit.SECONDS).state());
                var samples = new ArrayList<SimulationSession.Sample>();
                host.drainSamples(samples::add);
                int count = Math.min(samples.size(), legacyTemperature.length);
                assertTrue(Math.abs(samples.size() - legacyTemperature.length) <= 1,
                        "sample counts: core " + samples.size() + ", legacy " + legacyTemperature.length);
                double peak = Arrays.stream(legacyTemperature).map(Math::abs).max().orElseThrow();
                assertTrue(peak > 1, "the junction must heat in the legacy run: " + peak);
                double worst = 0;
                for (int index = 0; index < count; index++) {
                    double core = samples.get(index).values()[temperatureRow];
                    // A floor of 0.1 % of the peak keeps the first, nearly cold samples from dominating.
                    worst = Math.max(worst, Math.abs(core - legacyTemperature[index]) / Math.max(Math.abs(legacyTemperature[index]), 1e-3 * peak));
                }
                double legacyRise = legacyTemperature[count - 1] - legacyTemperature[0];
                double coreRise = samples.get(count - 1).values()[temperatureRow] - samples.get(0).values()[temperatureRow];
                double[] coreSwitch = new double[count], coreDiode = new double[count];
                for (int index = 0; index < count; index++) {
                    coreSwitch[index] = samples.get(index).values()[switchRow];
                    coreDiode[index] = samples.get(index).values()[diodeRow];
                }
                int window = Math.max(1, (int) Math.rint(1e-3 / (samples.get(1).time() - samples.get(0).time())));
                StringBuilder report = new StringBuilder("THERMAL_PARITY: samples=" + count + " peak=" + peak + " worstPointwise=" + worst
                        + " legacyRise=" + legacyRise + " coreRise=" + coreRise);
                for (int from = 0; from + window <= count; from += window) {
                    report.append("\n  window ").append(from).append(": PSw legacy=").append(mean(legacySwitch, from, from + window))
                            .append(" core=").append(mean(coreSwitch, from, from + window))
                            .append(" PD legacy=").append(mean(legacyDiode, from, from + window))
                            .append(" core=").append(mean(coreDiode, from, from + window));
                }
                System.out.println(report);
                assertTrue(worst <= TOLERANCE, "pointwise junction temperature, worst relative deviation " + worst);
                assertEquals(legacyRise, coreRise, Math.abs(legacyRise) * TOLERANCE, "junction temperature rise");
                for (int from = 0; from + window <= count; from += window) {
                    double expectedSwitch = mean(legacySwitch, from, from + window), expectedDiode = mean(legacyDiode, from, from + window);
                    assertEquals(expectedSwitch, mean(coreSwitch, from, from + window), Math.abs(expectedSwitch) * TOLERANCE + 1e-9, "PSw window " + from);
                    assertEquals(expectedDiode, mean(coreDiode, from, from + window), Math.abs(expectedDiode) * TOLERANCE + 1e-9, "PD window " + from);
                }
            }
        } finally {
            if (backend == null) System.clearProperty("gecko.backend");
            else System.setProperty("gecko.backend", backend);
            MainWindow._solverSettings._tDURATION.setValueWithoutUndo(duration);
            SwingUtilities.invokeAndWait(() -> { for (var window : java.awt.Window.getWindows()) window.dispose(); });
        }
    }
}

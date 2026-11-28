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

/** Preserves the desktop inductor-current minimum without a tolerance or golden change. */
@Tag("regression")
class PfcCurrentParityTest {
    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void nearZeroInductorMinimumMatchesLegacyAndGolden() throws Exception {
        String backend = System.getProperty("gecko.backend");
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[0]);
        try {
            String path = Path.of("src/test/resources/ipes/education/boostPFC_currentControl.ipes").toAbsolutePath().toString();
            GeckoExternal.openFile(path);
            var scripts = MainWindow._scripter;
            var snapshot = new EditorCircuitCapture().capture(MainWindow._se._circuitSheet.getAllElements(),
                    MainWindow._solverSettings, MainWindow.optimizerParameterData, MainWindow._fileManager,
                    new EditorCircuitCapture.ScriptSources(scripts.getScriptCode(), scripts.getImportCode(),
                            scripts.getDeclarationCode(), scripts.getExtraFilesHashes()), path);
            var ready = assertInstanceOf(PreparationResult.Ready.class,
                    new CircuitPreparation().prepare(snapshot, PreparationRequest.defaults()));
            System.setProperty("gecko.backend", "legacy");
            GeckoExternal.runSimulation();
            var legacy = NetzlisteCONTROL.globalData;
            int row = -1;
            for (int index = 0; index < legacy.getRowLength(); index++) {
                if (legacy.getSignalName(index).equals("iL")) row = index;
            }
            assertTrue(row >= 0, "Legacy recording must contain the inductor current.");
            double legacyMinimum = Double.POSITIVE_INFINITY;
            for (int index = 0; index <= legacy.getMaximumTimeIndex(row); index++) {
                double time = legacy.getTimeValue(index, row);
                if (time >= 0.016 && time <= 0.02) legacyMinimum = Math.min(legacyMinimum, legacy.getValue(row, index));
            }
            try (var host = SessionHost.open(ready.execution(), ExecutionOptions.defaults())) {
                int coreRow = Arrays.asList(host.signalNames()).indexOf("iL");
                assertTrue(coreRow >= 0, "Core recording must contain the inductor current.");
                assertEquals(SimulationSession.State.COMPLETED, host.run().get(60, TimeUnit.SECONDS).state());
                var samples = new ArrayList<SimulationSession.Sample>();
                host.drainSamples(samples::add);
                double coreMinimum = samples.stream().filter(sample -> sample.time() >= 0.016 && sample.time() <= 0.02)
                        .mapToDouble(sample -> sample.values()[coreRow]).min().orElseThrow();
                var golden = GoldenReferenceHelper.readGoldenCsv(
                        Path.of("src/test/resources/golden/boostPFC_currentControl.golden.csv")).stream()
                        .filter(entry -> entry.scopeName.equals("SCOPE.1") && entry.port == 4).findFirst().orElseThrow();
                double expected = golden.characteristics[3];
                System.out.println("PFC_MINIMUM: golden=" + expected + ", legacy=" + legacyMinimum + ", core=" + coreMinimum);
                assertEquals(expected, legacyMinimum, Math.abs(expected) * 0.01, "Legacy must match the unchanged golden.");
                assertEquals(expected, coreMinimum, Math.abs(expected) * 0.01, "Core must match the unchanged golden.");
                assertEquals(legacyMinimum, coreMinimum, Math.abs(legacyMinimum) * 0.01, "Core must match legacy execution.");
            }
        } finally {
            if (backend == null) System.clearProperty("gecko.backend");
            else System.setProperty("gecko.backend", backend);
            SwingUtilities.invokeAndWait(() -> { for (var window : java.awt.Window.getWindows()) window.dispose(); });
        }
    }
}

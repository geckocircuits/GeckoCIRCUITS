package gecko.geckocircuits.allg;

import gecko.GeckoSim;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.importer.Main;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.geckocircuits.circuit.CircuitSheet;
import gecko.geckocircuits.circuit.ConnectorType;
import gecko.geckocircuits.circuit.Verbindung;
import gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.*;

@Tag("regression")
class ImportIssueDesktopTest {
    @Test
    @Timeout(90)
    void rejectsTheSamePartialImportInCoreAndDesktop(@TempDir Path directory) throws Exception {
        Path ipes = importPartialCircuit(directory, "first", 1);
        Path additional = importPartialCircuit(directory, "second", 2);
        var parser = new CircuitFileParser();
        var model = parser.parse(ipes.toString());
        var expected = model.getConversionIssues();
        assertEquals(1, expected.size());
        String code = expected.getFirst().code();
        var preparation = new CircuitPreparation();
        var core = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        assertFalse(core.report().readyForHeadless());
        assertTrue(core.report().diagnostics().stream().anyMatch(issue -> issue.code().equals(code)));

        var check = new FutureTask<Void>(() -> {
            try {
                if (GeckoSim._win == null || !GeckoSim._win.isDisplayable()) {
                    GeckoSim._isTestingMode = true;
                    GeckoSim.main(new String[]{ipes.toString()});
                } else {
                    GeckoSim._win.openFile(ipes.toString());
                }
                var window = GeckoSim._win;
                assertEquals(ipes.toString(), MainWindow.getOpenFileName());
                assertEquals(expected, MainWindow._se.getConversionIssues());
                var route = DesktopSimulationController.route(MainWindow._se, MainWindow._solverSettings);
                assertFalse(route.core());
                assertTrue(route.fallback().contains(code));
                assertDesktopRejects(window, code);

                Path saved = directory.resolve("partial-saved.ipes");
                window.rawSaveFile(saved.toFile());
                window.createNewFile();
                assertTrue(MainWindow._se.getConversionIssues().isEmpty());
                window.openFile(saved.toString());
                assertEquals(expected, parser.parse(saved.toString()).getConversionIssues());
                assertEquals(expected, MainWindow._se.getConversionIssues());
                assertDesktopRejects(window, code);

                MainWindow.importComponentsFromFile(additional.toString(), null);
                var combined = new ArrayList<>(expected);
                combined.addAll(parser.parse(additional.toString()).getConversionIssues());
                assertEquals(combined, MainWindow._se.getConversionIssues());
                window.createNewFile();
                assertTrue(MainWindow._se.getConversionIssues().isEmpty());
                assertAttachmentFailureRetainsFindings(window, ipes, false);
                assertAttachmentFailureRetainsFindings(window, additional, true);
            } finally {
                if (GeckoSim._win != null && GeckoSim._win.isDisplayable()) GeckoSim._win.createNewFile();
                for (var window : Window.getWindows()) window.dispose();
            }
            return null;
        });
        SwingUtilities.invokeAndWait(check);
        check.get();
    }

    private static Path importPartialCircuit(Path directory, String name, int suffix) throws Exception {
        Path source = directory.resolve(name + ".cir");
        Path ipes = directory.resolve(name + ".ipes");
        Files.writeString(source, """
                .tran 1u 1m
                V%1$d input%1$d 0 1
                R%1$d input%1$d 0 1k
                Q%1$d input%1$d input%1$d 0 arbitrary
                """.formatted(suffix));
        assertEquals(2, Main.run(new String[]{source.toString(), ipes.toString()}, System.out, System.err));
        return ipes;
    }

    private static void assertDesktopRejects(MainWindow window, String code) {
        var runner = window._simRunner;
        var settings = MainWindow._solverSettings;
        Executable[] starts = {
            () -> runner.startCalculation(true, settings),
            () -> runner.continueCalculation(true, settings),
            () -> runner.initSim(1e-6, 1e-3),
            () -> runner.external_init(1e-3)
        };
        for (var start : starts) {
            var refusal = assertThrows(IllegalArgumentException.class, start);
            assertTrue(refusal.getMessage().contains(code));
        }
    }

    private static void assertAttachmentFailureRetainsFindings(MainWindow window, Path file, boolean merge)
            throws Exception {
        var editor = MainWindow._se;
        var project = MainWindow.loadProjectDataFromFile(file.toString(), false, null);
        var first = project._allSheetComponents.getFirst();
        assertFalse(editor._circuitSheet.getAllElements().contains(first));
        var expected = new ArrayList<>(merge ? editor.getConversionIssues() : List.of());
        expected.addAll(project.getConversionIssues());
        project._allSheetComponents.add(new Verbindung(ConnectorType.LK, new CircuitSheet(editor)) {
            @Override
            public void findAndSetReferenceToParentSheet(List<SubcircuitBlock> subcircuits, String parent) {
                throw new IllegalStateException("Injected attachment failure");
            }
        });

        var failure = assertThrows(IllegalStateException.class, () -> {
            if (merge) editor.importProjectComponents(project, null);
            else editor.ladeGespeicherteNetzlisteVonProjectData(project, null);
        });
        assertEquals("Injected attachment failure", failure.getMessage());
        assertTrue(editor._circuitSheet.getAllElements().contains(first));
        assertEquals(expected, editor.getConversionIssues());
        assertDesktopRejects(window, project.getConversionIssues().getFirst().code());
    }
}

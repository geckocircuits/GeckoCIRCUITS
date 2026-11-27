package gecko.geckocircuits.allg;

import gecko.GeckoSim;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.importer.SpiceImporter;
import gecko.core.io.importer.Main;
import gecko.core.circuit.waveform.PiecewiseLinearWaveform;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.geckocircuits.circuit.CircuitSheet;
import gecko.geckocircuits.circuit.ConnectorType;
import gecko.geckocircuits.circuit.Verbindung;
import gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import gecko.modelviewcontrol.AbstractUndoGenericModel;
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
    private static final String ATTACHMENT_ISSUE = "PROJECT_ATTACHMENT_INCOMPLETE";
    private static final String AC_SOURCE = """
            .ac dec 4 10 100
            V1 input 0 AC 1
            R1 input 0 1k
            """;

    @Test
    @Timeout(90)
    void preservesExplicitUicInDesktopSaveAndRejectsInvalidMarkerBeforeReplacingTheDocument(
            @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER) Path directory) throws Exception {
        var source = directory.resolve("uic.ipes");
        var model = new SpiceImporter().convert(
                "V1 in 0 1\nR1 in out 1k\nC1 out 0 1u IC=0.25\n.tran 1u 2u uic", "uic.cir");
        new CircuitFileWriter().write(model, source);
        var invalid = directory.resolve("invalid-uic.ipes");
        String duplicate = "tDURATION 0.002\ntransientUic 1\ntransientUic 0\n";
        try (var stream = new java.util.zip.GZIPOutputStream(Files.newOutputStream(invalid))) {
            stream.write(duplicate.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        runOnDesktop(window -> {
            window.openFile(source.toString());
            assertTrue(MainWindow._solverSettings.transientUic.getValue());
            var saved = directory.resolve("uic-saved.ipes");
            window.rawSaveFile(saved.toFile());
            assertTrue(new CircuitFileParser().parse(saved.toString()).isTransientUic());
            window.openFile(saved.toString());
            var elements = List.copyOf(MainWindow._se._circuitSheet.getAllElements());
            var title = window.getTitle();
            assertThrows(IllegalArgumentException.class, () -> window.openFile(invalid.toString()));
            assertThrows(IllegalArgumentException.class, () -> window.openFile(
                    new java.io.BufferedReader(new java.io.StringReader(duplicate))));
            assertIterableEquals(elements, MainWindow._se._circuitSheet.getAllElements());
            assertEquals(title, window.getTitle());
            assertTrue(MainWindow._solverSettings.transientUic.getValue());
            window.createNewFile();
            assertFalse(MainWindow._solverSettings.transientUic.getValue());
            window.openFile(saved.toString());
            assertTrue(MainWindow._solverSettings.transientUic.getValue());
        });
    }

    @Test
    @Timeout(90)
    void transientRecordingStartSurvivesDesktopSaveAndInvalidOpenIsAtomic(
            @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER) Path directory) throws Exception {
        var original = directory.resolve("recording.ipes");
        var model = new SpiceImporter().convert(
                ".tran 1u 2m 1m\nV1 in 0 PULSE(0 1 0 1p 1p 10m 20m)\nR1 in out 1k\nC1 out 0 1u", "recording.cir");
        new CircuitFileWriter().write(model, original);
        var invalid = directory.resolve("bad-recording.ipes");
        String bad = "tDURATION 0.002\ntOUTPUT_START 0.003\n";
        try (var stream = new java.util.zip.GZIPOutputStream(Files.newOutputStream(invalid))) {
            stream.write(bad.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        runOnDesktop(window -> {
            window.openFile(original.toString());
            assertEquals(0.001, MainWindow._solverSettings.outputStartTime.getValue());
            var saved = directory.resolve("saved-recording.ipes");
            window.rawSaveFile(saved.toFile());
            assertEquals(0.001, new CircuitFileParser().parse(saved.toString()).getOutputStartTime());
            window.openFile(saved.toString());
            var elements = List.copyOf(MainWindow._se._circuitSheet.getAllElements());
            var title = window.getTitle();
            assertThrows(IllegalArgumentException.class, () -> window.openFile(invalid.toString()));
            assertThrows(IllegalArgumentException.class, () -> window.openFile(
                    new java.io.BufferedReader(new java.io.StringReader(bad))));
            assertIterableEquals(elements, MainWindow._se._circuitSheet.getAllElements());
            assertEquals(title, window.getTitle());
            assertEquals(0.001, MainWindow._solverSettings.outputStartTime.getValue());
            assertThrows(IllegalStateException.class, () -> {
                MainWindow._solverSettings._tDURATION.setValueWithoutUndo(0.0005);
                window.rawSaveFile(saved.toFile());
            });
            assertEquals(0.001, new CircuitFileParser().parse(saved.toString()).getOutputStartTime());
            MainWindow._solverSettings._tDURATION.setValueWithoutUndo(0.002);
        });
    }

    @Test
    @Timeout(90)
    void completePwlCircuitSurvivesDesktopSaveAndReopen(@TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER)
            Path directory) throws Exception {
        Path spice = directory.resolve("pwl.cir");
        Path imported = directory.resolve("pwl.ipes");
        Files.writeString(spice, """
                .tran 1u 11u
                V1 voltage 0 PWL(0 -2 4u 3 8u -1)
                I1 0 current PWL(0 2 4u -3 8u 1)
                R1 voltage 0 1k
                R2 current 0 1k
                """);
        assertEquals(0, Main.run(new String[]{spice.toString(), imported.toString()}, System.out, System.err));
        var parser = new CircuitFileParser();
        var original = parser.parse(imported.toString());
        assertEquals(2, original.getResources().size());
        Path plainSpice = directory.resolve("plain.cir");
        Path plainImport = directory.resolve("plain.ipes");
        Files.writeString(plainSpice, ".tran 1u 11u\nV2 other 0 3\nR3 other 0 1k\n");
        assertEquals(0, Main.run(new String[]{plainSpice.toString(), plainImport.toString()}, System.out, System.err));

        runOnDesktop(window -> {
            window.openFile(imported.toString());
            for (var file : MainWindow._fileManager._allAvailableFiles) {
                var expected = original.getResources().get(file.getHashValue());
                assertArrayEquals(expected.bytes(), file.getContentsByteCopy());
                assertTrue(file.isSoleNumericOwner(expected.ownerIds().getFirst()));
            }
            Path saved = directory.resolve("pwl-saved.ipes");
            window.rawSaveFile(saved.toFile());
            window.createNewFile();
            window.openFile(saved.toString());

            var reopened = parser.parse(saved.toString());
            for (var id : original.getResources().keySet()) {
                var resource = reopened.getResources().get(id);
                assertNotNull(resource);
                assertEquals(original.getResources().get(id).ownerIds(), resource.ownerIds());
                assertArrayEquals(original.getResources().get(id).bytes(), resource.bytes());
                assertTrue(PiecewiseLinearWaveform.hasHeader(resource.bytes()));
            }
            assertEquals(2, reopened.getCircuitComponents().stream().filter(component ->
                    "403".equals(component.getSavedBlock().tokens().get("sourceType"))).count());
            var route = DesktopSimulationController.route(MainWindow._se, MainWindow._solverSettings);
            assertTrue(route.core(), route.fallback());
            var editor = MainWindow._se;
            editor.selectAll();
            editor.setDirtyFlag();
            var elements = List.copyOf(editor._circuitSheet.getAllElements());
            var identifiers = elements.stream().map(element -> element.getUniqueObjectIdentifier()).toList();
            var files = MainWindow._fileManager;
            var resources = List.copyOf(files._allAvailableFiles);
            var undo = AbstractUndoGenericModel.undoManager.getUndoPresentationName();
            var redo = AbstractUndoGenericModel.undoManager.getRedoPresentationName();
            var title = window.getTitle();
            var mergeFailure = assertThrows(IllegalArgumentException.class,
                    () -> MainWindow.importComponentsFromFile(imported.toString(), null));
            assertTrue(mergeFailure.getMessage().contains("PWL"));
            assertSame(editor, MainWindow._se);
            assertSame(files, MainWindow._fileManager);
            assertIterableEquals(resources, files._allAvailableFiles);
            assertIterableEquals(elements, editor._circuitSheet.getAllElements());
            assertEquals(identifiers, elements.stream().map(element -> element.getUniqueObjectIdentifier()).toList());
            assertTrue(editor.getZustandGeaendert());
            assertEquals(title, window.getTitle());
            assertEquals(undo, AbstractUndoGenericModel.undoManager.getUndoPresentationName());
            assertEquals(redo, AbstractUndoGenericModel.undoManager.getRedoPresentationName());

            String previousBackend = System.getProperty("gecko.backend");
            try {
                System.setProperty("gecko.backend", "legacy");
                assertTrue(assertThrows(IllegalArgumentException.class, () -> window._simRunner
                        .startCalculation(true, MainWindow._solverSettings)).getMessage().contains("Core backend"));
                assertThrows(IllegalArgumentException.class, () -> window._simRunner.initSim(1e-6, 11e-6));
                assertThrows(IllegalArgumentException.class, () -> window._simRunner.external_init(11e-6));
            } finally {
                if (previousBackend == null) System.clearProperty("gecko.backend");
                else System.setProperty("gecko.backend", previousBackend);
            }
            MainWindow.importComponentsFromFile(plainImport.toString(), null);
            assertTrue(editor._circuitSheet.getAllElements().size() > elements.size());
        });
    }

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

    @Test
    @Timeout(90)
    void resetKeepsDocumentMetadataWhenTheFirstDeletionFails(@TempDir Path directory) throws Exception {
        Path input = directory.resolve("partial-ac.ipes");
        var model = new SpiceImporter().convert(AC_SOURCE + ".foo\n", "partial-ac.cir");
        new CircuitFileWriter().write(model, input);
        runOnDesktop(window -> {
            var editor = MainWindow._se;
            var failing = new Verbindung(ConnectorType.LK, editor._circuitSheet) {
                private boolean first = true;

                @Override
                public void deleteComponent() {
                    if (first) {
                        first = false;
                        throw new IllegalStateException("Injected deletion failure");
                    }
                    super.deleteComponent();
                }
            };
            var project = MainWindow.loadProjectDataFromFile(input.toString(), false, null);
            editor.ladeGespeicherteNetzlisteVonProjectData(project, null);
            assertSame(failing, editor._circuitSheet.getAllElements().getFirst());
            var failure = assertThrows(IllegalStateException.class, editor::resetCircuitSheetsForNewFile);
            assertEquals("Injected deletion failure", failure.getMessage());
            assertSame(failing, editor._circuitSheet.getAllElements().getFirst());
            assertEquals(project.getAcSettings(), editor.getAcSettings());
            assertEquals(project.getAcProvenance(), editor.getAcProvenance());
            assertFalse(project.getConversionIssues().isEmpty());
            assertEquals(project.getConversionIssues(), editor.getConversionIssues());
            assertDesktopRejects(window, project.getConversionIssues().getFirst().code());
            editor.resetCircuitSheetsForNewFile();
            assertNull(editor.getAcSettings());
            assertNull(editor.getAcProvenance());
            assertTrue(editor.getConversionIssues().isEmpty());
        });
    }

    @Test
    @Timeout(90)
    void failedCleanAttachmentBlocksDesktopAndSavedCoreUntilRecovery(@TempDir Path directory) throws Exception {
        Path input = directory.resolve("clean-ac.ipes");
        new CircuitFileWriter().write(new SpiceImporter().convert(AC_SOURCE, "clean-ac.cir"), input);
        runOnDesktop(window -> {
            var editor = MainWindow._se;
            var project = MainWindow.loadProjectDataFromFile(input.toString(), false, null);
            assertTrue(project.getConversionIssues().isEmpty());
            assertAttachmentFailureRetainsFindings(window, input, false);
            assertEquals(project.getAcSettings(), editor.getAcSettings());
            assertEquals(project.getAcProvenance(), editor.getAcProvenance());
            var route = DesktopSimulationController.route(editor, MainWindow._solverSettings);
            assertFalse(route.core());
            assertTrue(route.fallback().contains(ATTACHMENT_ISSUE));

            Path saved = directory.resolve("failed-open.ipes");
            window.rawSaveFile(saved.toFile());
            var savedModel = new CircuitFileParser().parse(saved.toString());
            assertEquals(editor.getConversionIssues(), savedModel.getConversionIssues());
            var preparation = new CircuitPreparation();
            var core = preparation.prepare(preparation.capture(savedModel), PreparationRequest.defaults());
            assertTrue(core.report().diagnostics().stream().anyMatch(issue -> issue.code().equals(ATTACHMENT_ISSUE)));
            assertFalse(core.report().readyForHeadless());

            window.openFile(saved.toString());
            assertEquals(savedModel.getConversionIssues(), editor.getConversionIssues());
            assertEquals(project.getAcSettings(), editor.getAcSettings());
            assertDesktopRejects(window, ATTACHMENT_ISSUE);

            window.openFile(input.toString());
            assertTrue(editor.getConversionIssues().isEmpty());
            assertAttachmentFailureRetainsFindings(window, input, true);
            assertEquals(project.getAcSettings(), editor.getAcSettings());
            assertEquals(project.getAcProvenance(), editor.getAcProvenance());
            assertTrue(DesktopSimulationController.route(editor, MainWindow._solverSettings)
                    .fallback().contains(ATTACHMENT_ISSUE));
            window.createNewFile();
            assertTrue(editor.getConversionIssues().isEmpty());
            assertNull(editor.getAcSettings());
        });
    }

    private static void runOnDesktop(DesktopCheck action) throws Exception {
        var task = new FutureTask<Void>(() -> {
            try {
                if (GeckoSim._win == null || !GeckoSim._win.isDisplayable()) {
                    GeckoSim._isTestingMode = true;
                    GeckoSim.main(new String[0]);
                }
                GeckoSim._win.createNewFile();
                action.run(GeckoSim._win);
            } finally {
                if (GeckoSim._win != null && GeckoSim._win.isDisplayable()) GeckoSim._win.createNewFile();
                for (var window : Window.getWindows()) window.dispose();
            }
            return null;
        });
        SwingUtilities.invokeAndWait(task);
        task.get();
    }

    @FunctionalInterface
    private interface DesktopCheck {
        void run(MainWindow window) throws Exception;
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
        assertEquals(expected.stream().filter(issue -> !issue.code().equals(ATTACHMENT_ISSUE)).toList(),
                editor.getConversionIssues().stream()
                        .filter(issue -> !issue.code().equals(ATTACHMENT_ISSUE)).toList());
        assertEquals(1, editor.getConversionIssues().stream()
                .filter(issue -> issue.code().equals(ATTACHMENT_ISSUE)).count());
        assertDesktopRejects(window, ATTACHMENT_ISSUE);
    }
}

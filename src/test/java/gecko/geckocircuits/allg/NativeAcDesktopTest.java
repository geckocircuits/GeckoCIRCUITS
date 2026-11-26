package gecko.geckocircuits.allg;

import gecko.GeckoSim;
import gecko.core.ac.AcComponent;
import gecko.core.ac.AcInputOrigin;
import gecko.core.io.importer.LtspiceImportReceipt;
import gecko.core.ac.AcResult;
import gecko.core.ac.Complex;
import gecko.core.ac.ParameterDefinition;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;
import gecko.core.io.HierarchyIndex;
import gecko.core.io.importer.LtspiceImporter;
import gecko.core.io.importer.SpiceImporter;
import gecko.core.io.importer.Main;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.AbstractCircuitSheetComponent;
import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.circuit.ConnectorType;
import gecko.geckocircuits.circuit.CircuitSheet;
import gecko.geckocircuits.circuit.EditorCircuitCapture;
import gecko.geckocircuits.circuit.SpecialTyp;
import gecko.geckocircuits.circuit.Verbindung;
import gecko.geckocircuits.circuit.circuitcomponents.AcControlledVoltageSource;
import gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import gecko.geckocircuits.control.ControlTyp;
import gecko.geckocircuits.control.ReglerCounter;
import gecko.geckocircuits.control.Point;
import java.awt.Component;
import java.awt.Container;
import java.awt.Color;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("regression")
class NativeAcDesktopTest {
    private static final String SYNTHETIC = """
            .ac dec 4 10 100
            V1 input 0 AC 1 27
            E1 output 0 0 input 4
            R1 output 0 1k
            """;
    private static final int WITNESS = 210;

    @Test
    void rollsBackSharedEditAndGroupsToggleAndCopyCommands() throws Exception {
        Path evidence = evidence();
        Path input = createHierarchyInput(evidence);
        try {
            onEdt(() -> {
                open(input);
                var first = showOccurrence("X1", "Xleaf");
                var firstShunt = hierarchyComponent(first, "Rsh");
                var second = showOccurrence("X2", "Xleaf");
                var secondShunt = hierarchyComponent(second, "Rsh");
                var original = firstShunt.getAcComponent();
                var changed = new AcComponent.Passive("11k");
                var fault = new java.util.concurrent.atomic.AtomicBoolean();
                java.awt.event.ActionListener inject = event -> {
                    if (secondShunt.getAcComponent().law().equals(changed) && fault.compareAndSet(false, true)) {
                        throw new IllegalArgumentException("Fail the second peer edit.");
                    }
                };
                var undo = gecko.modelviewcontrol.AbstractUndoGenericModel.undoManager;
                undo.discardAllEdits();
                var listeners = gecko.modelviewcontrol.AbstractUndoGenericModel.globalEventListeners;
                listeners.add(inject);
                try {
                    assertThrows(IllegalArgumentException.class, () -> firstShunt.setAcLaw(changed));
                } finally {
                    listeners.remove(inject);
                }
                assertTrue(fault.get());
                assertEquals(original, firstShunt.getAcComponent());
                assertEquals(original, secondShunt.getAcComponent());
                assertFalse(undo.canUndo());
                assertFalse(MainWindow._se.getZustandGeaendert());
                assertHierarchyOutputs(solve(), 1000, 4000, 2000);

                showOccurrence("X1", "Xleaf");
                selectHierarchyMembers(firstShunt);
                MainWindow._se.toggleEnable(true);
                assertEquals(gecko.geckocircuits.circuit.Enabled.DISABLED_SHORT, firstShunt.isCircuitEnabled());
                assertEquals(firstShunt.isCircuitEnabled(), secondShunt.isCircuitEnabled());
                assertEquals(gecko.geckocircuits.circuit.Enabled.ENABLED, hierarchyComponent(first, "Rseries").isCircuitEnabled());
                undo.undo();
                assertFalse(undo.canUndo());
                assertEquals(gecko.geckocircuits.circuit.Enabled.ENABLED, firstShunt.isCircuitEnabled());
                assertEquals(firstShunt.isCircuitEnabled(), secondShunt.isCircuitEnabled());

                var wire = first.getLocalSheetComponents().stream().filter(Verbindung.class::isInstance)
                        .map(Verbindung.class::cast).filter(item -> item.getLabel().equals("N001")).findFirst().orElseThrow();
                selectHierarchyMembers(wire);
                MainWindow._se.toggleEnable(false);
                assertEquals(gecko.geckocircuits.circuit.Enabled.DISABLED, wire.isCircuitEnabled());
                assertCopies(first, second, wire);
                undo.undo();
                assertEquals(gecko.geckocircuits.circuit.Enabled.ENABLED, wire.isCircuitEnabled());
                assertCopies(first, second, wire, firstShunt);
                assertCopies(first, second, firstShunt, hierarchyComponent(first, "Csh"));
                assertFalse(undo.canUndo());
                assertHierarchyOutputs(solve(), 1000, 4000, 2000);
                var root = showOccurrence();
                int originalCount = root.getLocalSheetComponents().size();
                selectHierarchyMembers(hierarchyComponent(root, "X1"));
                withDialog("ASC edit", dialog -> { }, null, MainWindow._se::kopiereAllesImBearbeitungsModus);
                assertEquals(originalCount, root.getLocalSheetComponents().size());
                assertFalse(undo.canUndo());
                return null;
            });
        } finally {
            onEdt(() -> { closeDesktop(); return null; });
        }
    }

    @Test
    void preservesTheOpenDocumentWhenSaveAndComponentImportAreRejected() throws Exception {
        Path evidence = evidence();
        Path input = createHierarchyInput(evidence);
        try {
            onEdt(() -> {
                var window = open(input);
                MainWindow._se.setDirtyFlag();
                String originalPath = MainWindow.getOpenFileName();
                var embedded = new gecko.core.allg.GeckoFile(evidence.resolve("resource.dat").toFile(),
                        originalPath, new byte[]{12, 34});
                MainWindow._fileManager.addFile(embedded);
                var originalResource = new StringBuffer();
                embedded.exportASCII(originalResource);
                var directory = Files.createDirectory(evidence.resolve("directory.ipes"));
                assertThrows(java.io.IOException.class, () -> window.saveDocumentTo(directory.toFile(), true));
                assertEquals(originalPath, MainWindow.getOpenFileName());
                assertTrue(MainWindow._se.getZustandGeaendert());
                var afterSaveFailure = new StringBuffer();
                embedded.exportASCII(afterSaveFailure);
                assertEquals(originalResource.toString(), afterSaveFailure.toString());
                var originalIssues = MainWindow._se.getConversionIssues();
                assertThrows(IllegalArgumentException.class, () -> MainWindow.importComponentsFromFile(input.toString(), null));
                assertEquals(originalIssues, MainWindow._se.getConversionIssues());
                assertEquals(originalPath, MainWindow.getOpenFileName());
                assertTrue(MainWindow._se.getZustandGeaendert());
                assertHierarchyOutputs(solve(), 1000, 4000, 2000);

                var shunt = hierarchyComponent(showOccurrence("X1", "Xleaf"), "Rsh");
                var position = shunt.getSheetPosition();
                shunt.setPositionWithoutUndo(position.x + 1, position.y);
                var invalid = evidence.resolve("invalid.ipes");
                assertThrows(IllegalArgumentException.class, () -> window.saveDocumentTo(invalid.toFile(), true));
                assertFalse(Files.exists(invalid));
                assertEquals(originalPath, MainWindow.getOpenFileName());
                assertTrue(MainWindow._se.getZustandGeaendert());
                shunt.setPositionWithoutUndo(position.x, position.y);
                assertHierarchyOutputs(solve(), 1000, 4000, 2000);
                verifyObsoleteBackupIsSkipped(evidence, input, window);
                var flatSource = Files.writeString(evidence.resolve("flat.cir"), SYNTHETIC);
                var flat = evidence.resolve("flat.ipes");
                assertEquals(0, Main.run(new String[]{flatSource.toString(), flat.toString()}, System.out, System.err));
                window.createNewFile();
                open(flat);
                var flatResult = solve();
                String savedPath = GlobalFilePathes.datnamAbsLoadIPES;
                assertThrows(IllegalArgumentException.class, () -> MainWindow.importComponentsFromFile(input.toString(), null));
                assertEquals(savedPath, GlobalFilePathes.datnamAbsLoadIPES);
                assertEquals(flatResult, solve());
                return null;
            });
        } finally {
            onEdt(() -> { closeDesktop(); return null; });
        }
    }

    @Test
    void preservesSavedModelAndRecoveryAfterPartialWrites() throws Exception {
        Path evidence = evidence();
        Path input = createHierarchyInput(evidence);
        try {
            onEdt(() -> {
                var window = open(input);
                var recovery = evidence.resolve("recovery.ipes");
                window.rawSaveFile(recovery.toFile());
                byte[] original = Files.readAllBytes(input);
                byte[] backup = Files.readAllBytes(recovery);
                MainWindow._se.setDirtyFlag();
                var stagedFiles = new ArrayList<Path>();
                MainWindow.OpenProjectOutput failsAfterHeader = staged -> {
                    stagedFiles.add(staged);
                    var destination = Files.newOutputStream(staged);
                    return new java.io.FilterOutputStream(destination) {
                        private int remaining = 20;

                        @Override
                        public void write(int value) throws java.io.IOException {
                            if (remaining == 0) throw new java.io.IOException("Injected partial output failure.");
                            out.write(value);
                            remaining--;
                        }
                    };
                };
                var error = assertThrows(java.io.IOException.class,
                        () -> window.saveDocumentTo(input.toFile(), false, failsAfterHeader));
                assertTrue(error.getMessage().contains("Injected partial output failure."));
                assertArrayEquals(original, Files.readAllBytes(input));
                assertEquals(input.toAbsolutePath().normalize(),
                        Path.of(MainWindow.getOpenFileName()).toAbsolutePath().normalize());
                assertTrue(MainWindow._se.getZustandGeaendert());
                error = assertThrows(java.io.IOException.class,
                        () -> window.backUpDocument(recovery.toFile(), failsAfterHeader));
                assertTrue(error.getMessage().contains("Injected partial output failure."));
                assertArrayEquals(backup, Files.readAllBytes(recovery));
                assertEquals(2, stagedFiles.size());
                for (Path staged : stagedFiles) {
                    assertEquals(evidence.toAbsolutePath().normalize(),
                            staged.getParent().toAbsolutePath().normalize());
                    assertEquals(20, Files.size(staged), "The fault must leave actual compressed bytes.");
                }
                assertHierarchyOutputs(solve(), 1000, 4000, 2000);
                error = assertThrows(java.io.IOException.class,
                        () -> window.saveDocumentTo(input.toAbsolutePath().getRoot().toFile(), false));
                assertTrue(error.getMessage().contains("requires a file name"));
                assertArrayEquals(original, Files.readAllBytes(input));
                assertTrue(MainWindow._se.getZustandGeaendert());
                window.saveDocumentTo(input.toFile(), false);
                assertFalse(MainWindow._se.getZustandGeaendert());
                open(input);
                assertHierarchyOutputs(solve(), 1000, 4000, 2000);
                return null;
            });
        } finally {
            onEdt(() -> { closeDesktop(); return null; });
        }
    }

    @Test
    void keepsTheCurrentDocumentAfterFailedOrCancelledSaveBeforeLeaving() throws Exception {
        Path evidence = evidence();
        Path input = createHierarchyInput(evidence);
        try {
            onEdt(() -> {
                var window = open(input);
                var previousMode = GeckoSim.operatingmode;
                GeckoSim.operatingmode = OperatingMode.STANDALONE;
                try {
                    Path recent = evidence.resolve("recent.ipes");
                    Path unwritable = Files.createDirectory(evidence.resolve("directory.ipes"));
                    window.rawSaveFile(recent.toFile());
                    Path recovery = evidence.resolve("recovery.ipes");
                    window.rawSaveFile(recovery.toFile());
                    byte[] backup = Files.readAllBytes(recovery);
                    byte[] saved = Files.readAllBytes(input);
                    var commands = List.of("New", "Open", "RECENT_1", "RECENT_2", "RECENT_3", "RECENT_4",
                            "Exit", "Close window");
                    GlobalFilePathes.RECENT_CIRCUITS_1 = recent.toString();
                    GlobalFilePathes.RECENT_CIRCUITS_2 = recent.toString();
                    GlobalFilePathes.RECENT_CIRCUITS_3 = recent.toString();
                    GlobalFilePathes.RECENT_CIRCUITS_4 = recent.toString();
                    GlobalFilePathes.DATNAM = unwritable.toString();
                    var sheet = MainWindow._se._circuitSheet;
                    MainWindow._se.setDirtyFlag();
                    for (String command : commands) {
                        tryLeaving(window, command, false);
                        assertTrue(window.isDisplayable());
                        assertSame(sheet, MainWindow._se._circuitSheet);
                        assertEquals(input.toAbsolutePath().normalize(),
                                Path.of(MainWindow.getOpenFileName()).toAbsolutePath().normalize());
                        assertEquals(unwritable.toString(), GlobalFilePathes.DATNAM);
                        assertTrue(MainWindow._se.getZustandGeaendert());
                    }
                    assertArrayEquals(saved, Files.readAllBytes(input));
                    assertHierarchyOutputs(solve(), 1000, 4000, 2000);

                    window.createNewFile();
                    String untitled = MainWindow.getOpenFileName();
                    String newTarget = GlobalFilePathes.DATNAM;
                    sheet = MainWindow._se._circuitSheet;
                    MainWindow._se.setDirtyFlag();
                    for (String command : commands) {
                        tryLeaving(window, command, true);
                        assertTrue(window.isDisplayable());
                        assertSame(sheet, MainWindow._se._circuitSheet);
                        assertEquals(untitled, MainWindow.getOpenFileName());
                        assertEquals(newTarget, GlobalFilePathes.DATNAM);
                        assertTrue(MainWindow._se.getZustandGeaendert());
                    }
                    assertArrayEquals(backup, Files.readAllBytes(recovery));
                    return null;
                } finally {
                    GeckoSim.operatingmode = previousMode;
                }
            });
        } finally {
            onEdt(() -> { closeDesktop(); return null; });
        }
    }

    private static void tryLeaving(MainWindow window, String command, boolean cancelSaveAs) {
        var seen = new ArrayList<String>();
        var responder = new Timer(40, event -> {
            for (Window child : Window.getWindows()) {
                if (!(child instanceof JDialog dialog) || !dialog.isShowing() || !dialog.isModal()) continue;
                var panes = descendants(dialog, JOptionPane.class);
                var choosers = descendants(dialog, javax.swing.JFileChooser.class);
                if (seen.isEmpty() && !panes.isEmpty()
                        && panes.getFirst().getOptionType() == JOptionPane.YES_NO_CANCEL_OPTION) {
                    seen.add("save prompt");
                    panes.getFirst().setValue(JOptionPane.YES_OPTION);
                } else if (seen.size() == 1 && !panes.isEmpty() && dialog.getTitle().equals("Save circuit")) {
                    seen.add("save error");
                    panes.getFirst().setValue(JOptionPane.OK_OPTION);
                } else if (seen.size() == 1 && !choosers.isEmpty()
                        && choosers.getFirst().getDialogType() == javax.swing.JFileChooser.SAVE_DIALOG) {
                    seen.add("save chooser");
                    choosers.getFirst().cancelSelection();
                } else {
                    seen.add("unexpected " + dialog.getTitle());
                    dialog.dispose();
                }
                return;
            }
        });
        responder.start();
        try {
            if (command.equals("Close window")) {
                window.dispatchEvent(new java.awt.event.WindowEvent(window, java.awt.event.WindowEvent.WINDOW_CLOSING));
            } else {
                window.actionPerformed(new java.awt.event.ActionEvent(window, 0, command));
            }
        } finally {
            responder.stop();
        }
        assertEquals(List.of("save prompt", cancelSaveAs ? "save chooser" : "save error"), seen, command);
    }

    private static void verifyObsoleteBackupIsSkipped(Path evidence, Path input, MainWindow window) throws Exception {
        var backup = evidence.resolve("recovery.ipes");
        window.rawSaveFile(backup.toFile());
        byte[] recoverable = Files.readAllBytes(backup);
        var loop = java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        var queued = new FutureTask<Void>(() -> {
            try {
                window.backUpDocument(backup.toFile());
                return null;
            } finally {
                SwingUtilities.invokeLater(loop::exit);
            }
        });
        var thread = Thread.ofPlatform().daemon().start(queued);
        long expiry = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (thread.getState() != Thread.State.WAITING && !queued.isDone() && System.nanoTime() < expiry) {
            Thread.sleep(5);
        }
        assertEquals(Thread.State.WAITING, thread.getState(), "The backup must be queued behind the editor transition.");
        window.createNewFile();
        open(input);
        MainWindow._se.setDirtyFlag();
        var timer = new Timer(10000, event -> loop.exit());
        timer.setRepeats(false);
        timer.start();
        try {
            if (!queued.isDone()) loop.enter();
        } finally {
            timer.stop();
        }
        assertTrue(queued.isDone(), "The obsolete backup request did not finish.");
        queued.get();
        assertArrayEquals(recoverable, Files.readAllBytes(backup));
    }

    private static void assertCopies(CircuitSheet first, CircuitSheet second,
                                     AbstractCircuitSheetComponent... originals) throws Exception {
        int before = first.getLocalSheetComponents().size();
        var previous = new HashSet<>(first.getLocalSheetComponents());
        selectHierarchyMembers(originals);
        MainWindow._se.kopiereAllesImBearbeitungsModus();
        for (var preview : first.getLocalSheetComponents()) {
            if (!previous.contains(preview)) preview.moveComponent(new Point(0, 25));
        }
        var finish = gecko.geckocircuits.circuit.SchematicEditor2.class.getDeclaredMethod("mausAbsetzenMarkierteGruppe");
        finish.setAccessible(true);
        var failure = new AtomicReference<String>();
        var dialogCheck = new Timer(150, event -> {
            for (var window : Window.getWindows()) {
                if (!(window instanceof JDialog dialog) || !dialog.isShowing() || !dialog.isModal()) continue;
                var panes = descendants(dialog, JOptionPane.class);
                failure.set(panes.isEmpty() ? dialog.getTitle() : String.valueOf(panes.getFirst().getMessage()));
                dialog.dispose();
            }
        });
        dialogCheck.start();
        try {
            finish.invoke(MainWindow._se);
        } finally {
            dialogCheck.stop();
        }
        assertNull(failure.get(), failure.get());
        assertEquals(before + originals.length, first.getLocalSheetComponents().size());
        assertEquals(first.getLocalSheetComponents().size(), second.getLocalSheetComponents().size());
        for (var created : first.getLocalSheetComponents()) {
            if (previous.contains(created)) continue;
            var key = created.getHierarchyMember();
            assertNotNull(key);
            assertTrue(previous.stream().noneMatch(old -> key.equals(old.getHierarchyMember())));
            var peer = second.getLocalSheetComponents().stream().filter(item -> key.equals(item.getHierarchyMember()))
                    .findFirst().orElseThrow();
            assertNotEquals(created.getUniqueObjectIdentifier(), peer.getUniqueObjectIdentifier());
            assertEquals(created.isCircuitEnabled(), peer.isCircuitEnabled());
            if (created instanceof Verbindung copied) {
                assertFalse(copied.getLabel().isEmpty());
                assertEquals(copied.getLabel(), assertInstanceOf(Verbindung.class, peer).getLabel());
                assertEquals(originals[0].isCircuitEnabled(), copied.isCircuitEnabled());
            }
        }
        MainWindow._se.inspectHierarchy();
        gecko.modelviewcontrol.AbstractUndoGenericModel.undoManager.undo();
        assertEquals(before, first.getLocalSheetComponents().size());
        assertEquals(before, second.getLocalSheetComponents().size());
        MainWindow._se.inspectHierarchy();
    }

    @SuppressWarnings("unchecked")
    private static void selectHierarchyMembers(AbstractCircuitSheetComponent... members) throws Exception {
        var field = gecko.geckocircuits.circuit.SchematicEditor2.class.getDeclaredField("_selectedComponents");
        field.setAccessible(true);
        var selection = (Set<AbstractCircuitSheetComponent>) field.get(MainWindow._se);
        selection.clear();
        selection.addAll(List.of(members));
    }

    @Test
    void editsSharedNativeHierarchyAndCapturesCoherentBackgroundSaves() throws Exception {
        Path evidence = evidence();
        Path input = createHierarchyInput(evidence);
        try {
            Path repaired = onEdt(() -> editAndSaveHierarchy(evidence, input));
            var writer = onEdt(() -> startBackgroundSaves(evidence, repaired));
            writer.get(15, TimeUnit.SECONDS);
            verifyBackgroundSaves(evidence);
        } finally {
            onEdt(() -> { closeDesktop(); return null; });
        }
    }

    private static Path createHierarchyInput(Path evidence) {
        Path source = Path.of("src", "modules", "gecko-simulation-core", "src", "test", "resources", "ltspice-hierarchy")
                .toAbsolutePath().normalize();
        Path input = evidence.resolve("nested.ipes");
        assertEquals(0, Main.run(new String[]{source.resolve("root.asc").toString(), input.toString(),
                "--symbols", source.toString(), "--source-root", source.toString()}, System.out, System.err));
        return input;
    }

    @Test
    void savesAndEditsSharedTransientHierarchyParameters() throws Exception {
        Path evidence = evidence();
        Path source = Path.of("src", "modules", "gecko-simulation-core", "src", "test", "resources", "ltspice-hierarchy")
                .toAbsolutePath().normalize();
        for (String name : List.of("leaf.asc", "leaf.asy", "wrapper.asc", "wrapper.asy", "res.asy", "cap.asy", "voltage.asy")) {
            Files.copy(source.resolve(name), evidence.resolve(name));
        }
        Path root = Files.writeString(evidence.resolve("root.asc"), Files.readString(source.resolve("root.asc"))
                .replace("Value AC 1", "Value 1").replace("first=1k", "base=2k").replace("{first}", "{base}")
                .replace(".ac dec 2 10 100k", ".tran 0 20m 0 100u"));
        Path input = evidence.resolve("transient.ipes");
        var imported = new LtspiceImporter().convert(root, evidence, List.of(evidence));
        assertTrue(imported.getConversionIssues().isEmpty(), imported.getConversionIssues().toString());
        new CircuitFileWriter().write(imported, input);

        onEdt(() -> {
            try {
                open(input);
                assertTransientDesktopValues(2000);
                assertTransientCapture(captureTransientEditor(), 2000, "{base}");

                showOccurrence("X1");
                editHierarchyParameters("caller=3k", "caller={base+1k}");
                assertTransientDesktopValues(3000);
                assertTransientCapture(captureTransientEditor(), 3000, "{base+1k}");

                Path saved = evidence.resolve("transient-edited.ipes");
                saveAndReopenHierarchy(saved);
                assertTransientDesktopValues(3000);
                assertTransientCapture(captureTransientEditor(), 3000, "{base+1k}");
                var before = Files.readAllBytes(saved);

                var firstCall = assertInstanceOf(SubcircuitBlock.class,
                        hierarchyComponent(MainWindow._se._circuitSheet, "X1"));
                var editsField = MainWindow._se.getClass().getDeclaredField("hierarchyEdits");
                editsField.setAccessible(true);
                var edits = editsField.get(MainWindow._se);
                var setParameters = edits.getClass().getDeclaredMethod("setParameters",
                        SubcircuitBlock.class, List.class, List.class);
                setParameters.setAccessible(true);
                var rejected = assertThrows(java.lang.reflect.InvocationTargetException.class, () ->
                        setParameters.invoke(edits, firstCall, List.of(new ParameterDefinition("caller", "3k")),
                                List.of(new ParameterDefinition("caller", "{-base}"))));
                assertInstanceOf(IllegalArgumentException.class, rejected.getCause());
                assertTransientDesktopValues(3000);
                assertTransientCapture(captureTransientEditor(), 3000, "{base+1k}");
                assertArrayEquals(before, Files.readAllBytes(saved));
            } finally {
                closeDesktop();
            }
            return null;
        });
    }

    private static void assertTransientDesktopValues(double first) {
        assertNull(MainWindow._se.getAcSettings());
        assertEquals(first, hierarchyComponent(showOccurrence("X1", "Xleaf"), "Rseries").getParameter()[0]);
        assertEquals(4000, hierarchyComponent(showOccurrence("X2", "Xleaf"), "Rseries").getParameter()[0]);
    }

    private static CircuitModel captureTransientEditor() throws Exception {
        var editor = MainWindow._se;
        return new EditorCircuitCapture().capture(editor._circuitSheet.getAllElements(),
                MainWindow._solverSettings, MainWindow.optimizerParameterData, MainWindow._fileManager,
                EditorCircuitCapture.ScriptSources.EMPTY, MainWindow.getOpenFileName(), editor.getAcSettings(),
                editor.getAcProvenance(), editor.getConversionIssues(), editor.getHierarchy(),
                editor.getRootParameterDefinitions()).toModel();
    }

    private static void assertTransientCapture(CircuitModel model, double first, String firstOverride) {
        assertNull(model.getAcSettings());
        assertEquals(List.of(new ParameterDefinition("base", "2k")), model.getRootParameterDefinitions());
        assertEquals(2, model.getHierarchy().definitions().size());
        var index = new HierarchyIndex(model);
        var resistors = model.getCircuitComponents().stream().filter(c -> c.getName().equals("Rseries")).toList();
        assertEquals(2, resistors.size());
        assertEquals(1, resistors.stream().map(c -> index.sheet(HierarchyIndex.parent(c.getSavedBlock()))
                .definition().id()).distinct().count());
        for (var resistor : resistors) {
            var leaf = index.sheet(HierarchyIndex.parent(resistor.getSavedBlock()));
            var wrapper = index.sheet(leaf.parent());
            boolean firstCall = wrapper.call().instance().equals("X1");
            assertNull(resistor.getAcComponent());
            assertEquals(Map.of(0, "{series}"), resistor.getScalarBindings());
            assertEquals(List.of(new ParameterDefinition("series", "{caller}")), leaf.call().overrides());
            assertEquals(List.of(new ParameterDefinition("caller", firstCall ? firstOverride : "4k")),
                    wrapper.call().overrides());
            assertEquals(firstCall ? first : 4000, resistor.getNumericParameters()[0]);
        }
    }

    private static Path editAndSaveHierarchy(Path evidence, Path input) throws Exception {
                open(input);
                assertHierarchyOutputs(solve(), 1000, 4000, 2000);
                showOccurrence("X1", "Xleaf");
                editHierarchyParameters("series=3k\nshunt=3k", "series={caller}");
                assertHierarchyOutputs(solve(), 1000, 4000, 3000);

                showOccurrence("X1");
                editHierarchyParameters("caller=3k", "caller=2k");
                assertHierarchyOutputs(solve(), 2000, 4000, 3000);
                saveAndReopenHierarchy(evidence.resolve("changed.ipes"));
                assertHierarchyOutputs(solve(), 2000, 4000, 3000);

                var secondLeaf = showOccurrence("X2", "Xleaf");
                var shunt = hierarchyComponent(secondLeaf, "Rsh");
                withDialog("AC component Rsh", dialog -> descendants(dialog, JTextField.class).getFirst().setText("5k"),
                        null, () -> shunt.doDoubleClickAction(shunt.getSheetPosition()));
                assertHierarchyOutputs(solve(), 2000, 4000, 5000);

                var firstLeaf = showOccurrence("X1", "Xleaf");
                var pin = hierarchyComponent(firstLeaf, "Rseries").YOUT.getFirst().getPosition();
                var wire = firstLeaf.getLocalSheetComponents().stream().filter(Verbindung.class::isInstance)
                        .map(Verbindung.class::cast).filter(item -> item.getAllPointCoordinates().contains(pin) && item.getLabel().isEmpty())
                        .min(java.util.Comparator.comparingDouble(item -> item.getStartPoint().distance(item.getEndPoint()))).orElseThrow();
                var member = wire.getHierarchyMember();
                moveHierarchyWire(wire, -3);
                assertHierarchySplit();
                saveAndReopenHierarchy(evidence.resolve("broken.ipes"));
                assertHierarchySplit();
                var peerWire = showOccurrence("X2", "Xleaf").getLocalSheetComponents().stream()
                        .filter(Verbindung.class::isInstance).map(Verbindung.class::cast)
                        .filter(item -> member.equals(item.getHierarchyMember())).findFirst().orElseThrow();
                moveHierarchyWire(peerWire, 3);
                assertHierarchyOutputs(solve(), 2000, 4000, 5000);
                Path repaired = evidence.resolve("repaired.ipes");
                saveAndReopenHierarchy(repaired);
                assertHierarchyOutputs(solve(), 2000, 4000, 5000);
                return repaired;
    }

    private static CircuitSheet showOccurrence(String... path) {
        var editor = MainWindow._se;
        CircuitSheet sheet = editor._circuitSheet;
        editor.setNewVisibleCircuitSheet(sheet);
        for (String name : path) {
            var owner = assertInstanceOf(SubcircuitBlock.class, hierarchyComponent(sheet, name));
            owner.doDoubleClickAction(owner.getSheetPosition());
            sheet = owner._myCircuitSheet;
        }
        assertSame(sheet, editor._visibleCircuitSheet);
        return sheet;
    }

    private static AbstractBlockInterface hierarchyComponent(CircuitSheet sheet, String name) {
        return sheet.getLocalSheetComponents().stream().filter(AbstractBlockInterface.class::isInstance)
                .map(AbstractBlockInterface.class::cast).filter(block -> block.getStringID().equals(name)).findFirst().orElseThrow();
    }

    private static void editHierarchyParameters(String defaults, String overrides) throws Exception {
        var menu = GeckoSim._win.getJMenuBar().getMenu(2).getPopupMenu();
        var command = descendants(menu, JMenuItem.class).stream()
                .filter(item -> item.getText().startsWith("ASC hierarchy parameters")).findFirst().orElseThrow();
        withDialog("ASC parameters", dialog -> {
            var fields = descendants(dialog, JTextArea.class);
            assertEquals(2, fields.size());
            fields.getFirst().setText(defaults);
            fields.getLast().setText(overrides);
        }, null, command::doClick);
    }

    private static void moveHierarchyWire(Verbindung wire, int y) {
        wire.moveComponent(new Point(0, y));
        wire.setPositionWithUndo();
    }

    private static void saveAndReopenHierarchy(Path file) throws Exception {
        GeckoSim._win.rawSaveFile(file.toFile());
        assertTrue(Files.isRegularFile(file), file.toString());
        GeckoSim._win.createNewFile();
        open(file);
    }

    private static void assertHierarchyOutputs(AcResult result, double seriesA, double seriesB, double shunt) {
        assertEquals(12, result.hierarchy().ports().size());
        for (int i = 0; i < result.frequencyHz().size(); i++) {
            double omega = 2 * Math.PI * result.frequencyHz().get(i);
            for (int caller = 1; caller <= 2; caller++) {
                double series = caller == 1 ? seriesA : seriesB;
                var expected = new Complex(shunt, 0).divide(new Complex(shunt + series, omega * shunt * series * 1e-8));
                close(expected, voltage(result, "X" + caller + ":Xleaf:Csh", 1, i));
            }
        }
    }

    private static void assertHierarchySplit() throws Exception {
        var editor = MainWindow._se;
        var prepared = DesktopAcAnalysis.prepare(editor, MainWindow._solverSettings);
        assertFalse(prepared.report().readyForHeadless());
        assertTrue(prepared.report().diagnostics().stream().anyMatch(issue ->
                issue.message().contains("'Rseries' has an unconnected output terminal")));
        var model = new gecko.geckocircuits.circuit.EditorCircuitCapture().capture(editor._circuitSheet.getAllElements(),
                MainWindow._solverSettings, MainWindow.optimizerParameterData, MainWindow._fileManager,
                gecko.geckocircuits.circuit.EditorCircuitCapture.ScriptSources.EMPTY, MainWindow.getOpenFileName(),
                editor.getAcSettings(), editor.getAcProvenance(), editor.getConversionIssues(), editor.getHierarchy()).toModel();
        var graph = CircuitTopology.resolve(model);
        var hierarchy = new gecko.core.io.HierarchyIndex(model);
        for (String caller : List.of("X1", "X2")) {
            var path = List.of(caller, "Xleaf");
            var resistor = model.getCircuitComponents().stream().filter(component -> component.getName().equals("Rseries")
                    && hierarchy.path(component.getSavedBlock().parentSheetIdentifier()).equals(path)).findFirst().orElseThrow();
            var output = hierarchy.boundaries().stream().filter(port -> port.formal().name().equals("OUT")
                    && hierarchy.path(port.sheet().id()).equals(path)).findFirst().orElseThrow();
            assertNotEquals(graph.group(resistor, CircuitTopology.Side.OUTPUT, 0).id(),
                    graph.group(output.terminal(), CircuitTopology.Side.INPUT, 0).id(), caller);
        }
    }

    private static FutureTask<Void> startBackgroundSaves(Path evidence, Path current) throws Exception {
        var started = new java.util.concurrent.CountDownLatch(1);
        var firstSave = new java.util.concurrent.CountDownLatch(1);
        var writer = new FutureTask<Void>(() -> {
            started.countDown();
            for (int i = 0; i < 8; i++) {
                GeckoSim._win.rawSaveFile(evidence.resolve("backup-" + i + ".ipes").toFile());
                firstSave.countDown();
            }
            return null;
        });
        Thread.ofPlatform().daemon().name("hierarchy-backup-regression").start(writer);
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertFalse(firstSave.await(200, TimeUnit.MILLISECONDS), "The save captured live document state off the editor thread.");
        for (int i = 0; i < 6; i++) {
            GeckoSim._win.createNewFile();
            open(current);
        }
        return writer;
    }

    private static void verifyBackgroundSaves(Path evidence) throws Exception {
        var reader = new CircuitFileParser();
        var preparation = new CircuitPreparation();
        for (int i = 0; i < 8; i++) {
            var backup = reader.parse(evidence.resolve("backup-" + i + ".ipes").toString());
            var ready = assertInstanceOf(PreparationResult.Ready.class,
                    preparation.prepare(preparation.capture(backup), PreparationRequest.defaults()));
            assertHierarchyOutputs(ready.execution().acPlan().execute(), 2000, 4000, 5000);
        }
    }

    @Test
    void rejectsTheSamePartialImportsInCoreAndProductionDesktop() throws Exception {
        Path evidence = evidence();
        var sources = java.util.Map.of("ac", SYNTHETIC + ".foo\n",
                "transient", ".tran 1u 1m\nV1 input 0 1\nR1 input 0 1k\nQ1 input input 0 arbitrary\n");
        try {
            for (var fixture : sources.entrySet()) {
                String name = fixture.getKey();
                Path source = evidence.resolve("partial-" + name + ".cir");
                Path ipes = evidence.resolve("partial-" + name + ".ipes");
                Files.writeString(source, fixture.getValue());
                assertEquals(2, Main.run(new String[]{source.toString(), ipes.toString()}, System.out, System.err));
                var model = new CircuitFileParser().parse(ipes.toString());
                assertEquals(1, model.getConversionIssues().size());
                var expected = model.getConversionIssues();
                var preparation = new CircuitPreparation();
                var core = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
                assertFalse(core.report().readyForHeadless());
                assertTrue(core.report().diagnostics().stream().anyMatch(item -> item.code().equals(expected.getFirst().code())));
                onEdt(() -> {
                    var window = open(ipes);
                    assertEquals(expected, MainWindow._se.getConversionIssues());
                    assertPartialDesktop(window, expected.getFirst().code());
                    Path saved = evidence.resolve("partial-" + name + "-saved.ipes");
                    window.rawSaveFile(saved.toFile());
                    window.createNewFile();
                    assertTrue(MainWindow._se.getConversionIssues().isEmpty());
                    open(saved);
                    assertEquals(expected, new CircuitFileParser().parse(saved.toString()).getConversionIssues());
                    assertEquals(expected, MainWindow._se.getConversionIssues());
                    assertPartialDesktop(window, expected.getFirst().code());
                    return null;
                });
            }
        } finally {
            onEdt(() -> { closeDesktop(); return null; });
        }
    }

    @Test
    void stopOpenAndNewStartSuppressQueuedResultsFromOlderRuns() throws Exception {
        Path evidence = evidence();
        Path original = evidence.resolve("lifecycle-original.ipes");
        Path replacement = evidence.resolve("lifecycle-replacement.ipes");
        Path longSweep = evidence.resolve("lifecycle-long-sweep.ipes");
        new CircuitFileWriter().write(new SpiceImporter().convert(SYNTHETIC, "lifecycle.cir"), original);
        new CircuitFileWriter().write(new SpiceImporter().convert(
                SYNTHETIC.replace("input", "replacementInput"), "lifecycle-replacement.cir"), replacement);
        new CircuitFileWriter().write(new SpiceImporter().convert(
                SYNTHETIC.replace(".ac dec 4 10 100", ".ac dec 47000 10 1000"), "lifecycle-long-sweep.cir"), longSweep);
        try {
            var stopRun = onEdt(() -> {
                var window = open(longSweep);
                window.initStartWithErrorDialogMessage();
                var stop = simulationItem("Pause");
                assertTrue(stop.isEnabled());
                var current = activeAcWorker(window);
                assertFalse(current.isDone());
                stop.doClick(0);
                assertFalse(stop.isEnabled());
                assertNull(activeAcWorker(window));
                return current;
            });
            assertTrue(stopRun.isCancelled());
            awaitAcCallback();
            assertEquals(0, onEdt(NativeAcDesktopTest::visibleAcResults));

            onEdt(() -> {
                var window = open(original);
                window.initStartWithErrorDialogMessage();
                // The worker finishes while its Swing completion callback waits on the EDT.
                var old = activeAcWorker(window);
                assertNotNull(old.get(5, TimeUnit.SECONDS));
                open(replacement);
                assertNull(activeAcWorker(window));
                return null;
            });
            awaitAcCallback();
            assertEquals(0, onEdt(NativeAcDesktopTest::visibleAcResults));

            onEdt(() -> {
                var window = open(original);
                window.initStartWithErrorDialogMessage();
                var old = activeAcWorker(window);
                assertNotNull(old.get(5, TimeUnit.SECONDS));
                // A new start owns the only result even if the first callback is ready.
                window.initStartWithErrorDialogMessage();
                assertNotSame(old, activeAcWorker(window));
                return null;
            });
            waitForResultDialog();
            awaitAcCallback();
            assertEquals(1, onEdt(NativeAcDesktopTest::visibleAcResults));

            onEdt(() -> {
                var window = GeckoSim._win;
                window.initStartWithErrorDialogMessage();
                assertNotNull(activeAcWorker(window).get(5, TimeUnit.SECONDS));
                window.createNewFile();
                assertNull(activeAcWorker(window));
                return null;
            });
            awaitAcCallback();
            assertEquals(0, onEdt(NativeAcDesktopTest::visibleAcResults));
        } finally {
            onEdt(() -> { closeDesktop(); return null; });
        }
    }

    private static void assertPartialDesktop(MainWindow window, String code) {
        var refused = assertThrows(IllegalArgumentException.class,
                () -> window._simRunner.startCalculation(true, MainWindow._solverSettings));
        assertTrue(refused.getMessage().contains(code));
        assertEquals(0, visibleAcResults());
    }

    private static JMenuItem simulationItem(String action) {
        return descendants(GeckoSim._win.getJMenuBar().getMenu(2).getPopupMenu(), JMenuItem.class).stream()
                .filter(item -> action.equals(item.getActionCommand())).findFirst().orElseThrow();
    }

    private static SwingWorker<?, ?> activeAcWorker(MainWindow window) throws Exception {
        var owner = SimulationRunner.class.getDeclaredField("acAnalysis");
        owner.setAccessible(true);
        var worker = DesktopAcAnalysis.class.getDeclaredField("active");
        worker.setAccessible(true);
        return (SwingWorker<?, ?>) worker.get(owner.get(window._simRunner));
    }

    private static long visibleAcResults() {
        return Arrays.stream(Window.getWindows()).filter(JDialog.class::isInstance).map(JDialog.class::cast)
                .filter(w -> w.isShowing() && w.getTitle().equals("AC frequency response")).count();
    }

    private static void awaitAcCallback() throws Exception {
        var event = new java.util.concurrent.CountDownLatch(1);
        onEdt(() -> {
            var timer = new Timer(140, ignored -> event.countDown());
            timer.setRepeats(false);
            timer.start();
            return null;
        });
        assertTrue(event.await(2, TimeUnit.SECONDS));
    }

    @Test
    void namespacesType53AndRoundTripsBothFactoriesOnOneSheet() throws Exception {
        Path evidence = evidence();
        Path input = evidence.resolve("mixed-original.ipes");
        Path saved = evidence.resolve("mixed-reopened.ipes");
        var model = new SpiceImporter().convert(SYNTHETIC, "mixed.cir");
        var counter = new CircuitModel.ComponentData(53, "COUNTER.1", 8, 22, 503);
        counter.setTerminalXLabels(new String[]{"CLOCK", "RESET"});
        counter.setTerminalYLabels(new String[]{"COUNT"});
        model.addControlComponent(counter);
        new CircuitFileWriter().write(model, input);
        onEdt(() -> {
            try {
                MainWindow window = open(input);
                var allTypes = new ArrayList<AbstractComponentTyp>();
                allTypes.addAll(List.of(CircuitTyp.values()));
                allTypes.addAll(List.of(ControlTyp.values()));
                allTypes.addAll(List.of(SpecialTyp.values()));
                Set<String> keys = new HashSet<>();
                for (var type : allTypes) {
                    var factory = type.getTypeInfo();
                    String key = factory.getExportImportCharacters() + ":" + type.getTypeNumber();
                    if (type == ControlTyp.C_SOURCE_IMPORT_DATA || type == ControlTyp.C_SOURCE_RANDOM) {
                        assertEquals("c:4", key);
                    } else {
                        assertTrue(keys.add(key), "Duplicate ID in IPES namespace: " + key);
                    }
                    assertSame(factory, AbstractTypeInfo.getTypeFromString(factory._fixedIDString));
                }
                assertEquals(CircuitTypCore.values().length, Arrays.stream(CircuitTypCore.values())
                        .map(CircuitTypCore::getTypeNumber).distinct().count());
                assertEquals(53, CircuitTypCore.LK_VCVS.getTypeNumber());
                assertSame(CircuitTyp.LK_VCVS, CircuitTyp.getFromIntNumber(53));
                assertSame(ControlTyp.C_COUNTER, ControlTyp.getFromIntNumber(53));
                assertSame(ControlTyp.C_SOURCE_IMPORT_DATA, ControlTyp.getFromIntNumber(89));
                assertSame(ControlTyp.C_SOURCE_RANDOM, ControlTyp.getFromIntNumber(90));
                assertInstanceOf(AcControlledVoltageSource.class, CircuitTyp.LK_VCVS.getTypeInfo().fabric());
                assertInstanceOf(ReglerCounter.class, ControlTyp.C_COUNTER.getTypeInfo().fabric());
                checkMixed();
                window.rawSaveFile(saved.toFile());
                assertTrue(Files.size(saved) > 0);
                window.createNewFile();
                open(saved);
                checkMixed();
                var parsed = new CircuitFileParser().parse(saved.toString());
                assertEquals(53, parsed.getCircuitComponents().stream().filter(c -> c.getName().equals("E1")).findFirst().orElseThrow().getType());
                assertEquals(53, parsed.getControlComponents().getFirst().getType());
            } finally {
                closeDesktop();
            }
            return null;
        });
    }

    @Test
    void rejectsScalarBoundComponentMergesBeforeChangingTheDestination() throws Exception {
        Path evidence = evidence();
        Path destination = evidence.resolve("scalar-destination.ipes");
        Path source = evidence.resolve("scalar-source.ipes");
        Path literal = evidence.resolve("literal-source.ipes");
        var importer = new SpiceImporter();
        var writer = new CircuitFileWriter();
        writer.write(importer.convert(".param base=3k\nRdest out 0 {base}\n", "destination.cir"), destination);
        writer.write(importer.convert(".param base=2k\nRbound other 0 {base}\n", "source.cir"), source);
        writer.write(importer.convert("Rliteral spare 0 1k\n", "literal.cir"), literal);
        var destinationBytes = Files.readAllBytes(destination);
        var sourceBytes = Files.readAllBytes(source);
        String[] sourceLines;
        try (var gzip = new java.util.zip.GZIPInputStream(Files.newInputStream(source))) {
            sourceLines = new String(gzip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .lines().toArray(String[]::new);
        }
        onEdt(() -> {
            try {
                open(destination);
                var editor = MainWindow._se;
                String loadPath = GlobalFilePathes.datnamAbsLoadIPES;
                var components = List.copyOf(editor._circuitSheet.getAllElements());
                var resistor = component("Rdest");
                var position = new Point(resistor.getSheetPosition().x, resistor.getSheetPosition().y);
                var files = List.copyOf(MainWindow._fileManager._allAvailableFiles);
                boolean dirty = editor.getZustandGeaendert();
                assertThrows(IllegalArgumentException.class, () -> MainWindow.importComponentsFromFile(source.toString(), null));
                assertEquals(loadPath, GlobalFilePathes.datnamAbsLoadIPES);
                var staged = new ProjectData(sourceLines, false, null);
                String stagedLoadPath = GlobalFilePathes.datnamAbsLoadIPES;
                assertThrows(IllegalArgumentException.class, () -> editor.importProjectComponents(staged, null));
                assertEquals(stagedLoadPath, GlobalFilePathes.datnamAbsLoadIPES);
                assertEquals(destination.toAbsolutePath().normalize(), Path.of(MainWindow.getOpenFileName()).toAbsolutePath().normalize());
                assertEquals(components, List.copyOf(editor._circuitSheet.getAllElements()));
                assertEquals(position, resistor.getSheetPosition());
                assertEquals(3000, resistor.getParameter()[0]);
                assertEquals(List.of(new ParameterDefinition("base", "3k")), editor.getRootParameterDefinitions());
                assertEquals(files, List.copyOf(MainWindow._fileManager._allAvailableFiles));
                assertEquals(dirty, editor.getZustandGeaendert());
                assertArrayEquals(destinationBytes, Files.readAllBytes(destination));
                assertArrayEquals(sourceBytes, Files.readAllBytes(source));
                MainWindow.importComponentsFromFile(literal.toString(), null);
                assertEquals(1000, component("Rliteral").getParameter()[0]);
            } finally {
                closeDesktop();
            }
            return null;
        });
    }

    @Test
    void rejectsAProfiledComponentWithoutProvenanceBeforeEitherMergePathChangesTheEditor() throws Exception {
        Path evidence = evidence();
        var destination = evidence.resolve("merge-destination.ipes");
        var ordinary = evidence.resolve("ordinary-portable.ipes");
        var writer = new CircuitFileWriter();
        writer.write(new SpiceImporter().convert(SYNTHETIC, "destination.cir"), destination);
        var portable = new SpiceImporter().convert(".ac dec 1 80 80\nLlegacy isolated 0 1u Rser=1m", "portable.cir");
        writer.write(portable, ordinary);
        var owned = portable.getCircuitComponents().getFirst().getAcComponent();
        var forged = new AcComponent(owned.instance(), owned.spiceOrders(), owned.aliases(), owned.mirrored(),
                new AcComponent.Inductance("1u", new AcComponent.ImportedDefault(),
                        AcInputOrigin.LTSPICE_26_1_1_NORMAL_27C));
        String token = gecko.core.io.AcMetadataCodec.COMPONENT;
        String original;
        try (var gzip = new java.util.zip.GZIPInputStream(Files.newInputStream(ordinary))) {
            original = new String(gzip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        String crafted = original.replace(token + " " + gecko.core.io.AcMetadataCodec.encode(owned),
                token + " " + gecko.core.io.AcMetadataCodec.encode(forged));
        assertNotEquals(original, crafted);
        var forgedPath = evidence.resolve("stripped-provenance.ipes");
        try (var gzip = new java.util.zip.GZIPOutputStream(Files.newOutputStream(forgedPath,
                java.nio.file.StandardOpenOption.CREATE_NEW))) {
            gzip.write(crafted.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            onEdt(() -> {
                var window = open(destination);
                var editor = MainWindow._se;
                editor.setDirtyFlag();
                var path = MainWindow.getOpenFileName();
                var before = solve();
                var blocks = List.copyOf(editor.getElementLK());
                var provenance = editor.getAcProvenance();
                var receipt = window.currentAscImport();
                var originalLoadPath = GlobalFilePathes.datnamAbsLoadIPES;
                assertThrows(IllegalArgumentException.class,
                        () -> MainWindow.importComponentsFromFile(forgedPath.toString(), null));
                assertEquals(originalLoadPath, GlobalFilePathes.datnamAbsLoadIPES);
                var unsupported = gecko.core.io.AcMetadataCodec.encode(forged).split("\\.", -1);
                unsupported[4] = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                        "INDUCTANCE_ORIGIN_V2".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                var unsupportedLines = crafted.replace(gecko.core.io.AcMetadataCodec.encode(forged),
                        String.join(".", unsupported)).lines().toArray(String[]::new);
                assertThrows(IllegalArgumentException.class, () -> ProjectData.requireComponentImport(unsupportedLines));
                var project = new ProjectData(crafted.lines().toArray(String[]::new), false, null);
                var pathAfterStaging = GlobalFilePathes.datnamAbsLoadIPES;
                assertThrows(IllegalArgumentException.class, () -> editor.importProjectComponents(project, null));
                assertEquals(pathAfterStaging, GlobalFilePathes.datnamAbsLoadIPES);
                assertEquals(path, MainWindow.getOpenFileName());
                assertSame(receipt, window.currentAscImport());
                assertSame(provenance, editor.getAcProvenance());
                assertEquals(blocks, List.copyOf(editor.getElementLK()));
                assertTrue(editor.getZustandGeaendert());
                assertEquals(before, solve());
                var after = evidence.resolve("merge-destination-after.ipes");
                window.rawSaveFile(after.toFile());
                open(after);
                assertEquals(before, solve());
                var valid = new ProjectData(original.lines().toArray(String[]::new), false, null);
                assertDoesNotThrow(() -> editor.requireComponentMergeSupported(valid));
                var legacy = new AcComponent(owned.instance(), owned.spiceOrders(), owned.aliases(), owned.mirrored(),
                        new AcComponent.Inductance("1u", "1m"));
                var legacyText = original.replace(gecko.core.io.AcMetadataCodec.encode(owned),
                        gecko.core.io.AcMetadataCodec.encode(legacy));
                assertDoesNotThrow(() -> ProjectData.requireComponentImport(legacyText.lines().toArray(String[]::new)));
                assertDoesNotThrow(() -> editor.requireComponentMergeSupported(
                        new ProjectData(legacyText.lines().toArray(String[]::new), false, null)));
                MainWindow.importComponentsFromFile(ordinary.toString(), null);
                assertEquals(AcInputOrigin.PORTABLE_CIR,
                        ((AcComponent.Inductance) component("Llegacy").getAcComponent().law()).origin());
                return null;
            });
        } finally {
            onEdt(() -> { closeDesktop(); return null; });
        }
    }

    @Test
    void opensAndRepairsInvalidPortableInductorExpressions() throws Exception {
        Path evidence = evidence();
        try {
            for (String invalid : List.of("0", "{unresolved}")) {
                String suffix = invalid.equals("0") ? "zero" : "unresolved";
                var input = evidence.resolve("portable-" + suffix + ".ipes");
                var model = new SpiceImporter().convert("""
                        .ac dec 1 80 80
                        V1 n 0 AC 1
                        Ltest n 0 1u Rser=%s
                        """.formatted(invalid), "portable.cir");
                new CircuitFileWriter().write(model, input);
                onEdt(() -> {
                    var window = open(input);
                    assertThrows(IllegalArgumentException.class, NativeAcDesktopTest::solve);
                    withDialog("AC component Ltest", dialog -> {
                        var fields = descendants(dialog, JTextField.class);
                        assertEquals(invalid, fields.get(1).getText());
                        assertTrue(descendants(dialog, javax.swing.JLabel.class).stream()
                                .anyMatch(label -> "Invalid value".equals(label.getText())));
                        fields.get(1).setText("1m");
                    }, null, () -> component("Ltest").doDoubleClickAction(component("Ltest").getSheetPosition()));
                    var repaired = solve();
                    checkInductor("Ltest", 1e-6, .001, AcComponent.ResistanceMode.EXPLICIT_POSITIVE, repaired);
                    var saved = evidence.resolve("repaired-" + suffix + ".ipes");
                    window.rawSaveFile(saved.toFile());
                    open(saved);
                    assertEquals(repaired, solve());
                    return null;
                });
            }
        } finally {
            onEdt(() -> { closeDesktop(); return null; });
        }
    }

    @Test
    void importsTheOriginalAscThroughTheDesktopAndRetainsInductorEdits() throws Exception {
        String source = System.getProperty("gecko.ac.gate3.asc");
        String symbols = System.getProperty("gecko.ac.gate3.symbols");
        assumeTrue(source != null && symbols != null, "Provide the original ASC and stock ASY directory.");
        Path evidence = evidence();
        Path baselinePath = evidence.resolve("gate3-original-desktop.ipes");
        var seed = evidence.resolve("desktop-seed.ipes");
        new CircuitFileWriter().write(new SpiceImporter().convert(SYNTHETIC, "desktop-seed.cir"), seed);
        try {
            onEdt(() -> {
                var window = open(seed);
                MainWindow._se.setDirtyFlag();
                var receipt = chooseLtspiceImport(window, Path.of(source), Path.of(symbols), ImportAttempt.SUCCESS);
                assertEquals(18, receipt.devices().size());
                assertEquals(73, receipt.wires().size());
                assertEquals(42, receipt.devices().stream().mapToInt(device -> device.pins().size()).sum());
                assertEquals(AcInputOrigin.LTSPICE_26_1_1_NORMAL_27C.profileId(), receipt.profileId());
                Files.writeString(evidence.resolve("gate3-original-import-receipt.json"), receipt.format());
                var liveImport = window.currentAscImport();
                String openPath = MainWindow.getOpenFileName();
                var blocks = List.copyOf(MainWindow._se.getElementLK());
                var badSource = Files.writeString(evidence.resolve("invalid-source.asc"), "not a schematic");
                for (var failure : List.of(ImportAttempt.LEAVE_CANCEL, ImportAttempt.SOURCE_CANCEL,
                        ImportAttempt.PROFILE_CANCEL, ImportAttempt.FAIL)) {
                    assertNull(chooseLtspiceImport(window, failure == ImportAttempt.FAIL ? badSource : Path.of(source),
                            Path.of(symbols), failure));
                    assertSame(liveImport, window.currentAscImport());
                    assertEquals(openPath, MainWindow.getOpenFileName());
                    assertEquals(blocks, List.copyOf(MainWindow._se.getElementLK()));
                    assertTrue(MainWindow._se.getZustandGeaendert());
                }
                var baseline = solve();
                assertEquals(401, baseline.frequencyHz().size());
                assertEquals(16, baseline.groups().size());
                assertEquals(18, baseline.currents().size());
                checkInductor("LoL", 1000, .001, AcComponent.ResistanceMode.IMPORTED_DEFAULT, baseline);

                window.rawSaveFile(baselinePath.toFile());
                window.createNewFile();
                open(baselinePath);
                assertNull(window.currentAscImport());
                assertEquals(baseline, solve());
                var parsed = new CircuitFileParser().parse(baselinePath.toString());
                Path coreCopy = evidence.resolve("gate3-core-rewritten.ipes");
                new CircuitFileWriter().write(parsed, coreCopy);
                var rewritten = new CircuitFileParser().parse(coreCopy.toString());
                assertEquals(parsed.getAcProvenance(), rewritten.getAcProvenance());

                var receiptLines = new ArrayList<String>();
                editGate3Inductor("LoL", "1k", "1m", false);
                checkInductor("LoL", 1000, .001, AcComponent.ResistanceMode.EXPLICIT_POSITIVE, solve());
                assertEquals(baseline, solve());
                saveGate3Inductor("equal-explicit", evidence, 1000, .001, AcComponent.ResistanceMode.EXPLICIT_POSITIVE, receiptLines);
                editGate3Inductor("LoL", "1k", "100k", false);
                assertNotEquals(current(baseline, "LoL", WITNESS), current(solve(), "LoL", WITNESS));
                saveGate3Inductor("high-resistance", evidence, 1000, 100000, AcComponent.ResistanceMode.EXPLICIT_POSITIVE, receiptLines);
                editGate3Inductor("LoL", "1k", "", true);
                saveGate3Inductor("restored-default", evidence, 1000, .001, AcComponent.ResistanceMode.IMPORTED_DEFAULT, receiptLines);
                assertEquals(baseline, solve());

                open(baselinePath);
                editGate3Inductor("LoL", "2k", "", true);
                assertNotEquals(current(baseline, "LoL", WITNESS), current(solve(), "LoL", WITNESS));
                saveGate3Inductor("double-inductance", evidence, 2000, .001, AcComponent.ResistanceMode.IMPORTED_DEFAULT, receiptLines);
                open(baselinePath);
                checkGate3WirePartition(window, baseline);
                window.rawSaveFile(evidence.resolve("gate3-wire-repaired.ipes").toFile());
                receiptLines.add("LoL terminal partition splits and recovers through native wire editing.");
                Files.write(evidence.resolve("gate3-desktop-edits.txt"), receiptLines);
                return null;
            });
        } finally {
            onEdt(() -> { closeDesktop(); return null; });
        }
    }

    private enum ImportAttempt { SUCCESS, LEAVE_CANCEL, SOURCE_CANCEL, PROFILE_CANCEL, FAIL }

    private static LtspiceImportReceipt chooseLtspiceImport(MainWindow window, Path source, Path symbols,
                                                           ImportAttempt attempt) {
        var previous = window.currentAscImport();
        var seenPrompt = new java.util.concurrent.atomic.AtomicBoolean();
        var stage = new java.util.concurrent.atomic.AtomicInteger();
        Timer timer = new Timer(100, event -> {
            for (Window candidate : Window.getWindows()) {
                if (!(candidate instanceof JDialog dialog) || !dialog.isShowing()) continue;
                var panes = descendants(dialog, JOptionPane.class);
                if (dialog.getTitle().equals("Import LTspice ASC") && seenPrompt.compareAndSet(false, true)) {
                    panes.getFirst().setValue(attempt == ImportAttempt.LEAVE_CANCEL
                            ? JOptionPane.CANCEL_OPTION : JOptionPane.NO_OPTION);
                    return;
                }
                var choosers = descendants(dialog, JFileChooser.class);
                if (stage.get() <= 1 && !choosers.isEmpty()) {
                    var chooser = choosers.getFirst();
                    var selected = stage.getAndIncrement() == 0 ? source : symbols;
                    if (attempt == ImportAttempt.SOURCE_CANCEL) {
                        chooser.cancelSelection();
                    } else {
                        chooser.setSelectedFile(selected.toFile());
                        chooser.approveSelection();
                    }
                    return;
                }
                if (stage.get() == 2 && dialog.getTitle().equals("LTspice import profile")) {
                    var pane = descendants(dialog, JOptionPane.class).getFirst();
                    var choices = descendants(pane, JComboBox.class).getFirst();
                    stage.incrementAndGet();
                    if (attempt == ImportAttempt.PROFILE_CANCEL) {
                        pane.setValue(JOptionPane.CANCEL_OPTION);
                    } else {
                        choices.setSelectedIndex(1);
                        pane.setInputValue(choices.getSelectedItem());
                        pane.setValue(JOptionPane.OK_OPTION);
                    }
                    return;
                }
                if (dialog.getTitle().equals("LTspice import")) {
                    stage.incrementAndGet();
                    dialog.dispose();
                    return;
                }
            }
        });
        Timer deadline = new Timer(8000, event -> {
            for (Window candidate : Window.getWindows()) {
                if (candidate instanceof JDialog dialog && dialog.isShowing() && dialog.isModal()) dialog.dispose();
            }
        });
        deadline.setRepeats(false);
        timer.start();
        deadline.start();
        try {
            menuItem(window, "Import LTspice ASC").doClick();
            assertTrue(seenPrompt.get(), "The dirty document must prompt before import.");
            int expectedStage = switch (attempt) {
                case LEAVE_CANCEL -> 0;
                case SOURCE_CANCEL -> 1;
                case PROFILE_CANCEL, SUCCESS -> 3;
                case FAIL -> 4;
            };
            assertEquals(expectedStage, stage.get());
            var current = window.currentAscImport();
            if (attempt != ImportAttempt.SUCCESS) {
                assertSame(previous, current);
                return null;
            }
            assertNotNull(current);
            assertNotSame(previous, current);
            return current.receipt();
        } finally {
            deadline.stop();
            timer.stop();
        }
    }

    private static JMenuItem menuItem(MainWindow window, String command) {
        for (int i = 0; i < window.getJMenuBar().getMenuCount(); i++) {
            var menu = window.getJMenuBar().getMenu(i);
            if (menu == null) continue;
            for (var item : descendants(menu.getPopupMenu(), JMenuItem.class)) {
                if (command.equals(item.getActionCommand())) return item;
            }
        }
        throw new AssertionError("Missing menu command " + command);
    }

    private static void editGate3Inductor(String name, String henries, String ohms, boolean useDefault) throws Exception {
        withDialog("AC component " + name, dialog -> {
            var fields = descendants(dialog, JTextField.class);
            var setting = descendants(dialog, JCheckBox.class).getFirst();
            if (setting.isSelected() != useDefault) setting.doClick();
            fields.get(0).setText(henries);
            if (!useDefault) fields.get(1).setText(ohms);
        }, null, () -> component(name).doDoubleClickAction(component(name).getSheetPosition()));
    }

    private static void saveGate3Inductor(String name, Path evidence, double henries, double ohms,
                                          AcComponent.ResistanceMode mode, List<String> receipt) throws Exception {
        var window = GeckoSim._win;
        var before = solve();
        checkInductor("LoL", henries, ohms, mode, before);
        Path output = evidence.resolve("gate3-" + name + ".ipes");
        window.rawSaveFile(output.toFile());
        window.createNewFile();
        open(output);
        var after = solve();
        checkInductor("LoL", henries, ohms, mode, after);
        assertEquals(before, after);
        receipt.add(name + " passes live and reopened impedance and mode checks.");
    }

    private static void checkInductor(String instance, double henries, double ohms, AcComponent.ResistanceMode mode,
                                      AcResult result) {
        var law = assertInstanceOf(AcComponent.Inductance.class, component(instance).getAcComponent().law());
        var value = law.evaluate(MainWindow._se.getAcSettings().parameterMap());
        assertEquals(mode, value.mode());
        assertEquals(henries, value.henries(), 0);
        assertEquals(ohms, value.ohms(), 0);
        for (int i = 0; i < result.frequencyHz().size(); i++) {
            double omega = 2 * Math.PI * result.frequencyHz().get(i);
            var impedance = delta(result, instance, 1, 2, i).divide(current(result, instance, i));
            assertEquals(ohms, impedance.real(), 2e-8);
            assertEquals(henries, impedance.imaginary() / omega, 1e-7);
        }
    }

    private static void checkGate3WirePartition(MainWindow window, AcResult baseline) throws Exception {
        var editor = MainWindow._se;
        var terminal = component("LoL").XIN.getFirst().getPosition();
        var wire = editor.getConnection(ConnectorType.LK_AND_RELUCTANCE).stream()
                .filter(item -> item.getLabel().isEmpty())
                .filter(item -> item.getStartPoint().equals(terminal) || item.getEndPoint().equals(terminal))
                .findFirst().orElseThrow();
        var ascii = new StringBuffer();
        wire.exportASCII(ascii);
        wire.deleteComponent();
        editor.setDirtyFlag();
        var captured = new gecko.geckocircuits.circuit.EditorCircuitCapture().capture(editor._circuitSheet.getAllElements(),
                MainWindow._solverSettings, MainWindow.optimizerParameterData, MainWindow._fileManager,
                gecko.geckocircuits.circuit.EditorCircuitCapture.ScriptSources.EMPTY, "gate3-broken", editor.getAcSettings(),
                editor.getAcProvenance()).toModel();
        var coil = captured.getCircuitComponents().stream().filter(c -> c.getName().equals("LoL")).findFirst().orElseThrow();
        var node = CircuitTopology.resolve(captured).group(coil, CircuitTopology.Side.INPUT, 0);
        assertTrue(node == null || node.terminals().size() == 1);
        var token = new gecko.core.circuit.TokenMap(ascii.toString().split("\\R"), true);
        new Verbindung(token, ConnectorType.LK).findAndSetReferenceToParentSheet(List.of(), null);
        editor.setDirtyFlag();
        assertEquals(baseline, solve());
    }

    @Test
    void runsEveryAuthorEditBeforeAndAfterDesktopSaveAndReopen() throws Exception {
        String input = System.getProperty("gecko.ac.author.ipes");
        assumeTrue(input != null, "Provide the isolated import IPES to run the author acceptance.");
        Path author = Path.of(input);
        Path evidence = evidence();
        var receipt = new ArrayList<String>();
        try {
            onEdt(() -> {
                var window = open(author);
                window.setSize(1540, 1050);
                AbstractCircuitSheetComponent.dpixValue.setValue(10);
                return null;
            });
            var baseline = onEdt(NativeAcDesktopTest::solve);
            assertEquals(401, baseline.frequencyHz().size());
            assertEquals(21, baseline.groups().size());
            assertEquals(22, baseline.currents().size());
            assertEquals(39, baseline.complexUnknowns());
            screenshot("pid-schematic", evidence, onEdt(() -> GeckoSim._win));
            onEdt(() -> { renderSchematic(evidence.resolve("pid-schematic-overview.png")); return null; });
            onEdt(() -> {
                GeckoSim._win.initStartWithErrorDialogMessage();
                return null;
            });
            var results = waitForResultDialog();
            onEdt(() -> {
                var selector = descendants(results, JComboBox.class).getFirst();
                for (int i = 0; i < selector.getItemCount(); i++) {
                    if (selector.getItemAt(i).toString().toLowerCase(java.util.Locale.ROOT).contains("outop")) {
                        selector.setSelectedIndex(i);
                        break;
                    }
                }
                return null;
            });
            screenshot("pid-frequency-response", evidence, results);
            onEdt(() -> {
                var selector = descendants(results, JComboBox.class).getFirst();
                for (int i = 0; i < selector.getItemCount(); i++) {
                    if (selector.getItemAt(i).toString().equals("I(R1)")) {
                        selector.setSelectedIndex(i);
                        break;
                    }
                }
                return null;
            });
            screenshot("pid-current-response", evidence, results);
            onEdt(() -> {
                results.dispose();
                return null;
            });

            edit(author, evidence, receipt, "gain", () -> editComponent("E1", List.of("20"), evidence.resolve("pid-gain-dialog.png")),
                    changed -> {
                        close(delta(changed, "E1", 3, 4, WITNESS).scale(20), delta(changed, "E1", 1, 2, WITNESS));
                        assertNotEquals(voltage(baseline, "E1", 1, WITNESS), voltage(changed, "E1", 1, WITNESS));
                    });
            edit(author, evidence, receipt, "resistance", () -> editComponent("R1", List.of("10k"), null),
                    changed -> {
                        close(delta(changed, "R1", 1, 2, WITNESS).scale(1e-4), current(changed, "R1", WITNESS));
                        assertNotEquals(voltage(baseline, "E1", 1, WITNESS), voltage(changed, "E1", 1, WITNESS));
                    });
            edit(author, evidence, receipt, "capacitance", () -> editComponent("C1", List.of("20n"), null),
                    changed -> {
                        var admittance = new Complex(0, 2 * Math.PI * changed.frequencyHz().get(WITNESS) * 20e-9);
                        close(admittance.multiply(delta(changed, "C1", 1, 2, WITNESS)), current(changed, "C1", WITNESS));
                        assertNotEquals(voltage(baseline, "E1", 1, WITNESS), voltage(changed, "E1", 1, WITNESS));
                    });
            edit(author, evidence, receipt, "excitation", () -> editComponent("V1", List.of("0", "", "1.5", "37"), null),
                    changed -> {
                        var ratio = new Complex(1.5 * Math.cos(37 * Math.PI / 180), 1.5 * Math.sin(37 * Math.PI / 180));
                        close(ratio, voltage(changed, "V1", 1, WITNESS));
                        close(ratio.multiply(voltage(baseline, "E1", 1, WITNESS)), voltage(changed, "E1", 1, WITNESS));
                    });
            edit(author, evidence, receipt, "sweep", () -> editSettings(dialog -> {
                var fields = descendants(dialog, JTextField.class);
                fields.get(0).setText("20");
                fields.get(1).setText("20");
                fields.get(2).setText("2k");
            }, evidence.resolve("pid-sweep-dialog.png")), changed -> {
                assertEquals(41, changed.frequencyHz().size());
                assertEquals(20.0, changed.frequencyHz().getFirst());
                assertEquals(2000.0, changed.frequencyHz().getLast());
            });
            edit(author, evidence, receipt, "parameter", () -> editSettings(dialog -> {
                var definitions = descendants(dialog, JTextArea.class).getFirst();
                assertTrue(definitions.getText().contains("R1 = 22k"));
                definitions.setText(definitions.getText().replace("R1 = 22k", "R1 = 44k"));
            }, null), changed -> {
                for (String name : List.of("R1", "C1", "R2", "RD", "CD")) {
                    close(current(baseline, name, WITNESS).scale(.5), current(changed, name, WITNESS));
                }
                close(voltage(baseline, "E1", 1, WITNESS), voltage(changed, "E1", 1, WITNESS));
            });
            breakAndRepair(author, evidence, baseline, receipt);
            Files.write(evidence.resolve("desktop-edit-receipt.txt"), receipt);
        } finally {
            onEdt(() -> { closeDesktop(); return null; });
        }
    }

    private static void edit(Path author, Path evidence, List<String> receipt, String name, CheckedAction mutation,
                             Consumer<AcResult> check) throws Exception {
        onEdt(() -> {
            var window = open(author);
            mutation.run();
            AcResult before = solve();
            check.accept(before);
            Path file = evidence.resolve("desktop-" + name + ".ipes");
            assertFalse(Files.exists(file));
            window.rawSaveFile(file.toFile());
            window.createNewFile();
            open(file);
            AcResult after = solve();
            check.accept(after);
            assertEquals(before, after, name);
            receipt.add(name + " edit, live run, save, close, reopen, run PASS");
            return null;
        });
    }

    private static void breakAndRepair(Path author, Path evidence, AcResult baseline, List<String> receipt) throws Exception {
        onEdt(() -> {
            var window = open(author);
            var originalLabels = electricalWireLabels();
            var cap = component("C1").XIN.getFirst().getPosition();
            var resistor = component("R1").YOUT.getFirst().getPosition();
            var middle = (cap.x + resistor.x) / 2;
            var wire = MainWindow._se.getConnection(ConnectorType.LK_AND_RELUCTANCE).stream()
                    .filter(w -> w.getLabel().isEmpty() && w.getStartPoint().y == cap.y && w.getEndPoint().y == cap.y)
                    .filter(w -> Math.min(w.getStartPoint().x, w.getEndPoint().x) < middle
                            && Math.max(w.getStartPoint().x, w.getEndPoint().x) > middle).findFirst().orElseThrow();
            StringBuffer shape = new StringBuffer();
            wire.exportASCII(shape);
            wire.deleteComponent();
            MainWindow._se.setDirtyFlag();
            assertEquals(originalLabels, electricalWireLabels());
            assertSplit();
            String before = runBroken();
            Path broken = evidence.resolve("desktop-wire-broken.ipes");
            window.rawSaveFile(broken.toFile());
            window.createNewFile();
            open(broken);
            assertSplit();
            assertEquals(before, runBroken());
            assertEquals(originalLabels, electricalWireLabels());
            var token = new gecko.core.circuit.TokenMap(shape.toString().split("\\R"), true);
            var restored = new Verbindung(token, ConnectorType.LK);
            restored.findAndSetReferenceToParentSheet(List.of(), null);
            MainWindow._se.setDirtyFlag();
            assertEquals(baseline, solve());
            Path repaired = evidence.resolve("desktop-wire-repaired.ipes");
            window.rawSaveFile(repaired.toFile());
            window.createNewFile();
            open(repaired);
            assertEquals(baseline, solve());
            receipt.add("wire split, live result, save, reopen, repeat, repair, live result, save, reopen, repeat PASS: " + before);
            return null;
        });
    }

    private static String runBroken() throws Exception {
        try {
            var result = solve();
            close(Complex.ZERO, current(result, "R1", WITNESS));
            assertEquals(22, result.groups().size());
            return "zero feedback current";
        } catch (IllegalArgumentException error) {
            assertTrue(error.getMessage().matches("(?is).*(singular|unconnected).*"), error.getMessage());
            return error.getMessage();
        }
    }

    private static void assertSplit() throws Exception {
        var editor = MainWindow._se;
        var snapshot = new gecko.geckocircuits.circuit.EditorCircuitCapture().capture(editor._circuitSheet.getAllElements(),
                MainWindow._solverSettings, MainWindow.optimizerParameterData, MainWindow._fileManager,
                gecko.geckocircuits.circuit.EditorCircuitCapture.ScriptSources.EMPTY, MainWindow.getOpenFileName(),
                editor.getAcSettings(), editor.getAcProvenance());
        var model = snapshot.toModel();
        var graph = CircuitTopology.resolve(model);
        var cap = model.getCircuitComponents().stream().filter(c -> c.getName().equals("C1")).findFirst().orElseThrow();
        var res = model.getCircuitComponents().stream().filter(c -> c.getName().equals("R1")).findFirst().orElseThrow();
        assertNotEquals(graph.group(cap, CircuitTopology.Side.INPUT, 0).id(), graph.group(res, CircuitTopology.Side.OUTPUT, 0).id());
    }

    private static List<String> electricalWireLabels() {
        return MainWindow._se.getConnection(ConnectorType.LK_AND_RELUCTANCE).stream().map(Verbindung::getLabel)
                .filter(label -> !label.isEmpty()).sorted().toList();
    }

    private static void checkMixed() {
        assertInstanceOf(AcControlledVoltageSource.class, component("E1"));
        assertEquals(2, component("E1").XIN.size());
        assertEquals(2, component("E1").YOUT.size());
        assertEquals(1, MainWindow._se.getElementCONTROL().size());
        assertInstanceOf(ReglerCounter.class, MainWindow._se.getElementCONTROL().getFirst());
    }

    private static void editComponent(String instance, List<String> values, Path image) throws Exception {
        withDialog("AC component " + instance, dialog -> {
            var fields = descendants(dialog, JTextField.class);
            assertEquals(values.size(), fields.size());
            for (int i = 0; i < fields.size(); i++) fields.get(i).setText(values.get(i));
        }, image, () -> component(instance).doDoubleClickAction(component(instance).getSheetPosition()));
    }

    private static void editSettings(Consumer<JDialog> mutation, Path image) throws Exception {
        JMenuItem menu = descendants(GeckoSim._win.getJMenuBar().getMenu(2).getPopupMenu(), JMenuItem.class).stream()
                .filter(item -> item.getText().startsWith("AC analysis settings")).findFirst().orElseThrow();
        withDialog("AC analysis settings", mutation, image, menu::doClick);
    }

    private static void withDialog(String title, Consumer<JDialog> edit, Path image, Runnable open) throws Exception {
        var error = new AtomicReference<Throwable>();
        var accepted = new AtomicReference<Boolean>(false);
        Timer timer = new Timer(180, null);
        timer.addActionListener(event -> {
            var dialog = Arrays.stream(Window.getWindows()).filter(JDialog.class::isInstance).map(JDialog.class::cast)
                    .filter(candidate -> candidate.isShowing() && candidate.getTitle().equals(title)).findFirst();
            if (dialog.isEmpty()) return;
            timer.stop();
            try {
                edit.accept(dialog.get());
                if (image != null) capture(image, dialog.get());
                descendants(dialog.get(), JOptionPane.class).getFirst().setValue(JOptionPane.OK_OPTION);
                accepted.set(true);
            } catch (Throwable problem) {
                error.set(problem);
                dialog.get().dispose();
            }
        });
        Timer deadline = new Timer(5000, event -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog dialog && dialog.isShowing() && dialog.isModal()) {
                    error.set(new IllegalStateException("Unexpected dialog: " + dialog.getTitle()));
                    dialog.dispose();
                }
            }
        });
        deadline.setRepeats(false);
        timer.start();
        deadline.start();
        try {
            open.run();
            if (error.get() != null) throw new AssertionError(error.get());
            assertTrue(accepted.get(), "Did not accept " + title);
        } finally {
            timer.stop();
            deadline.stop();
        }
    }

    private static JDialog waitForResultDialog() throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < deadline) {
            var found = onEdt(() -> Arrays.stream(Window.getWindows()).filter(JDialog.class::isInstance)
                    .map(JDialog.class::cast).filter(w -> w.isShowing() && w.getTitle().equals("AC frequency response"))
                    .findFirst().orElse(null));
            if (found != null) return found;
            Thread.sleep(40);
        }
        throw new AssertionError("The AC frequency response did not open.");
    }

    private static void screenshot(String name, Path evidence, Window window) throws Exception {
        new Robot().waitForIdle();
        onEdt(() -> { capture(evidence.resolve(name + ".png"), window); return null; });
    }

    private static void capture(Path path, Window window) throws Exception {
        var bounds = new java.awt.Rectangle(window.getLocationOnScreen(), window.getSize());
        ImageIO.write(new Robot().createScreenCapture(bounds), "png", path.toFile());
    }

    private static void renderSchematic(Path path) throws Exception {
        var editor = MainWindow._se;
        var points = editor.getConnection(ConnectorType.LK_AND_RELUCTANCE).stream()
                .flatMap(wire -> wire.getAllPointCoordinates().stream()).toList();
        int grid = AbstractCircuitSheetComponent.dpix;
        int width = points.stream().mapToInt(point -> point.x).max().orElseThrow() * grid + 145;
        int height = points.stream().mapToInt(point -> point.y).max().orElseThrow() * grid + 75;
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setClip(0, 0, width, height);
        editor._circuitSheet.paintComponent(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", path.toFile());
    }

    private static MainWindow open(Path file) throws Exception {
        if (GeckoSim._win == null || !GeckoSim._win.isDisplayable()) {
            GeckoSim._isTestingMode = true;
            GeckoSim.main(new String[]{file.toString()});
        } else {
            GeckoSim._win.openFile(file.toString());
        }
        assertEquals(file.toAbsolutePath().normalize(),
                Path.of(MainWindow.getOpenFileName()).toAbsolutePath().normalize());
        return GeckoSim._win;
    }

    private static void closeDesktop() {
        if (GeckoSim._win != null && GeckoSim._win.isDisplayable()) GeckoSim._win.createNewFile();
        for (var window : Window.getWindows()) window.dispose();
    }

    private static <T extends Component> List<T> descendants(Container parent, Class<T> kind) {
        var result = new ArrayList<T>();
        for (var component : parent.getComponents()) {
            if (kind.isInstance(component)) result.add(kind.cast(component));
            if (component instanceof Container nested) result.addAll(descendants(nested, kind));
        }
        return result;
    }

    private static AcResult solve() throws Exception {
        var prepared = DesktopAcAnalysis.prepare(MainWindow._se, MainWindow._solverSettings);
        if (!(prepared instanceof PreparationResult.Ready ready)) throw new IllegalArgumentException(prepared.report().toString());
        return ready.execution().acPlan().execute();
    }

    private static AbstractBlockInterface component(String instance) {
        return MainWindow._se.getElementLK().stream().filter(block -> block.getStringID().equals(instance)).findFirst().orElseThrow();
    }

    private static Complex voltage(AcResult result, String instance, int terminal, int sample) {
        return result.groups().stream().filter(g -> g.members().contains(new AcResult.Member(instance, terminal)))
                .findFirst().orElseThrow().voltageV().get(sample);
    }

    private static Complex current(AcResult result, String instance, int sample) {
        return result.currents().stream().filter(i -> i.instance().equals(instance)).findFirst().orElseThrow().currentA().get(sample);
    }

    private static Complex delta(AcResult result, String instance, int p, int n, int sample) {
        return voltage(result, instance, p, sample).subtract(voltage(result, instance, n, sample));
    }

    private static void close(Complex expected, Complex actual) {
        assertTrue(expected.subtract(actual).abs() <= 1e-12 + 1e-9 * expected.abs(), expected + " != " + actual);
    }

    private static Path evidence() throws Exception {
        String root = System.getProperty("gecko.ac.evidence");
        return Files.createTempDirectory(root == null ? Path.of("target") : Path.of(root), "native-ac-desktop-");
    }

    private static <T> T onEdt(Callable<T> task) throws Exception {
        var work = new FutureTask<>(task);
        SwingUtilities.invokeAndWait(work);
        return work.get();
    }

    @FunctionalInterface
    private interface CheckedAction { void run() throws Exception; }
}

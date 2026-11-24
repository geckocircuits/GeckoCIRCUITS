package gecko.geckocircuits.allg;

import gecko.GeckoSim;
import gecko.core.ac.AcComponent;
import gecko.core.ac.AcResult;
import gecko.core.ac.Complex;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;
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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
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
            assertEquals(37, baseline.complexUnknowns());
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

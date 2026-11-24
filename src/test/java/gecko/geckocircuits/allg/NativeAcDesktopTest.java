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
import gecko.geckocircuits.circuit.SpecialTyp;
import gecko.geckocircuits.circuit.Verbindung;
import gecko.geckocircuits.circuit.circuitcomponents.AcControlledVoltageSource;
import gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import gecko.geckocircuits.control.ControlTyp;
import gecko.geckocircuits.control.ReglerCounter;
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

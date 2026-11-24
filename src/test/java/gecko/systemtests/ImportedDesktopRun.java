package gecko.systemtests;

import gecko.GeckoSim;
import gecko.core.simulation.SimulationSession;
import gecko.geckocircuits.allg.MainWindow;
import gecko.geckocircuits.circuit.SchematicEditor2;
import gecko.geckocircuits.control.NetzlisteCONTROL;
import gecko.geckocircuits.control.ReglerOSZI;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import static org.junit.jupiter.api.Assertions.*;

/** Runs an imported model in an isolated desktop process. */
final class ImportedDesktopRun {
    private ImportedDesktopRun() { }

    static void launch(String fixture, Class<?> worker) throws Exception {
        Path target = Path.of("target").toAbsolutePath();
        Path reports = Files.createDirectories(target.resolve("surefire-reports"));
        Path evidence = Files.createTempDirectory(reports, "m6-" + fixture + "-");
        Path appHome = Files.createTempDirectory(target, "m6-" + fixture + "-home-");
        Path log = evidence.resolve("desktop.log");
        String classPath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        var builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Duser.home=" + appHome, "-Djava.io.tmpdir=" + appHome, "-Dgecko.backend=core", "-cp", classPath,
                worker.getName(), fixture, evidence.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("LOCALAPPDATA", appHome.toString());
        Process process = builder.start();
        try {
            assertTrue(process.waitFor(60, TimeUnit.SECONDS), "The desktop test timed out. See " + log);
            assertEquals(0, process.exitValue(), Files.readString(log));
            assertTrue(Files.isRegularFile(evidence.resolve("receipt.txt")), "Desktop evidence is missing. See " + log);
        } finally {
            if (process.isAlive()) process.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
        }
    }

    @FunctionalInterface
    interface ResultCheck {
        void accept(ReglerOSZI scope, int steps) throws Exception;
    }

    static void open(Path circuit, double duration, double step, Callable<ReglerOSZI> checkLoadedCircuit,
                     ResultCheck checkResult) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
            new AssertionError("Unexpected desktop error on " + thread.getName(), failure).printStackTrace();
            System.exit(1);
        });
        var dialogTitle = new AtomicReference<String>();
        Timer dialogMonitor = new Timer(50, event -> recordModalDialogs(dialogTitle));
        dialogMonitor.start();
        try {
            ReglerOSZI scope = onEventThread(() -> {
                GeckoSim._isTestingMode = true;
                GeckoSim.main(new String[]{circuit.toString()});
                assertNull(dialogTitle.get(), "Opening the circuit showed a modal dialog");
                assertTrue(GeckoSim._win.isShowing(), "The desktop window is hidden");
                assertEquals(circuit.toString(), MainWindow.getOpenFileName());
                ReglerOSZI importedScope = checkLoadedCircuit.call();
                GeckoSim._win.initStartWithErrorDialogMessage();
                assertNull(dialogTitle.get(), "Starting the circuit showed a modal dialog");
                return importedScope;
            });

            var runner = GeckoSim._win._simRunner;
            var status = assertNotNullStatus(runner.getCoreStatus());
            long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
            while (System.nanoTime() < deadline && status.state() != SimulationSession.State.FAILED
                    && (status.state() != SimulationSession.State.COMPLETED
                    || onEventThread(NetzlisteCONTROL.globalData::getContainerStatus) != ContainerStatus.PAUSED)) {
                Thread.sleep(20);
                status = assertNotNullStatus(runner.getCoreStatus());
            }
            assertEquals(SimulationSession.State.COMPLETED, status.state(), "Core simulation status " + status);
            assertEquals(duration, status.time(), step + 1e-12);
            int steps = status.committedSteps();
            onEventThread(() -> {
                assertEquals(ContainerStatus.PAUSED, NetzlisteCONTROL.globalData.getContainerStatus());
                assertNull(dialogTitle.get(), "The simulation showed a modal dialog");
                checkResult.accept(scope, steps);
                recordModalDialogs(dialogTitle);
                assertNull(dialogTitle.get(), "Capturing the result showed a modal dialog");
                return null;
            });
            awaitIdle();
        } finally {
            try {
                onEventThread(() -> {
                    if (GeckoSim._win != null) GeckoSim._win._simRunner.endSimulation();
                    recordModalDialogs(dialogTitle);
                    for (Window window : Window.getWindows()) window.dispose();
                    return null;
                });
                awaitIdle();
                onEventThread(() -> {
                    recordModalDialogs(dialogTitle);
                    return null;
                });
            } finally {
                dialogMonitor.stop();
            }
        }
        assertNull(dialogTitle.get(), "The desktop showed a modal dialog during capture or cleanup");
    }

    private static void recordModalDialogs(AtomicReference<String> dialogTitle) {
        for (Window window : Window.getWindows()) {
            if (window instanceof Dialog dialog && dialog.isShowing() && dialog.isModal()) {
                dialogTitle.compareAndSet(null, Objects.toString(dialog.getTitle(), "<untitled modal dialog>"));
                dialog.dispose();
            }
        }
    }

    private static void awaitIdle() throws Exception {
        var idle = new CompletableFuture<Void>();
        var queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        // Wait between checks so lower-priority paint events can run.
        var check = new Timer(10, event -> {
            if (queue.peekEvent() == null) idle.complete(null);
            else ((Timer) event.getSource()).restart();
        });
        check.setRepeats(false);
        check.start();
        try {
            idle.get(5, TimeUnit.SECONDS);
        } finally {
            check.stop();
        }
    }

    static void captureSchematic(Path file) throws Exception {
        SchematicEditor2._controlDisplayMode.showName = true;
        GeckoSim._win.setSize(1100, 750);
        GeckoSim._win.validate();
        var canvas = MainWindow._se._circuitSheet;
        Rectangle content = null;
        for (var element : canvas.getLocalSheetComponents()) {
            int[] bounds = element.getAussenabmessungenRechteckEckpunkte();
            var rect = new Rectangle(bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1]);
            content = content == null ? rect : content.union(rect);
        }
        assertNotNull(content, "The schematic must contain components");
        content.grow(70, 50);
        content = content.intersection(new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight()));
        var image = writePng(canvas, content, file);
        long symbolPixels = Arrays.stream(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
                .filter(pixel -> (pixel & 0x00ffffff) < 0x00808080).limit(200).count();
        assertEquals(200, symbolPixels, "The schematic capture must show component symbols");
    }

    private static SimulationSession.Status assertNotNullStatus(SimulationSession.Status status) {
        assertNotNull(status, "The desktop runner must use a core session");
        return status;
    }

    static void captureScope(ReglerOSZI scope, double start, double end, Path file, boolean checkColors) throws Exception {
        var frame = scope._scopeFrame;
        frame.setVisible(true);
        frame.validate();
        var graph = frame.getGrafer();
        var diagram = graph.getManager().getDiagrams().getFirst();
        assertSame(scope.getZVDatenImRAM(), graph.getDataContainer());
        assertTrue(diagram.isShowing(), "The SCOPE plot must be visible");
        graph.setAxisPositions();
        graph.setSimulationTimeBoundaries(start, end);
        graph.doZoomAutoFit();
        var image = writePng(frame._scope, new Rectangle(frame._scope.getSize()), file);
        if (!checkColors) return;

        int left = graph.getXPixFromValue(start) + 2;
        int top = diagram.getTopOffset() + 1;
        int width = graph.getXPixFromValue(end) - left - 2;
        int height = diagram.getHeight() - diagram.getBottomOffset() - top - 1;
        assertTrue(width > 0 && height > 0, "The SCOPE plot must have a drawable area");
        var plot = SwingUtilities.convertRectangle(diagram, new Rectangle(left, top, width, height), frame._scope);
        int[] pixels = image.getRGB(plot.x, plot.y, plot.width, plot.height, null, 0, plot.width);
        for (var curve : diagram.getCurves()) {
            Color color = curve.getColor().getJavaColor();
            double opacity = curve.getTransparency();
            long tracePixels = Arrays.stream(pixels).filter(pixel -> hasTraceColor(pixel, color, opacity)).limit(20).count();
            assertEquals(20, tracePixels, "Missing rendered trace for channel " + curve.getValueDataIndex());
        }
    }

    private static boolean hasTraceColor(int pixel, Color trace, double opacity) {
        var actual = new Color(pixel);
        double background = 255 * (1 - opacity);
        return Math.abs(actual.getRed() - (trace.getRed() * opacity + background)) <= 3
                && Math.abs(actual.getGreen() - (trace.getGreen() * opacity + background)) <= 3
                && Math.abs(actual.getBlue() - (trace.getBlue() * opacity + background)) <= 3;
    }

    private static BufferedImage writePng(Component component, Rectangle bounds, Path file) throws Exception {
        assertTrue(component.isShowing(), "The component to capture must be visible");
        var image = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setClip(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.translate(-bounds.x, -bounds.y);
            component.paint(graphics);
        } finally {
            graphics.dispose();
        }
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        assertTrue(Arrays.stream(pixels).anyMatch(pixel -> pixel != pixels[0]), "The capture is blank " + file);
        assertTrue(ImageIO.write(image, "png", file.toFile()), "No PNG writer is available");
        return image;
    }

    private static <T> T onEventThread(Callable<T> action) throws Exception {
        var task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}

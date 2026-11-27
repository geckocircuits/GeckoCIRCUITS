package gecko.geckocircuits.allg;

import gecko.GeckoExternal;
import gecko.GeckoSim;
import gecko.core.simulation.SimulationSession;
import gecko.geckocircuits.circuit.NetListContainer;
import gecko.geckocircuits.control.NetzlisteCONTROL;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

@Tag("regression")
class DesktopTerminalPublicationTest {
    @Test
    @Timeout(45)
    void waitsForTheRunningCommandBeforePublishingCompletionOrAcceptingContinue() throws Exception {
        String previousBackend = System.getProperty("gecko.backend");
        boolean previousTestingMode = GeckoSim._isTestingMode;
        var completion = new CompletableFuture<SimulationSession.Status>();
        var producer = new AtomicReference<Future<SimulationSession.Status>>();
        var submissions = new AtomicInteger();
        DesktopSimulationController controller = null;
        MainWindow ownedWindow = null;
        try {
            System.setProperty("gecko.backend", "core");
            GeckoSim._isTestingMode = true;
            if (GeckoSim._win == null || !GeckoSim._win.isDisplayable()) {
                GeckoSim.main(new String[0]);
                ownedWindow = GeckoSim._win;
            }
            GeckoExternal.openFile(Path.of("src/test/resources/ipes/education/buck_simple.ipes").toAbsolutePath().toString());
            var settings = MainWindow._solverSettings;
            var window = GeckoSim._win;
            var editor = MainWindow._se;
            controller = new DesktopSimulationController(window, editor,
                    () -> NetListContainer.fabricStartSimulation(editor, null), host -> {
                        Future<SimulationSession.Status> run = host.run();
                        if (submissions.incrementAndGet() != 1) return run;
                        producer.set(run);
                        return completion;
                    });

            assertTrue(controller.startAsync(settings));
            var terminal = producer.get().get(10, TimeUnit.SECONDS);
            assertEquals(SimulationSession.State.COMPLETED, terminal.state());
            var data = NetzlisteCONTROL.globalData;
            await(() -> data.getMaximumTimeIndex(0) + 1 == terminal.committedSteps());
            // The real EDT timer has drained the run. Use an EDT checkpoint after that tick.
            SwingUtilities.invokeAndWait(() -> assertEquals(ContainerStatus.RUNNING, data.getContainerStatus(),
                    "Terminal publication must wait for the outstanding command"));
            assertFalse(completion.isDone());
            assertTrue(controller.continueAsync(settings));
            assertEquals(1, submissions.get(), "A pending terminal publication still owns the run");

            completion.complete(terminal);
            await(() -> data.getContainerStatus() == ContainerStatus.PAUSED);
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(terminal.committedSteps(), data.getMaximumTimeIndex(0) + 1);
                assertEquals(terminal.time(), data.getTimeValue(terminal.committedSteps() - 1, 0), 1e-12);
                double step = terminal.stepWidth();
                for (int i = 0; i < terminal.committedSteps(); i++) {
                    assertEquals(i * step, data.getTimeValue(i, 0), 1e-12, "sample " + i);
                }
            });
            assertEquals(1, submissions.get(), "The premature continue must not be queued");
            assertTrue(controller.continueAsync(settings));
            assertEquals(2, submissions.get(), "Continue is available after terminal publication");
            await(() -> data.getMaximumTimeIndex(0) >= terminal.committedSteps()
                    && data.getContainerStatus() == ContainerStatus.PAUSED);
        } finally {
            try {
                Future<SimulationSession.Status> submitted = producer.get();
                if (submitted != null && !completion.isDone()) {
                    try {
                        completion.complete(submitted.get(5, TimeUnit.SECONDS));
                    } catch (Exception failure) {
                        completion.completeExceptionally(failure);
                    }
                }
                if (controller != null) controller.reset();
                var windowToClose = ownedWindow;
                if (windowToClose != null) SwingUtilities.invokeAndWait(windowToClose::dispose);
            } finally {
                GeckoSim._isTestingMode = previousTestingMode;
                if (previousBackend == null) System.clearProperty("gecko.backend");
                else System.setProperty("gecko.backend", previousBackend);
            }
        }
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean(), "Timed out waiting for desktop timer");
    }
}

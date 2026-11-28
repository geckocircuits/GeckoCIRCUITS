package gecko.geckocircuits.control;

import gecko.geckocircuits.datacontainer.AbstractDataContainer;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import gecko.geckocircuits.datacontainer.DataIndexItem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataSaverCancellationTest {
    @Test
    void cancellationReleasesTheEndExportClaimWithoutCreatingACompletedExport() throws Exception {
        var data = data(new AtomicReference<>(ContainerStatus.RUNNING));
        var settings = settings(ReglerSaveData.SaveModus.SIMULATION_END);
        int before = DataSaver.WAIT_COUNTER.get();
        var saver = new DataSaver(data, settings);
        saver.update(data, null);
        assertEquals(before + 1, DataSaver.WAIT_COUNTER.get());

        saver.cancelSimulationExport();
        saver.cancelSimulationExport();
        saver.update(data, null);

        assertEquals(before, DataSaver.WAIT_COUNTER.get());
        verify(data).deleteObserver(saver);
        assertFalse(Files.exists(Path.of(settings._file.getValue())));
        assertEquals(0, saver.getPercentage());
    }

    @Test
    void cancellationStopsTheDuringExporterWithoutItsFinalSave() throws Exception {
        verifyDuringExport(true);
    }

    @Test
    void successfulDuringExportStillWritesEverySample() throws Exception {
        verifyDuringExport(false);
    }

    private static void verifyDuringExport(boolean cancel) throws Exception {
        var status = new AtomicReference<>(ContainerStatus.RUNNING);
        var data = data(status);
        var settings = settings(ReglerSaveData.SaveModus.DURING_SIMULATION);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var worker = new AtomicReference<Thread>();
        when(data.getValue(eq(0), anyInt())).thenAnswer(call -> {
            int sample = call.getArgument(1, Integer.class);
            if (sample == 1) {
                worker.set(Thread.currentThread());
                entered.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
            }
            return (float) sample;
        });
        var saver = new DataSaver(data, settings);
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            status.set(cancel ? ContainerStatus.DELETED : ContainerStatus.PAUSED);
            if (cancel) saver.cancelSimulationExport();
        } finally { release.countDown(); }
        worker.get().join(5000);
        assertFalse(worker.get().isAlive());
        var lines = Files.readAllLines(Path.of(settings._file.getValue()));
        assertEquals(cancel ? 2 : 3, lines.size());
        if (cancel) assertTrue(saver.getPercentage() < 100);
        else assertEquals(100, saver.getPercentage());
    }

    private static ReglerSaveData settings(ReglerSaveData.SaveModus mode) throws Exception {
        var settings = new ReglerSaveData();
        settings._saveModus = mode;
        settings._printHeader.setValueWithoutUndo(false);
        settings._file.setValueWithoutUndo(Files.createTempDirectory(Path.of("target"), "B1-export-")
                .resolve("samples.txt").toString());
        settings.setSelectedSignals(new DataIndexItem[]{new DataIndexItem(0, "signal")});
        return settings;
    }

    private static AbstractDataContainer data(AtomicReference<ContainerStatus> status) {
        var data = mock(AbstractDataContainer.class);
        when(data.getContainerStatus()).thenAnswer(call -> status.get());
        when(data.getRowLength()).thenReturn(1);
        when(data.getSignalName(0)).thenReturn("signal");
        when(data.getMaximumTimeIndex(0)).thenReturn(2);
        when(data.getTimeValue(anyInt(), eq(0))).thenAnswer(call -> call.getArgument(0, Integer.class).doubleValue());
        when(data.getValue(eq(0), anyInt())).thenAnswer(call -> call.getArgument(1, Integer.class).floatValue());
        return data;
    }
}

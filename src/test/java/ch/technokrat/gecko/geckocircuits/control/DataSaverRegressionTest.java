package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataIndexItem;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataSaverRegressionTest {

    private AbstractDataContainer _data;
    private ReglerSaveData _settings;
    private Path _tempDir;
    private Path _exportFile;

    @Before
    public void setUp() throws IOException {
        _data = mock(AbstractDataContainer.class);
        _settings = new ReglerSaveData();
        _settings._saveModus = ReglerSaveData.SaveModus.MANUAL;
        _settings._outputType = ReglerSaveData.OutputType.TEXT;
        _settings._printHeader.setValueWithoutUndo(false);
        _settings._transposeData.setValueWithoutUndo(false);
        _settings._skipDataPoints.setValueWithoutUndo(1);

        _tempDir = Files.createTempDirectory("datasaver_regression_");
        _exportFile = _tempDir.resolve("export.txt");
        _settings._file.setValueWithoutUndo(_exportFile.toString());

        when(_data.getContainerStatus()).thenReturn(ContainerStatus.PAUSED);
        when(_data.getXDataName()).thenReturn("time");
    }

    @After
    public void tearDown() throws IOException {
        if (_tempDir != null && Files.exists(_tempDir)) {
            Files.walk(_tempDir)
                .sorted((left, right) -> right.compareTo(left))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException ignored) {
                    }
                });
        }
    }

    @Test
    public void testTransposedTextExportIncludesLastSample() throws Exception {
        _settings._transposeData.setValueWithoutUndo(true);
        _settings.setSelectedSignals(new DataIndexItem[]{new DataIndexItem(0, "sigA")});

        when(_data.getRowLength()).thenReturn(1);
        when(_data.getSignalName(0)).thenReturn("sigA");
        when(_data.getMaximumTimeIndex(0)).thenReturn(2);
        when(_data.getTimeValue(anyInt(), eq(0))).thenAnswer(invocation -> invocation.getArgument(0, Integer.class).doubleValue());
        when(_data.getValue(eq(0), anyInt())).thenAnswer(invocation -> ((Integer) invocation.getArgument(1)) * 10.0f);

        DataSaver saver = new DataSaver(_data, _settings);
        saver.doManualSaveBlocking();

        List<String> lines = Files.readAllLines(_exportFile, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertArrayEquals(new String[]{"0.0", "1.0", "2.0"}, lines.get(0).split(" "));
        assertArrayEquals(new String[]{"0", "10", "20"}, lines.get(1).split(" "));
    }

    @Test
    public void testRepeatedManualSaveWritesCompleteFileFromStart() throws Exception {
        _settings.setSelectedSignals(new DataIndexItem[]{new DataIndexItem(0, "sigA")});

        when(_data.getRowLength()).thenReturn(1);
        when(_data.getSignalName(0)).thenReturn("sigA");
        when(_data.getMaximumTimeIndex(0)).thenReturn(2, 4);
        when(_data.getTimeValue(anyInt(), eq(0))).thenAnswer(invocation -> invocation.getArgument(0, Integer.class).doubleValue());
        when(_data.getValue(eq(0), anyInt())).thenAnswer(invocation -> ((Integer) invocation.getArgument(1)).floatValue());

        DataSaver saver = new DataSaver(_data, _settings);
        saver.doManualSaveBlocking();
        List<String> firstSaveLines = Files.readAllLines(_exportFile, StandardCharsets.UTF_8);
        assertEquals(3, firstSaveLines.size());

        saver.doManualSaveBlocking();
        List<String> secondSaveLines = Files.readAllLines(_exportFile, StandardCharsets.UTF_8);
        assertEquals(5, secondSaveLines.size());
        assertEquals("0.0 0", secondSaveLines.get(0));
        assertEquals("4.0 4", secondSaveLines.get(4));
    }

    @Test
    public void testMismatchedNamesAndIndicesAreCorrectedWithoutCrash() throws Exception {
        _settings.setSelectedSignals(new DataIndexItem[]{new DataIndexItem(0, "sigA")});
        appendSelectedName("sigB");

        when(_data.getRowLength()).thenReturn(2);
        when(_data.getSignalName(0)).thenReturn("sigA");
        when(_data.getSignalName(1)).thenReturn("sigB");
        when(_data.getMaximumTimeIndex(0)).thenReturn(0);
        when(_data.getTimeValue(anyInt(), eq(0))).thenReturn(0.0);
        when(_data.getValue(eq(0), anyInt())).thenReturn(1.0f);
        when(_data.getValue(eq(1), anyInt())).thenReturn(2.0f);

        DataSaver saver = new DataSaver(_data, _settings);
        saver.doManualSaveBlocking();

        assertEquals(Arrays.asList(0, 1), _settings.getSelectedSignalIndices());

        List<String> lines = Files.readAllLines(_exportFile, StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        assertArrayEquals(new String[]{"0.0", "1", "2"}, lines.get(0).split(" "));
    }

    @SuppressWarnings("unchecked")
    private void appendSelectedName(String signalName) throws Exception {
        Field namesField = ReglerSaveData.class.getDeclaredField("_selectedSignalNames");
        namesField.setAccessible(true);
        List<String> names = (List<String>) namesField.get(_settings);
        names.add(signalName);
    }
}

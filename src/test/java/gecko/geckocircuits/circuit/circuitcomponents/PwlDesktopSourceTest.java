package gecko.geckocircuits.circuit.circuitcomponents;

import gecko.core.allg.GeckoFile;
import gecko.core.circuit.CircuitSourceType;
import gecko.core.circuit.TokenMap;
import gecko.core.circuit.waveform.PiecewiseLinearWaveform;
import gecko.geckocircuits.allg.GeckoFileManager;
import gecko.geckocircuits.allg.MainWindow;
import gecko.geckocircuits.allg.OptimizerParameterData;
import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.circuit.CircuitSheet;
import gecko.geckocircuits.circuit.EditorCircuitCapture;
import gecko.geckocircuits.circuit.SolverSettings;
import gecko.geckocircuits.control.ControlGlobalTerminal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PwlDesktopSourceTest {
    private static final double US = 1e-6;
    private static final PiecewiseLinearWaveform WAVEFORM =
            new PiecewiseLinearWaveform(new double[]{0, 4 * US, 8 * US}, new double[]{-2, 3, -1});

    private GeckoFileManager originalFiles;

    @BeforeEach
    void selectDocumentFileManager() {
        originalFiles = MainWindow._fileManager;
        MainWindow._fileManager = new GeckoFileManager();
    }

    @AfterEach
    void restoreDocumentFileManager() {
        MainWindow._fileManager = originalFiles;
    }

    @Test
    void exportAndReopenPreserveThePhysicalTimeAndSourceMode() throws Exception {
        for (CircuitTyp type : List.of(CircuitTyp.LK_U, CircuitTyp.LK_I)) {
            AbstractCircuitSource source = createSource(type);
            GeckoFile embedded = source.getFiles().getFirst();
            var saved = new StringBuffer();
            source.exportASCII(saved);
            assertTrue(saved.toString().contains("\nsourceType 403\n"));
            assertTrue(saved.toString().contains("\npwlResourceId " + embedded.getHashValue() + "\n"));

            var reopened = (AbstractCircuitSource) AbstractTypeInfo.fabricFromFile(type,
                    new TokenMap(saved.toString().lines().toArray(String[]::new)));
            GeckoFile loaded = readEmbedded(embedded, reopened, null);
            MainWindow._fileManager = new GeckoFileManager(List.of(loaded));
            reopened.initExtraFiles();
            assertTrue(reopened.isPwlSource());
            assertTrue(loaded.isSoleNumericOwner(reopened.getUniqueObjectIdentifier()));
            assertArrayEquals(WAVEFORM.toBytes(), reopened.requirePwlWaveform().toBytes());
            assertSamples(reopened);
            assertNotNull(new EditorCircuitCapture().capture(List.of(reopened), new SolverSettings(),
                    new OptimizerParameterData(), MainWindow._fileManager,
                    EditorCircuitCapture.ScriptSources.EMPTY, "pwl.ipes"));
            MainWindow._fileManager = new GeckoFileManager();
        }
    }

    @Test
    void rejectsLegacyLabelsAndDuplicateResourcesBeforeCapture() throws Exception {
        AbstractCircuitSource source = createSource(CircuitTyp.LK_U);
        GeckoFile original = source.getFiles().getFirst();
        GeckoFile legacy = readEmbedded(original, source,
                "<usageList>\n" + source.getStringID() + "\n<\\usageList>");
        assertEquals(List.of(source.getUniqueObjectIdentifier()), legacy.getOwnerIds());
        assertFalse(legacy.isSoleNumericOwner(source.getUniqueObjectIdentifier()));
        GeckoFile duplicateOwner = readEmbedded(original, source,
                "usageList[] /" + source.getUniqueObjectIdentifier() + "/" + source.getUniqueObjectIdentifier());
        for (List<GeckoFile> resources : List.of(List.of(legacy), List.of(duplicateOwner), List.of(original, legacy))) {
            var manager = new GeckoFileManager(resources);
            assertThrows(IllegalArgumentException.class, () -> new EditorCircuitCapture().capture(
                    List.of(source), new SolverSettings(), new OptimizerParameterData(), manager,
                    EditorCircuitCapture.ScriptSources.EMPTY, "pwl.ipes"));
        }
    }

    @Test
    void rejectsACrossDomainOwnerCollisionBeforeCapture() {
        var source = createSource(CircuitTyp.LK_U);
        var terminal = new ControlGlobalTerminal();
        terminal.getIdentifier().createNewIdentifier(source.getUniqueObjectIdentifier());
        var failure = assertThrows(IllegalArgumentException.class, () -> new EditorCircuitCapture().capture(
                List.of(source, terminal), new SolverSettings(), new OptimizerParameterData(),
                MainWindow._fileManager, EditorCircuitCapture.ScriptSources.EMPTY, "pwl.ipes"));
        assertTrue(failure.getMessage().contains("unique numeric owner"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"isExternal 1", "", "isExternal 2", "isExternal 0\nisExternal 1"})
    void rejectsRawPwlStorageBeforeResolvingTheSavedPath(String storage) {
        var source = createSource(CircuitTyp.LK_U);
        var resource = source.getFiles().getFirst();
        var serialized = new StringBuffer();
        resource.exportASCII(serialized);
        String body = serialized.toString().replace("isExternal 0", storage);
        var tokens = new TokenMap(body.lines().toArray(String[]::new));
        var failure = assertThrows(IllegalArgumentException.class, () ->
                new GeckoFile(tokens, List.of(source), MainWindow.getOpenFileName(), null, true));
        assertTrue(failure.getMessage().contains("Declare the PWL resource internal"));
    }

    @Test
    void copyOwnsItsDataAndDeletionDetachesItUntilUndo() {
        AbstractCircuitSource source = createSource(CircuitTyp.LK_U);
        var sheet = new CircuitSheet(null);
        source.setParentCircuitSheet(sheet);
        var copy = (AbstractCircuitSource) source.copyFabric(100);
        copy.setParentCircuitSheet(sheet);
        GeckoFile copyFile = copy.getFiles().getFirst();
        assertNotEquals(source.getFiles().getFirst().getHashValue(), copyFile.getHashValue());
        assertTrue(copyFile.isSoleNumericOwner(copy.getUniqueObjectIdentifier()));

        copy.deleteComponent();
        assertFalse(MainWindow._fileManager._allAvailableFiles.contains(copyFile));
        copy.setParentCircuitSheet(sheet);
        assertSame(copyFile, copy.getFiles().getFirst());
        assertArrayEquals(WAVEFORM.toBytes(), copy.requirePwlWaveform().toBytes());

        copy.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_DC);
        var dc = (VoltageSourceCalculator) copy.getCircuitCalculatorsForSimulationStart().getFirst();
        dc.setZValue(1);
        double[] rhs = new double[2];
        dc.stampVectorB(rhs, 2 * US, US);
        assertEquals(12, rhs[1]);
        copy.sourceType.setValueWithoutUndo(CircuitSourceType.PWL);
        copy.setPwlWaveform(new PiecewiseLinearWaveform(new double[]{0}, new double[]{7}));
        assertEquals(7, copy.requirePwlWaveform().calculate(0, US));
        assertEquals(-2, source.requirePwlWaveform().calculate(0, US));
    }

    private static AbstractCircuitSource createSource(CircuitTyp type) {
        var source = (AbstractCircuitSource) type.getTypeInfo().fabric();
        source.getIdentifier().createNewIdentifier(49);
        source.setPwlWaveform(WAVEFORM);
        source.sourceType.setValueWithoutUndo(CircuitSourceType.PWL);
        return source;
    }

    private static GeckoFile readEmbedded(GeckoFile file, AbstractCircuitSource source, String owners)
            throws Exception {
        var serialized = new StringBuffer();
        file.exportASCII(serialized);
        String body = serialized.toString();
        if (owners != null) {
            int start = body.indexOf("usageList[]");
            int end = body.indexOf('\n', start);
            body = body.substring(0, start) + owners + body.substring(end);
        }
        return new GeckoFile(new TokenMap(body.lines().toArray(String[]::new)), List.of(source),
                MainWindow.getOpenFileName(), null, owners == null);
    }

    private static void assertSamples(AbstractCircuitSource source) {
        double[] times = {0, 2 * US, 4 * US, 6 * US, 8 * US, 11 * US};
        double[] expected = {-2, 0.5, 3, 1, -1, -1};
        var calculator = source.getCircuitCalculatorsForSimulationStart().getFirst();
        for (int i = 0; i < times.length; i++) {
            double[] rhs = new double[2];
            if (calculator instanceof VoltageSourceCalculator voltage) {
                voltage.setZValue(1);
                voltage.stampVectorB(rhs, times[i], US);
                assertEquals(expected[i], rhs[1], 1e-12);
            } else {
                var current = (CurrentSourceCalculator) calculator;
                current.setMatrixIndices(0, 1);
                current.stampVectorB(rhs, times[i], US);
                assertArrayEquals(new double[]{-expected[i], expected[i]}, rhs, 1e-12);
                assertFalse(current.isBasisStampable());
            }
        }
    }
}

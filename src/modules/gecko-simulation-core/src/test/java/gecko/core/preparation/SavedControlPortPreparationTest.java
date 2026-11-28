package gecko.core.preparation;

import gecko.core.simulation.HeadlessSimulationEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static gecko.core.preparation.SourcePreparationTest.element;
import static gecko.core.preparation.SourcePreparationTest.parse;
import static org.junit.jupiter.api.Assertions.*;

class SavedControlPortPreparationTest {
    private static String electricalCircuit() {
        return element("LK", 4, "source", "401 6", "/1", "/0", "")
                + element("LK", 1, "R", "10", "/1", "/0", "");
    }

    @ParameterizedTest
    @CsvSource(value = {"3,NIX_NIX_NIX", "7,NIX_NIX_NIX", "3,''", "7,''"})
    void savedBlankPortsCannotMoveAConnectionIntoTheFirstPort(int type, String placeholder) throws Exception {
        String labels = "/" + placeholder + "/drive";
        String blocks = type == 3
                ? element("CONTROL", 3, "bad", "2", "/NIX_NIX_NIX", labels, "")
                    + element("CONTROL", 5, "scope", "", "/drive", "/NIX_NIX_NIX", "")
                : element("CONTROL", 3, "constant", "2", "/NIX_NIX_NIX", "/drive", "")
                    + element("CONTROL", 7, "bad", "3", labels, "/amplified", "")
                    + element("CONTROL", 5, "scope", "", "/amplified", "/NIX_NIX_NIX", "");
        var model = parse(electricalCircuit() + blocks);
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        var result = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        assertEquals(ReadinessReport.Outcome.INVALID, result.report().outcome());
        assertTrue(result.report().diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_CONTROL_PORT")
                && "CONTROL".equals(d.domain()) && "bad".equals(d.component())), result.report().toString());
        var saved = snapshot.toModel().getControlComponents().stream().filter(c -> c.getName().equals("bad")).findFirst().orElseThrow();
        assertArrayEquals(new String[]{"", "drive"}, type == 3 ? saved.getTerminalYLabels() : saved.getTerminalXLabels());
        assertEquals(result.report(), preparation.prepare(snapshot, PreparationRequest.defaults()).report());
    }

    @Test
    void unusedSavedPortsAndScopePlaceholdersKeepTheirRecordedPortPositions() throws Exception {
        var model = parse(electricalCircuit()
                + element("CONTROL", 3, "constant", "2", "/NIX_NIX_NIX", "/drive/NIX_NIX_NIX", "")
                + element("CONTROL", 7, "gain", "3", "/drive/NIX_NIX_NIX", "/amplified/NIX_NIX_NIX", "")
                + element("CONTROL", 5, "scope", "", "/NIX_NIX_NIX/amplified/NIX_NIX_NIX/drive/NIX_NIX_NIX", "/NIX_NIX_NIX", ""));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(new String[]{"", "amplified", "", "drive", ""}, result.getSignalNames());
        assertArrayEquals(new float[]{0, 0, 0}, result.getSignalData(0));
        assertArrayEquals(new float[]{6, 6, 6}, result.getSignalData(1));
        assertArrayEquals(new float[]{0, 0, 0}, result.getSignalData(2));
        assertArrayEquals(new float[]{2, 2, 2}, result.getSignalData(3));
        assertArrayEquals(new float[]{0, 0, 0}, result.getSignalData(4));
    }

    @Test
    void savedScopeDuplicatesAndBlankPortKeepThreeChannelsAndTwoRecordedRows() throws Exception {
        var model = parse(element("CONTROL", 3, "constant", "2", "", "/drive", "")
                + element("CONTROL", 5, "scope", "", "/drive/NIX_NIX_NIX/drive", "", ""));
        assertArrayEquals(new String[]{"drive", "", "drive"}, model.getControlComponents().get(1).getTerminalXLabels());
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(new String[]{"drive", ""}, result.getSignalNames());
        assertArrayEquals(new float[]{2, 2, 2}, result.getSignalData(0));
        assertArrayEquals(new float[]{0, 0, 0}, result.getSignalData(1));
        @SuppressWarnings("unchecked")
        var channels = (java.util.List<gecko.core.simulation.ScopeDataExtractor.ScopeChannel>) result.getMetadata().get("scopeChannels");
        assertEquals(java.util.List.of(0, 1, 2), channels.stream()
                .map(gecko.core.simulation.ScopeDataExtractor.ScopeChannel::channelIndex).toList());
        assertEquals(java.util.List.of(0, 1, 0), channels.stream()
                .map(gecko.core.simulation.ScopeDataExtractor.ScopeChannel::row).toList());
    }

    @Test
    void uploadedImportCannotReadHostProvenanceButCanExecuteSuppliedBytes() throws Exception {
        var directory = java.nio.file.Files.createTempDirectory(java.nio.file.Path.of("target"), "control-resource-review-").toAbsolutePath();
        var hostFile = directory.resolve("wave.txt");
        java.nio.file.Files.writeString(hostFile, "0 2\n1 4\n");
        String text = "dt 0.125\ntDURATION 0.25\n"
                + element("CONTROL", 4, "signal", "406 1 1 0 0 0.5", "", "/wave",
                        "uniqueObjectIdentifier 123\nexternalDataFileHashValue 42")
                + element("CONTROL", 5, "scope", "", "/wave", "", "")
                + "GeckoFileManager\n<GeckoFileManager>\n<GeckoFile>\nhashValue 42\nisExternal 1\nabsPath " + hostFile
                + "\nusageList[] 123\n<\\GeckoFile>\n<\\GeckoFileManager>\n";
        var model = new gecko.core.io.CircuitFileParser().parse(
                new java.io.ByteArrayInputStream(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                directory.resolve("uploaded.ipes").toString());
        assertFalse(model.getResources().containsKey(42L));
        assertTrue(model.getResourceFailures().containsKey(42L));
        var preparation = new CircuitPreparation();
        var rejected = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        assertTrue(rejected.report().diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_PARAMETER")
                && "signal".equals(d.component())));
        model.getResources().put(42L, new gecko.core.io.CircuitResource(42, "wave.txt", hostFile.toString(),
                "0 2\n1 4\n".getBytes(java.nio.charset.StandardCharsets.UTF_8), java.util.List.of(123L)));
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(new float[]{2, 2.25f, 2.5f}, result.getSignalData(0));
    }
}

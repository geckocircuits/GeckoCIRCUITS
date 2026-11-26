package gecko.core.io;

import gecko.core.circuit.waveform.PiecewiseLinearWaveform;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CircuitResourceReaderPwlTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "isExternal 1\nfileContents[] 80 87 76 49",
        "isExternal 1\nfileContents[] null",
        "fileContents[] 80 87 76 49",
        "isExternal 2\nfileContents[] 80 87 76 49",
        "isExternal 0\nisExternal 1\nfileContents[] 80 87 76 49",
        "isExternal 0\nisExternal 0\nfileContents[] 80 87 76 49",
        "isExternal 0\nfileContents[] null",
        "isExternal 0\nfileContents[] 0"
    })
    void rejectsInvalidPwlDataWithoutOpeningAnExternalResource(String storage) {
        assertNoReads(sourceModel("42", ""), block("hashValue 42\nusageList[] /100\n" + storage));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "hashValue 99\nhashValue 42", "hashValue 42\nhashValue 99", "hashValue 42\nhashValue 42",
        "hashValue +42", "hashValue 042", "hashValue 42 junk", "hashValue 99"
    })
    void checksAllHashRowsAndMissingTargetsBeforeOrdinaryCapture(String hashes) {
        assertNoReads(sourceModel("42", ""), block(hashes + "\nisExternal 1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "fileContents[] null", "fileContents[] @payload", "fileContents null", "<fileContents>\n0\n<\\fileContents>",
        "usageList[] /100", "usageList[] /101", "usageList /101", "<usageList>\nV1\n<\\usageList>"
    })
    void rejectsCompetingDeclarationsAfterAValidFirstValue(String conflict) {
        assertNoReads(sourceModel("42", ""), block(validRecord(42) + "\n" + conflict.replace("@payload", payload())));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "+42", "042", "0", "-0", "42 extra", "42\npwlResourceId 42", "42\npwlResourceId -42"
    })
    void rejectsMalformedSourcePointersBeforeOrdinaryCapture(String pointer) {
        assertNoReads(sourceModel(pointer, ""), block(validRecord(42)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "uniqueObjectIdentifier 100", "uniqueObjectIdentifier 101", "sourceType 403", "sourceType 0",
        "typ 4", "typ 1"
    })
    void rejectsDuplicateSavedSourceSelectors(String selector) throws Exception {
        String text = "\ne\n<ElementLK>\n" + sourceRows("42", selector) + "\n<\\ElementLK>\n"
                + manager(block(validRecord(42)));
        assertThrows(CircuitFileParser.CircuitParseException.class, () ->
                new CircuitFileParser().parse(new BufferedReader(new StringReader(text)), "pwl.ipes"));
    }

    @Test
    void acceptsCanonicalPositiveAndNegativeIdsAlongsideAnOrdinaryExternalResource() throws Exception {
        for (long id : new long[]{42, -42}) {
            String text = "\ne\n<ElementLK>\n" + sourceRows(Long.toString(id), "") + "\n<\\ElementLK>\n"
                    + manager(block(validRecord(id)));
            var reopened = new CircuitFileParser().parse(new BufferedReader(new StringReader(text)), "pwl.ipes");
            assertArrayEquals(waveform().toBytes(), reopened.getResources().get(id).bytes());
        }

        var model = sourceModel("-42", "");
        String[] lines = manager(block("hashValue 99\nisExternal 1") + block(validRecord(-42))).lines().toArray(String[]::new);
        var calls = new AtomicInteger();
        CircuitResourceReader.retain(model, lines, lines, CircuitFileParser.ResourceAccess.LOCAL_FILES,
                (circuit, block) -> {
                    assertEquals("99", block.tokens().get("hashValue"));
                    calls.incrementAndGet();
                    return new byte[]{7};
                });
        assertEquals(1, calls.get());
        assertArrayEquals(new byte[]{7}, model.getResources().get(99L).bytes());
        assertArrayEquals(waveform().toBytes(), model.getResources().get(-42L).bytes());
    }

    @Test
    void rejectsDuplicateBlocksRepeatedOwnersAndMissingTargetsBeforeAnyRead() {
        for (String records : new String[]{"", block(validRecord(42)) + block(validRecord(42)),
                block(validRecord(42).replace("/100", "/100/100"))}) {
            assertNoReads(sourceModel("42", ""), records);
        }
    }

    private static void assertNoReads(CircuitModel model, String records) {
        String[] lines = manager(block("hashValue 199\nisExternal 1") + records).lines().toArray(String[]::new);
        assertThrows(IllegalArgumentException.class, () -> CircuitResourceReader.retain(model, lines, lines,
                CircuitFileParser.ResourceAccess.LOCAL_FILES,
                (circuit, block) -> fail("PWL preflight must finish before any external resource is read.")));
        assertTrue(model.getResources().isEmpty());
    }

    private static CircuitModel sourceModel(String pointer, String extra) {
        var model = new CircuitModel();
        var source = new CircuitModel.ComponentData(4, "V1");
        source.setSavedBlock(SavedCircuitBlock.parse("ElementLK", sourceRows(pointer, extra).lines().toArray(String[]::new)));
        model.addCircuitComponent(source);
        return model;
    }

    private static String sourceRows(String pointer, String extra) {
        return "typ 4\nsourceType 403\nuniqueObjectIdentifier 100\nparameter[] 403 -2\n"
                + PiecewiseLinearWaveform.RESOURCE_ID_TOKEN + " " + pointer + "\n" + extra;
    }

    private static String manager(String records) {
        return "<GeckoFileManager>\n" + records + "<\\GeckoFileManager>\n";
    }

    private static String block(String rows) {
        return "<GeckoFile>\nabsPath unused.bin\n" + rows + "\n<\\GeckoFile>\n";
    }

    private static String validRecord(long id) {
        return "hashValue " + id + "\nisExternal 0\nusageList[] /100\nfileContents[] " + payload();
    }

    private static PiecewiseLinearWaveform waveform() {
        return new PiecewiseLinearWaveform(new double[]{0, 1e-6}, new double[]{-2, 3});
    }

    private static String payload() {
        var text = new StringBuilder();
        for (byte value : waveform().toBytes()) text.append(value).append(' ');
        return text.toString().strip();
    }
}

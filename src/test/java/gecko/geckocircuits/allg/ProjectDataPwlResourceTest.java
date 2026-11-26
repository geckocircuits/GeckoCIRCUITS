package gecko.geckocircuits.allg;

import gecko.core.circuit.TokenMap;
import gecko.core.circuit.waveform.PiecewiseLinearWaveform;
import gecko.core.io.SavedPwlResources;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ProjectDataPwlResourceTest {
    private static final SavedPwlResources.Source SOURCE = new SavedPwlResources.Source(42, 100);
    private static final byte[] WAVEFORM = new PiecewiseLinearWaveform(
            new double[]{0, 1e-6}, new double[]{-2, 3}).toBytes();
    private static final String PWL = "hashValue 42\nisExternal 0\nusageList[] /100\nfileContents[] " + payload();

    @ParameterizedTest
    @ValueSource(strings = {
        "hashValue 99\nhashValue 42\nisExternal 1",
        "hashValue 42\nhashValue 99\nisExternal 1",
        "hashValue 99\nisExternal 1",
        "{pwl}\nhashValue 42",
        "{pwl}\nisExternal 0",
        "{pwl}\nfileContents[] null",
        "{pwl}\nusageList[] /100",
        "{pwl}\n<usageList>\nOtherSource\n<\\usageList>"
    })
    void rejectsMalformedPwlBeforeReadingAnyResource(String rows) {
        var blocks = List.of(ordinaryExternalFile(), tokens(rows.replace("{pwl}", PWL)));

        assertThrows(IllegalArgumentException.class, () -> ProjectData.readSavedFiles(List.of(SOURCE),
                blocks, blocks.size(), (file, embedded) -> fail("Resource loader called before PWL validation.")));
    }

    @Test
    void validEmbeddedPwlLeavesOrdinaryExternalLoadingAvailable() {
        var ordinary = ordinaryExternalFile();
        var pwl = tokens(PWL);
        var loaded = new ArrayList<TokenMap>();

        ProjectData.readSavedFiles(List.of(SOURCE), List.of(ordinary, pwl), 2, (file, embedded) -> {
            loaded.add(file);
            if (file == ordinary) {
                assertNull(embedded);
            } else {
                assertNotNull(embedded);
                assertEquals(SOURCE.resourceId(), embedded.resourceId());
                assertEquals(SOURCE.ownerId(), embedded.ownerId());
                assertArrayEquals(WAVEFORM, embedded.bytes());
            }
        });

        assertEquals(List.of(ordinary, pwl), loaded);
    }

    private static TokenMap ordinaryExternalFile() {
        return tokens("hashValue 199\nabsPath ordinary.bin\nisExternal 1");
    }

    private static TokenMap tokens(String rows) {
        return new TokenMap(rows.lines().toArray(String[]::new));
    }

    private static String payload() {
        var result = new StringBuilder();
        for (byte value : WAVEFORM) result.append(value).append(' ');
        return result.toString().stripTrailing();
    }
}

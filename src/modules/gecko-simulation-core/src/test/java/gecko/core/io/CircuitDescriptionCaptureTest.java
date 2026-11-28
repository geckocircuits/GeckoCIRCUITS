package gecko.core.io;

import gecko.core.preparation.CircuitPreparation;
import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CircuitDescriptionCaptureTest {
    @Test
    void disconnectedScopeChannelsRetainTheirSavedPortIndicesAndTrailingChannel() throws Exception {
        String source = """
                c
                <ElementCONTROL>
                typ 5
                idStringDialog scope
                labelAnfangsKnoten[] /signal/NIX_NIX_NIX/signal/
                <\\ElementCONTROL>
                """;
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(source)), "scope.ipes");
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        assertArrayEquals(new String[]{"signal", "", "signal", ""},
                snapshot.toModel().getControlComponents().getFirst().getTerminalXLabels());
        var recaptured = preparation.capture(snapshot.toModel());
        assertArrayEquals(new String[]{"signal", "", "signal", ""},
                recaptured.toModel().getControlComponents().getFirst().getTerminalXLabels());
    }

    @Test
    void preparedInputOwnsEmbeddedResourceBytesAndOwnerIdentity() throws Exception {
        String source = """
                GeckoFileManager
                <GeckoFileManager>
                <GeckoFile>
                hashValue 12345678901
                absPath /unavailable/control.java
                isExternal 0
                usageList[] 9100000001 9100000002
                fileContents[] 65 0 -1 10
                <\\GeckoFile>
                <\\GeckoFileManager>
                dt 0.0001
                tDURATION 0.01
                """;
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(source)), "unsaved.ipes");
        var snapshot = new CircuitPreparation().capture(model);
        model.getResources().clear();
        var resource = snapshot.toModel().getResources().get(12345678901L);
        assertNotNull(resource);
        assertArrayEquals(new byte[]{65, 0, -1, 10}, resource.bytes());
        assertEquals(java.util.List.of(9100000001L, 9100000002L), resource.ownerIds());
        resource.bytes()[0] = 90;
        assertArrayEquals(new byte[]{65, 0, -1, 10}, snapshot.toModel().getResources().get(12345678901L).bytes());
    }

    @Test
    void capturedDescriptionRetainsExtensionSourceHierarchyAndPortIdentity() throws Exception {
        String source = """
                c
                <ElementCONTROL>
                typ 89
                idStringDialog custom
                uniqueObjectIdentifier 9100000001
                parentSheetIdentifier 9000000001
                enabledShorted 1
                parameter[] 2 3
                labelAnfangsKnoten[] /first/NIX_NIX_NIX/third
                labelEndKnoten[] /out
                <sourceCode>
                // Preserve indentation and text that looks like a circuit block.
                  double value = 17;
                e
                <ElementLK>
                typ 999
                <\\ElementLK>
                <\\sourceCode>
                <\\ElementCONTROL>
                sp
                <ElementSPECIAL>
                typ 71
                idStringDialog child
                uniqueObjectIdentifier 9000000001
                parentSheetIdentifier 0
                enabledShorted 0
                <\\ElementSPECIAL>
                dt 0.0001
                tDURATION 0.01
                """;
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(source)), "unsaved.ipes");
        var snapshot = new CircuitPreparation().capture(model);
        model.getControlComponents().clear();
        var captured = snapshot.toModel();
        assertEquals(0, captured.getCircuitComponents().size(), "Source text must not create electrical elements");
        var block = captured.getControlComponents().getFirst().getSavedBlock();
        assertEquals(9100000001L, block.uniqueObjectIdentifier());
        assertEquals(9000000001L, block.parentSheetIdentifier());
        assertTrue(block.sections().get("sourceCode").contains("  double value = 17;"));
        assertEquals("/first/NIX_NIX_NIX/third", block.tokens().get("labelAnfangsKnoten[]"));
        assertEquals(0, captured.getSpecialComponents().getFirst().getSavedBlock().enabledState());
        assertEquals("0.0001", captured.getSavedSettings().get("dt"));
        assertThrows(UnsupportedOperationException.class, () -> block.tokens().put("typ", "1"));
    }
}
